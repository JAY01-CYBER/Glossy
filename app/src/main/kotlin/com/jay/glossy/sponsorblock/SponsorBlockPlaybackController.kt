/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * SponsorBlock support ported from ArchiveTune
 * (https://github.com/rukamori/ArchiveTune), GPL-3.0.
 */

package com.jay.glossy.sponsorblock

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
        private var observationJob: Job? = null

        private val playerListener =
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    currentMediaId.value = mediaItem.normalizedMediaId()
                }

                override fun onTimelineChanged(
                    timeline: androidx.media3.common.Timeline,
                    reason: Int,
                ) {
                    currentMediaId.value = attachedPlayer?.currentMediaItem.normalizedMediaId()
                }
            }

        fun attach(
            player: Player,
            scope: CoroutineScope,
        ) {
            if (attachedPlayer === player && observationJob?.isActive == true) return
            detach()

            attachedPlayer = player
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

                                val segments = repository.getSegments(activeMediaId, settings)
                                if (segments.isEmpty() || currentMediaId.value != activeMediaId) {
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
            attachedPlayer?.removeListener(playerListener)
            attachedPlayer = null
            currentMediaId.value = null
        }

        private suspend fun monitorPlayback(
            player: Player,
            mediaId: String,
            segments: List<SponsorBlockSegment>,
        ) {
            // The end of the last segment that was jumped. It keeps the same
            // segment from being sought twice while the player settles after a
            // seek and still reports a position inside it.
            var lastSkippedEndMs: Long? = null
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
                if (activeSegment == null) {
                    lastSkippedEndMs = null
                } else if (lastSkippedEndMs != activeSegment.endMs) {
                    lastSkippedEndMs = activeSegment.endMs
                    val knownDuration =
                        player.duration.takeIf { duration ->
                            duration != C.TIME_UNSET && duration > 0L
                        }
                    val targetPositionMs =
                        knownDuration
                            ?.let { duration -> activeSegment.endMs.coerceAtMost(duration) }
                            ?: activeSegment.endMs
                    if (targetPositionMs - positionMs >= MIN_SEEK_DISTANCE_MILLIS) {
                        player.seekTo(targetPositionMs)
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
        }
    }
