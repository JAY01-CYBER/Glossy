/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lossless

import com.jay.glossy.BuildConfig
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * A lossless master, handed over by a source module.
 *
 * `url` is a direct FLAC/ALAC stream; the rest describes it, so the player can
 * name what is playing instead of guessing from a MIME type it never saw.
 */
data class LosslessStream(
    val url: String,
    val codec: String = "FLAC",
    val bitDepth: Int = 0,
    val sampleRateHz: Int = 0,
    val bitrate: Int = 0,
    val expiresInSeconds: Int = LOSSLESS_DEFAULT_EXPIRY_SECONDS,
) {
    /** The container Media3's extractors expect for [codec]. */
    val mimeType: String
        get() =
            when (codec.uppercase(Locale.ROOT)) {
                "ALAC" -> "audio/mp4"
                "WAV", "PCM" -> "audio/wav"
                "MP3" -> "audio/mpeg"
                else -> "audio/flac"
            }

    /** `FLAC · 24-bit · 96.0 kHz`, the line the settings screen prints. */
    val badge: String
        get() =
            listOfNotNull(
                codec.uppercase(Locale.ROOT).takeIf { it.isNotBlank() },
                bitDepth.takeIf { it > 0 }?.let { "$it-bit" },
                sampleRateHz.takeIf { it > 0 }?.let { String.format(Locale.ROOT, "%.1f kHz", it / 1000f) },
            ).joinToString(" · ")

}

/** How long a module's answer is assumed to stay valid when it does not say. */
private const val LOSSLESS_DEFAULT_EXPIRY_SECONDS = 600

/**
 * The lossless lane's source, in the shape BitChord uses: an endpoint that
 * turns a track into a FLAC/ALAC stream.
 *
 * A build can carry one (`LOSSLESS_SOURCE_URL`), so the listener never has to
 * configure anything; a URL saved in settings overrides it. YouTube tops out at
 * a lossy Opus master, so a real lossless copy has to come from somewhere the
 * app cannot host itself. There is deliberately no folder to scan and no index
 * to keep: the source is asked per track, its answer is cached until the URL
 * expires, and when it has nothing — or the build carries none —
 * `AudioQuality.LOSSLESS` falls back to YouTube's best audio, so choosing the
 * tier can never leave a song silent.
 *
 * ## The contract
 *
 * The URL — whether the build supplied it or a listener saved one — is either an
 * endpoint that accepts query parameters, or a template containing `{videoId}`,
 * `{title}`, `{artist}` and/or `{duration}`.
 * Query form:
 *
 * ```
 * GET https://module.example/resolve?videoId=…&id=…&title=…&artist=…&duration=…
 * ```
 *
 * It answers with a JSON object — flat, or under `stream` / `data` / `track`:
 *
 * ```json
 * {
 *   "url": "https://cdn.example/song.flac",
 *   "codec": "FLAC",
 *   "bitDepth": 24,
 *   "sampleRate": 96000,
 *   "bitrate": 2304000,
 *   "expiresInSeconds": 3600
 * }
 * ```
 *
 * Only `url` is required; `codec` defaults to FLAC and the numbers are reported
 * as unknown when absent. A 404, an `{"error": …}` body or a timeout all mean
 * "no master" and are logged, never thrown: the player must keep working with
 * the stream it already resolved.
 */
object LosslessSource {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

    /**
     * The endpoint the build carries, so choosing Lossless needs no setup.
     *
     * Supplied by `local.properties` or the build environment, never by the
     * source tree: an endpoint is deployment detail, not a library the listener
     * or the repository should have to maintain. Null leaves the lane on
     * YouTube's best stream until a listener saves an override.
     */
    private val builtInUrl: String? =
        BuildConfig.LOSSLESS_SOURCE_URL
            .trim()
            .takeIf {
                it.startsWith("http://", ignoreCase = true) ||
                    it.startsWith("https://", ignoreCase = true)
            }

    /** The source in use: the build's endpoint until a saved URL overrides it. */
    @Volatile
    private var moduleUrl: String? = builtInUrl

    /** Bounded so a long queue cannot grow it without limit. */
    private val cache = LinkedHashMap<String, Entry>()

    private data class Entry(val stream: LosslessStream, val expiresAt: Long)

    /**
     * Applies the saved setting; safe to call as often as the preference changes.
     *
     * A different module means a different catalogue, so the answers cached for
     * the old one are dropped rather than replayed against the new endpoint.
     */
    fun configure(url: String?) {
        // Blank or malformed means "no override", not "no lossless": the
        // build's endpoint takes over whenever it has one.
        val override = url?.trim()?.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }
        val next = override ?: builtInUrl
        if (next == moduleUrl) return
        moduleUrl = next
        synchronized(cache) { cache.clear() }
    }

    /** The host to name in settings, so a URL need not be shown. */
    fun hostOf(url: String): String? = url.trim().toHttpUrlOrNull()?.host

    /** The build's endpoint host, or null when listeners must supply their own. */
    fun builtInHost(): String? = builtInUrl?.let(::hostOf)

    /**
     * The master for a track, or null when there is none.
     *
     * Called from the stream resolver, which already runs on an IO dispatcher;
     * the cache hit path is a locked map read and costs nothing.
     */
    suspend fun resolve(
        videoId: String,
        title: String?,
        artist: String?,
        durationSeconds: Int,
    ): LosslessStream? =
        withContext(Dispatchers.IO) {
            val base = moduleUrl ?: return@withContext null
            cached(videoId)?.let { return@withContext it }

            val endpoint =
                endpointFor(base, videoId, title, artist, durationSeconds)
                    ?: return@withContext null
            val stream =
                runCatching {
                    client.newCall(Request.Builder().url(endpoint).header("Accept", "application/json").build())
                        .execute()
                        .use { response ->
                            if (!response.isSuccessful) {
                                Timber.tag(TAG).d("Lossless source answered ${response.code} for $videoId")
                                return@use null
                            }
                            parse(response.body?.string().orEmpty())
                        }
                }
                    .onFailure { Timber.tag(TAG).w(it, "Lossless source failed for $videoId") }
                    .getOrNull()
                    ?: return@withContext null

            remember(videoId, stream)
            Timber.tag(TAG).d("Lossless master for $videoId: ${stream.badge}")
            stream
        }

    private fun cached(videoId: String): LosslessStream? =
        synchronized(cache) {
            val entry = cache[videoId] ?: return null
            if (entry.expiresAt <= System.currentTimeMillis()) {
                cache.remove(videoId)
                null
            } else {
                entry.stream
            }
        }

    private fun remember(videoId: String, stream: LosslessStream) {
        synchronized(cache) {
            if (cache.size >= MAX_CACHED) {
                cache.keys.firstOrNull()?.let { cache.remove(it) }
            }
            // A minute of slack, so a cached URL is never handed out as it expires.
            val lifetime = (stream.expiresInSeconds - 60).coerceAtLeast(30) * 1000L
            cache[videoId] = Entry(stream, System.currentTimeMillis() + lifetime)
        }
    }

    private fun endpointFor(
        base: String,
        videoId: String,
        title: String?,
        artist: String?,
        durationSeconds: Int,
    ): HttpUrl? {
        if ("{" in base) {
            val replaced =
                base
                    .replace("{videoId}", videoId)
                    .replace("{title}", encode(title.orEmpty()))
                    .replace("{artist}", encode(artist.orEmpty()))
                    .replace("{duration}", durationSeconds.toString())
            return replaced.toHttpUrlOrNull()
        }
        return base.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("videoId", videoId)
            // Most modules in the wild key on `id`; sending both costs nothing.
            ?.addQueryParameter("id", videoId)
            ?.addQueryParameter("title", title.orEmpty())
            ?.addQueryParameter("artist", artist.orEmpty())
            ?.addQueryParameter("duration", durationSeconds.toString())
            ?.build()
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    /**
     * Reads the module's answer, tolerating the handful of shapes modules
     * actually use: a flat object, or one nested under `stream`/`data`/`track`.
     */
    private fun parse(body: String): LosslessStream? {
        if (body.isBlank()) return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val payload = payloadOf(root) ?: return null
        val url = payload.string("url", "streamUrl", "stream_url") ?: return null
        if (!url.startsWith("http", ignoreCase = true)) return null

        return LosslessStream(
            url = url,
            codec = payload.string("codec", "format", "type")?.uppercase(Locale.ROOT) ?: "FLAC",
            bitDepth = payload.int("bitDepth", "bit_depth", "bitsPerSample") ?: 0,
            sampleRateHz = payload.int("sampleRate", "sampleRateHz", "sample_rate") ?: 0,
            bitrate = payload.int("bitrate", "bitRate") ?: 0,
            expiresInSeconds = payload.int("expiresInSeconds", "expiresIn", "expires_in") ?: 600,
        )
    }

    private fun payloadOf(root: JsonObject): JsonObject? {
        if (root.string("url", "streamUrl", "stream_url") != null) return root
        return NESTED.firstNotNullOfOrNull { key ->
            (root[key] as? JsonObject)?.takeIf { it.string("url", "streamUrl", "stream_url") != null }
        }
    }

    private fun JsonObject.string(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private fun JsonObject.int(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        }

    private val NESTED = listOf("stream", "data", "track")

    private const val TAG = "LosslessSource"

    private const val MAX_CACHED = 48
}
