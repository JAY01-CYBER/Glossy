/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lyrics

import android.content.Context
import android.util.LruCache
import com.jay.glossy.constants.LyricsProviderOrderKey
import com.jay.glossy.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.jay.glossy.utils.NetworkConnectivityObserver
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.reportException
import com.metrolist.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

private const val MAX_LYRICS_FETCH_MS = 12000L
private const val PER_PROVIDER_TIMEOUT_MS = 3500L
private const val PROVIDER_NONE = ""

<<<<<<< HEAD
/**
 * How long a plain-text answer is held back while the other racing providers get
 * a chance to return a time-synced version of the same song. Only lyrics with
 * timestamps can be followed by the player, so a synced result is always worth
 * this short wait.
 */
private const val SYNCED_PREFERENCE_WINDOW_MS = 1500L

=======
>>>>>>> origin/main
/** How long a negative ("no lyrics found") result is remembered before retrying. */
private const val NEGATIVE_RESULT_TTL_MS = 24 * 60 * 60 * 1000L

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    val preferred =
        context.dataStore.data
            .map { preferences ->
                resolveLyricsProviders(preferences)
            }.distinctUntilChanged()

    private val singleLyricsCache = LruCache<String, LyricsWithProvider>(MAX_CACHE_SIZE)
    private val allLyricsCache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)

    /**
     * Remembers songs for which every provider returned nothing, together with the
     * timestamp of the failed attempt. Prevents re-hitting every provider on every
     * playback start for instrumentals / unavailable songs.
     */
    private val negativeResultCache = LruCache<String, Long>(MAX_CACHE_SIZE)

    private var currentLyricsJob: Job? = null

    /**
     * Removes stale negative entries so songs can be retried after the TTL.
     * Called opportunistically; cheap because LRU size is bounded.
     */
    private fun pruneNegativeCache(now: Long = System.currentTimeMillis()) {
        val snapshot = negativeResultCache.snapshot()
        val iterator: MutableIterator<Map.Entry<String, Long>> = snapshot.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > NEGATIVE_RESULT_TTL_MS) negativeResultCache.remove(entry.key)
        }
    }

<<<<<<< HEAD
    /**
     * @param forceRefresh skips every cache and races all enabled providers at
     * once, so a timed version anywhere in the provider order can replace an
     * untimed one that is already stored. The player asks for this when the
     * lyrics it holds cannot be followed; normal playback keeps the tiered fast
     * race and the caches.
     */
    suspend fun getLyrics(mediaMetadata: MediaMetadata, forceRefresh: Boolean = false): LyricsWithProvider {
=======
    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
>>>>>>> origin/main
        currentLyricsJob?.cancel()

        val songKey = "${mediaMetadata.artists.joinToString { it.name }}-${mediaMetadata.title}".replace(" ", "").lowercase()

<<<<<<< HEAD
        // Check memory cache first (instant 0ms return). A forced refresh exists
        // to replace a stale result, so it has to step over the caches holding
        // that very result: otherwise the retry hands back the same untimed text
        // it was meant to replace and the player never recovers for that song.
        if (!forceRefresh) {
            singleLyricsCache.get(mediaMetadata.id)?.let { return it }
            singleLyricsCache.get(songKey)?.let { return it }

            // Also check if we have results in allLyricsCache
            allLyricsCache.get(songKey)?.firstOrNull()?.let {
                val result = LyricsWithProvider(it.lyrics, it.providerName)
                singleLyricsCache.put(mediaMetadata.id, result)
                singleLyricsCache.put(songKey, result)
                return result
            }
=======
        // Check memory cache first (instant 0ms return)
        singleLyricsCache.get(mediaMetadata.id)?.let { return it }
        singleLyricsCache.get(songKey)?.let { return it }

        // Also check if we have results in allLyricsCache
        allLyricsCache.get(songKey)?.firstOrNull()?.let {
            val result = LyricsWithProvider(it.lyrics, it.providerName)
            singleLyricsCache.put(mediaMetadata.id, result)
            singleLyricsCache.put(songKey, result)
            return result
>>>>>>> origin/main
        }

        // Skip providers entirely for songs that recently returned no lyrics
        pruneNegativeCache()
<<<<<<< HEAD
        if (!forceRefresh && negativeResultCache.get(mediaMetadata.id) != null) {
=======
        if (negativeResultCache.get(mediaMetadata.id) != null) {
>>>>>>> origin/main
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val orderedProviders = context.dataStore.data
            .map { preferences -> resolveLyricsProviders(preferences) }
            .first()

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }
        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val artists = mediaMetadata.artists.joinToString { it.name }
            val enabledProviders = orderedProviders.filter { it.isEnabled(context) }

            Timber.tag("LyricsHelper").d("Fast parallel lyrics fetch: $cleanedTitle by $artists across ${enabledProviders.size} providers")

            if (enabledProviders.isEmpty()) {
                return@withTimeoutOrNull LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
            }

<<<<<<< HEAD
            if (forceRefresh) {
                // One race across the whole order, keeping the same
                // synced-preference window the tiers use: an untimed answer is
                // only accepted if no provider in the order can time the song.
                val refreshed = raceProviders(
                    providers = enabledProviders,
                    mediaId = mediaMetadata.id,
                    cleanedTitle = cleanedTitle,
                    artists = artists,
                    duration = mediaMetadata.duration,
                    album = mediaMetadata.album?.title,
                )
                if (refreshed != null) {
                    Timber.tag("LyricsHelper").i("Refresh got lyrics from ${refreshed.provider}")
                    singleLyricsCache.put(mediaMetadata.id, refreshed)
                    singleLyricsCache.put(songKey, refreshed)
                    return@withTimeoutOrNull refreshed
                }
                // Deliberately no negative-cache entry: a refresh that finds
                // nothing must not stop the normal path from serving the lyrics
                // that are already stored for this song.
                Timber.tag("LyricsHelper").w("Refresh found nothing")
                return@withTimeoutOrNull LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
            }

=======
>>>>>>> origin/main
            // Tier 1: Race top 3 preferred providers concurrently for sub-second resolution
            val tier1Providers = enabledProviders.take(3)
            val tier1Winner = raceProviders(
                providers = tier1Providers,
                mediaId = mediaMetadata.id,
                cleanedTitle = cleanedTitle,
                artists = artists,
                duration = mediaMetadata.duration,
                album = mediaMetadata.album?.title
            )

            if (tier1Winner != null) {
                Timber.tag("LyricsHelper").i("Got lyrics in Tier 1 from ${tier1Winner.provider}")
                singleLyricsCache.put(mediaMetadata.id, tier1Winner)
                singleLyricsCache.put(songKey, tier1Winner)
                return@withTimeoutOrNull tier1Winner
            }

            // Tier 2: If top 3 didn't have it, race the remaining fallback providers
            val remainingProviders = enabledProviders.drop(3)
            if (remainingProviders.isNotEmpty()) {
                val tier2Winner = raceProviders(
                    providers = remainingProviders,
                    mediaId = mediaMetadata.id,
                    cleanedTitle = cleanedTitle,
                    artists = artists,
                    duration = mediaMetadata.duration,
                    album = mediaMetadata.album?.title
                )

                if (tier2Winner != null) {
                    Timber.tag("LyricsHelper").i("Got lyrics in Tier 2 from ${tier2Winner.provider}")
                    singleLyricsCache.put(mediaMetadata.id, tier2Winner)
                    singleLyricsCache.put(songKey, tier2Winner)
                    return@withTimeoutOrNull tier2Winner
                }
            }

            Timber.tag("LyricsHelper").w("No lyrics found after racing all providers")
            // Remember the miss so the next playback start returns instantly
            negativeResultCache.put(mediaMetadata.id, System.currentTimeMillis())
            LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val finalResult = result ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        if (finalResult.lyrics != LYRICS_NOT_FOUND) {
            singleLyricsCache.put(mediaMetadata.id, finalResult)
            singleLyricsCache.put(songKey, finalResult)
        }
        return finalResult
    }

    /**
<<<<<<< HEAD
     * Races the given providers concurrently, but the winner is chosen by
     * *rank*, not by who answers fastest: [providers] is already in the user's
     * priority order, a synced answer from the top of the list wins instantly,
     * and a synced answer from anyone lower gets [SYNCED_PREFERENCE_WINDOW_MS]
     * to see whether a higher-ranked synced answer turns up before it is
     * locked in. Plain text still only ever serves as the fallback, held the
     * same short window against a timed answer appearing.
     *
     * Racing by raw speed alone is what made moving a provider up or down in
     * Settings meaningless — the fastest scraper always won no matter where it
     * sat in the list.
=======
     * Races the given providers concurrently; returns the first non-blank successful lyrics result.
>>>>>>> origin/main
     */
    private suspend fun raceProviders(
        providers: List<LyricsProvider>,
        mediaId: String,
        cleanedTitle: String,
        artists: String,
        duration: Int,
        album: String?
    ): LyricsWithProvider? = coroutineScope {
        if (providers.isEmpty()) return@coroutineScope null
        val channel = Channel<LyricsWithProvider>(Channel.BUFFERED)

        val jobs = providers.map { provider ->
            launch(Dispatchers.IO) {
                try {
                    val res = withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(context, mediaId, cleanedTitle, artists, duration, album)
                    }
                    if (res != null && res.isSuccess) {
                        val raw = res.getOrNull()
                        if (!raw.isNullOrBlank()) {
                            val filtered = LyricsUtils.filterLyricsCreditLines(raw)
                            if (filtered.isNotBlank()) {
                                channel.trySend(LyricsWithProvider(filtered, provider.name))
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag("LyricsHelper").w("${provider.name} threw: ${e.message}")
                }
            }
        }

        val monitorJob = launch {
            jobs.forEach { it.join() }
            channel.close()
        }

<<<<<<< HEAD
        try {
            // Rank here is the provider's position in the user's priority list.
            val rank = providers.withIndex().associate { (index, provider) -> provider.name to index }
            var bestSynced: Pair<Int, LyricsWithProvider>? = null
            var plainFallback: Pair<Int, LyricsWithProvider>? = null
            // When the collecting loop is allowed to settle; every answer that
            // arrives from a lower rank than the best one yet pushes this out,
            // bounded by the preference window.
            var settleAtMillis = Long.MAX_VALUE

            while (true) {
                val remaining = settleAtMillis - System.currentTimeMillis()
                val result = if (remaining == Long.MAX_VALUE) {
                    // Nothing worth waiting for has arrived yet; block until
                    // an answer does or every provider has had its timeout.
                    channel.receiveCatching().getOrNull() ?: break
                } else {
                    withTimeoutOrNull(remaining.coerceAtLeast(1L)) {
                        channel.receiveCatching().getOrNull()
                    } ?: break // the window closed, or the channel did
                }
                val position = rank[result.provider] ?: Int.MAX_VALUE

                if (lyricsTextLooksSynced(result.lyrics)) {
                    if (bestSynced == null || position < bestSynced.first) {
                        bestSynced = position to result
                        if (position == 0) break // the top of the list answered; nothing outranks it
                        settleAtMillis = minOf(
                            settleAtMillis,
                            System.currentTimeMillis() + SYNCED_PREFERENCE_WINDOW_MS,
                        )
                    }
                } else if (plainFallback == null || position < plainFallback.first) {
                    plainFallback = position to result
                    if (settleAtMillis == Long.MAX_VALUE) {
                        settleAtMillis = System.currentTimeMillis() + SYNCED_PREFERENCE_WINDOW_MS
                    }
                }
            }
            bestSynced?.second ?: plainFallback?.second
        } finally {
            jobs.forEach { it.cancel() }
            monitorJob.cancel()
        }
=======
        val winner = channel.receiveCatching().getOrNull()
        jobs.forEach { it.cancel() }
        monitorJob.cancel()
        winner
>>>>>>> origin/main
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "").lowercase()
        allLyricsCache.get(cacheKey)?.let { results ->
            results.forEach { callback(it) }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) return

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            val allProviders = context.dataStore.data
                .map { preferences -> resolveLyricsProviders(preferences) }
                .first()
            val enabledProviders = allProviders.filter { it.isEnabled(context) }

            val otherProviders = enabledProviders.filter { it.name != "LyricsPlus" }
            val lyricsPlusProvider = enabledProviders.find { it.name == "LyricsPlus" }

            val callbackMutex = Any()

            val otherJobs = otherProviders.map { provider ->
                launch {
                    try {
                        provider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(provider.name, filteredLyrics)
                            synchronized(callbackMutex) {
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            otherJobs.forEach { it.join() }

            val otherLyricsCount = allResult.count { it.providerName != "LyricsPlus" }
            if (lyricsPlusProvider != null && otherLyricsCount <= 2) {
                launch {
                    try {
                        lyricsPlusProvider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(lyricsPlusProvider.name, filteredLyrics)
                            synchronized(callbackMutex) {
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }.join()
            }

            allLyricsCache.put(cacheKey, allResult)
            // Also seed singleLyricsCache with the first provider result
            allResult.firstOrNull()?.let { firstResult ->
                val single = LyricsWithProvider(firstResult.lyrics, firstResult.providerName)
                singleLyricsCache.put(mediaId, single)
                singleLyricsCache.put(cacheKey, single)
            }
        }

        currentLyricsJob?.join()
    }

    private fun resolveLyricsProviders(preferences: androidx.datastore.preferences.core.Preferences): List<LyricsProvider> {
        val providerOrder = preferences[LyricsProviderOrderKey].orEmpty()
<<<<<<< HEAD
        val configured =
            if (providerOrder.isNotBlank()) {
                LyricsProviderRegistry.getOrderedProviders(providerOrder)
            } else {
                LyricsProviderRegistry.getDefaultProviderOrder()
                    .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
            }

        // A provider added after the stored order was written — "Spotify" is
        // the case that motivated this — would otherwise never take part in
        // the race at all: an old order string cannot mention it, and the
        // priority screen cannot show what the resolver drops. They join at
        // the bottom, where the user can see them and move them up.
        val missing = LyricsProviderRegistry.getDefaultProviderOrder()
            .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
            .filter { known -> configured.none { it.name == known.name } }

        return configured + missing
=======
        if (providerOrder.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(providerOrder)
        }

        return LyricsProviderRegistry.getDefaultProviderOrder()
            .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
>>>>>>> origin/main
    }

    companion object {
        private const val MAX_CACHE_SIZE = 100
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
