package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.constants.EnableLrcLibKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

// Models for LRCLIB API Response
@Serializable
data class LrcLibResponse(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Int? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    // Future-proofing: When LRCLIB fully releases the .yaml format in API
    val lyricsfileYaml: String? = null 
)

object LrcLibLyricsProvider : LyricsProvider {

    override val name = "LrcLib"

    private const val BASE_URL = "https://lrclib.net/api/"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    }
                )
            }
            defaultRequest {
                url(BASE_URL)
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        
        val response = client.get("get") {
            parameter("track_name", title)
            parameter("artist_name", artist)
            if (!album.isNullOrBlank()) parameter("album_name", album)
            if (duration > 0) parameter("duration", duration)
        }

        if (response.status.value != 200) {
            throw Exception("LRCLIB returned ${response.status.value}")
        }

        val data = response.body<LrcLibResponse>()

        // 1. Future Proofing: If YAML format is available, parse it natively
        if (!data.lyricsfileYaml.isNullOrBlank()) {
            return@runCatching parseLyricsfileYamlToLrc(data.lyricsfileYaml)
        }

        // 2. Standard LRC fallback
        val lyrics = data.syncedLyrics.takeIf { !it.isNullOrBlank() }
            ?: data.plainLyrics.takeIf { !it.isNullOrBlank() }
            ?: throw Exception("No lyrics found on LRCLIB")

        // Apply our Apple Music v2 logic to standard LRC
        applyAppleMusicV2Format(lyrics)
    }

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit
    ) {
        val result = getLyrics(context, id, title, artist, duration, album)
        result.onSuccess { callback(it) }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Parsing Logic & Formatting
    // ──────────────────────────────────────────────────────────────────────

    private fun applyAppleMusicV2Format(raw: String): String {
        return raw.lines().joinToString("\n") { line ->
            val match = "\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]".toRegex().find(line)
            if (match != null) {
                val timeTag = match.value
                val textPart = line.substringAfter(timeTag).trim()
                // Convert (Uh-huh) into v2: Uh-huh
                if (textPart.startsWith("(") && textPart.endsWith(")")) {
                    "$timeTag v2: " + textPart.removeSurrounding("(", ")")
                } else line
            } else line
        }
    }

    /**
     * Custom YAML Parser to convert the new "Lyricsfile" draft format into 
     * our RichSync LRC format with v2: Background Vocals support.
     */
    private fun parseLyricsfileYamlToLrc(yaml: String): String {
        val lrcBuilder = java.lang.StringBuilder()
        var currentLineTime = ""
        var currentLineWords = mutableListOf<String>()
        var inWordsSection = false
        var isBackground = false

        val lines = yaml.lines()
        for (i in lines.indices) {
            val line = lines[i].trimEnd()
            val trimmed = line.trimStart()

            if (trimmed.startsWith("lines:")) continue

            if (line.startsWith("  - text:")) {
                // Save previous line before starting a new one
                if (currentLineTime.isNotEmpty() || currentLineWords.isNotEmpty()) {
                    val agent = if (isBackground) "v2: " else ""
                    val lineText = if (currentLineWords.isNotEmpty()) currentLineWords.joinToString("") else ""
                    lrcBuilder.append("[$currentLineTime]$agent$lineText\n")
                }
                
                currentLineWords.clear()
                inWordsSection = false
                currentLineTime = ""
                isBackground = false
                
                val rawText = line.substringAfter("text:").trim().removeSurrounding("'").removeSurrounding("\"")
                if (rawText.startsWith("(") && rawText.endsWith(")")) {
                    isBackground = true
                }

                // Look ahead up to 3 lines to find start_ms for this line
                for (j in i + 1..minOf(i + 3, lines.lastIndex)) {
                    val ahead = lines[j].trim()
                    if (ahead.startsWith("start_ms:")) {
                        val ms = ahead.substringAfter("start_ms:").trim().toLongOrNull() ?: 0L
                        currentLineTime = formatMsToLrcTime(ms)
                        break
                    }
                }
            } else if (trimmed.startsWith("words:")) {
                inWordsSection = true
            } else if (inWordsSection && line.startsWith("      - text:")) {
                // Parse individual words for RichSync Karaoke
                var wordText = line.substringAfter("text:").trim().removeSurrounding("'").removeSurrounding("\"")
                if (isBackground) {
                    wordText = wordText.replace("(", "").replace(")", "")
                }
                
                var wordTime = ""
                // Look ahead to find word start_ms
                for (j in i + 1..minOf(i + 3, lines.lastIndex)) {
                    val ahead = lines[j].trim()
                    if (ahead.startsWith("start_ms:")) {
                        val ms = ahead.substringAfter("start_ms:").trim().toLongOrNull() ?: 0L
                        wordTime = formatMsToLrcTime(ms)
                        break
                    }
                }
                if (wordTime.isNotEmpty() && wordText.isNotEmpty()) {
                    currentLineWords.add("<$wordTime>$wordText")
                }
            }
        }

        // Append the very last line
        if (currentLineTime.isNotEmpty() || currentLineWords.isNotEmpty()) {
            val agent = if (isBackground) "v2: " else ""
            lrcBuilder.append("[$currentLineTime]$agent${currentLineWords.joinToString("")}\n")
        }

        return lrcBuilder.toString().trimEnd()
    }

    private fun formatMsToLrcTime(ms: Long): String {
        val mm = ms / 60000
        val ss = (ms % 60000) / 1000f
        val ssStr = String.format(Locale.US, "%05.2f", ss) 
        return String.format(Locale.US, "%02d:%s", mm, ssStr)
    }
}
