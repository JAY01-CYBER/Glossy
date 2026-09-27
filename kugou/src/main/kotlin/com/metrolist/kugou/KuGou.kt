package com.metrolist.kugou

import com.metrolist.kugou.models.DownloadLyricsResponse
import com.metrolist.kugou.models.Keyword
import com.metrolist.kugou.models.SearchLyricsResponse
import com.metrolist.kugou.models.SearchSongResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import java.lang.Integer.min
import kotlin.math.abs

@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
private val client = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        json(json)
        json(json, ContentType.Text.Html)
        json(json, ContentType.Text.Plain)
    }

    install(ContentEncoding) {
        gzip()
        deflate()
    }
}

private const val PAGE_SIZE = 8
private const val HEAD_CUT_LIMIT = 30

/**
 * KuGou Lyrics Library
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
object KuGou {
    var useTraditionalChinese: Boolean = false

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String? = null): Result<String> =
        runCatching {
            val keyword = generateKeyword(title, artist, album)
            getLyricsCandidate(keyword, duration)?.let { candidate ->
                Base64.Default.decode(downloadLyrics(candidate.id, candidate.accesskey).content).decodeToString()
                    .normalize()
            } ?: throw IllegalStateException("No lyrics candidate")
        }

    suspend fun getAllPossibleLyricsOptions(
        title: String, artist: String, duration: Int, album: String? = null, callback: (String) -> Unit
    ) {
        val keyword = generateKeyword(title, artist, album)
        val response = searchSongs(keyword)
        val songs = response.data.info

        // Sort songs by text similarity and duration
        val sortedSongs = songs.sortedByDescending { song ->
            var score = 0.0
            val sName = song.songname.takeIf { it.isNotEmpty() } ?: song.filename
            val sArtist = song.singername.takeIf { it.isNotEmpty() } ?: song.filename

            score += calculateSimilarity(keyword.title, sName)
            score += calculateSimilarity(keyword.artist, sArtist)

            if (duration != -1) {
                val diff = abs(song.duration - duration)
                if (diff <= DURATION_TOLERANCE) score += 1.0
                score -= (diff * 0.01)
            }
            score
        }

        var count = 0
        sortedSongs.forEach { song ->
            if (count >= 3) return@forEach // limit hash requests to top 3 best matches
            if (duration == -1 || abs(song.duration - duration) <= DURATION_TOLERANCE) {
                searchLyricsByHash(song.hash).candidates.firstOrNull()?.let { candidate ->
                    val decoded = Base64.Default.decode(downloadLyrics(candidate.id, candidate.accesskey).content).decodeToString().normalize()
                    if (decoded.isNotBlank()) {
                        count++
                        callback(decoded)
                    }
                }
            }
        }

        // Fallback to keyword search if not enough found
        searchLyricsByKeyword(keyword, duration).candidates.forEach { candidate ->
            if (count >= 5) return@forEach
            val decoded = Base64.Default.decode(downloadLyrics(candidate.id, candidate.accesskey).content).decodeToString().normalize()
            if (decoded.isNotBlank()) {
                count++
                callback(decoded)
            }
        }
    }

    suspend fun getLyricsCandidate(
        keyword: Keyword, duration: Int
    ): SearchLyricsResponse.Candidate? {
        val songs = searchSongs(keyword).data.info

        // Select the absolute best match checking both Name and Duration
        val bestSong = songs.maxByOrNull { song ->
            var score = 0.0
            val sName = song.songname.takeIf { it.isNotEmpty() } ?: song.filename
            val sArtist = song.singername.takeIf { it.isNotEmpty() } ?: song.filename

            score += calculateSimilarity(keyword.title, sName)
            score += calculateSimilarity(keyword.artist, sArtist)

            if (duration != -1) {
                val diff = abs(song.duration - duration)
                if (diff <= DURATION_TOLERANCE) score += 1.0 
                score -= (diff * 0.01)
            }
            score
        }

        if (bestSong != null) {
            val isDurationOk = duration == -1 || abs(bestSong.duration - duration) <= DURATION_TOLERANCE
            if (isDurationOk) {
                val candidate = searchLyricsByHash(bestSong.hash).candidates.firstOrNull()
                if (candidate != null) return candidate
            }
        }

        return searchLyricsByKeyword(keyword, duration).candidates.firstOrNull()
    }

    suspend fun searchSongs(keyword: Keyword) =
        client.get("https://mobileservice.kugou.com/api/v3/search/song") {
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", PAGE_SIZE)
            parameter("showtype", 0)
            val searchQuery = buildString {
                append(keyword.title)
                append(" - ")
                append(keyword.artist)
                if (!keyword.album.isNullOrBlank()) {
                    append(" ")
                    append(keyword.album)
                }
            }
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchSongResponse>()

    private suspend fun searchLyricsByKeyword(keyword: Keyword, duration: Int) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter(
                "duration", duration.takeIf { it != -1 }?.times(1000)
            ) // if duration == -1, we don't care duration
            val searchQuery = buildString {
                append(keyword.title)
                append(" - ")
                append(keyword.artist)
                if (!keyword.album.isNullOrBlank()) {
                    append(" ")
                    append(keyword.album)
                }
            }
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchLyricsResponse>()

    private suspend fun searchLyricsByHash(hash: String) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter("hash", hash)
        }.body<SearchLyricsResponse>()

    private suspend fun downloadLyrics(id: Long, accessKey: String) =
        client.get("https://lyrics.kugou.com/download") {
            parameter("fmt", "lrc")
            parameter("charset", "utf8")
            parameter("client", "pc")
            parameter("ver", 1)
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.body<DownloadLyricsResponse>()

    private fun normalizeTitle(title: String) =
        title.replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")
            .replace("「.*」".toRegex(), "").replace("『.*』".toRegex(), "")
            .replace("<.*>".toRegex(), "").replace("《.*》".toRegex(), "")
            .replace("〈.*〉".toRegex(), "").replace("＜.*＞".toRegex(), "")

    private fun normalizeArtist(artist: String) =
        artist.replace(", ", "、").replace(" & ", "、").replace(".", "").replace("和", "、")
            .replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")

    fun generateKeyword(title: String, artist: String, album: String? = null) =
        Keyword(normalizeTitle(title), normalizeArtist(artist), album)

    private fun String.normalize(): String =
        lines().filter { line -> line.matches(ACCEPTED_REGEX) }
            .let { lines ->
                // Remove useless information such as singer, writer, composer, guitar, etc.
                var headCutLine = 0
                for (i in min(HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[i].matches(BANNED_REGEX)) {
                        headCutLine = i + 1
                        break
                    }
                }
                val filteredLines = lines.drop(headCutLine)

                var tailCutLine = 0
                for (i in min(lines.size - HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[lines.lastIndex - i].matches(BANNED_REGEX)) {
                        tailCutLine = i + 1
                        break
                    }
                }
                val finalLines = filteredLines.dropLast(tailCutLine)

                return@let finalLines.joinToString("\n")
            }

    @Suppress("RegExpRedundantEscape")
    private val ACCEPTED_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\].*".toRegex()
    private val BANNED_REGEX = ".+].+[:：].+".toRegex()

    private const val DURATION_TOLERANCE = 8

    // Similarity Logic Added
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = str1.trim().lowercase()
        val s2 = str2.trim().lowercase()
        
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        
        return when {
            s1.contains(s2) || s2.contains(s1) -> 0.8
            else -> {
                val maxLength = maxOf(s1.length, s2.length)
                val distance = levenshteinDistance(s1, s2)
                1.0 - (distance.toDouble() / maxLength)
            }
        }
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      
                    matrix[i][j - 1] + 1,      
                    matrix[i - 1][j - 1] + cost 
                )
            }
        }
        
        return matrix[len1][len2]
    }
}
