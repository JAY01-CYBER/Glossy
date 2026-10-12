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

/**
 * What one probe of a canvas URL learned.
 *
 * The size is reported because a canvas is a *loop*: it is read in full, over
 * and over, and a clip that is already on disk wraps around without touching
 * the network. Whoever is about to cache it has to know how big it is before it
 * decides — see `CanvasPrefetcher`.
 */
data class CanvasProbe(
    val health: CanvasUrlHealth,
    /** The clip's size in bytes, when the server says; null when it does not. */
    val contentLengthBytes: Long? = null,
)

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

    /**
     * How long a verdict is trusted before the URL is looked at again.
     *
     * "Will not play" was trusted for half an hour, which was long enough to
     * outlive the reason it was written: a clip that failed once — a CDN
     * hiccup, a rate limit, a surface that was being torn down — was refused by
     * every surface for the rest of the session's attention span. Five minutes
     * is cheap to re-learn and quick to forgive.
     */
    private const val PlayableTtlMillis = 6L * 60L * 60L * 1000L
    private const val UnplayableTtlMillis = 5L * 60L * 1000L
    private const val HealthLimit = 256

    /** Short enough that a background probe never piles up. */
    private const val ProbeConnectTimeoutMillis = 3_000
    private const val ProbeReadTimeoutMillis = 4_000

    private class HealthEntry(
        val health: CanvasUrlHealth,
        val expiresAtMillis: Long,
        /**
         * The clip's size, when a probe learned it.
         *
         * Kept beside the verdict because the two arrive together: the answer
         * "this URL plays" short-circuits the next probe, and the preloader
         * still has to know how big the clip is before it may cache it whole.
         */
        val contentLengthBytes: Long? = null,
    )

    private val healthCache = Collections.synchronizedMap(LinkedHashMap<String, HealthEntry>())

    /**
     * The verdict for one provider's answer.
     *
     * @param songTitle the normalized title the provider was asked for.
     * @param artistName the normalized artist it was asked for.
     * @param alternativeTitle a second name the clip may legitimately carry —
     *   the album, for a provider that resolves animated artwork at album
     *   level. An answer named after the album is not the wrong song; refusing
     *   it was why that whole provider never painted anything.
     */
    fun match(
        songTitle: String,
        artistName: String,
        answerTitle: String?,
        answerArtist: String?,
        alternativeTitle: String? = null,
        rawSongTitle: String = songTitle,
    ): CanvasMatch {
        val answerName = answerTitle.orEmpty().trim()
        val answerBy = answerArtist.orEmpty().trim()
        // Nothing to judge: a provider may return only a URL. Accept it, and
        // say so honestly rather than pretending it was verified.
        if (answerName.isBlank() && answerBy.isBlank()) return CanvasMatch.UNVERIFIED

        if (answerName.isNotBlank()) {
            // A clip branded as a different mix or edit of the song — a remix, a
            // slowed reverb, a mashup with another song — is a different
            // recording wearing this song's name. It is compared against the
            // *raw* title, because normalization strips "(Remix)" and would make
            // "Kalyani" and "Kalyani Remix" indistinguishable. An answer that
            // carries a version tag the song's own title does not is the wrong
            // clip, however much the base name overlaps.
            if (isForeignVersion(answerName, rawSongTitle)) return CanvasMatch.MISMATCH

            val normalizedAnswer = normalizeCanvasSongTitle(answerName)
            val titleScore = similarity(songTitle, normalizedAnswer)
            val alternativeScore =
                alternativeTitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { similarity(normalizeCanvasSongTitle(it), normalizedAnswer) }
                    ?: 0f
            if (titleScore < TitleMatchThreshold && alternativeScore < TitleMatchThreshold) {
                return CanvasMatch.MISMATCH
            }
        }

        if (answerBy.isNotBlank() && artistName.isNotBlank()) {
            val artistScore = similarity(artistName, normalizeCanvasArtistName(answerBy))
            if (artistScore < ArtistMatchThreshold) return CanvasMatch.MISMATCH
        }
        return CanvasMatch.MATCHED
    }

    /**
     * Whether the answer is a *different version* of the song — a remix, an
     * edit, a slowed/sped reverb, a mashup — rather than the song itself.
     *
     * This is the check that normalization cannot make: `normalizeCanvasSongTitle`
     * strips "(Remix)", "(Edit)" and "(Mix)" so that "Kalyani" and
     * "Kalyani Remix" normalize to the same string, and the similarity score
     * then calls them a match. That is exactly how "Kalyani" ended up showing
     * "Kalyani Remix"'s canvas, and how a request for "Kalyani Remix" pulled a
     * "Kalyani x <other song>" edit off YouTube.
     *
     * It is judged on the *raw* titles, and it is symmetric: an answer is a
     * foreign version only when it carries a version marker the song's own title
     * does not. So a track genuinely called "Kalyani Remix" still matches its own
     * remix canvas (both carry "remix"), while the plain "Kalyani" refuses a
     * clip branded "Kalyani (Remix)".
     *
     * "Live" is deliberately not a version marker here: the similarity step is
     * built to accept "Song" and "Song (Live at Home)" as the same song, and
     * overruling that would refuse a live clip for the studio track.
     */
    private fun isForeignVersion(answerTitle: String, rawSongTitle: String): Boolean {
        val answerTags = versionTagMarkers.filter { it.containsMatchIn(answerTitle) }
        val songTags = versionTagMarkers.filter { it.containsMatchIn(rawSongTitle) }
        if (answerTags.any { it !in songTags }) return true
        // A clip that mashes two songs together is never either one's canvas.
        if (isMashupTitle(answerTitle) && !isMashupTitle(rawSongTitle)) return true
        return false
    }

    /**
     * Markers that make a clip a different mix or edit of a song. Kept apart
     * from the `notCanvasMarkers` list in the YouTube provider because this one
     * is a provider-agnostic backstop: Spotify, Tidal and Apple canvases are
     * matched here too, and every provider's answer for the wrong variant must
     * be refused the same way.
     */
    private val versionTagMarkers =
        listOf(
            """\bremix(es)?\b""",
            """\bmix(es)?\b""",
            """\bedits?\b""",
            """\bmashups?\b""",
            """\bmedley\b""",
            """\bslowed\b""",
            """\breverb\b""",
            """\bsped\s*-?\s*up\b""",
            """\bnightcore\b""",
            """\bbass\s*-?\s*boost(ed)?\b""",
            """\blo\s*-?\s*fi\b""",
            """\b8d\b""",
            """\bbootleg\b""",
            """\brework\b""",
            """\bflip\b""",
            """\bkaraoke\b""",
            """\binstrumental\b""",
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

    /** A title that joins two songs with " x ", "×" or " vs ". */
    private fun isMashupTitle(title: String): Boolean =
        Regex("""\s+[x×]\s+|\s+vs\.?\s+""", RegexOption.IGNORE_CASE).containsMatchIn(title)

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
        remember(url, health, null)
    }

    /** The clip's size, if some probe already learned it. Never a network call. */
    fun contentLength(url: String?): Long? {
        val key = url?.trim().orEmpty()
        if (key.isEmpty()) return null
        synchronized(healthCache) {
            val entry = healthCache[key]
            if (entry == null || entry.expiresAtMillis <= System.currentTimeMillis()) return null
            return entry.contentLengthBytes
        }
    }

    private fun remember(
        url: String?,
        health: CanvasUrlHealth,
        contentLengthBytes: Long?,
    ) {
        val key = url?.trim().orEmpty()
        if (key.isEmpty() || health == CanvasUrlHealth.UNKNOWN) return
        val ttl = if (health == CanvasUrlHealth.PLAYABLE) PlayableTtlMillis else UnplayableTtlMillis
        synchronized(healthCache) {
            if (healthCache.size >= HealthLimit) healthCache.clear()
            // A verdict learned from playback carries no size of its own, so an
            // earlier probe's size is carried over rather than wiped: the
            // preloader decides whether it may cache a clip whole on it.
            val knownSize = contentLengthBytes ?: healthCache[key]?.contentLengthBytes
            healthCache[key] = HealthEntry(health, System.currentTimeMillis() + ttl, knownSize)
        }
    }

    /**
     * Fetches a URL to see whether it will play. Runs off the UI path, by the
     * preloader, and answers [CanvasUrlHealth.UNKNOWN] for anything it could not
     * decide — a timeout says nothing about the clip, and pretending it did
     * would reject a perfectly good canvas because the network was briefly
     * busy.
     */
    suspend fun probe(url: String): CanvasProbe = withContext(Dispatchers.IO) {
        val known = health(url)
        if (known != CanvasUrlHealth.UNKNOWN) {
            // Already decided: reuse both halves of the answer instead of asking
            // the network again, and keep the size a verdict-only call would
            // otherwise drop.
            return@withContext CanvasProbe(known, contentLength(url))
        }

        val probed = headRequest(url) ?: rangedGetRequest(url)
        remember(url, probed.health, probed.contentLengthBytes)
        probed
    }

    /**
     * A HEAD answer when the server gives one. A CDN that refuses HEAD (403,
     * 405, 501) is not a clip that will not play, so that answers null and the
     * ranged GET below decides.
     */
    private fun headRequest(url: String): CanvasProbe? {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(url, "HEAD")
            val code = connection.responseCode
            val contentType = connection.contentType.orEmpty()
            when {
                code in 200..399 ->
                    if (contentType.startsWith("text/html", ignoreCase = true)) {
                        CanvasProbe(CanvasUrlHealth.UNPLAYABLE)
                    } else {
                        CanvasProbe(
                            CanvasUrlHealth.PLAYABLE,
                            connection.contentLengthLong.takeIf { it > 0L },
                        )
                    }

                // A definitive "no such clip".
                code == HttpURLConnection.HTTP_NOT_FOUND ||
                    code == HttpURLConnection.HTTP_GONE -> CanvasProbe(CanvasUrlHealth.UNPLAYABLE)

                // Everything else a server can answer — 403 from a CDN that
                // dislikes the client, 405 from one that refuses HEAD, 408, 429,
                // any 5xx — is the server talking about the request, not about
                // the clip. It falls through to the ranged GET instead of
                // condemning a URL that plays perfectly well.
                else -> null
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    /** One byte of the clip — the smallest request that proves it is served. */
    private fun rangedGetRequest(url: String): CanvasProbe {
        var connection: HttpURLConnection? = null
        return try {
            connection =
                open(url, "GET").apply {
                    setRequestProperty("Range", "bytes=0-0")
                }
            val code = connection.responseCode
            val contentType = connection.contentType.orEmpty()
            when {
                code in 200..399 ->
                    if (contentType.startsWith("text/html", ignoreCase = true)) {
                        CanvasProbe(CanvasUrlHealth.UNPLAYABLE)
                    } else {
                        // A 206 answers with `Content-Range: bytes 0-0/<total>`,
                        // which is the only place the total is stated when the
                        // server refuses HEAD. A 200 to a ranged request states
                        // it the ordinary way.
                        CanvasProbe(
                            CanvasUrlHealth.PLAYABLE,
                            totalFromContentRange(connection.getHeaderField("Content-Range"))
                                ?: connection.contentLengthLong.takeIf { it > 0L },
                        )
                    }

                code == HttpURLConnection.HTTP_NOT_FOUND ||
                    code == HttpURLConnection.HTTP_GONE -> CanvasProbe(CanvasUrlHealth.UNPLAYABLE)

                // A refusal, a rate limit or a server error is not evidence
                // about the clip. The player learns the truth the moment it
                // actually feeds the URL to a decoder.
                else -> CanvasProbe(CanvasUrlHealth.UNKNOWN)
            }
        } catch (e: Exception) {
            // The request never completed: unknown, not unplayable.
            CanvasProbe(CanvasUrlHealth.UNKNOWN)
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    /** `bytes 0-0/1234` → 1234; null for an unknown or absent total. */
    private fun totalFromContentRange(header: String?): Long? =
        header
            ?.substringAfter('/', missingDelimiterValue = "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it != "*" }
            ?.toLongOrNull()
            ?.takeIf { it > 0L }

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

    /**
     * Records that a lookup's clip was refused *after* it was accepted — the
     * preloader probed the URL in the background and it would not play, so the
     * clip was dropped before any surface could show it.
     *
     * Without this the report kept saying a canvas was "shown from Tidal"
     * while nothing on screen ever showed one: the lookup had succeeded, and
     * the thing that took the canvas away happened somewhere the report could
     * not see. The row now says it found nothing, and the detail says why.
     */
    fun noteUrlRefused(mediaId: String, provider: String = "Probe") {
        _lookups.update { previous ->
            val index = previous.indexOfFirst { it.mediaId == mediaId }
            if (index < 0) return@update previous
            val lookup = previous[index]
            if (lookup.answers.any { it.provider == provider && it.verdict == Verdict.DEAD_URL }) {
                return@update previous
            }
            val updated =
                lookup.copy(
                    health = CanvasUrlHealth.UNPLAYABLE,
                    answers = lookup.answers + Answer(provider, Verdict.DEAD_URL),
                    chosenProvider = null,
                )
            previous.toMutableList().also { it[index] = updated }
        }
    }

    fun clear() {
        _lookups.update { emptyList() }
    }
}
