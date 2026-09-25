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

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val songKey = "${mediaMetadata.artists.joinToString { it.name }}-${mediaMetadata.title}".replace(" ", "").lowercase()

        // Check memory cache first (instant 0ms return)
        singleLyricsCache.get(mediaMetadata.id)?.let { return it }
        singleLyricsCache.get(songKey)?.let { return it }

        // Also check if we have results in allLyricsCache
        allLyricsCache.get(songKey)?.firstOrNull()?.let {
            val result = LyricsWithProvider(it.lyrics, it.providerName)
            singleLyricsCache.put(mediaMetadata.id, result)
            singleLyricsCache.put(songKey, result)
            return result
        }

        // Skip providers entirely for songs that recently returned no lyrics
        pruneNegativeCache()
        if (negativeResultCache.get(mediaMetadata.id) != null) {
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
     * Races the given providers concurrently; returns the first non-blank successful lyrics result.
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

        val winner = channel.receiveCatching().getOrNull()
        jobs.forEach { it.cancel() }
        monitorJob.cancel()
        winner
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
        if (providerOrder.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(providerOrder)
        }

        return LyricsProviderRegistry.getDefaultProviderOrder()
            .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
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
