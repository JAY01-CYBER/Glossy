package com.jay.glossy.applecanvas

import com.jay.glossy.canvas.CanvasLookupUnavailable
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Base64

/**
 * The web player's bearer token, scraped out of Apple Music's own bundle.
 *
 * Two things used to make this the reason canvases silently stopped working:
 *
 *  - the token was cached forever. Once it lapsed — Apple's live for about an
 *    hour — every lookup answered 401, and this object kept handing out the
 *    dead token until the process died. The expiry is on the token itself
 *    (`exp`), so it is read and respected.
 *  - when the scrape failed, a hardcoded fallback JWT was returned. That token
 *    expired in June 2026, so every path that reached it asked Apple with a
 *    dead credential and was told 401 — an error that the provider then
 *    reported as "this song has no canvas". A failure now throws
 *    [CanvasLookupUnavailable], which the resolver records as a failed lookup
 *    and never as a song without a canvas.
 */
internal object AppleMusicTokenProvider {
    /** Re-scrape with a minute to spare, so a request never races the expiry. */
    private const val TokenSafetyMarginMillis = 60_000L

    /** Used only when the token carries no readable `exp`. */
    private const val AssumedTokenLifetimeMillis = 30L * 60_000L

    private var cached: CachedToken? = null
    private val mutex = Mutex()

    private val httpClient =
        HttpClient(OkHttp) {
            expectSuccess = true
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 20_000
                socketTimeoutMillis = 20_000
            }
        }

    private data class CachedToken(val token: String, val expiresAtMillis: Long)

    suspend fun getToken(): String =
        mutex.withLock {
            cached
                ?.takeIf { it.expiresAtMillis > System.currentTimeMillis() }
                ?.let { return@withLock it.token }

            val token =
                try {
                    scrapeToken()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw CanvasLookupUnavailable("Apple Music token unavailable", e)
                }

            val expiresAt = tokenExpiryMillis(token)
            if (expiresAt != null && expiresAt <= System.currentTimeMillis() + TokenSafetyMarginMillis) {
                throw CanvasLookupUnavailable("Apple Music served an already-expired token")
            }
            cached = CachedToken(token, expiresAt ?: (System.currentTimeMillis() + AssumedTokenLifetimeMillis))
            token
        }

    private suspend fun scrapeToken(): String {
        val htmlResponse = httpClient.get("https://beta.music.apple.com")
        val htmlBody = htmlResponse.bodyAsText()
        val indexJsRegex = Regex("""src="(/assets/index-[^"]+\.js)"""")
        val match =
            indexJsRegex.find(htmlBody)
                ?: throw IllegalStateException("Could not find Apple Music's index.js")

        val indexJsResponse = httpClient.get("https://beta.music.apple.com${match.groupValues[1]}")
        val indexJsBody = indexJsResponse.bodyAsText()

        val tokenRegex = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
        return tokenRegex.find(indexJsBody)?.value
            ?: throw IllegalStateException("Could not find a token in Apple Music's bundle")
    }

    /** The `exp` claim of a JWT, in millis, or null when it cannot be read. */
    private fun tokenExpiryMillis(token: String): Long? =
        runCatching {
            val payload = token.split('.').getOrNull(1) ?: return null
            val json = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
            Regex("\"exp\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()?.times(1000L)
        }.getOrNull()
}
