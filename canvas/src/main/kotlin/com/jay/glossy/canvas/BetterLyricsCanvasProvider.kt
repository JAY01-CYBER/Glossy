/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * BetterLyrics canvas provider — ported from ArchiveTune (2026)
 * © Rukamori — github.com/rukamori (GPL-3.0).
 * Queries the community artwork service (artwork.boidu.dev) used by ArchiveTune
 * to resolve animated canvas videos for a song + artist pair.
 */

package com.jay.glossy.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object BetterLyricsCanvasProvider {
    private const val BASE_URL = "https://artwork.boidu.dev/"
    private const val CACHE_TTL_MS = 60_000L

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 18_000
                socketTimeoutMillis = 18_000
            }
            expectSuccess = false
        }
    }

    private data class CacheEntry(val artwork: CanvasArtwork, val expiresAtMs: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        storefront: String = "us",
        forceRefresh: Boolean = false,
    ): CanvasArtwork? {
        val key = "s=$song|a=$artist|sf=$storefront".lowercase(Locale.ROOT)
        if (!forceRefresh) {
            cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.artwork }
        } else {
            cache.remove(key)
        }
        return try {
            val response = client.get(BASE_URL) {
                parameter("s", song)
                parameter("a", artist)
                parameter("storefront", storefront)
                if (forceRefresh) header(HttpHeaders.CacheControl, "no-cache")
            }
            if (response.status != HttpStatusCode.OK) return null
            val artwork = response.body<CanvasArtwork>()
            if (artwork.preferredAnimationUrl.isNullOrBlank()) return null
            if (cache.size >= 128) cache.clear()
            cache[key] = CacheEntry(artwork, System.currentTimeMillis() + CACHE_TTL_MS)
            artwork
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
    }

    suspend fun getByAlbumId(albumId: String): CanvasArtwork? {
        val key = "id=$albumId".lowercase(Locale.ROOT)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.artwork }
        return try {
            val response = client.get(BASE_URL) { parameter("id", albumId) }
            if (response.status != HttpStatusCode.OK) return null
            val artwork = response.body<CanvasArtwork>()
            if (artwork.preferredAnimationUrl.isNullOrBlank()) return null
            cache[key] = CacheEntry(artwork, System.currentTimeMillis() + CACHE_TTL_MS)
            artwork
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
    }
}
