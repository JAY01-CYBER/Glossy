/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The canvas check.
 *
 * A canvas is four providers being raced and the first URL that comes back
 * winning, which is fast but credulous: nothing in that path asked whether the
 * clip it just accepted had anything to do with the song on screen. A provider
 * matching on a title alone hands back a cover, a remix or a same-named track
 * by someone else, and the result is somebody else's animated cover playing
 * under this song's title — and on a dead URL, a canvas that never draws a
 * frame while the still artwork has already faded out for it.
 *
 * This is the one place that answers both questions, for every surface a canvas
 * appears on: the player designs, the Apple Music cover and the Featured
 * Spotlight carousel all resolve through [CanvasResolver], and every answer it
 * collects passes through here before it can be shown.
 *
 *  - **Does it belong to this song?** [match] compares the provider's own
 *    `name` / `artist` for the clip with the song that was asked for, using the
 *    same normalization the providers already query with. Only a *disagreement*
 *    is a rejection: an answer that carries no metadata at all cannot be judged
 *    and is let through as [CanvasMatch.UNVERIFIED] rather than dropped, so a
 *    provider that simply does not echo names keeps working.
 *
 *    The album is deliberately not part of the verdict. The same recording is
 *    released under a single, an album and a compilation with three different
 *    names, so an album mismatch says nothing about the clip; the diagostics
 *    line reports it, the verdict ignores it.
 *
 *  - **Will it play?** A URL that is known dead is refused instantly, from
 *    [health], which costs the playback path nothing. The verdict is *learned*
 *    off the UI path: [probe] checks a URL in the background (the preloader
 *    does this while it is warming the clip anyway), and the player itself
 *    reports back when it runs out of retries, which is the most honest check
 *    available — the clip was actually fed to a decoder and refused to draw.
 *    Nothing on screen ever waits for a network round trip to find out.
 *
 * Rejections are not failures: a refused answer means "that provider had
 * nothing for this song", which is exactly what a provider that answered
 * nothing means. The resolver keeps asking the others — see
 * `CanvasResolver.raceAllProviders`.
 */

package com.jay.glossy.ui.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.Locale

/** How one provider's answer scored against the song that was asked for. */
internal enum class CanvasMatch {
    /** The clip's own name and artist agree with the song. */
    MATCHED,

    /**
     * The answer carries no name or artist to judge it by, so it is accepted
     * unjudged: refusing it would break a provider that does not echo names.
     */
    UNVERIFIED,

    /** The answer plainly belongs to another song. */
    MISMATCH,
}

/** Whether a canvas URL is known to play. */
enum class CanvasUrlHealth {
    /** Nothing has been learned about this URL yet. */
    UNKNOWN,

    /** A probe, or an actual playback, says this URL serves a video. */
    PLAYABLE,

    /** The URL 404s, redirects into an error page, or refused to decode. */
    UNPLAYABLE,
}

internal object CanvasVerifier {
    /**
     * How alike two names must be before the clip is taken to be this song's.
     * Both numbers are on a token-overlap scale where identical names score 1.0
     * and two-word names sharing one word score ~0.67.
     *
     * The artist bar is the lower of the two, because that is where honest
     * differences live: a `feat.` credit that only one side carries, a name the
     * service spells differently, a producer credited on one release and not the
     * other. It cannot be dropped altogether, though — a cover, a remix or a
     * karaoke track shares the title *exactly* and differs only in the artist,
     * so a title match alone would wave through precisely the wrong clip this
     * check exists to refuse.
     */
    private const val TitleMatchThreshold = 0.62f
    private const val ArtistMatchThreshold = 0.4f

    /** Words that carry no identity, dropped before two names are compared. */
    private val MatchStopWords =
        setOf(
            "the", "a", "an", "of", "and", "to", "in", "on", "for", "with",
            "feat", "ft", "featuring", "prod", "by", "official", "video",
            "audio", "lyrics", "visualizer",
        )

    /** How long a verdict is trusted before the URL is looked at again. */
    private const val PlayableTtlMillis = 6L * 60L * 60L * 1000L
    private const val UnplayableTtlMillis = 30L * 60L * 1000L
    private const val HealthLimit = 256

    /** Short enough that a background probe never piles up. */
    private const val ProbeConnectTimeoutMillis = 3_000
    private const val ProbeReadTimeoutMillis = 4_000

    private class HealthEntry(val health: CanvasUrlHealth, val expiresAtMillis: Long)

    private val healthCache = Collections.synchronizedMap(LinkedHashMap<String, HealthEntry>())

    /**
     * The verdict for one provider's answer.
     *
     * @param songTitle the normalized title the provider was asked for.
     * @param artistName the normalized artist it was asked for.
     */
    fun match(
        songTitle: String,
        artistName: String,
        answerTitle: String?,
        answerArtist: String?,
    ): CanvasMatch {
        val answerName = answerTitle.orEmpty().trim()
        val answerBy = answerArtist.orEmpty().trim()
        // Nothing to judge: a provider may return only a URL. Accept it, and
        // say so honestly rather than pretending it was verified.
        if (answerName.isBlank() && answerBy.isBlank()) return CanvasMatch.UNVERIFIED

        val titleScore = similarity(songTitle, normalizeCanvasSongTitle(answerName))
        if (answerName.isNotBlank() && titleScore < TitleMatchThreshold) return CanvasMatch.MISMATCH

        if (answerBy.isNotBlank() && artistName.isNotBlank()) {
            val artistScore = similarity(artistName, normalizeCanvasArtistName(answerBy))
            if (artistScore < ArtistMatchThreshold) return CanvasMatch.MISMATCH
        }
        return CanvasMatch.MATCHED
    }

    /**
     * How much two names look like the same thing, 0..1.
     *
     * Token overlap (Sørensen–Dice) rather than a prefix test, so word order,
     * punctuation and an extra credit do not decide the answer. One name
     * containing the other scores high even when the token sets differ — which
     * is the "Song" vs "Song (Live at Home)" case that normalization alone does
     * not strip.
     */
    private fun similarity(a: String, b: String): Float {
        val left = a.lowercase(Locale.ROOT).trim()
        val right = b.lowercase(Locale.ROOT).trim()
        if (left.isEmpty() || right.isEmpty()) return 0f
        if (left == right) return 1f

        val leftTokens = tokens(left)
        val rightTokens = tokens(right)
        val contained = left.contains(right) || right.contains(left)

        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return if (contained) 0.9f else 0f

        val shared = leftTokens.count { it in rightTokens }
        val dice = 2f * shared / (leftTokens.size + rightTokens.size)
        return if (contained) maxOf(dice, 0.9f) else dice
    }

    private fun tokens(raw: String): Set<String> =
        raw
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() && it !in MatchStopWords }
            .toSet()

    /** What is known about this URL right now, without touching the network. */
    fun health(url: String?): CanvasUrlHealth {
        val key = url?.trim().orEmpty()
        if (key.isEmpty()) return CanvasUrlHealth.UNKNOWN
        synchronized(healthCache) {
            val entry = healthCache[key] ?: return CanvasUrlHealth.UNKNOWN
            if (entry.expiresAtMillis <= System.currentTimeMillis()) {
                healthCache.remove(key)
                return CanvasUrlHealth.UNKNOWN
            }
            return entry.health
        }
    }

    /** Records a verdict learned elsewhere — the player's own retry budget. */
    fun rememberHealth(url: String?, health: CanvasUrlHealth) {
        val key = url?.trim().orEmpty()
        if (key.isEmpty() || health == CanvasUrlHealth.UNKNOWN) return
        val ttl = if (health == CanvasUrlHealth.PLAYABLE) PlayableTtlMillis else UnplayableTtlMillis
        synchronized(healthCache) {
            if (healthCache.size >= HealthLimit) healthCache.clear()
            healthCache[key] = HealthEntry(health, System.currentTimeMillis() + ttl)
        }
    }

    /**
     * Fetches a URL to see whether it will play. Runs off the UI path, by the
     * preloader, and answers [CanvasUrlHealth.UNKNOWN] for anything it could not
     * decide — a timeout says nothing about the clip, and pretending it did
     * would reject a perfectly good canvas because the network was briefly
     * busy.
     */
    suspend fun probe(url: String): CanvasUrlHealth = withContext(Dispatchers.IO) {
        val known = health(url)
        if (known != CanvasUrlHealth.UNKNOWN) return@withContext known

        val verdict = headRequest(url) ?: rangedGetRequest(url)
        rememberHealth(url, verdict)
        verdict
    }

    /**
     * A HEAD answer when the server gives one. A CDN that refuses HEAD (403,
     * 405, 501) is not a clip that will not play, so that answers null and the
     * ranged GET below decides.
     */
    private fun headRequest(url: String): CanvasUrlHealth? {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(url, "HEAD")
            val code = connection.responseCode
            val contentType = connection.contentType.orEmpty()
            when {
                code in 200..399 ->
                    if (contentType.startsWith("text/html", ignoreCase = true)) {
                        CanvasUrlHealth.UNPLAYABLE
                    } else {
                        CanvasUrlHealth.PLAYABLE
                    }

                code == HttpURLConnection.HTTP_FORBIDDEN ||
                    code == HttpURLConnection.HTTP_BAD_METHOD ||
                    code == HttpURLConnection.HTTP_NOT_IMPLEMENTED -> null

                else -> CanvasUrlHealth.UNPLAYABLE
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    /** One byte of the clip — the smallest request that proves it is served. */
    private fun rangedGetRequest(url: String): CanvasUrlHealth {
        var connection: HttpURLConnection? = null
        return try {
            connection =
                open(url, "GET").apply {
                    setRequestProperty("Range", "bytes=0-0")
                }
            val code = connection.responseCode
            val contentType = connection.contentType.orEmpty()
            if (code in 200..399 && !contentType.startsWith("text/html", ignoreCase = true)) {
                CanvasUrlHealth.PLAYABLE
            } else {
                CanvasUrlHealth.UNPLAYABLE
            }
        } catch (e: Exception) {
            // The request never completed: unknown, not unplayable.
            CanvasUrlHealth.UNKNOWN
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private fun open(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ProbeConnectTimeoutMillis
            readTimeout = ProbeReadTimeoutMillis
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("User-Agent", "Glossy/CanvasCheck")
        }
}

/**
 * What the canvas check did, most recent first, for the Settings row that
 * reports it.
 *
 * The point of keeping this is the question it answers without a debugger: when
 * a song shows no canvas, did the providers have nothing, did they answer with
 * something that was not this song's, or was the URL refused — and did the
 * player get its answer from the cache or from a lookup. Every surface resolves
 * through one place, so one list covers the player and the Spotlight carousel
 * alike.
 */
object CanvasDiagnostics {
    /** Why one provider's answer was or was not used. */
    enum class Verdict {
        /** Shown: the clip's own metadata agrees with the song. */
        MATCHED,

        /** Shown, unjudged: the provider echoed no names to check. */
        UNVERIFIED,

        /** Refused: the clip belongs to another song. */
        WRONG_SONG,

        /** Refused: the provider had nothing for this song. */
        EMPTY,

        /** Refused: the lookup itself failed (timeout, rate limit, offline). */
        FAILED,

        /** Refused: the URL is known not to play. */
        DEAD_URL,
    }

    data class Answer(
        val provider: String,
        val verdict: Verdict,
        /** What the provider claimed the clip was, when it said. */
        val claimed: String? = null,
    )

    data class Lookup(
        val mediaId: String,
        val title: String,
        val artist: String,
        val style: String,
        val atMillis: Long,
        val elapsedMillis: Long,
        /** True when the answer came from the playback cache, not a lookup. */
        val fromCache: Boolean,
        val answers: List<Answer>,
        val chosenProvider: String?,
        val url: String?,
        val health: CanvasUrlHealth,
    ) {
        /** The one who answered, or null when nobody had a canvas for the song. */
        val winner: Answer?
            get() =
                answers.firstOrNull {
                    it.provider == chosenProvider &&
                        (it.verdict == Verdict.MATCHED || it.verdict == Verdict.UNVERIFIED)
                }
    }

    /** How many lookups are kept for the report. */
    private const val Limit = 12

    private val _lookups = MutableStateFlow<List<Lookup>>(emptyList())

    /** Most recent lookup first. */
    val lookups: StateFlow<List<Lookup>> = _lookups.asStateFlow()

    fun record(lookup: Lookup) {
        _lookups.update { previous -> (listOf(lookup) + previous).take(Limit) }
    }

    fun clear() {
        _lookups.update { emptyList() }
    }
}
