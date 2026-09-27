package com.jay.glossy.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import com.jay.glossy.constants.EnableMegalobizKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get

object MegalobizLyricsProvider : LyricsProvider {
    override val name: String = "Megalobiz"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableMegalobizKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = "$artist $title".trim()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val searchUrl = "https://www.megalobiz.com/searchall?qry=$encodedQuery"

                val request = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val searchHtml = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string()
                } ?: return@runCatching null

                val lrcPathRegex = Regex("""href=["'](/lrc/maker/download/[^"']+)["']""")
                val match = lrcPathRegex.find(searchHtml) ?: return@runCatching null
                val lrcUrl = "https://www.megalobiz.com" + match.groupValues[1]

                val lrcRequest = Request.Builder()
                    .url(lrcUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val detailHtml = client.newCall(lrcRequest).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string()
                } ?: return@runCatching null

                val lrcSpanRegex = Regex("""id=["']lrc_[^"']*_details["'][^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
                val rawLrcText = lrcSpanRegex.find(detailHtml)?.groupValues?.get(1) ?: detailHtml

                val cleanedText = rawLrcText
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("<br>", "\n")
                    .replace("<br/>", "\n")
                    .replace("<br />", "\n")
                    .replace(Regex("""<[^>]+>"""), "")
                    .trim()

                // If it has basic LRC timestamps, return it
                if (cleanedText.contains(Regex("""\[\d{2}:\d{2}\.\d{2,3}]"""))) {
                    cleanedText
                } else {
                    null
                }
            }.mapCatching {
                it ?: throw Exception("Lyrics not found on Megalobiz")
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
        val result = getLyrics(context, id, title, artist, duration, album)
        result.onSuccess { callback(it) }
    }
}
