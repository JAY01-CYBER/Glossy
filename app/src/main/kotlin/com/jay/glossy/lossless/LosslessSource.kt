/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lossless

import com.jay.glossy.BuildConfig
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
                "ALAC", "M4A", "MP4", "AAC" -> "audio/mp4"
                "WAV", "PCM" -> "audio/wav"
                "MP3" -> "audio/mpeg"
                "OGG", "OPUS", "VORBIS" -> "audio/ogg"
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

/** BitChord re-asks an addon for a stream every five minutes; match that. */
private const val LOSSLESS_ADDON_EXPIRY_SECONDS = 300

/** How long a "not an addon" answer is trusted before the manifest is re-probed. */
private const val ADDON_PROBE_RETRY_MS = 10 * 60 * 1000L

/** What a BitChord addon calls its top quality tier. */
private const val LOSSLESS_QUALITY = "lossless"

/** The labels an addon uses for the tier this lane wants. */
private val LOSSLESS_QUALITY_HINT =
    Regex("""lossless|hi[-_ ]?res|flac|alac|24[-_ ]?bit""", RegexOption.IGNORE_CASE)

private const val DEFAULT_CODEC = "FLAC"

/** Codec names an addon may put in `format`, `codec` or `fileCodec`. */
private val KNOWN_CODECS =
    listOf("FLAC", "ALAC", "WAV", "PCM", "OPUS", "VORBIS", "AAC", "MP3", "M4A", "MP4")

/** A BitChord addon, reduced to what the lossless lane needs. */
private data class AddonSource(
    /** The addon's base URL, with `/manifest.json` and trailing slashes removed. */
    val baseUrl: String,
    /**
     * The manifest's settings defaults, with `quality` resolved to the option
     * that names lossless. BitChord sends the same set on search and stream, so
     * an addon that needs a region or a token gets it from here.
     */
    val params: Map<String, String>,
)

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
 * ## The two source shapes
 *
 * A direct resolver is asked per track. Its URL — whether the build supplied it
 * or a listener saved one — is either an endpoint that accepts query
 * parameters, or a template containing `{videoId}`, `{title}`, `{artist}`
 * and/or `{duration}`. Query form:
 *
 * ```
 * GET https://module.example/resolve?videoId=…&id=…&title=…&artist=…&duration=…
 * ```
 *
 * A BitChord addon is the other shape: its manifest declares `search` and
 * `stream`, so the lane searches the catalogue with the track's title and
 * artist, then asks for the best matching row by id. Addons that put a
 * `streamURL` on the search row are taken at their word and skip the second
 * call, exactly as BitChord does.
 *
 * Both answer with a JSON object — flat, or under `stream` / `data` / `track`:
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
 * as unknown when absent. Metadata nested beside the URL — BitChord's `track`
 * object, for instance — is read too, so a response that names the URL at the
 * top and describes the master one level down still gets its badge. A 404, an
 * `{"error": …}` body or a timeout all mean "no master" and are logged, never
 * thrown: the player must keep working with the stream it already resolved.
 */
object LosslessSource {
    /**
     * What the source said the last time it was asked.
     *
     * Choosing Lossless and being served a master, and choosing Lossless and
     * silently landing on Opus, look identical from the player — the stream
     * either plays or it does not. This is the missing half of that story, kept
     * for the Lossless screen so a fallback can be explained instead of only
     * being heard.
     */
    data class Check(
        /** The track it was asked about, so the note is about a song and not the app. */
        val track: String,
        /** `FLAC · 24-bit · 96.0 kHz`, `The source answered 404` … */
        val outcome: String,
    )

    private val _lastCheck = MutableStateFlow<Check?>(null)

    /** The most recent answer, or null before the lossless tier has been used. */
    val lastCheck: StateFlow<Check?> = _lastCheck.asStateFlow()

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
     * Manifest detection runs before the first track is resolved, so its budget
     * is deliberately smaller than a real request's: a direct resolver must not
     * wait eight seconds to be told it is not an addon.
     */
    private val probeClient =
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
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

    /**
     * Whether [moduleUrl] is a BitChord addon. The pair holds the URL that was
     * probed and the answer; a null answer is a real answer ("not an addon").
     * A positive answer stands for the life of the setting; a negative one is
     * revisited after [ADDON_PROBE_RETRY_MS], because a source that was offline
     * when it was probed may be back, and re-probing every track would make a
     * direct resolver pay for the look every time.
     */
    @Volatile
    private var addonProbe: Pair<String, AddonSource?>? = null

    @Volatile
    private var addonProbeAt: Long = 0L

    /** Bounded so a long queue cannot grow it without limit. */
    private val cache = LinkedHashMap<String, Entry>()

    private data class Entry(val stream: LosslessStream, val expiresAt: Long)

    /** A source's answer: a master, or the reason there is none. */
    private data class Answer(val stream: LosslessStream?, val failure: String? = null)

    /** One row from an addon's search, and what it takes to turn it into a stream. */
    private data class AddonTrack(
        val id: String?,
        val streamUrl: String?,
        val format: String?,
    )

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
        addonProbe = null
        addonProbeAt = 0L
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
            val track = title?.takeIf { it.isNotBlank() } ?: videoId
            val base = moduleUrl
            if (base == null) {
                // Not an error: this is the shipped shape of the tier until a
                // source is configured. Say so, so "Lossless" is never read as
                // a promise the stream did not keep.
                record(track, "No source configured")
                return@withContext null
            }
            cached(videoId)?.let {
                record(track, "${it.badge} (cached)")
                return@withContext it
            }

            val addon = addonFor(base)
            val answer =
                if (addon != null) {
                    Timber.tag(TAG).d("Lossless source is a BitChord addon at ${addon.baseUrl}")
                    resolveAddon(addon, videoId, title, artist, durationSeconds)
                } else {
                    resolveDirect(base, videoId, title, artist, durationSeconds)
                }

            val stream = answer.stream
            if (stream == null) {
                record(track, answer.failure ?: "The source had no master for this track")
                return@withContext null
            }

            remember(videoId, stream)
            record(track, stream.badge)
            Timber.tag(TAG).d("Lossless master for $videoId: ${stream.badge}")
            stream
        }

    private fun record(track: String, outcome: String) {
        _lastCheck.value = Check(track, outcome)
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

    // ── Direct resolver ──────────────────────────────────────────────────────

    private fun resolveDirect(
        base: String,
        videoId: String,
        title: String?,
        artist: String?,
        durationSeconds: Int,
    ): Answer {
        val endpoint =
            endpointFor(base, videoId, title, artist, durationSeconds)
                ?: return Answer(null, "The source URL is not valid")
        return runCatching {
            client.newCall(Request.Builder().url(endpoint).header("Accept", "application/json").build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        Timber.tag(TAG).d("Lossless source answered ${response.code} for $videoId")
                        return@use Answer(null, "The source answered ${response.code}")
                    }
                    parse(response.body?.string().orEmpty())
                        ?.let { Answer(it) }
                        ?: Answer(null, "The source had no master for this track")
                }
        }
            .onFailure {
                Timber.tag(TAG).w(it, "Lossless source failed for $videoId")
            }
            .getOrElse { Answer(null, "The source could not be reached") }
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

    // ── BitChord addon ───────────────────────────────────────────────────────

    /**
     * The addon at [base], or null when the URL is a direct resolver.
     *
     * Detection costs one request with a short budget. An addon answers its
     * manifest; a direct resolver answers anything else, including a 404, and
     * that answer is cached so later tracks go straight to the resolver. A
     * negative answer is revisited after [ADDON_PROBE_RETRY_MS] rather than
     * never, since a source that was offline when the probe ran may be back.
     */
    private fun addonFor(base: String): AddonSource? {
        addonProbe?.let { (probed, found) ->
            if (probed == base) {
                if (found != null || System.currentTimeMillis() - addonProbeAt < ADDON_PROBE_RETRY_MS) {
                    return found
                }
            }
        }

        val addonBase = addonBaseOf(base) ?: return null
        val manifest =
            runCatching {
                probeClient.newCall(
                    Request.Builder()
                        .url("$addonBase/manifest.json")
                        .header("Accept", "application/json")
                        .build(),
                ).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    parseObject(response.body?.string().orEmpty())
                }
            }.getOrNull()
        val source = manifest?.let { parseManifest(addonBase, it) }
        addonProbe = base to source
        addonProbeAt = System.currentTimeMillis()
        return source
    }

    /**
     * The addon's base URL, accepting the same spellings BitChord does: a bare
     * base, `…/manifest.json`, or any `…/name.json` manifest document.
     */
    private fun addonBaseOf(base: String): String? {
        // A placeholder belongs to a direct resolver's URL template, and a
        // query belongs to a direct resolver's key. Neither is a manifest base.
        val trimmed = base.trim().substringBefore('#').substringBefore('?').trimEnd('/')
        if (trimmed.isEmpty() || "{" in trimmed) return null
        val withoutManifest =
            when {
                trimmed.endsWith("/manifest.json", ignoreCase = true) ->
                    trimmed.dropLast("/manifest.json".length)
                trimmed.endsWith(".json", ignoreCase = true) ->
                    trimmed.substringBeforeLast('/')
                else -> trimmed
            }.trimEnd('/')
        return withoutManifest.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true)
        }
    }

    private fun parseManifest(baseUrl: String, manifest: JsonObject): AddonSource? {
        if (manifest.string("id").isNullOrBlank()) return null
        val resources =
            (manifest["resources"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .orEmpty()
        // BitChord treats an empty resource list as "ask the endpoints", and
        // requires only `search` when the manifest names anything. Follow it:
        // refusing a manifest for not repeating the obvious would lock out the
        // addons that rely on the benefit of the doubt.
        if (resources.isNotEmpty() && "search" !in resources) return null
        return AddonSource(baseUrl, settingsFor(manifest))
    }

    /**
     * The parameters an addon expects on every call. Manifest defaults come
     * first, then `quality` is replaced by the option that names lossless:
     * an addon that lists its tiers worst-first is still asked for the top of
     * the ladder, and one that enumerates nothing is asked for `lossless`.
     */
    private fun settingsFor(manifest: JsonObject): Map<String, String> {
        val settings = manifest["settings"] as? JsonArray ?: return defaultQualityParams()
        val params = LinkedHashMap<String, String>()
        val qualityOptions = mutableListOf<String>()

        settings.mapNotNull { it as? JsonObject }.forEach { setting ->
            val key = setting.string("key")?.takeIf { it.isNotBlank() } ?: return@forEach
            setting.string("default")?.let { params[key] = it }
            if (key == "quality") {
                (setting["options"] as? JsonArray)
                    ?.mapNotNull { option ->
                        val obj = option as? JsonObject
                        obj?.string("value") ?: obj?.string("label")
                    }
                    ?.let(qualityOptions::addAll)
            }
        }

        val chosen =
            qualityOptions.firstOrNull { LOSSLESS_QUALITY_HINT.containsMatchIn(it) }
                ?: qualityOptions.firstOrNull()
                ?: LOSSLESS_QUALITY
        params["quality"] = chosen
        return params
    }

    private fun defaultQualityParams(): Map<String, String> =
        mapOf("quality" to LOSSLESS_QUALITY)

    private fun resolveAddon(
        addon: AddonSource,
        videoId: String,
        title: String?,
        artist: String?,
        durationSeconds: Int,
    ): Answer {
        val query =
            listOfNotNull(
                title?.takeIf { it.isNotBlank() },
                artist?.takeIf { it.isNotBlank() },
            ).joinToString(" ").ifBlank { videoId }

        val searchUrl =
            addonEndpoint(addon, listOf("search"), query = mapOf("q" to query))
                ?: return Answer(null, "The source URL is not valid")

        return runCatching {
            client.newCall(Request.Builder().url(searchUrl).header("Accept", "application/json").build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        Timber.tag(TAG).d("Addon search answered ${response.code} for $videoId")
                        return@use Answer(null, "The source answered ${response.code}")
                    }
                    val track =
                        bestTrack(response.body?.string().orEmpty(), title, artist, durationSeconds)
                            ?: return@use Answer(null, "The addon has no match for this track")
                    streamForTrack(addon, track)
                }
        }
            .onFailure {
                Timber.tag(TAG).w(it, "Addon search failed for $videoId")
            }
            .getOrElse { Answer(null, "The source could not be reached") }
    }

    /** `{base}/{segments}?{settings}&{query}`, with every part encoded. */
    private fun addonEndpoint(
        addon: AddonSource,
        segments: List<String>,
        query: Map<String, String> = emptyMap(),
    ): HttpUrl? {
        val builder = addon.baseUrl.toHttpUrlOrNull()?.newBuilder() ?: return null
        segments.forEach { builder.addPathSegment(it) }
        addon.params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }

    /**
     * The search row that best answers the track: exact titles first, then a
     * title one side contains, with artist and runtime breaking the tie.
     */
    private fun bestTrack(
        body: String,
        title: String?,
        artist: String?,
        durationSeconds: Int,
    ): AddonTrack? {
        val root = parseObject(body) ?: return null
        val rows =
            (root["tracks"] as? JsonArray)
                ?: ((root["data"] as? JsonObject)?.get("tracks") as? JsonArray)
                ?: return null
        return rows
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .map { row ->
                trackScore(
                    trackTitle = row.string("title").orEmpty(),
                    trackArtist = row.string("artist").orEmpty(),
                    trackDuration = row.int("duration")?.takeIf { it > 0 },
                    title = title,
                    artist = artist,
                    durationSeconds = durationSeconds,
                ) to row
            }
            .filter { it.first >= MIN_TRACK_SCORE }
            .maxByOrNull { it.first }
            ?.second
            ?.let { row ->
                AddonTrack(
                    id = row.string("id"),
                    streamUrl = row.string("streamURL", "streamUrl", "stream_url"),
                    format = row.string("format", "audioQuality"),
                )
            }
    }

    private fun trackScore(
        trackTitle: String,
        trackArtist: String,
        trackDuration: Int?,
        title: String?,
        artist: String?,
        durationSeconds: Int,
    ): Int {
        val wantedTitle = normalise(title)
        val foundTitle = normalise(trackTitle)
        if (wantedTitle.isEmpty() || foundTitle.isEmpty()) return 0
        val titleScore =
            when {
                foundTitle == wantedTitle -> 100
                foundTitle.contains(wantedTitle) || wantedTitle.contains(foundTitle) -> 60
                else -> return 0
            }
        val wantedArtist = normalise(artist)
        val foundArtist = normalise(trackArtist)
        val artistScore =
            if (
                wantedArtist.isNotEmpty() &&
                foundArtist.isNotEmpty() &&
                (foundArtist.contains(wantedArtist) || wantedArtist.contains(foundArtist))
            ) {
                40
            } else {
                0
            }
        val durationScore = trackDuration?.let {
            when (kotlin.math.abs(it - durationSeconds)) {
                in 0..3 -> 30
                in 4..10 -> 20
                in 11..30 -> 10
                else -> 0
            }
        } ?: 0
        return titleScore + artistScore + durationScore
    }

    /**
     * Comparing titles and artists needs their words, not their punctuation:
     * `Song (Remastered)` and `Song` have to meet, while a title in a script
     * without ASCII letters must not be reduced to nothing.
     */
    private fun normalise(value: String?): String =
        value.orEmpty().lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun streamForTrack(addon: AddonSource, track: AddonTrack): Answer {
        val direct = track.streamUrl?.let { absoluteUrl(addon.baseUrl, it) }
        if (direct != null) {
            return Answer(
                LosslessStream(
                    url = direct,
                    codec = codecFor(track.format),
                    expiresInSeconds = LOSSLESS_ADDON_EXPIRY_SECONDS,
                ),
            )
        }

        val id = track.id?.takeIf { it.isNotBlank() }
            ?: return Answer(null, "The addon returned a track without an id")
        val streamUrl =
            addonEndpoint(addon, listOf("stream", id))
                ?: return Answer(null, "The source URL is not valid")

        return runCatching {
            client.newCall(Request.Builder().url(streamUrl).header("Accept", "application/json").build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        Timber.tag(TAG).d("Addon stream answered ${response.code} for $id")
                        return@use Answer(null, "The source answered ${response.code}")
                    }
                    parse(
                        response.body?.string().orEmpty(),
                        defaultExpirySeconds = LOSSLESS_ADDON_EXPIRY_SECONDS,
                    )
                        ?.let { Answer(it) }
                        ?: Answer(null, "The addon returned no stream for this track")
                }
        }
            .onFailure {
                Timber.tag(TAG).w(it, "Addon stream failed for $id")
            }
            .getOrElse { Answer(null, "The source could not be reached") }
    }

    private fun absoluteUrl(base: String, url: String): String? {
        val value = url.trim()
        if (value.isEmpty()) return null
        if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            return value
        }
        return base.toHttpUrlOrNull()?.resolve(value)?.toString()
    }

    // ── Response parsing ─────────────────────────────────────────────────────

    /**
     * Reads the source's answer, tolerating the handful of shapes modules and
     * addons actually use: a flat object, or one nested under
     * `stream`/`data`/`track`. Metadata may sit in a different object from the
     * URL — BitChord's `track` descriptor is the common case — so every field
     * is looked up across all of them.
     */
    private fun parse(
        body: String,
        defaultExpirySeconds: Int = LOSSLESS_DEFAULT_EXPIRY_SECONDS,
    ): LosslessStream? {
        if (body.isBlank()) return null
        val root = parseObject(body) ?: return null
        val payload = payloadOf(root) ?: return null
        val url = payload.string("url", "streamUrl", "stream_url") ?: return null
        if (!url.startsWith("http", ignoreCase = true)) return null

        val objects = metadataObjects(root, payload)
        val sampleRate = objects.firstNotNullOfOrNull {
            it.int("sampleRate", "sampleRateHz", "sample_rate")
        }
        return LosslessStream(
            url = url,
            codec = codecFor(
                objects.firstNotNullOfOrNull { it.string("codec", "fileCodec") },
                objects.firstNotNullOfOrNull { it.string("format", "type") },
            ),
            bitDepth = objects.firstNotNullOfOrNull {
                it.int("bitDepth", "bit_depth", "bitsPerSample")
            } ?: 0,
            sampleRateHz = sampleRate?.let(::normaliseSampleRate) ?: 0,
            bitrate = objects.firstNotNullOfOrNull { it.int("bitrate", "bitRate") } ?: 0,
            expiresInSeconds = objects.firstNotNullOfOrNull {
                it.int("expiresInSeconds", "expiresIn", "expires_in")
            } ?: defaultExpirySeconds,
        )
    }

    private fun parseObject(body: String): JsonObject? =
        runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()

    private fun payloadOf(root: JsonObject): JsonObject? {
        if (root.string("url", "streamUrl", "stream_url") != null) return root
        return NESTED.firstNotNullOfOrNull { key ->
            (root[key] as? JsonObject)?.takeIf { it.string("url", "streamUrl", "stream_url") != null }
        }
    }

    /** The object holding the URL first, then every other object that may describe it. */
    private fun metadataObjects(root: JsonObject, payload: JsonObject): List<JsonObject> =
        buildList {
            add(payload)
            if (payload !== root) add(root)
            NESTED.forEach { key ->
                (root[key] as? JsonObject)?.let { if (it !== payload) add(it) }
            }
        }

    /**
     * Some addons report a sample rate in kHz (`96`) rather than hertz
     * (`96000`). No audio at 96 Hz exists, so a small number can be read as kHz
     * without guessing wrong on a real one.
     */
    private fun normaliseSampleRate(value: Int): Int =
        if (value in 1..500) value * 1000 else value

    private fun codecFor(vararg hints: String?): String =
        hints.firstNotNullOfOrNull { hint ->
            KNOWN_CODECS.firstOrNull { codec -> hint?.contains(codec, ignoreCase = true) == true }
        } ?: DEFAULT_CODEC

    private fun JsonObject.string(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private fun JsonObject.int(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key ->
            val raw = (this[key] as? JsonPrimitive)?.contentOrNull ?: return@firstNotNullOfOrNull null
            raw.toIntOrNull() ?: raw.toDoubleOrNull()?.toInt()
        }

    private val NESTED = listOf("stream", "data", "track")

    /** A row needs a real title match; artist and runtime only order the rest. */
    private const val MIN_TRACK_SCORE = 60

    private const val TAG = "LosslessSource"

    private const val MAX_CACHED = 48
}
