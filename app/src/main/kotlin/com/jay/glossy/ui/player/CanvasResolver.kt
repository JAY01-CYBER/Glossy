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
 */

package com.jay.glossy.ui.player

import android.content.Context
import com.jay.glossy.applecanvas.AppleMusicCanvasProvider
import com.jay.glossy.canvas.BetterLyricsCanvasProvider
import com.jay.glossy.canvas.CanvasArtwork
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
import java.util.Locale

object CanvasResolver {
    private val styleMutex = Mutex()
    @Volatile
    private var cachedStyle: CanvasStyle? = null

    suspend fun currentStyle(context: Context): CanvasStyle {
        cachedStyle?.let { return it }
        return styleMutex.withLock {
            cachedStyle ?: context.dataStore.data
                .map { prefs ->
                    prefs[CanvasStyleKey]?.let { stored ->
                        CanvasStyle.entries.firstOrNull { it.name == stored }
                    } ?: CanvasStyle.GLOSSY
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

        // Instant path: playback cache first
        CanvasArtworkPlaybackCache.get(mediaId)?.let { return@withContext it }

        val normalizedTitle = normalizeCanvasSongTitle(songTitle)
        val normalizedArtist = normalizeCanvasArtistName(artistName)
        if (normalizedTitle.isBlank() || normalizedArtist.isBlank()) return@withContext null

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
                artist = normalizedArtist,
                album = albumName,
                storefront = storefront,
            )
        }

        if (fetched != null) {
            CanvasArtworkPlaybackCache.put(mediaId, fetched)
        }
        fetched
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

    fun defaultStorefront(): String {
        val country = Locale.getDefault().country
        return if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }
}
