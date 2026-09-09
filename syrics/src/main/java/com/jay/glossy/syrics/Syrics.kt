package com.jay.glossy.syrics

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
import com.jay.glossy.syrics.models.SyricsLyricsResponse
import com.jay.glossy.syrics.models.SyricsSearchResponse
import java.util.Locale
import kotlin.math.abs

object Syrics {
    // Note: You can change this BASE_URL if you host your own Syrics instance
    private const val BASE_URL = "https://spotify-lyric-api-984e7b4face0.herokuapp.com"

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
                header(HttpHeaders.UserAgent, "SyricsClient/1.0")
            }
            expectSuccess = false
        }
    }

    private fun formatMsToLrcTime(ms: Long): String {
        val mm = ms / 60000
        val ss = (ms % 60000) / 1000f
        val ssStr = String.format(Locale.US, "%05.2f", ss) 
        return String.format(Locale.US, "%02d:%s", mm, ssStr)
    }

    /**
     * Parses Spotify Lyrics JSON into our RichSync LRC format with v2: Background Vocals.
     */
    private fun parseSyricsToLrc(response: SyricsLyricsResponse): String {
        val lrcBuilder = java.lang.StringBuilder()

        response.lines.forEach { line ->
            val ms = line.startTimeMs?.toLongOrNull() ?: return@forEach
            val mainTime = formatMsToLrcTime(ms)
            
            var text = line.words ?: ""
            if (text.isBlank()) {
                lrcBuilder.append("[$mainTime]\n")
                return@forEach
            }

            var isBackground = false
            if (text.startsWith("(") && text.endsWith(")")) {
                isBackground = true
                text = text.removeSurrounding("(", ")")
            }

            val agent = if (isBackground) "v2: " else ""
            lrcBuilder.append("[$mainTime]$agent")

            // If it has word-by-word (syllables) data
            if (line.syllables.isNotEmpty()) {
                line.syllables.forEach { syl ->
                    val sylMs = syl.startTimeMs?.toLongOrNull() ?: ms
                    val sylTime = formatMsToLrcTime(sylMs)
                    var sylText = syl.chars ?: ""
                    if (isBackground) {
                        sylText = sylText.replace("(", "").replace(")", "")
                    }
                    lrcBuilder.append("<$sylTime>$sylText")
                }
                lrcBuilder.append("\n")
            } else {
                // Just Line Sync
                lrcBuilder.append("$text\n")
            }
        }

        return lrcBuilder.toString().trimEnd()
    }

    private suspend fun searchSpotifyId(title: String, artist: String): String? = runCatching {
        // Some Syrics wrappers have a built-in search. If not, this is a placeholder 
        // to match standard Syrics API wrapper behaviors.
        val searchUrl = "https://spoti-search-proxy.vercel.app/search" // Example Proxy
        val response = client.get(searchUrl) {
            parameter("q", "$title $artist")
        }
        if (response.status == HttpStatusCode.OK) {
            val data = response.body<SyricsSearchResponse>()
            data.tracks.firstOrNull()?.id
        } else null
    }.getOrNull()

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int = 0,
    ): Result<String> = runCatching {
        
        // In a real scenario, you pass the spotify TrackId. 
        // Since we only have Title/Artist, we search first (or assume the provider handles text search).
        // For standard Syrics wrapper:
        val response = client.get(BASE_URL) {
            // Passing trackid as search text if proxy supports it, or rely on a search first.
            parameter("trackid", "$title $artist") 
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Lyrics not found on Syrics")
        }

        val data = response.body<SyricsLyricsResponse>()
        if (data.error == true || data.lines.isEmpty()) {
            throw IllegalStateException("Empty lyrics on Syrics")
        }

        parseSyricsToLrc(data)
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int = 0,
        callback: (String) -> Unit,
    ) {
        val result = getLyrics(title, artist, duration)
        result.onSuccess { callback(it) }
    }
}
