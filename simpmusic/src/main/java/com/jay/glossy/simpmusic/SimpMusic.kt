package com.jay.glossy.simpmusic

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.jay.glossy.simpmusic.models.LyricsData
import com.jay.glossy.simpmusic.models.SimpMusicApiResponse
import kotlin.math.abs

object SimpMusic {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1/"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
                header(HttpHeaders.ContentType, "application/json")
            }

            expectSuccess = false
        }
    }

    private fun applyAppleMusicV2Format(raw: String): String {
        return raw.lines().joinToString("\n") { line ->
            val match = "\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]".toRegex().find(line)
            if (match != null) {
                val timeTag = match.value
                val textPart = line.substringAfter(timeTag).trim()
                if (textPart.startsWith("(") && textPart.endsWith(")")) {
                    "$timeTag v2: " + textPart.removeSurrounding("(", ")")
                } else line
            } else line
        }
    }

    // Blank timestamp check
    private fun hasMeaningfulText(lyrics: String?): Boolean {
        if (lyrics.isNullOrBlank()) return false
        val textOnly = lyrics.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}?\\]"), "")
                             .replace(Regex("<\\d{2}:\\d{2}\\.\\d{2,3}?>"), "")
        return textOnly.isNotBlank()
    }

    private suspend fun getLyricsByVideoId(videoId: String): List<LyricsData> =
        runCatching {
            val response = client.get(BASE_URL + videoId)
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<SimpMusicApiResponse>()
                if (apiResponse.success) apiResponse.data else emptyList()
            } else emptyList()
        }.getOrDefault(emptyList())

    private suspend fun searchLyrics(q: String): List<LyricsData> =
        runCatching {
            val response = client.get(BASE_URL + "search") {
                parameter("q", q)
            }
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<SimpMusicApiResponse>()
                if (apiResponse.success) apiResponse.data else emptyList()
            } else emptyList()
        }.getOrDefault(emptyList())

    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        duration: Int = 0,
    ): Result<String> = runCatching {
        
        var tracks = getLyricsByVideoId(videoId).filter { hasMeaningfulText(it.syncedLyrics) || hasMeaningfulText(it.plainLyrics) }

        if (tracks.isEmpty()) {
            tracks = searchLyrics("$title $artist").filter { hasMeaningfulText(it.syncedLyrics) || hasMeaningfulText(it.plainLyrics) }
        }

        if (tracks.isEmpty()) {
            throw IllegalStateException("Lyrics unavailable")
        }

        val bestMatch = if (duration > 0 && tracks.size > 1) {
            tracks.minByOrNull { abs((it.duration ?: 0) - duration) }
        } else {
            tracks.firstOrNull()
        }

        val lyrics = bestMatch?.syncedLyrics?.takeIf { hasMeaningfulText(it) }
            ?: bestMatch?.plainLyrics?.takeIf { hasMeaningfulText(it) }
            ?: throw IllegalStateException("Lyrics unavailable")

        applyAppleMusicV2Format(lyrics)
    }

    suspend fun getAllLyrics(
        videoId: String,
        title: String,
        artist: String,
        duration: Int = 0,
        callback: (String) -> Unit,
    ) {
        var tracks = getLyricsByVideoId(videoId).filter { hasMeaningfulText(it.syncedLyrics) || hasMeaningfulText(it.plainLyrics) }

        if (tracks.isEmpty()) {
            tracks = searchLyrics("$title $artist").filter { hasMeaningfulText(it.syncedLyrics) || hasMeaningfulText(it.plainLyrics) }
        }

        var count = 0
        var plain = 0

        val sortedTracks = if (duration > 0) {
            tracks.sortedBy { abs((it.duration ?: 0) - duration) }
        } else {
            tracks
        }

        sortedTracks.forEach { track ->
            if (count <= 4) {
                if (hasMeaningfulText(track.syncedLyrics) && abs((track.duration ?: 0) - duration) <= 5) {
                    count++
                    callback(applyAppleMusicV2Format(track.syncedLyrics!!))
                }
                if (hasMeaningfulText(track.plainLyrics) && abs((track.duration ?: 0) - duration) <= 5 && plain == 0) {
                    count++
                    plain++
                    callback(applyAppleMusicV2Format(track.plainLyrics!!))
                }
            }
        }
    }
}
