/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lossless

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.Preferences
import com.jay.glossy.constants.LosslessFolderKey
import com.jay.glossy.constants.LosslessIndexKey
import com.jay.glossy.constants.PreferLosslessKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.abs

/**
 * The lossless files on the device, indexed by what they are rather than where
 * they sit.
 *
 * YouTube's own streams top out at roughly 256 kbps, so a song played from
 * Glossy is lossy by construction — the only lossless audio the app can ever
 * play is a file the listener already owns. This object is the bridge: it walks
 * a folder the listener picks, keeps the result as a small JSON index in
 * DataStore (no schema change, no migration), and answers one question for the
 * player — "is there a file that is this song?".
 *
 * The answer is only ever yes when the file's own tags agree with the stream's
 * title and artist, or when the titles agree and the lengths are within three
 * seconds of each other. Anything looser would silently replace a live version
 * with a studio one.
 */
object LosslessLibrary {
    private val json = Json { ignoreUnknownKeys = true }

    private val _tracks = MutableStateFlow<List<LosslessTrack>>(emptyList())

    /** Everything the folder scan found, for the settings screen to count. */
    val tracks: StateFlow<List<LosslessTrack>> = _tracks.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    /** Whether the listener has asked for the lossless copy to be preferred. */
    @Volatile
    var enabled: Boolean = false
        private set

    @Volatile
    private var byPair: Map<String, LosslessTrack> = emptyMap()

    @Volatile
    private var byTitle: Map<String, List<LosslessTrack>> = emptyMap()

    /** Reads the saved preference and index. Safe to call more than once. */
    suspend fun restore(context: Context) = restore(context, context.dataStore.data.first())

    /**
     * The same, from a preference snapshot the caller has already read.
     *
     * The player restores the index before it rebuilds its queue, and reading
     * the same store a second time there would only widen the window in which
     * the first songs are built without it.
     *
     * A folder grant can be revoked — or the card holding it unmounted —
     * between runs. The files are then unreadable whatever the index says, so
     * publishing it would hand the player URIs it can only fail on; the index
     * is reported as empty while the saved JSON stays for when the folder is
     * granted again.
     */
    suspend fun restore(context: Context, preferences: Preferences) {
        enabled = preferences[PreferLosslessKey] ?: false
        val folder = preferences[LosslessFolderKey]?.takeIf { it.isNotBlank() }
        val saved = preferences[LosslessIndexKey]?.takeIf { it.isNotBlank() }
        if (folder == null || saved == null || !hasFolderGrant(context, Uri.parse(folder))) {
            publish(emptyList())
            return
        }
        publish(runCatching { json.decodeFromString<List<LosslessTrack>>(saved) }.getOrDefault(emptyList()))
    }

    /** Whether the tree the index was built from can still be read. */
    private fun hasFolderGrant(context: Context, treeUri: Uri): Boolean {
        // Compared by tree id rather than by URI string: the stored URI and the
        // one the system reports may differ in escaping or a trailing slash.
        val treeId =
            runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return false
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission &&
                runCatching { DocumentsContract.getTreeDocumentId(permission.uri) }.getOrNull() == treeId
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /**
     * The file that stands in for a streamed song, or null when there is none.
     *
     * Called from the queue builder, so it stays allocation-light: two lookups
     * and, at worst, a scan of the handful of files sharing a title.
     */
    fun findFor(title: String, artists: List<String>, durationSeconds: Int): LosslessTrack? {
        if (!enabled || byPair.isEmpty()) return null
        val target = if (durationSeconds > 0) durationSeconds * 1000L else 0L
        byPair[LosslessKey.pair(title, artists.firstOrNull().orEmpty())]?.let { track ->
            if (isSameRecording(track, target)) return track
        }

        val candidates = byTitle[LosslessKey.normalise(title)] ?: return null
        // A stream whose length is unknown cannot be told from a live take or a
        // different edit, so only an unambiguous title match may stand in.
        if (target == 0L) return candidates.singleOrNull()
        return candidates
            .filter { isSameRecording(it, target) }
            .minByOrNull { abs(it.durationMs - target) }
    }

    /**
     * Whether a file is long enough like the stream to be the same recording.
     * A file that does not report its length cannot disagree, so it is allowed.
     */
    private fun isSameRecording(track: LosslessTrack, targetMs: Long): Boolean =
        targetMs == 0L ||
            track.durationMs <= 0L ||
            abs(track.durationMs - targetMs) <= DURATION_TOLERANCE_MS

    /**
     * Walks the picked folder and rebuilds the index.
     *
     * Runs entirely on IO: the walk is a stack of `DocumentsContract` child
     * queries, and each candidate file costs one `MediaMetadataRetriever`
     * open. Folders of a few thousand files are slow enough that the caller
     * should show [progress] — the count is updated as files are read.
     *
     * Returns how many files were indexed, or -1 when the folder could not be
     * read at all — in which case the previous index is left untouched, since
     * a folder that cannot be opened is not the same thing as an empty one.
     */
    suspend fun scan(context: Context, treeUri: Uri): Int =
        withContext(Dispatchers.IO) {
            _scanning.value = true
            _progress.value = 0
            try {
                val found = mutableListOf<LosslessTrack>()
                walk(context, treeUri) { uri, name ->
                    // The walk is thousands of blocking reads with no suspension
                    // in between; without this, leaving the screen would not
                    // stop it and the progress counter would keep ticking.
                    ensureActive()
                    read(context, uri, name)?.let {
                        found.add(it)
                        _progress.value = found.size
                    }
                }
                val sorted = found.distinctBy { it.uri }.sortedBy { it.title.lowercase(Locale.ROOT) }
                publish(sorted)
                context.safeDataStoreEdit { it[LosslessIndexKey] = json.encodeToString(sorted) }
                sorted.size
            } catch (c: CancellationException) {
                // Leaving the screen cancels the scan; that is not a failure and
                // must not be reported as one.
                throw c
            } catch (t: Throwable) {
                // A revoked folder grant or an unreadable card leaves the old
                // index in place rather than emptying it.
                UNREADABLE
            } finally {
                _scanning.value = false
            }
        }

    /** Drops the index; the folder grant itself is left alone. */
    suspend fun forget(context: Context) {
        publish(emptyList())
        context.safeDataStoreEdit { it[LosslessIndexKey] = "" }
    }

    private fun publish(list: List<LosslessTrack>) {
        _tracks.value = list
        byPair = list.associateBy { LosslessKey.pair(it.title, it.artist) }
        byTitle = list.groupBy { LosslessKey.normalise(it.title) }
    }

    private fun walk(context: Context, treeUri: Uri, onFile: (Uri, String) -> Unit) {
        val resolver = context.contentResolver
        val queue = ArrayDeque<String>().apply { add(DocumentsContract.getTreeDocumentId(treeUri)) }
        var isRoot = true
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            val children =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parent)
            // One unreadable subtree should not stop the rest of the walk, so
            // each parent's children are queried on their own. The root is the
            // exception: it is the grant itself, so if it cannot be read
            // nothing below it can either and the scan has failed rather than
            // found an empty folder.
            val cursor =
                try {
                    resolver.query(children, PROJECTION, null, null, null)
                } catch (t: Throwable) {
                    if (isRoot) throw t
                    null
                }
            isRoot = false
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0) ?: continue
                    val name = it.getString(1) ?: continue
                    val mime = it.getString(2).orEmpty()
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(id)
                    } else {
                        onFile(DocumentsContract.buildDocumentUriUsingTree(treeUri, id), name)
                    }
                }
            }
        }
    }

    private fun read(context: Context, uri: Uri, name: String): LosslessTrack? {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension !in EXTENSIONS) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            LosslessTrack(
                uri = uri.toString(),
                title =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?: name.substringBeforeLast('.'),
                artist =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() }
                        ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                            ?.takeIf { it.isNotBlank() }
                        ?: UNKNOWN_ARTIST,
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L,
                sampleRateHz = sampleRate(retriever),
                bitDepth = bitDepth(retriever),
                codec = CODECS[extension] ?: extension.uppercase(Locale.ROOT),
            )
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Both of these keys arrived in Android 12; older phones simply have no answer. */
    private fun sampleRate(retriever: MediaMetadataRetriever): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.toIntOrNull() ?: 0
        } else {
            0
        }

    private fun bitDepth(retriever: MediaMetadataRetriever): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                ?.toIntOrNull() ?: 0
        } else {
            0
        }

    private val PROJECTION =
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

    /**
     * What the lane will open. Both decode inside Media3's own extractors and
     * codecs, so playing one needs no new decoder and no native code — which is
     * also why AIFF and ALAC are absent: Android ships no extractor for the
     * first and no decoder for the second, and an index full of files that
     * cannot play is worse than a shorter one that can.
     */
    private val EXTENSIONS = setOf("flac", "wav", "wave")

    private val CODECS = mapOf("flac" to "FLAC", "wav" to "WAV", "wave" to "WAV")

    private const val UNKNOWN_ARTIST = "Unknown artist"

    /** How far two recordings of the same song may drift and still be the same. */
    private const val DURATION_TOLERANCE_MS = 3_000L

    /** [scan]'s answer when the folder itself could not be read. */
    private const val UNREADABLE = -1
}
