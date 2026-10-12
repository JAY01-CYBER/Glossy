/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Canvas preloading.
 *
 * A canvas is a network lookup (four providers raced) followed by a video
 * download, and both used to happen only once a card was already on screen.
 * That is what made a canvas arrive late: the lookup was cancelled by the same
 * swipe that made the card visible, and the download started after the swipe
 * had already landed.
 *
 * This owns the lookups instead of the screen that wants them:
 *
 *  - **It outlives the composable.** The scope is process-wide, so scrolling
 *    past a card, swiping away from it or flipping to the next track no longer
 *    cancels the work that card asked for. Previously the prefetch ran inside
 *    the card's own composition and died with it.
 *  - **It warms the video, not just the URL.** A canvas-sized clip is pulled
 *    into the very same [SimpleCache] the player reads from (same cache, same
 *    default key — the URI), so when the card does start it decodes from local
 *    disk and shows its first frame almost immediately instead of after a
 *    download — and, because the whole clip is there, the loop wraps around
 *    without ever going back to the network.
 *  - **It is polite.** Two lookups run at once at most, a song is looked up
 *    once per session, each URL is warmed once, and everything is skipped on
 *    mobile data unless the user allowed canvases there.
 */

package com.jay.glossy.ui.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.jay.glossy.constants.CanvasCacheMode
import com.jay.glossy.constants.CanvasCacheModeKey
import com.jay.glossy.utils.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

object CanvasPrefetcher {
    private const val TAG = "CanvasPrefetcher"

    /** Lookups that may run at once. Each one fans out to four providers. */
    private const val MaxConcurrentLookups = 2

    /** How many finished songs are remembered before the oldest is forgotten. */
    private const val HandledLimit = 96

    /** How many warmed URLs are remembered, so a clip is never fetched twice. */
    private const val WarmedUrlLimit = 48

    /**
     * How big a clip may be and still be warmed *whole*.
     *
     * Whole, never part: the cache entry is keyed by the URI playback opens, so
     * a partial entry is a lie about the clip's length — the player reads to the
     * end of what is stored, cannot continue, and gives up a couple of seconds
     * in, over and over. Warming only what fits keeps the entry truthful and
     * keeps the loop on disk. Anything larger than this is not a canvas anyway;
     * it streams, and playback caches it correctly by itself.
     */
    private const val WarmMaxClipBytes = 8L * 1024 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lookups = Semaphore(MaxConcurrentLookups)

    private val inFlight = HashSet<String>()
    private val handled = LinkedHashMap<String, Boolean>(HandledLimit, 0.75f, true)
    private val warmed = LinkedHashMap<String, Boolean>(WarmedUrlLimit, 0.75f, true)

    /**
     * Asks for a song's canvas to be ready before it is needed. Fire and forget:
     * the work runs on this object's own scope, so the caller may leave the
     * screen — or the track may change — without taking the prefetch with it.
     *
     * @param warmVideo also pull the clip's opening bytes into the player cache.
     * @param allowMetered whether this may happen on mobile data. Off means
     *   Wi-Fi only, decided by [isMobileDataConnection].
     */
    fun request(
        context: Context,
        mediaId: String,
        songTitle: String,
        artistName: String,
        albumName: String,
        warmVideo: Boolean = true,
        allowMetered: Boolean = true,
    ) {
        if (mediaId.isBlank() || songTitle.isBlank() || artistName.isBlank()) return

        synchronized(this) {
            if (handled.containsKey(mediaId)) return
            // Already being looked up: the second ask is the same ask.
            if (!inFlight.add(mediaId)) return
        }

        val appContext = context.applicationContext
        scope.launch {
            try {
                lookups.withPermit {
                    // Wi-Fi only, and Wi-Fi is asked about by transport: a
                    // network that happens to be flagged metered, or one that
                    // is not up yet, is not "mobile data".
                    if (!allowMetered && appContext.isMobileDataConnection()) return@withPermit

                    val artwork = CanvasArtworkPlaybackCache.get(mediaId)
                        ?: CanvasResolver.resolve(
                            context = appContext,
                            mediaId = mediaId,
                            songTitle = songTitle,
                            artistName = artistName,
                            albumName = albumName,
                        )

                    val url = artwork?.preferredAnimationUrl?.takeIf(String::isNotBlank)
                    if (url == null) {
                        // No canvas for this song right now. Not remembered as
                        // handled: a miss is often a provider hiccup, and the
                        // card that asks again is cheap.
                        return@withPermit
                    }

                    // The canvas check's other half, and the reason it costs the
                    // screen nothing: whether the URL actually serves a video is
                    // learned here, in the background, before anything is
                    // waiting on it. The verdict is remembered, so the next
                    // lookup — a swipe back to the card, the same track a second
                    // time — refuses a dead URL instantly instead of handing it
                    // to a decoder and fading the artwork out for a frame that
                    // never comes.
                    val known = CanvasVerifier.health(url)
                    val probe =
                        if (known == CanvasUrlHealth.UNKNOWN) {
                            CanvasVerifier.probe(url)
                        } else {
                            CanvasProbe(known)
                        }
                    if (probe.health == CanvasUrlHealth.UNPLAYABLE) {
                        // Drop it, so the next lookup for this song asks the
                        // providers again rather than being answered from the
                        // cache with a clip that will not play.
                        CanvasArtworkPlaybackCache.remove(mediaId)
                        // The lookup itself said "shown from Tidal"; the clip
                        // was then dropped here, out of the report's sight.
                        // Writing the refusal down keeps the check honest about
                        // a canvas nobody will ever see.
                        CanvasDiagnostics.noteUrlRefused(mediaId)
                        return@withPermit
                    }

                    markHandled(mediaId)
                    // The probe normally carries the size; a URL whose verdict
                    // was already known reports it from the same record.
                    val clipBytes = probe.contentLengthBytes ?: CanvasVerifier.contentLength(url)
                    if (warmVideo) warmVideoCache(appContext, url, clipBytes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).d(e, "Canvas prefetch failed for %s", mediaId)
            } finally {
                synchronized(this@CanvasPrefetcher) { inFlight.remove(mediaId) }
            }
        }
    }

    /**
     * Writes the whole of [url] into the cache the player reads from, when it is
     * a clip small enough to be a canvas.
     *
     * The write is an unbounded read of the same URI playback will open — no
     * subrange — so the cached entry's length *is* the clip's length and the
     * player, reading through [CacheDataSource], finds a complete file where it
     * expected one. Its own [CacheWriter] then has nothing left to fetch, and
     * the loop never leaves the disk.
     *
     * A canvas that is already cached is left alone — [CacheWriter] verifies
     * what it is asked for and returns without touching the network.
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    private suspend fun warmVideoCache(
        context: Context,
        url: String,
        contentLengthBytes: Long?,
    ) {
        if (!url.startsWith("http", ignoreCase = true)) return
        // A playlist is not the animation: it is a few hundred bytes that expire.
        // Caching one would pin *stale* segment addresses — signed URLs that stop
        // working — and the canvas would then fail at the same point forever,
        // from disk, no matter what the network did. HLS clips stream.
        if (url.contains(".m3u8", ignoreCase = true)) return
        // Whole or nothing. See [WarmMaxClipBytes].
        val clipBytes = contentLengthBytes ?: return
        if (clipBytes <= 0L || clipBytes > WarmMaxClipBytes) return
        // With the video cache switched off, playback reads straight from the
        // network and a warm entry would never be consulted.
        val cacheMode =
            context.dataStore.data
                .map { it[CanvasCacheModeKey] }
                .first()
                ?.let { stored -> CanvasCacheMode.entries.firstOrNull { it.name == stored } }
                ?: CanvasCacheMode.VIDEO_AND_URL
        if (cacheMode != CanvasCacheMode.VIDEO_AND_URL) return

        if (!tryMarkWarmed(url)) return

        try {
            val cache = CanvasCacheManager.getVideoCache(context)
            val dataSource =
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(CanvasCacheManager.getUpstreamDataSourceFactory(context))
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()

            try {
                CacheWriter(
                    dataSource,
                    DataSpec(Uri.parse(url)),
                    ByteArray(CacheWriter.DEFAULT_BUFFER_SIZE_BYTES),
                    null,
                ).cache()
            } finally {
                // DataSource is a DataReader with its own close(), not a
                // Closeable, so Kotlin's use {} does not apply to it.
                runCatching { dataSource.close() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed warm is not worth reporting; the player simply downloads
            // the clip itself, exactly as it did before. Let it be retried.
            synchronized(this) { warmed.remove(url) }
            Timber.tag(TAG).d(e, "Canvas warm failed for %s", url)
        }
    }

    /** Marks a song resolved, so it is not looked up again this session. */
    @Synchronized
    private fun markHandled(mediaId: String) {
        handled[mediaId] = true
        trimLru(handled, HandledLimit)
    }

    /** True the first time a URL is warmed; false when it already has been. */
    @Synchronized
    private fun tryMarkWarmed(url: String): Boolean {
        if (warmed.containsKey(url)) return false
        warmed[url] = true
        trimLru(warmed, WarmedUrlLimit)
        return true
    }

    /** These maps track access order, so the first key is the least recently used. */
    private fun <K> trimLru(map: LinkedHashMap<K, Boolean>, limit: Int) {
        while (map.size > limit) {
            val oldest = map.keys.firstOrNull() ?: break
            map.remove(oldest)
        }
    }

    /** Forget everything: a fresh canvas style or a cleared cache changes the answers. */
    @Synchronized
    fun reset() {
        handled.clear()
        warmed.clear()
        inFlight.clear()
    }
}
