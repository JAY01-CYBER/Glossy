/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * SponsorBlock support ported from ArchiveTune
 * (https://github.com/rukamori/ArchiveTune), GPL-3.0.
 */

package com.jay.glossy.sponsorblock

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.jay.glossy.BuildConfig
import com.jay.glossy.constants.SponsorBlockCategoriesKey
import com.jay.glossy.constants.SponsorBlockEnabledKey
import com.jay.glossy.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.MessageDigest

/**
 * Reads the listener's SponsorBlock choices out of the app's DataStore and asks
 * the public SponsorBlock database for the segments of a video.
 *
 * The database is keyed by video id and answers with *all* segment kinds in one
 * list. Filtering by category is therefore ours to do, and it has to happen
 * before anything is skipped: a listener who leaves "outros" unselected means
 * the outro must not be skipped, not that it should be skipped and then
 * explained away.
 */
@Singleton
class SponsorBlockRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val client =
            HttpClient(OkHttp) {
                expectSuccess = false
                install(HttpTimeout) {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                engine {
                    config {
                        retryOnConnectionFailure(true)
                    }
                }
            }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        /**
         * Recent answers, so scrubbing back over a segment does not re-ask the
         * API for a list we already have. Small and access-ordered: the last
         * videos played are the ones a listener returns to.
         */
        private val responseCache =
            object : LinkedHashMap<SponsorBlockRequestKey, List<SponsorBlockRawSegment>>(
                MAX_CACHE_ENTRIES + 1,
                0.75f,
                true,
            ) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<SponsorBlockRequestKey, List<SponsorBlockRawSegment>>,
                ): Boolean = size > MAX_CACHE_ENTRIES
            }

        fun observeSettings(): Flow<SponsorBlockSettings> =
            context.dataStore.data
                .map(::settingsFromPreferences)
                .distinctUntilChanged()

        suspend fun setEnabled(enabled: Boolean) {
            withContext(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                    preferences[SponsorBlockEnabledKey] = enabled
                }
            }
        }

        suspend fun setCategories(categories: Set<SponsorBlockCategory>) {
            val storedCategories = categories.mapTo(mutableSetOf(), SponsorBlockCategory::apiValue)
            withContext(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                    preferences[SponsorBlockCategoriesKey] = storedCategories
                }
            }
        }

        /**
         * The segments of [videoId] the listener asked to skip, in playback
         * order and with overlapping entries merged so one skip can cover both.
         *
         * Never throws. `null` means the question was never answered — the API
         * was unreachable or still failing when the retry gave up — which is not
         * the same as "this video has nothing to skip": only a real answer is
         * cached, so a caller that gets `null` may ask again and a failed
         * lookup can never be remembered as an empty one.
         *
         * Skipping is an optimisation on top of playback, so nothing in here is
         * allowed to interrupt the song.
         */
        suspend fun getSegments(
            videoId: String,
            settings: SponsorBlockSettings,
        ): List<SponsorBlockSegment>? {
            if (!isLookupWorthwhile(videoId, settings)) return emptyList()

            val orderedCategories =
                SponsorBlockCategory.entries.filter(settings.categories::contains)
            val segments = fetchWithRetry(videoId, orderedCategories) ?: return null

            val selected =
                segments
                    .mapNotNull { rawSegment -> rawSegment.toSegmentOrNull(settings.categories) }
                    .sortedWith(compareBy(SponsorBlockSegment::startMs, SponsorBlockSegment::endMs))

            if (selected.isEmpty()) return emptyList()

            val mergedSegments = ArrayList<SponsorBlockSegment>(selected.size)
            selected.forEach { segment ->
                val previous = mergedSegments.lastOrNull()
                if (previous != null && segment.startMs <= previous.endMs) {
                    mergedSegments[mergedSegments.lastIndex] =
                        previous.copy(endMs = max(previous.endMs, segment.endMs))
                } else {
                    mergedSegments += segment
                }
            }
            return mergedSegments
        }

        /**
         * Asks for the segments of songs that have not started yet.
         *
         * A queued track begins the instant the one before it ends, so a lookup
         * that only starts then is racing the very segment it means to skip: a
         * sponsor read from 0:00 is usually over before the answer lands, which
         * is why the same song skipped correctly when it was replayed from the
         * cache a moment later. Asking while the previous song still plays costs
         * one request per song and removes that race entirely.
         *
         * Best effort by design: a song that cannot be answered now is simply not
         * cached, and the next call for it asks again.
         */
        suspend fun prefetchSegments(
            videoIds: Collection<String>,
            settings: SponsorBlockSettings,
        ) {
            if (!settings.enabled || settings.categories.isEmpty()) return

            val orderedCategories =
                SponsorBlockCategory.entries.filter(settings.categories::contains)
            val categoryValues = orderedCategories.map(SponsorBlockCategory::apiValue)

            videoIds.forEach { videoId ->
                if (!YOUTUBE_VIDEO_ID.matches(videoId)) return@forEach
                val requestKey = SponsorBlockRequestKey(videoId, categoryValues)
                if (getCached(requestKey) != null) return@forEach
                try {
                    fetchWithRetry(videoId, orderedCategories)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    Timber.tag(TAG).w(throwable, "SponsorBlock prefetch failed for $videoId")
                }
            }
        }

        private fun isLookupWorthwhile(
            videoId: String,
            settings: SponsorBlockSettings,
        ): Boolean =
            YOUTUBE_VIDEO_ID.matches(videoId) &&
                settings.enabled &&
                settings.categories.isNotEmpty()

        /**
         * One lookup, retried once when the failure looks transient.
         *
         * The API rate-limits and occasionally drops requests, and a song only
         * gets one chance at a skip: a single timeout used to mean the whole
         * sponsor read played. The wait between attempts is short and bounded so
         * a dead connection cannot hold the skip for long.
         */
        private suspend fun fetchWithRetry(
            videoId: String,
            categories: List<SponsorBlockCategory>,
        ): List<SponsorBlockRawSegment>? {
            repeat(LOOKUP_ATTEMPTS) { attemptIndex ->
                val segments =
                    try {
                        fetchSegments(videoId, categories)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        Timber.tag(TAG).w(throwable, "SponsorBlock lookup failed for $videoId")
                        null
                    }
                if (segments != null) return segments
                if (attemptIndex < LOOKUP_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MILLIS * (attemptIndex + 1))
                }
            }
            return null
        }

        private suspend fun fetchSegments(
            videoId: String,
            categories: List<SponsorBlockCategory>,
        ): List<SponsorBlockRawSegment>? =
            withContext(Dispatchers.IO) {
                val categoryValues = categories.map(SponsorBlockCategory::apiValue)
                val requestKey = SponsorBlockRequestKey(videoId, categoryValues)
                getCached(requestKey)?.let { return@withContext it }

                val response =
                    client.get("$DEFAULT_SPONSOR_BLOCK_API_URL/api/skipSegments/${videoId.sha256Prefix()}") {
                        parameter("categories", json.encodeToString(categoryValues))
                        parameter("actionTypes", SKIP_ACTION_TYPES_JSON)
                        parameter("service", YOUTUBE_SERVICE)
                        header(HttpHeaders.UserAgent, "Glossy/${BuildConfig.VERSION_NAME}")
                    }

                val segments =
                    when (response.status.value) {
                        // The database has nothing for this video, which is the
                        // common case for music and not an error at all.
                        404 -> emptyList()
                        in 200..299 ->
                            json
                                .decodeFromString<List<SponsorBlockVideoResponse>>(response.bodyAsText())
                                .firstOrNull { it.videoId == videoId }
                                ?.segments
                                .orEmpty()
                                .map { segment ->
                                    SponsorBlockRawSegment(
                                        segment = segment.segment,
                                        category = segment.category,
                                        actionType = segment.actionType,
                                    )
                                }
                        else -> {
                            // Rate limited, server trouble, or anything else:
                            // nothing was answered, so nothing is cached and the
                            // retry — or the next request for this song — can
                            // still get a real answer.
                            Timber
                                .tag(TAG)
                                .w("SponsorBlock answered ${response.status.value} for $videoId")
                            return@withContext null
                        }
                    }

                putCached(requestKey, segments)
                segments
            }

        private fun settingsFromPreferences(preferences: Preferences): SponsorBlockSettings {
            val storedCategoryValues = preferences[SponsorBlockCategoriesKey]
            val categories =
                if (storedCategoryValues == null) {
                    // Absent means "not chosen yet", which is not the same as
                    // "chosen nothing": a fresh install skips every kind.
                    SponsorBlockCategory.entries.toSet()
                } else {
                    storedCategoryValues.mapNotNullTo(linkedSetOf(), SponsorBlockCategory::fromApiValue)
                }

            return SponsorBlockSettings(
                enabled = preferences[SponsorBlockEnabledKey] ?: false,
                categories = categories,
            )
        }

        @Synchronized
        private fun getCached(key: SponsorBlockRequestKey): List<SponsorBlockRawSegment>? =
            responseCache[key]

        @Synchronized
        private fun putCached(
            key: SponsorBlockRequestKey,
            segments: List<SponsorBlockRawSegment>,
        ) {
            responseCache[key] = segments
        }

        private fun SponsorBlockRawSegment.toSegmentOrNull(
            selectedCategories: Set<SponsorBlockCategory>,
        ): SponsorBlockSegment? {
            if (actionType != SKIP_ACTION_TYPE) return null
            val parsedCategory = SponsorBlockCategory.fromApiValue(category) ?: return null
            if (parsedCategory !in selectedCategories || segment.size < 2) return null

            val startSeconds = segment[0]
            val endSeconds = segment[1]
            if (
                !startSeconds.isFinite() ||
                !endSeconds.isFinite() ||
                startSeconds < 0.0 ||
                endSeconds <= startSeconds
            ) {
                return null
            }
            if (endSeconds > MAX_SUPPORTED_SECONDS) return null

            val startMs = (startSeconds * MILLIS_PER_SECOND).roundToLong().coerceAtLeast(0L)
            val endMs = (endSeconds * MILLIS_PER_SECOND).roundToLong()
            if (endMs - startMs < MIN_SEGMENT_DURATION_MILLIS) return null
            return SponsorBlockSegment(startMs = startMs, endMs = endMs)
        }

        /**
         * The first [HASH_PREFIX_LENGTH] hex characters of the video id's SHA-256.
         *
         * This is the API's privacy prefix: the request says "give me everything
         * that hashes to abcd" and the app picks its own video out of the answer,
         * so the database never learns which video is being watched.
         */
        private fun String.sha256Prefix(): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
            return buildString(HASH_PREFIX_LENGTH) {
                for (index in 0 until HASH_PREFIX_LENGTH / 2) {
                    val value = digest[index].toInt() and 0xFF
                    append(HEX_DIGITS[value ushr 4])
                    append(HEX_DIGITS[value and 0x0F])
                }
            }
        }

        private data class SponsorBlockRequestKey(
            val videoId: String,
            val categories: List<String>,
        )

        private companion object {
            const val TAG = "SponsorBlock"
            const val REQUEST_TIMEOUT_MILLIS = 10_000L
            const val CONNECT_TIMEOUT_MILLIS = 8_000L
            const val LOOKUP_ATTEMPTS = 2
            const val RETRY_DELAY_MILLIS = 500L
            const val MAX_CACHE_ENTRIES = 64
            const val HASH_PREFIX_LENGTH = 4
            const val SKIP_ACTION_TYPES_JSON = "[\"skip\"]"
            const val YOUTUBE_SERVICE = "YouTube"
            const val HEX_DIGITS = "0123456789abcdef"
            const val SKIP_ACTION_TYPE = "skip"
            const val MILLIS_PER_SECOND = 1_000.0
            const val MIN_SEGMENT_DURATION_MILLIS = 100L
            const val MAX_SUPPORTED_SECONDS = Long.MAX_VALUE / 1_000.0
            val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
        }
    }
