package com.jay.glossy.spotify

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.canvas.CanvasLookupUnavailable
import com.jay.glossy.spotifycore.Spotify
import com.jay.glossy.spotifycore.SpotifyHashProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream

/**
 * Spotify's own Canvas Provider updated to work seamlessly 
 * with the new spotifycore backend module.
 */
object SpotifyCanvasProvider {
    private const val SEARCH_URL = "https://api.spotify.com/v1/search"
    private const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
    private const val CANVAS_URL = "https://spclient.wg.spotify.com/canvaz-cache/v0/canvases"

    /**
     * The persisted query the web player uses for search. The hash lives in
     * [SpotifyHashProvider] precisely so it can be refreshed when Spotify
     * rotates it, instead of being frozen into this file.
     */
    private const val SEARCH_OPERATION = "searchDesktop"

    private const val SPOTIFY_APP_UA = "Spotify/9.0.34.593 iOS/18.4 (iPhone15,3)"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client by lazy { HttpClient { install(ContentNegotiation) { json(json) }; expectSuccess = false } }
    private val CANVAS_URL_REGEX = Regex("""https://[^"'\s - ]+\.cnvs\.mp4""")

    private inline fun <T> runSuspend(block: () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        accessToken: String? = null,
        clientToken: String? = null,
    ): CanvasArtwork? {
        // Prefer the token the caller already holds (minted from the web-player
        // cookie or restored by the library session). Falling back to the
        // spotifycore global keeps older call sites working.
        val token = accessToken?.takeIf { it.isNotBlank() }
            ?: Spotify.accessToken?.takeIf { it.isNotBlank() }
            ?: return null
        
        val uri = resolveTrackUri(song, artist, token, clientToken) ?: return null

        val canvasUrl = fetchCanvasUrl(uri, token, clientToken) ?: return null
        return CanvasArtwork(name = song, artist = artist, animated = canvasUrl, videoUrl = canvasUrl)
    }

    /**
     * “Which Spotify track is this song?” — the persisted-query search first,
     * the public REST search as backup. Used for canvases and, identically, by
     * [com.jay.glossy.lyrics.SpotifyLyricsProvider]: one answer to that question
     * is enough, and both features should trust the same track.
     */
    internal suspend fun resolveTrackUri(
        song: String,
        artist: String,
        token: String,
        clientToken: String? = null,
    ): String? =
        searchViaPathfinder(song, artist, token, clientToken) ?: searchViaRest(song, artist, token, clientToken)

    /**
     * Current + previously-known hash for the search operation, so one stale
     * hash costs a retry instead of the whole lookup.
     */
    private fun searchHashes(): List<String> {
        val primary = runCatching { SpotifyHashProvider.getHash(SEARCH_OPERATION) }.getOrNull()
            ?: return emptyList()
        val previous = SpotifyHashProvider.getPreviousHash(SEARCH_OPERATION)
        return if (previous != null && previous != primary) listOf(primary, previous) else listOf(primary)
    }

    /**
     * Resolves a track URI with the web player's own search operation, the same
     * request [com.jay.glossy.spotifycore.Spotify.search] already issues for the
     * Spotify library screens. Using the shared hash registry means a rotated
     * hash recovers on its own instead of silently returning nothing.
     */
    private suspend fun searchViaPathfinder(
        song: String,
        artist: String,
        token: String,
        clientToken: String?,
    ): String? {
        val variables = buildJsonObject {
            put("searchTerm", "$song $artist")
            put("offset", 0)
            put("limit", 10)
            put("numberOfTopResults", 5)
            put("includeAudiobooks", false)
            put("includeArtistHasConcertsField", false)
            put("includePreReleases", false)
            put("includeLocalConcertsField", false)
            put("includeAuthors", false)
        }

        for (sha256Hash in searchHashes()) {
            val body = buildJsonObject {
                put("variables", variables)
                put("operationName", SEARCH_OPERATION)
                putJsonObject("extensions") {
                    putJsonObject("persistedQuery") {
                        put("version", 1)
                        put("sha256Hash", sha256Hash)
                    }
                }
            }.toString()

            val response = runSuspend {
                client.post(PATHFINDER_URL) {
                    header("Authorization", "Bearer $token")
                    // The header the web player sends alongside the bearer; the
                    // endpoints turn a bearer-only request away with a 429.
                    clientToken?.let { header("Client-Token", it) }
                    header("App-platform", "WebPlayer")
                    header("User-Agent", SPOTIFY_APP_UA)
                    header("Accept", "application/json")
                    setBody(
                        TextContent(
                            body,
                            ContentType.Application.Json.withParameter("charset", "UTF-8"),
                        ),
                    )
                }
            } ?: continue
            if (response.status.value !in 200..299) continue
            val text = runSuspend { response.bodyAsText() } ?: continue
            findTrackUri(text, song, artist)?.let { return it }
        }
        return null
    }

    /**
     * Picks a track URI out of a searchDesktop response: an item whose title
     * matches is preferred, otherwise the first track the response offers.
     */
    private fun findTrackUri(body: String, song: String, artist: String): String? = runCatching {
        val items = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject
            ?.get("searchV2")?.jsonObject
            ?.get("tracksV2")?.jsonObject
            ?.get("items")?.jsonArray
            ?: return@runCatching null

        var fallback: String? = null
        for (element in items) {
            val wrapper = element.jsonObject["item"]?.jsonObject ?: continue
            val data = wrapper["data"]?.jsonObject ?: continue
            val uri = data["uri"]?.jsonPrimitive?.contentOrNull
                ?: wrapper["_uri"]?.jsonPrimitive?.contentOrNull
                ?: wrapper["uri"]?.jsonPrimitive?.contentOrNull
                ?: data["id"]?.jsonPrimitive?.contentOrNull?.let { "spotify:track:$it" }
                ?: continue
            if (fallback == null) fallback = uri
            val name = data["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val artistNames = data["artists"]?.jsonObject?.get("items")?.jsonArray
                ?.mapNotNull { it.jsonObject["profile"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull }
                .orEmpty()
            val titleMatches = name.contains(song, ignoreCase = true)
            val artistMatches = artistNames.isEmpty() || artistNames.any { it.contains(artist, ignoreCase = true) }
            if (titleMatches && artistMatches) return@runCatching uri
        }
        fallback
    }.getOrNull()

    private suspend fun searchViaRest(
        song: String,
        artist: String,
        token: String,
        clientToken: String?,
    ): String? {
        val response = runSuspend {
            client.get(SEARCH_URL) {
                header("Authorization", "Bearer $token")
                clientToken?.let { header("Client-Token", it) }
                header("User-Agent", SPOTIFY_APP_UA)
                parameter("q", "$song $artist")
                parameter("type", "track")
                parameter("limit", "10")
            }
        } ?: return null
        if (response.status.value !in 200..299) {
            // A rate limit or an outage is not "Spotify has no canvas"; only a
            // real answer may be remembered as one.
            throwUnavailableForTransient(response.status.value)
            return null
        }
        val body = runSuspend { response.bodyAsText() } ?: return null

        return runCatching {
            val tracks = json.parseToJsonElement(body).jsonObject["tracks"]?.jsonObject?.get("items")
                ?.jsonArray.orEmpty()
            for (item in tracks) {
                val track = item.jsonObject
                val title = track["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val artists = track["artists"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.orEmpty()
                if (!title.contains(song, true) || artists.none { it.contains(artist, true) }) continue
                val uri = track["uri"]?.jsonPrimitive?.contentOrNull ?: continue
                return uri
            }
            null
        }.getOrNull()
    }

    private data class CanvasHit(val url: String, val trackUri: String?)

    private suspend fun fetchCanvasUrl(trackUri: String, token: String, clientToken: String?): String? {
        val body = ByteArrayOutputStream().also { output ->
            val coded = CodedOutputStream.newInstance(output)
            ByteArrayOutputStream().also { nested ->
                CodedOutputStream.newInstance(nested).apply { writeString(1, trackUri); flush() }
                    .let { coded.writeByteArray(1, nested.toByteArray()) }
            }
            coded.flush()
        }
        val response = runSuspend {
            client.post(CANVAS_URL) {
                header("Authorization", "Bearer $token")
                // spclient refuses a bearer-only request with a 429, which used
                // to end every Spotify canvas lookup in silence.
                clientToken?.let { header("Client-Token", it) }
                header("Accept", "application/protobuf")
                header("Accept-Language", "en")
                header("Content-Type", "application/protobuf")
                header("User-Agent", SPOTIFY_APP_UA)
                setBody(body.toByteArray())
            }
        } ?: return null
        if (response.status.value !in 200..299) {
            throwUnavailableForTransient(response.status.value)
            return null
        }
        val bytes = runSuspend { response.body<ByteArray>() } ?: return null

        val hits = decodeCanvasResponse(bytes)
        hits.firstOrNull { it.trackUri == trackUri }?.let { return it.url }

        // Every identified canvas belongs to some other track: refusing is the
        // point of the whole check. Returning the first one anyway is how a
        // different song's clip ended up labelled as this one.
        if (hits.any { !it.trackUri.isNullOrBlank() }) return null

        // The response does not echo the track at all, so its canvas cannot
        // contradict the request; use it. A raw URL scraped out of the payload
        // is used only when it is the only candidate there.
        hits.firstOrNull()?.url?.let { return it }
        return CANVAS_URL_REGEX.findAll(String(bytes, Charsets.ISO_8859_1))
            .map { it.value }
            .distinct()
            .singleOrNull()
    }

    /**
     * Spotify's way of saying "not now" — a rate limit or an outage. Anything
     * else that is not a 2xx is a real answer about this track, which is a null
     * at the call site, not a failure.
     */
    private fun throwUnavailableForTransient(statusCode: Int) {
        if (statusCode == 429 || statusCode in 500..599) {
            throw CanvasLookupUnavailable("Spotify answered HTTP $statusCode")
        }
    }

    private fun decodeCanvasResponse(bytes: ByteArray): List<CanvasHit> = runCatching {
        val parsed = mutableListOf<CanvasHit>()
        val input = CodedInputStream.newInstance(bytes)
        while (!input.isAtEnd) {
            val tag = input.readTag()
            if (tag ushr 3 != 1) { input.skipField(tag); continue }
            val canvas = CodedInputStream.newInstance(input.readByteArray())
            var canvasUrl: String? = null
            var canvasTrackUri: String? = null
            while (!canvas.isAtEnd) {
                val canvasTag = canvas.readTag()
                when (canvasTag ushr 3) {
                    2 -> canvasUrl = canvas.readString()
                    5 -> canvasTrackUri = canvas.readString()
                    else -> canvas.skipField(canvasTag)
                }
            }
            if (!canvasUrl.isNullOrBlank()) parsed += CanvasHit(canvasUrl, canvasTrackUri)
        }
        parsed
    }.getOrElse { emptyList() }
}
