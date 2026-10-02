/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Canvas resolver — picks providers according to the user's Canvas style:
 *  - ALL:          Race Spotify + ArchiveTune + Glossy (Tidal + Apple) in
 *                  parallel and show the first animated canvas that lands.
 *  - GLOSSY:       Tidal + Apple Music (original Glossy engine)
 *  - ARCHIVE_TUNE: BetterLyrics community service (ported from ArchiveTune)
 *  - BOTH:         BetterLyrics first, then Tidal + Apple Music as fallback
 *  - SPOTIFY:      Spotify web-player Canvas, Glossy fallback
 *
 * Results are cached per mediaId in CanvasArtworkPlaybackCache so switching
 * songs back and forth is instant. Also exposes prefetch() so the service and
 * the player can warm the cache before the user even opens the player.
 *
 * Every answer a provider gives passes the canvas check ([CanvasVerifier])
 * before it can be used — it has to belong to the song that was asked for, and
 * its URL must not already be known dead. A refused answer is treated exactly
 * like a provider that had nothing, so the others are still worth asking: this
 * is the difference between "the first thing that came back" and "the first
 * thing that came back *for this song*". What the check decided is recorded in
 * [CanvasDiagnostics], which is what the Settings row reports.
 */

package com.jay.glossy.ui.player

import android.content.Context
import com.jay.glossy.applecanvas.AppleMusicCanvasProvider
import com.jay.glossy.canvas.BetterLyricsCanvasProvider
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.canvas.TidalCanvasProvider
import com.jay.glossy.constants.CanvasStyle
import com.jay.glossy.constants.CanvasStyleKey
import com.jay.glossy.spotify.SpotifyAccessTokenExpiresAtKey
import com.jay.glossy.spotify.SpotifyCanvasProvider
import com.jay.glossy.spotify.SpotifySession
import com.jay.glossy.spotifycore.Spotify
import com.jay.glossy.utils.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale

object CanvasResolver {
    private val styleMutex = Mutex()

    /** How many "this song has no canvas" answers are remembered at once. */
    private const val DefinitiveMissLimit = 512

    /** How the report names the answer that came from the cache, not a lookup. */
    private const val CacheProvider = "Cache"

    private const val SpotifyProvider = "Spotify"
    private const val ArchiveTuneProvider = "ArchiveTune"
    private const val TidalProvider = "Tidal"
    private const val AppleProvider = "Apple Music"

    /**
     * Songs whose lookup was answered "no canvas" by every provider, with none
     * of them failing on the way.
     *
     * "Empty" and "failed" look identical to a caller holding only a null, and
     * they call for opposite reactions: the first is the truth about the song
     * and will not change if it is asked again, the second means the question
     * was never really answered. Whoever is about to retry can ask here first —
     * [wasDefinitiveMiss] — so a retry is spent on the flaky lookup and not on
     * the song that genuinely has no canvas.
     *
     * An answer that arrived and was refused — a clip for another song, a URL
     * already known dead — counts as answered, not as failed. The provider did
     * reply about this song; it simply had nothing usable, which is the same
     * final answer as having nothing at all.
     */
    private val definitiveMisses = Collections.synchronizedSet(mutableSetOf<String>())

    /** True when every provider positively answered that this song has no canvas. */
    fun wasDefinitiveMiss(mediaId: String): Boolean = mediaId in definitiveMisses

    private fun markDefinitiveMiss(mediaId: String) {
        if (mediaId.isBlank()) return
        if (definitiveMisses.size >= DefinitiveMissLimit) definitiveMisses.clear()
        definitiveMisses.add(mediaId)
    }

    /** One provider's outcome: an answer (possibly "no canvas"), or a failure. */
    private class NamedAnswer(val provider: String, val artwork: CanvasArtwork?, val failed: Boolean)

    /** The song a lookup is for, normalized once and passed around. */
    private class CanvasQuery(
        val context: Context,
        val mediaId: String,
        val title: String,
        val artist: String,
        val album: String,
        val storefront: String,
    )

    @Volatile
    private var cachedStyle: CanvasStyle? = null

    suspend fun currentStyle(context: Context): CanvasStyle {
        cachedStyle?.let { return it }
        return styleMutex.withLock {
            cachedStyle ?: context.dataStore.data
                .map { prefs ->
                    prefs[CanvasStyleKey]?.let { stored ->
                        CanvasStyle.entries.firstOrNull { it.name == stored }
                    } ?: CanvasStyle.ALL
                }
                .first()
                .also { cachedStyle = it }
        }
    }

    /** Invalidate the memoized style — call after the user changes the setting. */
    fun invalidateStyle() {
        cachedStyle = null
    }

    /**
     * Resolves animated canvas artwork for the given song, honoring the
     * selected CanvasStyle. Returns null when nothing is available.
     */
    suspend fun resolve(
        context: Context,
        mediaId: String,
        songTitle: String,
        artistName: String,
        albumName: String,
        storefront: String = defaultStorefront(),
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (songTitle.isBlank() || artistName.isBlank()) return@withContext null

        val style = currentStyle(context)

        val normalizedTitle = normalizeCanvasSongTitle(songTitle)
        val normalizedArtist = normalizeCanvasArtistName(artistName)
        if (normalizedTitle.isBlank() || normalizedArtist.isBlank()) return@withContext null

        val startedAt = System.currentTimeMillis()
        val answers = mutableListOf<CanvasDiagnostics.Answer>()
        // The application context is held only for the duration of one lookup:
        // the Spotify race needs it to mint a token, and an Activity that was
        // just closed should not be kept alive by a canvas lookup.
        val query =
            CanvasQuery(
                context = context.applicationContext,
                mediaId = mediaId,
                title = normalizedTitle,
                artist = normalizedArtist,
                album = albumName,
                storefront = storefront,
            )

        // Instant path: playback cache first — and still checked. A cached
        // entry can be a clip the song was never really a match for (written
        // before the check existed, or by a provider racing ahead of another),
        // or a URL that has stopped serving since. Rejected here it is dropped
        // and the lookup below gets its chance, instead of the wrong clip being
        // shown again for as long as it stays in the cache.
        CanvasArtworkPlaybackCache.get(mediaId)?.let { cached ->
            verify(query, CacheProvider, cached, answers)?.let { accepted ->
                report(query, style, startedAt, answers, accepted)
                return@withContext accepted
            }
            CanvasArtworkPlaybackCache.remove(mediaId)
        }

        val fetched: CanvasArtwork? =
            when (style) {
                CanvasStyle.ARCHIVE_TUNE -> fetchArchiveTune(query, answers)

                CanvasStyle.GLOSSY -> fetchGlossy(query, answers)

                CanvasStyle.BOTH ->
                    fetchArchiveTune(query, answers)
                        ?: fetchGlossy(query, answers)

                CanvasStyle.SPOTIFY -> {
                    val token = spotifyToken(context)
                    val spotify =
                        if (token == null) {
                            verify(query, SpotifyProvider, null, answers)
                        } else {
                            verify(
                                query,
                                SpotifyProvider,
                                SpotifyCanvasProvider.getBySongArtist(query.title, query.artist, token),
                                answers,
                            )
                        }
                    // Spotify rarely has canvases for every track; fall back to the
                    // Glossy engine so an animated canvas still shows instead of nothing.
                    spotify ?: fetchGlossy(query, answers)
                }

                CanvasStyle.ALL -> raceAllProviders(query, answers)
            }

        if (fetched != null) {
            CanvasArtworkPlaybackCache.put(mediaId, fetched)
        }
        report(query, style, startedAt, answers, fetched)
        fetched
    }

    /**
     * The canvas check, applied to one provider's answer.
     *
     * Returns the artwork when it may be shown, and null when it may not —
     * because the provider had nothing, because the clip belongs to another
     * song, or because its URL is already known not to play. Every outcome is
     * written to [answers], so the report says *why* a song ended up without a
     * canvas instead of just that it did.
     */
    private fun verify(
        query: CanvasQuery,
        provider: String,
        artwork: CanvasArtwork?,
        answers: MutableList<CanvasDiagnostics.Answer>,
    ): CanvasArtwork? {
        if (artwork == null) {
            answers += CanvasDiagnostics.Answer(provider, CanvasDiagnostics.Verdict.EMPTY)
            return null
        }

        val claimed = claimedBy(artwork)
        val url = artwork.preferredAnimationUrl?.takeIf { it.isNotBlank() }
        if (url == null) {
            answers += CanvasDiagnostics.Answer(provider, CanvasDiagnostics.Verdict.EMPTY, claimed)
            return null
        }

        val match = CanvasVerifier.match(query.title, query.artist, artwork.name, artwork.artist)
        if (match == CanvasMatch.MISMATCH) {
            answers += CanvasDiagnostics.Answer(provider, CanvasDiagnostics.Verdict.WRONG_SONG, claimed)
            return null
        }

        if (CanvasVerifier.health(url) == CanvasUrlHealth.UNPLAYABLE) {
            answers += CanvasDiagnostics.Answer(provider, CanvasDiagnostics.Verdict.DEAD_URL, claimed)
            return null
        }

        answers +=
            CanvasDiagnostics.Answer(
                provider,
                if (match == CanvasMatch.MATCHED) {
                    CanvasDiagnostics.Verdict.MATCHED
                } else {
                    CanvasDiagnostics.Verdict.UNVERIFIED
                },
                claimed,
            )
        return artwork
    }

    /** What the provider said the clip was, for the report. */
    private fun claimedBy(artwork: CanvasArtwork): String? =
        listOfNotNull(
            artwork.name?.takeIf { it.isNotBlank() },
            artwork.artist?.takeIf { it.isNotBlank() },
        )
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" — ")

    /** Writes one lookup to the report the Settings row reads. */
    private fun report(
        query: CanvasQuery,
        style: CanvasStyle,
        startedAt: Long,
        answers: List<CanvasDiagnostics.Answer>,
        artwork: CanvasArtwork?,
    ) {
        val now = System.currentTimeMillis()
        val chosen = answers.acceptedProvider()
        CanvasDiagnostics.record(
            CanvasDiagnostics.Lookup(
                mediaId = query.mediaId,
                title = query.title,
                artist = query.artist,
                style = style.name,
                atMillis = now,
                elapsedMillis = now - startedAt,
                fromCache = chosen == CacheProvider,
                answers = answers.toList(),
                chosenProvider = chosen,
                url = artwork?.preferredAnimationUrl,
                health = CanvasVerifier.health(artwork?.preferredAnimationUrl),
            ),
        )
    }

    /**
     * Which provider the answer came from: the last one whose canvas was
     * accepted. Empty when nothing was — which is what a song with no canvas
     * looks like in the report.
     */
    private fun List<CanvasDiagnostics.Answer>.acceptedProvider(): String? =
        lastOrNull {
            it.verdict == CanvasDiagnostics.Verdict.MATCHED ||
                it.verdict == CanvasDiagnostics.Verdict.UNVERIFIED
        }?.provider

    /**
     * Resolves a usable Spotify bearer token for the Canvas provider.
     *
     * The library session (SpotifyLibraryRepository) mints a token at login and
     * keeps it in [Spotify.accessToken], but nothing re-mints it while music is
     * playing, so it lapses a little while after logging in. Handing that token
     * out regardless is what used to kill the animated canvas moments after a
     * login: once it had expired every lookup came back 401/429, the provider
     * reports that as a plain "no canvas", and the dead token kept being
     * preferred over the harvest that could have replaced it.
     *
     * The only freshness record the library writes is the expiry it stores in
     * [SpotifyAccessTokenExpiresAtKey], so the library token is used only while
     * that expiry still has room; past it we ask [SpotifySession], which
     * re-mints the real web-player token before the old one lapses and is
     * therefore the source that keeps answering minutes — or hours — after
     * login. A fresh library token is still preferred, since it is instant and
     * avoids spinning up the offscreen WebView harvest.
     */
    private suspend fun spotifyToken(context: Context): String? {
        val libraryToken = Spotify.accessToken?.takeIf { it.isNotBlank() }

        if (libraryToken != null) {
            // Same safety margin the library session itself uses when it
            // decides whether a stored token is still worth restoring.
            val expiresAt = context.dataStore.data.first()[SpotifyAccessTokenExpiresAtKey] ?: 0L
            if (expiresAt > System.currentTimeMillis() + 60_000L) return libraryToken
        }

        SpotifySession.token(context)?.accessToken?.takeIf { it.isNotBlank() }?.let { return it }

        // The session could not answer (no sp_dc cookie saved, or the harvest
        // failed). Fall back to the lapsed bearer so a token whose recorded
        // expiry is off still gets its chance — the provider treats an expired
        // token and no token alike, so nothing is lost by trying.
        return libraryToken
    }

    /** Warm the playback cache for a song (used for prefetching the next track). */
    suspend fun prefetch(
        context: Context,
        mediaId: String,
        songTitle: String,
        artistName: String,
        albumName: String,
        storefront: String = defaultStorefront(),
    ) {
        if (CanvasArtworkPlaybackCache.get(mediaId) != null) return
        try {
            resolve(context, mediaId, songTitle, artistName, albumName, storefront)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Propagate — never let a cancelled prefetch complete late.
            throw e
        } catch (e: Exception) {
            // Prefetch is best-effort; ignore lookup failures.
        }
    }

    private suspend fun fetchArchiveTune(
        query: CanvasQuery,
        answers: MutableList<CanvasDiagnostics.Answer>,
    ): CanvasArtwork? =
        verify(
            query,
            ArchiveTuneProvider,
            BetterLyricsCanvasProvider.getBySongArtist(query.title, query.artist, query.storefront),
            answers,
        )

    /**
     * ALL style: fire every provider at once (Spotify, ArchiveTune/BetterLyrics,
     * Tidal, Apple Music) and return the first canvas that answers *and passes
     * the check*. Losers are cancelled the moment a winner lands, so this costs
     * no more than the fastest provider instead of the sum of all four.
     *
     * The check is what makes racing safe. Without it the fastest provider wins
     * outright, and the fastest provider is the one most likely to have matched
     * on a title alone; with it, an answer for another song is discarded and the
     * race keeps waiting, so the winner is the first provider that had this song
     * rather than the first to reply about something.
     *
     * A provider that answered nothing and a provider that fell over are
     * recorded apart. If every provider answered and none had a canvas for this
     * song, the song has no canvas and that is remembered
     * ([wasDefinitiveMiss]); if any of them failed, the lookup was never really
     * answered and a retry is worthwhile.
     */
    private suspend fun raceAllProviders(
        query: CanvasQuery,
        answers: MutableList<CanvasDiagnostics.Answer>,
    ): CanvasArtwork? = coroutineScope {
        val results = Channel<NamedAnswer>(Channel.UNLIMITED)

        /** Runs one provider's lookup and reports it as an answer or a failure. */
        suspend fun ask(provider: String, lookup: suspend () -> CanvasArtwork?) {
            val answer =
                try {
                    NamedAnswer(provider, lookup(), failed = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    NamedAnswer(provider, null, failed = true)
                }
            results.send(answer)
        }

        val jobs =
            listOf(
                launch {
                    ask(SpotifyProvider) {
                        spotifyToken(query.context)?.let { token ->
                            SpotifyCanvasProvider.getBySongArtist(query.title, query.artist, token)
                        }
                    }
                },
                launch { ask(ArchiveTuneProvider) { BetterLyricsCanvasProvider.getBySongArtist(query.title, query.artist, query.storefront) } },
                launch { ask(TidalProvider) { TidalCanvasProvider.getBySongArtist(query.title, query.artist, query.album) } },
                launch {
                    ask(AppleProvider) {
                        if (query.album.isNotBlank()) {
                            AppleMusicCanvasProvider.getByAlbumArtist(query.album, query.artist, query.storefront)
                        } else {
                            null
                        } ?: AppleMusicCanvasProvider.getBySongArtist(query.title, query.artist, query.album, query.storefront)
                    }
                },
            )

        var winner: CanvasArtwork? = null
        var answered = 0
        var failed = 0
        while (answered < jobs.size && winner == null) {
            val answer = results.receive()
            answered++
            if (answer.failed) {
                failed++
                answers += CanvasDiagnostics.Answer(answer.provider, CanvasDiagnostics.Verdict.FAILED)
            } else {
                verify(query, answer.provider, answer.artwork, answers)?.let { winner = it }
            }
        }
        jobs.forEach { it.cancel() }
        // "Every provider answered, and the answer is no canvas" is a fact worth
        // remembering. "Every provider answered, and every URL it offered was one
        // the verifier already knows is dead" is a different thing wearing the
        // same shape: that verdict expires, so the song deserves asking again
        // rather than being written off as canvas-less for the rest of the run.
        // Without this, one bad playback verdict made the *next* lookup a
        // permanent miss — which is the second half of why a canvas that
        // vanished after a background trip never came back.
        val refusedForDeadUrl =
            answers.any { it.verdict == CanvasDiagnostics.Verdict.DEAD_URL }
        if (winner == null && failed == 0 && !refusedForDeadUrl) markDefinitiveMiss(query.mediaId)
        winner
    }

    /**
     * GLOSSY style: Tidal first, then Apple Music — each candidate checked
     * before it is used, so an album-level Apple hit that is not this song, or a
     * Tidal result that drifted onto a cover, does not end the search while a
     * provider that does have the song is still available.
     */
    private suspend fun fetchGlossy(
        query: CanvasQuery,
        answers: MutableList<CanvasDiagnostics.Answer>,
    ): CanvasArtwork? {
        verify(
            query,
            TidalProvider,
            TidalCanvasProvider.getBySongArtist(query.title, query.artist, query.album),
            answers,
        )?.let { return it }

        if (query.album.isNotBlank()) {
            verify(
                query,
                AppleProvider,
                AppleMusicCanvasProvider.getByAlbumArtist(query.album, query.artist, query.storefront),
                answers,
            )?.let { return it }
        }

        return verify(
            query,
            AppleProvider,
            AppleMusicCanvasProvider.getBySongArtist(query.title, query.artist, query.album, query.storefront),
            answers,
        )
    }

    fun defaultStorefront(): String {
        val country = Locale.getDefault().country
        return if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }
}
