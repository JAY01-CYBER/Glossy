package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.constants.EnableBiniLyricsKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object BiniLyricsProvider : LyricsProvider {

    override val name = "BiniLyrics"

    private const val BASE_URL = "https://lyrics-api.binimum.org"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableBiniLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/getLyrics".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track", title)
                ?.addQueryParameter("artist", artist)
                ?.build()
                ?.toString() ?: return@withContext Result.failure(Exception("Invalid URL"))

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext Result.failure(Exception("Bini Lyrics API returned ${response.code}"))
            }

            val body = response.body?.string() ?: run {
                response.close()
                return@withContext Result.failure(Exception("Empty API response"))
            }
            response.close()

            val json = JSONObject(body)
            val results = json.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return@withContext Result.failure(Exception("Bini Lyrics: no results found"))
            }

            // Get the first result's TTML url
            val firstResult = results.getJSONObject(0)
            val lyricsUrl = firstResult.optString("lyricsUrl").takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(Exception("Bini Lyrics: no lyricsUrl in result"))

            // Fetch the TTML
            val ttmlReq = Request.Builder().url(lyricsUrl).build()
            val ttmlRes = client.newCall(ttmlReq).execute()
            if (!ttmlRes.isSuccessful) {
                ttmlRes.close()
                return@withContext Result.failure(Exception("Failed to fetch TTML"))
            }

            val ttmlBody = ttmlRes.body?.string() ?: run {
                ttmlRes.close()
                return@withContext Result.failure(Exception("Empty TTML response"))
            }
            ttmlRes.close()

            val lrc = ttmlToLrc(ttmlBody)
            if (lrc.isNotBlank()) {
                Result.success(lrc)
            } else {
                Result.failure(Exception("Failed to parse TTML or empty lyrics"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        getLyrics(context, id, title, artist, duration, album).onSuccess(callback)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Parsing Logic
    // ──────────────────────────────────────────────────────────────────────

    private val pRegex = """<p\s+begin="([\d.]+)"[^>]*>(.*?)</p>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val spanRegex = """<span\s+begin="([\d.]+)"[^>]*>(.*?)</span>""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Parse Apple Music TTML format and convert to standard LRC or RichSync LRC
     */
    private fun ttmlToLrc(ttml: String): String {
        val lrcBuilder = java.lang.StringBuilder()

        for (pMatch in pRegex.findAll(ttml)) {
            val pBeginSec = pMatch.groupValues[1].toFloatOrNull() ?: continue
            val innerHtml = pMatch.groupValues[2]

            // Apple Music v2: check for Background Vocals (Uh-huh)
            val rawTextForCheck = innerHtml.replace(Regex("<[^>]*>"), "").trim()
            val isBackground = rawTextForCheck.startsWith("(") && rawTextForCheck.endsWith(")")
            val agentLabel = if (isBackground) "v2: " else ""

            val mainTime = formatLrcTime(pBeginSec)
            lrcBuilder.append("[$mainTime]")
            if (agentLabel.isNotEmpty()) {
                lrcBuilder.append(agentLabel)
            }

            val spanMatches = spanRegex.findAll(innerHtml).toList()
            if (spanMatches.isNotEmpty()) {
                for (spanMatch in spanMatches) {
                    val spanBeginSec = spanMatch.groupValues[1].toFloatOrNull() ?: continue
                    var word = spanMatch.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
                    
                    if (isBackground) {
                        word = word.replace("(", "").replace(")", "")
                    }
                    
                    if (word.isNotEmpty()) {
                        val spanTime = formatLrcTime(spanBeginSec)
                        lrcBuilder.append("<$spanTime>$word ")
                    }
                }
            } else {
                // Unsynced spans or just plain text inside <p>
                var plainText = innerHtml.replace(Regex("<[^>]*>"), "").trim()
                if (isBackground) {
                    plainText = plainText.removeSurrounding("(", ")")
                }
                lrcBuilder.append(plainText)
            }
            lrcBuilder.append("\n")
        }

        return lrcBuilder.toString().trimEnd()
    }

    private fun formatLrcTime(sec: Float): String {
        val mm = (sec / 60).toInt()
        val ss = sec % 60
        val ssStr = String.format(Locale.US, "%05.2f", ss) 
        return String.format(Locale.US, "%02d:%s", mm, ssStr)
    }
}
