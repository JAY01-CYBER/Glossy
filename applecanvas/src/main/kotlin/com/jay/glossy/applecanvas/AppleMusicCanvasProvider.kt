package com.jay.glossy.applecanvas

import com.jay.glossy.canvas.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AppleMusicCanvasProvider {
    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
                register(ContentType.Text.JavaScript, KotlinxSerializationConverter(json))
            }
            install(HttpTimeout) { connectTimeoutMillis = 15_000; requestTimeoutMillis = 25_000 }
            install(ContentEncoding) { gzip(); deflate() }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private val cache = ConcurrentHashMap<String, Pair<CanvasArtwork?, Long>>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24

    suspend fun getByAlbumArtist(album: String, artist: String, storefront: String = "us"): CanvasArtwork? {
        val key = "$album|$artist|$storefront".lowercase(Locale.ROOT)
        cache[key]?.takeIf { it.second > System.currentTimeMillis() }?.let { return it.first }

        val result = searchAndFetchMotion(album, artist, album, storefront, "albums")
        if (result != null) cache[key] = result to (System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    suspend fun getBySongArtist(song: String, artist: String, album: String? = null, storefront: String = "us"): CanvasArtwork? {
        val key = "$song|$artist|${album ?: ""}|$storefront".lowercase(Locale.ROOT)
        cache[key]?.takeIf { it.second > System.currentTimeMillis() }?.let { return it.first }

        val result = searchAndFetchMotion(song, artist, album, storefront, "songs")
        if (result != null) cache[key] = result to (System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    private suspend fun searchAndFetchMotion(term: String, artist: String, album: String?, storefront: String, type: String): CanvasArtwork? {
        return try {
            val query = if (term.contains(artist, true)) term else "$artist $term"
            val token = AppleMusicTokenProvider.getToken()
            
            val response = client.get("$AMP_BASE_URL/v1/catalog/$storefront/search") {
                header("Authorization", "Bearer $token")
                header("Origin", "https://music.apple.com")
                parameter("term", query)
                parameter("types", type)
                parameter("limit", "10")
                parameter("extend", "editorialVideo")
                parameter("include", "albums")
            }
            if (response.status != HttpStatusCode.OK) return null

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject?.get(type)?.jsonObject?.get("data")?.jsonArray ?: return null

            for (item in results) {
                val attributes = item.jsonObject["attributes"]?.jsonObject ?: continue

                // Validate the hit actually is the requested song before
                // accepting its motion video — otherwise a different track
                // with an editorial video gets painted for this song.
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val resultArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (!resultName.contains(term, ignoreCase = true)) continue
                if (artist.isNotBlank() && !resultArtist.contains(artist, ignoreCase = true)) continue

                val ev = attributes["editorialVideo"]?.jsonObject
                
                if (ev != null) {
                    val url = extractEditorialVideoUrl(ev)
                    if (!url.isNullOrBlank()) {
                        return CanvasArtwork(
                            name = resultName,
                            artist = resultArtist,
                            animated = url
                        )
                    }
                }
            }
            null
        } catch (e: CancellationException) {
            // Never swallow cancellation — a lookup cancelled by a track change
            // must not complete and paint the previous song's artwork.
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun extractEditorialVideoUrl(ev: JsonObject): String? {
        val assets = listOf("motionDetailRaw", "motionDetailSquare", "motionDetailTall")
        for (assetKey in assets) {
            val asset = ev[assetKey]?.jsonObject
            val videoUrl = asset?.get("video")?.jsonPrimitive?.contentOrNull ?: asset?.get("url")?.jsonPrimitive?.contentOrNull
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        return null
    }
}
