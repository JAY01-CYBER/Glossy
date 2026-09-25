package com.jay.glossy.canvas

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
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object TidalCanvasProvider {
    private const val BASE_URL = "https://api.tidal.com/v1/"
    private const val TIDAL_TOKEN = "vNVdglQOjFJJGG2U"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

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
            install(HttpCache)
            expectSuccess = false
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private data class CacheEntry(val value: CanvasArtwork?, val expiresAtMs: Long)
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 

    private val countryCode by lazy {
        val country = Locale.getDefault().country
        if (country.length == 2) country.uppercase(Locale.ROOT) else "US"
    }

    suspend fun getBySongArtist(song: String, artist: String, album: String? = null): CanvasArtwork? {
        val key = "$song|$artist|${album ?: ""}".lowercase(Locale.ROOT)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val query = if (!album.isNullOrBlank()) "$album $artist $song" else "$artist $song"
        val result = searchOnTidal(query, "TRACKS", song, artist)
        
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    private suspend fun searchOnTidal(
        query: String, types: String, songValidation: String?, artistValidation: String?
    ): CanvasArtwork? {
        try {
            val response = client.get("${BASE_URL}search") {
                header("X-Tidal-Token", TIDAL_TOKEN)
                parameter("query", query)
                parameter("limit", "10")
                parameter("types", types)
                parameter("countryCode", countryCode)
            }
            if (response.status != HttpStatusCode.OK) return null

            val root = response.body<JsonObject>()
            val items = findSearchSection(root, types.lowercase(Locale.ROOT))?.jsonObject?.get("items")?.jsonArray ?: return null

            for (item in items) {
                val obj = item.jsonObject
                val resultTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                
                if (songValidation != null && resultTitle != null && !resultTitle.contains(songValidation, true)) continue

                val artists = obj["artists"]?.jsonArray
                val primaryArtist = obj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                    ?: artists?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                val creditedArtists = buildList {
                    primaryArtist?.let { add(it) }
                    artists?.forEach { credited ->
                        credited.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.let { add(it) }
                    }
                }

                // Artist must match too — a title-only check lets a remix,
                // cover or another artist's similarly named track through,
                // which paints the wrong song's animated cover.
                if (artistValidation != null && artistValidation.isNotBlank() &&
                    creditedArtists.none { it.contains(artistValidation, true) }
                ) continue

                val albumObj = if (types == "TRACKS") obj["album"]?.jsonObject else obj
                val videoCover = albumObj?.get("videoCover")?.jsonPrimitive?.contentOrNull

                if (!videoCover.isNullOrBlank()) {
                    formatVideoUrl(videoCover)?.let { videoUrl ->
                        return CanvasArtwork(
                            name = resultTitle ?: "",
                            artist = primaryArtist ?: "",
                            videoUrl = videoUrl,
                            albumName = albumObj?.get("title")?.jsonPrimitive?.contentOrNull
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            // A lookup cancelled by a track change must not complete —
            // otherwise the stale result paints over the new song.
            throw e
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    private fun findSearchSection(source: JsonElement, key: String): JsonElement? {
        if (source is JsonObject) {
            if (source.containsKey("items") && source["items"] is JsonArray) return source
            if (source.containsKey(key)) return findSearchSection(source[key]!!, key)
            for (value in source.values) {
                findSearchSection(value, key)?.let { return it }
            }
        } else if (source is JsonArray) {
            for (element in source) {
                findSearchSection(element, key)?.let { return it }
            }
        }
        return null
    }

    private fun formatVideoUrl(id: String): String? {
        val parts = id.split("-")
        if (parts.size != 5) return null
        return "https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4"
    }
}
