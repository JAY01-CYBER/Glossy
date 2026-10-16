/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Lyrics from the same Spotify account the app already signs in with for
 * playlists and canvases.
 *
 * Two things make this read as a sync rather than another scraper:
 *
 *  - Whatever Spotify answers is written into app storage (filesDir/
 *    `spotify-lyrics`) and served from there on every later playback —
 *    instantly and offline. The app keeps what your account has given it.
 *  - The song is matched to a Spotify track with the exact same
 *    [SpotifyCanvasProvider.resolveTrackUri] call the animated canvas uses,
 *    so the lyrics and the canvas always agree on which track this is —
 *    they cannot diverge into two different songs.
 *
 * It is otherwise a plain participant in the provider race and sits wherever
 * Settings → Content → Lyrics provider priority puts it. With nothing stored
 * yet, it costs the race exactly one search plus one lyrics request against
 * Spotify's own service — usually the most reliable lyrics there are for a
 * commercially released track.
 *
 * Off by default: without the sign-in there is nothing to ask, so enabling
 * it lives behind an explicit switch next to the other providers.
 */

package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.constants.EnableSpotifyLyricsKey
import com.jay.glossy.spotify.SpotifyCanvasProvider
import com.jay.glossy.spotify.SpotifySession
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object SpotifyLyricsProvider : LyricsProvider {
    override val name = "Spotify"

    /** The endpoint the Spotify web player itself reads its lyrics from. */
    private const val LYRICS_URL_BASE = "https://spclient.wg.spotify.com/color-lyrics/v2/track/"
    private const val SPOTIFY_APP_UA = "Spotify/9.0.34.593 iOS/18.4 (iPhone15,3)"

    private const val CACHE_DIR_NAME = "spotify-lyrics"

    /** Lyrics files are a few KB; this only guards against unbounded drift. */
    private const val CACHED_FILE_LIMIT = 512

    private val stampRegex = Regex("""\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?\]""")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            expectSuccess = false
        }
    }

    /**
     * On when the switch in Settings says so — and, until that switch has been
     * answered for itself, on when a Spotify account is signed in.
     *
     * The flat "off" this used to be is why a signed-in account could have
     * Spotify's lyrics sitting one endpoint away and never see them: the
     * provider was enabled only for a listener who had found a toggle buried in
     * Content settings. A stored `false` still means false, so turning the
     * switch off keeps working, and a listener who never touches it gets the
     * lyrics their own account can already provide.
     */
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableSpotifyLyricsKey]
            ?: context.dataStore[SpotifySession.CookieKey].orEmpty().isNotBlank()

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        // A synced copy always beats the network.
        readCache(context, id, title, artist)?.let { return Result.success(it) }

        // The raw library token lapses a little while after login and nothing
        // re-mints it, so reading it directly is what made Spotify lyrics load
        // right after signing in and then never again. Resolve a fresh one the
        // same way the canvas resolver does.
        val credentials = SpotifySession.canvasCredentials(context)
            ?: return Result.failure(IllegalStateException("Not signed in to Spotify"))

        val trackUri = SpotifyCanvasProvider.resolveTrackUri(title, artist, credentials.accessToken, credentials.clientToken)
            ?: return Result.failure(NoSuchElementException("No Spotify track matches this song"))
        val trackId = trackUri.substringAfterLast(':')

        val body = fetchLyricsBody(trackId, credentials.accessToken, credentials.clientToken)
            ?: return Result.failure(NoSuchElementException("Spotify has no lyrics for this track"))

        val lyrics = lyricsFromColorResponse(body)
            ?: return Result.failure(NoSuchElementException("Spotify returned no usable lyrics"))

        // Sync a timed copy into storage; a plain answer is used once and not
        // kept, so a later, better answer can still take its place.
        if (stampRegex.containsMatchIn(lyrics)) writeCache(context, id, title, artist, lyrics)
        return Result.success(lyrics)
    }

    private suspend fun fetchLyricsBody(trackId: String, token: String, clientToken: String?): String? =
        try {
            val response = client.get("$LYRICS_URL_BASE$trackId") {
                parameter("format", "json")
                parameter("market", "from_token")
                header("Authorization", "Bearer $token")
                // The header the web player sends alongside the bearer; the
                // endpoints turn a bearer-only request away with a 429.
                clientToken?.let { header("Client-Token", it) }
                header("App-platform", "WebPlayer")
                header("User-Agent", SPOTIFY_APP_UA)
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) response.bodyAsText() else null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    /**
     * Turns the color-lyrics payload into LRC, every line stamped `[mm:ss.xx]`.
     *
     * The live endpoint answers with `lyrics.lines`, and each line carries
     * `startTimeMs` as a *string* of milliseconds beside its finished text in
     * `words` — also a string. This parser used to read a `syncedLines` array
     * whose entries held `words` as an array of word objects, which is not a
     * shape this endpoint returns: every line was rejected, the provider
     * reported "no usable lyrics", and Spotify's lyrics never reached the
     * screen even on a track it had answered for. Both shapes are read now. The
     * string form is the one that actually comes back; the array form is kept so
     * that nothing which used to match stops matching.
     *
     * A line with no start time is dropped rather than returned as text. The
     * lyric view scrolls and animates off those timestamps, so an unstamped
     * answer would look like a working request and behave like a broken one.
     */
    private fun lyricsFromColorResponse(body: String): String? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val lyrics = (root["lyrics"] ?: root).jsonObject

        val lines = lyrics["lines"].asArray().orEmpty()
            .ifEmpty { lyrics["syncedLines"].asArray().orEmpty() }

        lines.mapNotNull { element ->
            val line = element.jsonObject
            val startMs = (line["startTimeMs"] ?: line["startTime"]).asMillis()
                ?: return@mapNotNull null
            val text = line["words"].asWords() ?: line["syllables"].asWords()
            if (text.isNullOrBlank()) null else "${lrcStamp(startMs)}$text"
        }.joinToString("\n").takeIf { it.isNotBlank() }
    }.getOrNull()

    /** `lines` and `syncedLines` are both arrays; anything else is not one. */
    private fun JsonElement?.asArray(): List<JsonElement>? = (this as? JsonArray)?.toList()

    /** Milliseconds, whichever way the endpoint spells the field. */
    private fun JsonElement?.asMillis(): Long? =
        (this as? JsonPrimitive)?.content?.trim()?.toLongOrNull()

    /**
     * A line's words: one string on the shape that comes back, or an array of
     * word objects on the older one. Both flatten to a single line of text,
     * because a stamped lyric line is one line of text.
     */
    private fun JsonElement?.asWords(): String? {
        val element = this ?: return null
        if (element is JsonNull) return null
        val raw = when (element) {
            is JsonPrimitive -> element.content
            is JsonArray ->
                element.joinToString(" ") { part ->
                    if (part is JsonPrimitive) {
                        part.content
                    } else {
                        (part as? JsonObject)?.get("words").asWords().orEmpty()
                    }
                }
            else -> null
        }
        return raw?.replace(Regex("\\s+"), " ")?.trim()
    }

    private fun lrcStamp(millis: Long): String {
        val minutes = millis / 60_000L
        val seconds = (millis % 60_000L) / 1_000L
        val hundredths = (millis % 1_000L) / 10L
        return "[%02d:%02d.%02d]".format(minutes, seconds, hundredths)
    }

    /**
     * One file per song, keyed on the media id when it is file-safe (YouTube
     * ids always are) so the cache is readable by hand, and on a hash of
     * title+artist when it is not.
     */
    private fun cacheFile(context: Context, id: String, title: String, artist: String): File {
        val directory = File(context.filesDir, CACHE_DIR_NAME).apply { mkdirs() }
        val rawKey = id.takeIf { it.isNotBlank() && it.matches(Regex("[A-Za-z0-9_-]{4,64}")) }
            ?: "$title|$artist"
        val name = MessageDigest.getInstance("SHA-1")
            .digest(rawKey.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$name.lrc")
    }

    private fun readCache(context: Context, id: String, title: String, artist: String): String? =
        runCatching {
            cacheFile(context, id, title, artist)
                .takeIf { it.isFile }
                ?.readText()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    private fun writeCache(context: Context, id: String, title: String, artist: String, lyrics: String) {
        runCatching {
            val file = cacheFile(context, id, title, artist)
            file.writeText(lyrics)
            file.parentFile?.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(CACHED_FILE_LIMIT)
                ?.forEach { stale -> runCatching { stale.delete() } }
        }
    }
}
