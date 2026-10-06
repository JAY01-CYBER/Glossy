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
<<<<<<< HEAD
 *
 * Every answer a provider gives passes the canvas check ([CanvasVerifier])
 * before it can be used — it has to belong to the song that was asked for, and
 * its URL must not already be known dead. A refused answer is treated exactly
 * like a provider that had nothing, so the others are still worth asking: this
 * is the difference between "the first thing that came back" and "the first
 * thing that came back *for this song*". What the check decided is recorded in
 * [CanvasDiagnostics], which is what the Settings row reports.
=======
>>>>>>> origin/main
 */

package com.jay.glossy.ui.player

import android.content.Context
import com.jay.glossy.applecanvas.AppleMusicCanvasProvider
import com.jay.glossy.canvas.BetterLyricsCanvasProvider
import com.jay.glossy.canvas.CanvasArtwork
<<<<<<< HEAD
import com.jay.glossy.canvas.CanvasLookupUnavailable
=======
>>>>>>> origin/main
import com.jay.glossy.canvas.TidalCanvasProvider
import com.jay.glossy.constants.CanvasStyle
import com.jay.glossy.constants.CanvasStyleKey
import com.jay.glossy.spotify.SpotifyCanvasProvider
import com.jay.glossy.spotify.SpotifySession
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
<<<<<<< HEAD
import java.util.Collections
=======
>>>>>>> origin/main
import java.util.Locale

object CanvasResolver {
    private val styleMutex = Mutex()
<<<<<<< HEAD

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
    private val definitiveMisses = Collections.synchronizedMap(LinkedHashMap<String, Long>())

    /** How long a "this song has no canvas" answer is trusted. */
    private const val DefinitiveMissTtlMillis = 15L * 60L * 1000L

    /** True when every provider positively answered that this song has no canvas. */
    fun wasDefinitiveMiss(mediaId: String): Boolean {
        val expiresAt = synchronized(definitiveMisses) { definitiveMisses[mediaId] } ?: return false
        if (expiresAt > System.currentTimeMillis()) return true
        synchronized(definitiveMisses) { definitiveMisses.remove(mediaId) }
        return false
    }

    /**
     * Remembers that this song has no canvas — for [DefinitiveMissTtlMillis],
     * not for the rest of the session.
     *
     * A miss is a statement about a moment, and the moment can be a bad one: a
     * provider that had not minted its token yet, a connection still coming up,
     * a rate limit. Remembering that as a fact about the song is how a canvas
     * that used to load silently stopped loading for the rest of the run. The
     * answer expires; only a later lookup that really gets an answer refreshes
     * it.
     */
    private fun markDefinitiveMiss(mediaId: String) {
        if (mediaId.isBlank()) return
        synchronized(definitiveMisses) {
            if (definitiveMisses.size >= DefinitiveMissLimit) definitiveMisses.clear()
            definitiveMisses[mediaId] = System.currentTimeMillis() + DefinitiveMissTtlMillis
        }
    }

    /** One provider's outcome: an answer (possibly "no canvas"), or a failure. */
    private class NamedAnswer(
        val provider: String,
        val artwork: CanvasArtwork?,
        val failed: Boolean,
        /** Set when the clip may legitimately be named after the album instead. */
        val alternativeTitle: String? = null,
    )

    /** The song a lookup is for, normalized once and passed around. */
    private class CanvasQuery(
        val context: Context,
        val mediaId: String,
        val title: String,
        val artist: String,
        val album: String,
        val storefront: String,
    )

=======
>>>>>>> origin/main
    @Volatile
    private var cachedStyle: CanvasStyle? = null

    suspend fun currentStyle(context: Context): CanvasStyle {
        cachedStyle?.let { return it }
        return styleMutex.withLock {
            cachedStyle ?: context.dataStore.data
                .map { prefs ->
                    prefs[CanvasStyleKey]?.let { stored ->
                        CanvasStyle.entries.firstOrNull { it.name == stored }
<<<<<<< HEAD
                    } ?: CanvasStyle.ALL
=======
                    } ?: CanvasStyle.GLOSSY
>>>>>>> origin/main
                }
                .first()
                .also { cachedStyle = it }
        }
    }

<<<<<<< HEAD
    /**
     * Invalidate the memoized style — call after the user changes the setting.
     *
     * The per-song "no canvas" memory goes with it: a song that answered empty
     * under one style — say Spotify-only with a lapsed token — can have a
     * canvas under the next. Keeping the miss made every canvas that used to
     * load silently stop loading after a style change, for the rest of the
     * session, because the view asks [wasDefinitiveMiss] before it even looks.
     */
    fun invalidateStyle() {
        cachedStyle = null
        synchronized(definitiveMisses) { definitiveMisses.clear() }
    }

    /**
     * Called when the Spotify account is connected or disconnected.
     *
     * Signing in is what makes a fourth provider able to answer, and until now
     * nothing told the canvas pipeline about it. Two things remembered "no
     * canvas for this song" while logged out and then went on remembering it
     * after the login that would have produced one:
     *
     *  - [definitiveMisses] — "this song has no canvas" is only a fact about
     *    the song when every provider was able to answer it, and the race never
     *    writes one while Spotify is unaskable. A look that did get all four
     *    answers while the account was lapsed is still worth forgetting: the
     *    song has not changed, but the set of services asked about it has.
     *  - [CanvasPrefetcher]'s handled set — a lookup that found nothing is
     *    deliberately not marked handled, so this one is belt and braces for
     *    the URL-warm bookkeeping.
     *
     * Both are cleared so the next card on screen asks again instead of
     * sitting on its still artwork until something else expires them.
     */
    fun invalidateCredentials() {
        synchronized(definitiveMisses) { definitiveMisses.clear() }
        CanvasPrefetcher.reset()
=======
    /** Invalidate the memoized style — call after the user changes the setting. */
    fun invalidateStyle() {
        cachedStyle = null
>>>>>>> origin/main
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

<<<<<<< HEAD
=======
        // Instant path: playback cache first
        CanvasArtworkPlaybackCache.get(mediaId)?.let { return@withContext it }

>>>>>>> origin/main
        val normalizedTitle = normalizeCanvasSongTitle(songTitle)
        val normalizedArtist = normalizeCanvasArtistName(artistName)
        if (normalizedTitle.isBlank() || normalizedArtist.isBlank()) return@withContext null

<<<<<<< HEAD
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
=======
        val fetched = when (style) {
            CanvasStyle.ARCHIVE_TUNE -> fetchArchiveTune(normalizedTitle, normalizedArtist, storefront)

            CanvasStyle.GLOSSY -> fetchGlossy(normalizedTitle, normalizedArtist, albumName, storefront)

            CanvasStyle.BOTH ->
                fetchArchiveTune(normalizedTitle, normalizedArtist, storefront)
                    ?: fetchGlossy(normalizedTitle, normalizedArtist, albumName, storefront)

            CanvasStyle.SPOTIFY ->
                SpotifySession.token(context)?.let {
                    SpotifyCanvasProvider.getBySongArtist(normalizedTitle, normalizedArtist)
                }
                    // Spotify rarely has canvases for every track; fall back to the
                    // Glossy engine so an animated canvas still shows instead of nothing.
                    ?: fetchGlossy(normalizedTitle, normalizedArtist, albumName, storefront)

            CanvasStyle.ALL -> raceAllProviders(
                context = context,
                song = normalizedTitle,
>>>>>>> origin/main
                artist = normalizedArtist,
                album = albumName,
                storefront = storefront,
            )
<<<<<<< HEAD

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
                    val credentials = spotifyCredentials(context)
                    if (credentials == null) {
                        // Could not be asked — a failure, never a song without a
                        // canvas: otherwise the next lookup is skipped entirely.
                        answers += CanvasDiagnostics.Answer(SpotifyProvider, CanvasDiagnostics.Verdict.FAILED)
                    }
                    val spotify =
                        credentials?.let { held ->
                            askProvider(SpotifyProvider, answers) {
                                SpotifyCanvasProvider.getBySongArtist(
                                    query.title,
                                    query.artist,
                                    held.accessToken,
                                    held.clientToken,
                                )
                            }?.let { verify(query, SpotifyProvider, it, answers) }
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
        alternativeTitle: String? = null,
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

        val match =
            CanvasVerifier.match(
                query.title,
                query.artist,
                artwork.name,
                artwork.artist,
                alternativeTitle = alternativeTitle,
            )
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
     * The credentials the Spotify canvas endpoints want, or null when Spotify
     * has not been connected.
     *
     * Not simply [SpotifySession.freshAccessToken]: that prefers the library
     * token, which is minted by replaying the web player's /api/token request
     * with a computed TOTP and is refused by spclient with a 429, so Spotify
     * canvases never answered once the library session was signed in. The
     * harvested web-player token and the client token beside it are what
     * [SpotifySession.canvasCredentials] returns.
     */
    private suspend fun spotifyCredentials(context: Context): SpotifySession.CanvasCredentials? =
        SpotifySession.canvasCredentials(context)

=======
        }

        if (fetched != null) {
            CanvasArtworkPlaybackCache.put(mediaId, fetched)
        }
        fetched
    }

>>>>>>> origin/main
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

<<<<<<< HEAD
    private suspend fun fetchArchiveTune(
        query: CanvasQuery,
        answers: MutableList<CanvasDiagnostics.Answer>,
    ): CanvasArtwork? =
        verify(
            query,
            ArchiveTuneProvider,
            askProvider(ArchiveTuneProvider, answers) {
                BetterLyricsCanvasProvider.getBySongArtist(query.title, query.artist, query.storefront)
            },
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
        suspend fun ask(
            provider: String,
            alternativeTitle: String? = null,
            lookup: suspend () -> CanvasArtwork?,
        ) {
            val answer =
                try {
                    NamedAnswer(provider, lookup(), failed = false, alternativeTitle = alternativeTitle)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    NamedAnswer(provider, null, failed = true, alternativeTitle = alternativeTitle)
                }
            results.send(answer)
        }

        val jobs =
            listOf(
                launch {
                    ask(SpotifyProvider) {
                        // Signed out is a known state, not a broken lookup: it is
                        // recorded as FAILED, and that verdict is what keeps the
                        // miss below from being written on the other three
                        // providers' say-so (see the answer check).
                        val credentials =
                            spotifyCredentials(query.context)
                                ?: throw CanvasLookupUnavailable("Spotify is not connected")
                        SpotifyCanvasProvider.getBySongArtist(
                            query.title,
                            query.artist,
                            credentials.accessToken,
                            credentials.clientToken,
                        )
                    }
                },
                launch { ask(ArchiveTuneProvider) { BetterLyricsCanvasProvider.getBySongArtist(query.title, query.artist, query.storefront) } },
                launch { ask(TidalProvider) { TidalCanvasProvider.getBySongArtist(query.title, query.artist, query.album) } },
                launch {
                    ask(AppleProvider, alternativeTitle = query.album.takeIf { it.isNotBlank() }) {
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
                verify(query, answer.provider, answer.artwork, answers, answer.alternativeTitle)?.let { winner = it }
            }
        }
        jobs.forEach { it.cancel() }
        // A miss is only written down when the providers really did answer. A
        // failed lookup says nothing about the song; a provider whose canvas
        // was refused was answering about something else (a cover, a dead
        // URL), which the *next* lookup may well answer differently. Only
        // "asked, and every one of them had nothing" is a fact worth
        // remembering — and even that expires.
        val refusedSomething =
            answers.any {
                it.verdict == CanvasDiagnostics.Verdict.WRONG_SONG ||
                    it.verdict == CanvasDiagnostics.Verdict.DEAD_URL
            }
        // A provider that could not be asked at all — Spotify before an account
        // is connected — leaves the answer to "does this song have a canvas?"
        // incomplete: the clip may be sitting on the one service nobody asked.
        // Recording that moment as "this song has no canvas" is what made a
        // card sit still until a login: the surfaces consult
        // [wasDefinitiveMiss] before they look, so the memory written while
        // signed out outlived the login that would have produced a clip (see
        // [invalidateCredentials]). Only a race every provider really answered
        // can decide that the song has no canvas.
        val answeredEmpty =
            answers.isNotEmpty() && answers.all { it.verdict == CanvasDiagnostics.Verdict.EMPTY }
        if (winner == null && failed == 0 && !refusedSomething && answeredEmpty) {
            markDefinitiveMiss(query.mediaId)
        }
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
            askProvider(TidalProvider, answers) {
                TidalCanvasProvider.getBySongArtist(query.title, query.artist, query.album)
            },
            answers,
        )?.let { return it }

        if (query.album.isNotBlank()) {
            // An album-level motion artwork is named after the album, so the
            // check is told the album is an acceptable identity for it. Without
            // that the answer was always refused as the wrong song — which is
            // why this whole lookup never painted anything.
            verify(
                query,
                AppleProvider,
                askProvider(AppleProvider, answers) {
                    AppleMusicCanvasProvider.getByAlbumArtist(query.album, query.artist, query.storefront)
                },
                answers,
                alternativeTitle = query.album,
            )?.let { return it }
        }

        return verify(
            query,
            AppleProvider,
            askProvider(AppleProvider, answers) {
                AppleMusicCanvasProvider.getBySongArtist(query.title, query.artist, query.album, query.storefront)
            },
            answers,
        )
    }

    /**
     * Runs one provider and turns a failure into a recorded one.
     *
     * A provider that could not answer — offline, rate limited, an expired
     * token ([CanvasLookupUnavailable]) — says nothing about the song, so it is
     * recorded as [CanvasDiagnostics.Verdict.FAILED] rather than as a provider
     * that looked and found nothing. That distinction is what stops a bad
     * moment from being remembered as a canvas-less song, and it lets the
     * others keep answering.
     */
    private suspend fun <T> askProvider(
        provider: String,
        answers: MutableList<CanvasDiagnostics.Answer>,
        lookup: suspend () -> T?,
    ): T? =
        try {
            lookup()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            answers += CanvasDiagnostics.Answer(provider, CanvasDiagnostics.Verdict.FAILED)
            null
        }
=======
    private suspend fun fetchArchiveTune(song: String, artist: String, storefront: String): CanvasArtwork? =
        BetterLyricsCanvasProvider.getBySongArtist(song, artist, storefront)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }

    /**
     * ALL style: fire every provider at once (Spotify, ArchiveTune/BetterLyrics,
     * Tidal, Apple Music) and return the first animated canvas that answers.
     * Losers are cancelled the moment a winner lands, so this costs no more
     * than the fastest provider instead of the sum of all four.
     */
    private suspend fun raceAllProviders(
        context: Context,
        song: String,
        artist: String,
        album: String,
        storefront: String,
    ): CanvasArtwork? = coroutineScope {
        val results = Channel<CanvasArtwork?>(Channel.UNLIMITED)
        val jobs = listOf(
            launch {
                val value = try {
                    SpotifySession.token(context)?.let {
                        SpotifyCanvasProvider.getBySongArtist(song, artist)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                results.send(value?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() })
            },
            launch {
                val value = try {
                    fetchArchiveTune(song, artist, storefront)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                results.send(value)
            },
            launch {
                val value = try {
                    TidalCanvasProvider.getBySongArtist(song, artist, album)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                results.send(value)
            },
            launch {
                val value = try {
                    if (album.isNotBlank()) {
                        AppleMusicCanvasProvider.getByAlbumArtist(album, artist, storefront)
                            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    } else {
                        null
                    } ?: AppleMusicCanvasProvider.getBySongArtist(song, artist, album, storefront)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                results.send(value)
            },
        )
        var winner: CanvasArtwork? = null
        var answered = 0
        while (answered < jobs.size && winner == null) {
            val value = results.receive()
            answered++
            if (value != null) winner = value
        }
        jobs.forEach { it.cancel() }
        winner
    }

    private suspend fun fetchGlossy(song: String, artist: String, album: String, storefront: String): CanvasArtwork? {
        val tidal = TidalCanvasProvider.getBySongArtist(song, artist, album)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
        if (tidal != null) return tidal
        return (if (album.isNotBlank()) {
            AppleMusicCanvasProvider.getByAlbumArtist(album, artist, storefront)
                ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
        } else {
            null
        }) ?: AppleMusicCanvasProvider.getBySongArtist(song, artist, album, storefront)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
    }
>>>>>>> origin/main

    fun defaultStorefront(): String {
        val country = Locale.getDefault().country
        return if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }
}
