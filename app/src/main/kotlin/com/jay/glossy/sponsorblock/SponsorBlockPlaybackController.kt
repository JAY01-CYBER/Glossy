/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * SponsorBlock support ported from ArchiveTune
 * (https://github.com/rukamori/ArchiveTune), GPL-3.0.
 */

package com.jay.glossy.sponsorblock

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Skips the segments SponsorBlock marks inside a playing video.
 *
 * Attached to the service's player rather than to a screen: the skip has to
 * happen with the phone in a pocket and no UI composed, so it lives as long as
 * playback does.
 *
 * Two races decide whether a sponsor read is really skipped, and both are
 * handled here instead of being left to luck:
 *
 * - A queued song begins the instant the previous one ends, so a lookup started
 *   at that moment is racing the very segment it means to skip. The next items
 *   of the queue are therefore asked for in advance — a queued track is already
 *   answered before it starts and can be skipped from its first millisecond,
 *   instead of only skipping when it is replayed and the answer is cached.
 * - A seek the player swallows must not be remembered as done. A segment only
 *   counts as skipped once the position has actually left it, and the seek is
 *   tried again when it has not.
 *
 * The check is a poll, not a listener — Media3 only reports position changes at
 * coarse intervals, and a 30-second sponsor read would be most of the way
 * through before a coarse report arrived. While playing it looks every
 * [ACTIVE_POLL_INTERVAL_MILLIS]; when paused it backs off, because a paused
 * player cannot enter a segment.
 */
@Singleton
class SponsorBlockPlaybackController
    @Inject
    constructor(
        private val repository: SponsorBlockRepository,
    ) {
        private val currentMediaId = MutableStateFlow<String?>(null)
        private var attachedPlayer: Player? = null
        private var attachedScope: CoroutineScope? = null
        private var observationJob: Job? = null
        private var prefetchJob: Job? = null

        private val playerListener =
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    currentMediaId.value = mediaItem.normalizedMediaId()
                    prefetchUpcoming()
                }

                override fun onTimelineChanged(
                    timeline: androidx.media3.common.Timeline,
                    reason: Int,
                ) {
                    currentMediaId.value = attachedPlayer?.currentMediaItem.normalizedMediaId()
                    prefetchUpcoming()
                }
            }

        fun attach(
            player: Player,
            scope: CoroutineScope,
        ) {
            if (attachedPlayer === player && observationJob?.isActive == true) return
            detach()

            attachedPlayer = player
            attachedScope = scope
            currentMediaId.value = player.currentMediaItem.normalizedMediaId()
            player.addListener(playerListener)
            observationJob =
                scope.launch {
                    try {
                        combine(
                            repository.observeSettings(),
                            currentMediaId,
                        ) { settings, mediaId -> settings to mediaId }
                            .collectLatest { (settings, mediaId) ->
                                val activeMediaId = mediaId ?: return@collectLatest
                                if (!settings.enabled || settings.categories.isEmpty()) {
                                    return@collectLatest
                                }

                                val segments = awaitSegments(activeMediaId, settings)
                                if (segments == null ||
                                    segments.isEmpty() ||
                                    currentMediaId.value != activeMediaId
                                ) {
                                    return@collectLatest
                                }

                                monitorPlayback(
                                    player = player,
                                    mediaId = activeMediaId,
                                    segments = segments,
                                )
                            }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        Timber.tag(TAG).w(throwable, "SponsorBlock settings observation failed")
                    }
                }

            // A queue can already be loaded when the player is swapped (audio
            // offload, crossfade), so the song after it needs asking for here as
            // well as on the next transition.
            prefetchUpcoming()
        }

        /**
         * Stops watching the currently attached player.
         *
         * Called both when the service swaps its player (audio-offload and
         * crossfade both build a new one) and when the controller itself is
         * replaced; keeping the listener on a dead player would otherwise leave
         * this object observing an instance nothing plays through.
         */
        fun detach() {
            observationJob?.cancel()
            observationJob = null
            prefetchJob?.cancel()
            prefetchJob = null
            attachedPlayer?.removeListener(playerListener)
            attachedPlayer = null
            attachedScope = null
            currentMediaId.value = null
        }

        /**
         * The segments of the song that is playing now, asked again when the
         * first answer never arrived.
         *
         * A song gets one continuous chance at a skip: if the lookup times out
         * at 0:00 and gives up, a sponsor read at 2:00 would play in full even
         * though the network came back a moment later. Asking again while the
         * song still plays costs nothing when there is nothing to skip and saves
         * the song when there is.
         */
        private suspend fun awaitSegments(
            mediaId: String,
            settings: SponsorBlockSettings,
        ): List<SponsorBlockSegment>? {
            repeat(CURRENT_LOOKUP_ATTEMPTS) { attemptIndex ->
                val segments = repository.getSegments(mediaId, settings)
                if (segments != null) return segments
                if (attemptIndex < CURRENT_LOOKUP_ATTEMPTS - 1) {
                    delay(CURRENT_LOOKUP_RETRY_MILLIS)
                }
            }
            return null
        }

        /**
         * Asks for the segments of what is queued up next, while the current
         * song is still playing.
         *
         * Debounced because setting a queue changes the timeline once per item
         * added: without the wait, dropping a fifty-song queue would fire fifty
         * lookups for songs nobody is going to hear for an hour.
         */
        private fun prefetchUpcoming() {
            val player = attachedPlayer ?: return
            val scope = attachedScope ?: return
            prefetchJob?.cancel()
            prefetchJob =
                scope.launch {
                    try {
                        delay(PREFETCH_DEBOUNCE_MILLIS)
                        // A player released by a crossfade in the meantime is no
                        // longer a queue worth reading, and touching it would
                        // throw out of this scope.
                        if (attachedPlayer !== player) return@launch
                        val mediaIds = upcomingMediaIds(player)
                        if (mediaIds.isEmpty()) return@launch
                        val settings = repository.observeSettings().first()
                        repository.prefetchSegments(mediaIds, settings)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        Timber.tag(TAG).w(throwable, "SponsorBlock prefetch failed")
                    }
                }
        }

        /**
         * The media ids of the few items that will play after the current one.
         *
         * Read through [Player.getMediaItemAt] on the current index rather than
         * from the service's song list so shuffle order and any item inserted
         * while playing are respected: this asks about the song that is actually
         * next, not the one that is next in the album.
         */
        private fun upcomingMediaIds(player: Player): List<String> {
            val currentIndex = player.currentMediaItemIndex
            if (currentIndex == C.INDEX_UNSET) return emptyList()

            return buildList {
                for (offset in 1..PREFETCH_AHEAD_ITEMS) {
                    val index = currentIndex + offset
                    if (index >= player.mediaItemCount) break
                    player.getMediaItemAt(index).normalizedMediaId()?.let { mediaId -> add(mediaId) }
                }
            }
        }

        private suspend fun monitorPlayback(
            player: Player,
            mediaId: String,
            segments: List<SponsorBlockSegment>,
        ) {
            // When the last skip was attempted, so a seek the player swallowed is
            // tried again instead of being remembered as a completed skip.
            var lastSeekAttemptMs: Long? = null
            while (
                currentCoroutineContext().isActive &&
                attachedPlayer === player &&
                player.currentMediaItem.normalizedMediaId() == mediaId
            ) {
                if (!player.isPlaying || player.playbackState != Player.STATE_READY) {
                    delay(INACTIVE_POLL_INTERVAL_MILLIS)
                    continue
                }

                val positionMs = player.currentPosition.coerceAtLeast(0L)
                val activeSegment =
                    segments.firstOrNull { segment ->
                        positionMs >= segment.startMs &&
                            positionMs < segment.endMs - END_TOLERANCE_MILLIS
                    }

                if (activeSegment != null) {
                    val knownDuration =
                        player.duration.takeIf { duration ->
                            duration != C.TIME_UNSET && duration > 0L
                        }
                    val targetPositionMs =
                        knownDuration
                            ?.let { duration -> activeSegment.endMs.coerceAtMost(duration) }
                            ?: activeSegment.endMs
                    val nowMs = SystemClock.elapsedRealtime()
                    val previousAttemptMs = lastSeekAttemptMs
                    val seekIsDue =
                        previousAttemptMs == null ||
                            nowMs - previousAttemptMs >= SEEK_RETRY_INTERVAL_MILLIS
                    if (targetPositionMs - positionMs >= MIN_SEEK_DISTANCE_MILLIS && seekIsDue) {
                        lastSeekAttemptMs = nowMs
                        player.seekTo(targetPositionMs)
                        Timber
                            .tag(TAG)
                            .d("Skipping ${targetPositionMs - positionMs}ms of $mediaId from $positionMs")
                    }
                }

                delay(ACTIVE_POLL_INTERVAL_MILLIS)
            }
        }

        private fun MediaItem?.normalizedMediaId(): String? =
            this
                ?.mediaId
                ?.trim()
                ?.takeIf(String::isNotEmpty)

        private companion object {
            const val TAG = "SponsorBlock"
            const val ACTIVE_POLL_INTERVAL_MILLIS = 200L
            const val INACTIVE_POLL_INTERVAL_MILLIS = 750L
            const val END_TOLERANCE_MILLIS = 50L
            const val MIN_SEEK_DISTANCE_MILLIS = 100L
            const val SEEK_RETRY_INTERVAL_MILLIS = 1_000L
            const val CURRENT_LOOKUP_ATTEMPTS = 2
            const val CURRENT_LOOKUP_RETRY_MILLIS = 12_000L
            const val PREFETCH_AHEAD_ITEMS = 2
            const val PREFETCH_DEBOUNCE_MILLIS = 250L
        }
    }
