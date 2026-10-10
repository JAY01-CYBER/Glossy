package com.jay.glossy.ui.player

import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.canvas.CanvasLookupUnavailable
import com.metrolist.innertube.InnerTube
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.getItems
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.innertube.models.response.SearchResponse
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.SearchPage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object YouTubeCanvasProvider {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val innerTube = InnerTube()

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            expectSuccess = false
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private data class CacheEntry(val value: CanvasArtwork?, val expiresAtMs: Long)
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24

    private const val MAX_VIDEO_DURATION_SECONDS = 90

    suspend fun getBySongArtist(song: String, artist: String, album: String? = null): CanvasArtwork? {
        val key = "$song|$artist|${album ?: ""}".lowercase(Locale.ROOT)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val queries = buildSearchQueries(song, artist)
        
        for (query in queries) {
            val result = searchAndExtractVideoUrl(query, song, artist)
            if (result != null) {
                cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
                return result
            }
        }

        return null
    }

    private fun buildSearchQueries(song: String, artist: String): List<String> {
        val normalizedSong = song
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(Regex("\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        val normalizedArtist = artist
            .split(Regex("(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)", RegexOption.IGNORE_CASE), limit = 2)
            .firstOrNull()?.replace(Regex("\\s+"), " ")?.trim() ?: ""
        
        return listOf(
            "$normalizedArtist $normalizedSong visualizer",
            "$normalizedArtist $normalizedSong canvas",
            "$normalizedArtist $normalizedSong #shorts",
            "$normalizedSong visualizer",
            "$normalizedSong #shorts",
        ).distinct()
    }

    private suspend fun searchAndExtractVideoUrl(
        query: String,
        songValidation: String,
        artistValidation: String,
    ): CanvasArtwork? {
        try {
            val searchResponse = innerTube.search(
                client = YouTubeClient.WEB_REMIX,
                query = query,
            ).body<SearchResponse>()

            val items = searchResponse.contents
                ?.tabbedSearchResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.flatMap { section ->
                    section.musicShelfRenderer?.contents?.getItems() 
                        ?: section.itemSectionRenderer?.contents?.mapNotNull { it.musicResponsiveListItemRenderer }
                        ?: emptyList()
                }
                ?.mapNotNull { SearchPage.toYTItem(it) }
                ?: return null

            for (item in items) {
                if (item is SongItem) {
                    val resultTitle = item.title
                    val resultArtist = item.artists?.firstOrNull()?.name.orEmpty()

                    if (songValidation.isNotBlank() && !resultTitle.contains(songValidation, true)) continue
                    if (artistValidation.isNotBlank() && !resultArtist.contains(artistValidation, true)) continue

                    val durationSeconds = item.duration ?: 0
                    val isShort = durationSeconds > 0 && durationSeconds <= MAX_VIDEO_DURATION_SECONDS
                    val isVisualizer = resultTitle.contains("visualizer", true) || 
                                       resultTitle.contains("canvas", true) ||
                                       resultTitle.contains("#shorts", true)

                    if (!isShort && !isVisualizer && durationSeconds > MAX_VIDEO_DURATION_SECONDS) continue

                    val videoUrl = getVideoStreamUrl(item.id)
                    if (!videoUrl.isNullOrBlank()) {
                        return CanvasArtwork(
                            name = resultTitle,
                            artist = resultArtist,
                            videoUrl = videoUrl,
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw CanvasLookupUnavailable("YouTube lookup failed", e)
        }
        return null
    }

    private suspend fun getVideoStreamUrl(videoId: String): String? {
        try {
            val playerResponse = innerTube.player(
                client = YouTubeClient.WEB_REMIX,
                videoId = videoId,
                playlistId = null,
                signatureTimestamp = null,
                poToken = null,
            ).body<PlayerResponse>()

            val streamingData = playerResponse.streamingData ?: return null
            
            val videoFormats = streamingData.adaptiveFormats
                .filter { !it.isAudio && it.mimeType.startsWith("video/") }
                .filter { it.width != null && it.height != null }
                .filter { it.url != null || it.signatureCipher != null || it.cipher != null }

            if (videoFormats.isEmpty()) return null

            val bestFormat = videoFormats
                .maxByOrNull { (it.height ?: 0) * (it.width ?: 0) }
            
            return bestFormat?.url ?: bestFormat?.signatureCipher?.let { decryptSignature(it) } ?: bestFormat?.cipher?.let { decryptSignature(it) }
        } catch (e: Exception) {
            return null
        }
    }

    private fun decryptSignature(cipher: String): String? {
        val params = cipher.split("&").associate { it.split("=").let { (k, v) -> k to v } }
        val signature = params["s"] ?: params["sig"] ?: params["signature"] ?: return null
        val url = params["url"] ?: return null
        return "$url&signature=$signature"
    }
}