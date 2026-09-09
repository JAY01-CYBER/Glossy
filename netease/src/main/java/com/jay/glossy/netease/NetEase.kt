package com.jay.glossy.netease

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.jay.glossy.netease.models.*
import java.util.Locale

object NetEase {
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
            expectSuccess = false
        }
    }

    private fun formatMsToLrcTime(ms: Long): String {
        val mm = ms / 60000
        val ss = (ms % 60000) / 1000f
        val ssStr = String.format(Locale.US, "%05.2f", ss)
        return String.format(Locale.US, "%02d:%s", mm, ssStr)
    }

    private suspend fun searchTrack(title: String, artist: String): Long? = runCatching {
        val cleanTitle = title.replace(Regex("(?i)\\(.*video.*\\)|\\[.*\\]|- official.*"), "").trim()
        val query = "$cleanTitle $artist"
        
        val response = client.get("http://music.163.com/api/search/pc") {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            parameter("s", query)
            parameter("type", 1)
            parameter("limit", 5)
        }
        
        if (response.status == HttpStatusCode.OK) {
            val data = response.body<NeteaseSearchResponse>()
            data.result?.songs?.firstOrNull()?.id
        } else null
    }.getOrNull()

    /**
     * Parses NetEase YRC / K-Lyric (Word-by-word) format to RichSync
     * Format: [start_ms,duration_ms](word_start_ms,word_duration_ms)Word
     */
    private fun parseYrcToLrc(rawYrc: String): String {
        val lrcBuilder = java.lang.StringBuilder()
        val lineRegex = """\[(\d+),\d+](.*)""".toRegex()
        val wordRegex = """\((\d+),\d+\)([^\(]*)""".toRegex()

        rawYrc.lines().forEach { line ->
            val lineMatch = lineRegex.find(line)
            if (lineMatch != null) {
                val lineStartMs = lineMatch.groupValues[1].toLongOrNull() ?: 0L
                val wordsStr = lineMatch.groupValues[2]

                val mainTime = formatMsToLrcTime(lineStartMs)
                
                // Apple Music v2: Check if line is background vocal
                val plainText = wordsStr.replace(wordRegex, "$2").trim()
                var isBackground = false
                var cleanWordsStr = wordsStr
                
                if (plainText.startsWith("(") && plainText.endsWith(")")) {
                    isBackground = true
                    cleanWordsStr = cleanWordsStr.replace("(", "").replace(")", "")
                }
                
                val agent = if (isBackground) "v2: " else ""
                lrcBuilder.append("[$mainTime]$agent")

                val words = wordRegex.findAll(cleanWordsStr).toList()
                if (words.isNotEmpty()) {
                    words.forEach { wMatch ->
                        val wordStartMs = wMatch.groupValues[1].toLongOrNull() ?: 0L
                        val wordText = wMatch.groupValues[2]
                        if (wordText.isNotBlank()) {
                            val wordTime = formatMsToLrcTime(lineStartMs + wordStartMs) // Sometimes NetEase uses relative word MS
                            lrcBuilder.append("<$wordTime>$wordText")
                        }
                    }
                    lrcBuilder.append("\n")
                } else {
                    lrcBuilder.append("$plainText\n")
                }
            }
        }
        return lrcBuilder.toString().trimEnd()
    }

    private fun applyAppleMusicV2Format(raw: String): String {
        return raw.lines().joinToString("\n") { line ->
            val match = "\\[\\d{2}:\\d{2}\\.\\d{2,3}]".toRegex().find(line)
            if (match != null) {
                val timeTag = match.value
                val textPart = line.substringAfter(timeTag).trim()
                if (textPart.startsWith("(") && textPart.endsWith(")")) {
                    "$timeTag v2: " + textPart.removeSurrounding("(", ")")
                } else line
            } else line
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int = 0,
    ): Result<String> = runCatching {
        val trackId = searchTrack(title, artist) ?: throw IllegalStateException("Track not found on NetEase")

        val response = client.get("http://music.163.com/api/song/lyric") {
            header(HttpHeaders.UserAgent, "Mozilla/5.0")
            parameter("id", trackId)
            parameter("lv", 1)
            parameter("kv", 1)
            parameter("tv", -1)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Lyrics API failed")
        }

        val data = response.body<NeteaseLyricsResponse>()
        
        // Prefer YRC/Klyric (Word-by-word) over normal LRC
        val yrcLyric = data.yrc?.lyric?.takeIf { it.isNotBlank() } ?: data.klyric?.lyric?.takeIf { it.isNotBlank() }
        if (yrcLyric != null) {
            val parsed = parseYrcToLrc(yrcLyric)
            if (parsed.isNotBlank()) return@runCatching parsed
        }

        // Fallback to standard LRC
        val lrcLyric = data.lrc?.lyric?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty lyrics on NetEase")

        applyAppleMusicV2Format(lrcLyric)
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
