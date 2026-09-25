/*
 * Sound FX controller ported from ArchiveTune (2026) © Rukamori — github.com/rukamori (GPL-3.0).
 * Bridges the app to Android's system audio effects (Equalizer, BassBoost,
 * Virtualizer, LoudnessEnhancer) attached to the player's audio session.
 */

package com.jay.glossy.eq.soundfx

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the system audio effect instances for the currently active audio
 * session and applies [SoundFxSettings] to them. Effects are attached when the
 * playback audio session becomes available (see MusicService#openAudioEffectSession)
 * and released when it closes. All state survives re-attachment.
 */
@Singleton
class PlaybackSoundFxController
    @Inject
    constructor() {
        private val lock = Any()

        private var equalizer: Equalizer? = null
        private var bassBoost: BassBoost? = null
        private var virtualizer: Virtualizer? = null
        private var loudnessEnhancer: LoudnessEnhancer? = null
        private var attachedSessionId: Int = SESSION_NONE

        @Volatile
        private var lastSettings: SoundFxSettings? = null

        private val _capabilities = kotlinx.coroutines.flow.MutableStateFlow<SoundFxCapabilities?>(null)
        val capabilities: kotlinx.coroutines.flow.StateFlow<SoundFxCapabilities?> = _capabilities

        /**
         * Attach (or re-attach) all effects to the given audio session. Any
         * previously attached session is released first. Safe to call
         * repeatedly with the same session id.
         */
        fun attach(sessionId: Int) =
            synchronized(lock) {
                if (sessionId <= 0) return
                if (attachedSessionId == sessionId && equalizer != null) return
                releaseInternal()
                try {
                    val eq = Equalizer(PRIORITY, sessionId)
                    val bb = runCatching { BassBoost(PRIORITY, sessionId) }.getOrNull()
                    val vr = runCatching { Virtualizer(PRIORITY, sessionId) }.getOrNull()
                    val le = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
                    equalizer = eq
                    bassBoost = bb
                    virtualizer = vr
                    loudnessEnhancer = le
                    attachedSessionId = sessionId
                    _capabilities.value = readCapabilities(eq)
                    lastSettings?.let { applyInternal(it) }
                    Timber
                        .tag(TAG)
                        .d("Sound fx attached to session %d (bands=%d, presets=%d)", sessionId, eq.numberOfBands, eq.numberOfPresets)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to attach sound fx to session %d", sessionId)
                    releaseInternal()
                }
            }

        /** Release all effect instances and clear capabilities. */
        fun release() =
            synchronized(lock) {
                releaseInternal()
            }

        /** Apply the given settings to the attached effects (stores them for re-attachment). */
        fun apply(settings: SoundFxSettings) =
            synchronized(lock) {
                lastSettings = settings
                applyInternal(settings)
            }

        /**
         * Activate a device-provided preset on the live equalizer. Returns false
         * when the equalizer is not attached yet or the device rejects it.
         */
        fun usePreset(index: Int): Boolean =
            synchronized(lock) {
                val eq = equalizer ?: return false
                runCatching {
                    eq.usePreset(index.toShort())
                    true
                }.getOrElse { e ->
                    Timber.tag(TAG).e(e, "Failed to use preset %d", index)
                    false
                }
            }

        /** Read the live band levels from the attached equalizer, if any. */
        fun readCurrentBandLevels(): List<Int>? =
            synchronized(lock) {
                val eq = equalizer ?: return null
                runCatching {
                    List(eq.numberOfBands.toInt()) { band -> eq.getBandLevel(band.toShort()).toInt() }
                }.getOrNull()
            }

        private fun applyInternal(settings: SoundFxSettings) {
            val eq = equalizer ?: return
            val capabilities = _capabilities.value

            runCatching { eq.enabled = settings.enabled }
                .onFailure { Timber.tag(TAG).w(it, "Failed to set equalizer enabled") }

            runCatching {
                val bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                val levels = resampleLevels(settings.bandLevelsMb, bandCount)
                for (band in 0 until bandCount) {
                    val target = (levels.getOrNull(band) ?: 0).coerceIn(minMb, maxMb)
                    eq.setBandLevel(band.toShort(), target.toShort())
                }
            }.onFailure { Timber.tag(TAG).w(it, "Failed to set band levels") }

            bassBoost?.let { bb ->
                runCatching {
                    bb.enabled = settings.enabled && settings.bassBoostEnabled
                    bb.setStrength(settings.bassBoostStrength.coerceIn(0, SoundFxSettings.MAX_EFFECT_STRENGTH).toShort())
                }.onFailure { Timber.tag(TAG).w(it, "Failed to apply bass boost") }
            }

            virtualizer?.let { vr ->
                runCatching {
                    vr.enabled = settings.enabled && settings.virtualizerEnabled
                    vr.setStrength(settings.virtualizerStrength.coerceIn(0, SoundFxSettings.MAX_EFFECT_STRENGTH).toShort())
                }.onFailure { Timber.tag(TAG).w(it, "Failed to apply virtualizer") }
            }

            loudnessEnhancer?.let { le ->
                runCatching {
                    val gainMb = settings.effectiveOutputGainMb(capabilities)
                    le.setTargetGain(gainMb)
                    le.enabled = settings.enabled && settings.outputGainEnabled && gainMb != 0
                }.onFailure { Timber.tag(TAG).w(it, "Failed to apply output gain") }
            }
        }

        private fun readCapabilities(eq: Equalizer): SoundFxCapabilities? =
            runCatching {
                val bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val centerFreqHz = (0 until bandCount).map { band -> eq.getCenterFreq(band.toShort()) / 1000 }
                val presetNames =
                    (0 until eq.numberOfPresets.toInt()).map { index ->
                        runCatching { eq.getPresetName(index.toShort()) }.getOrDefault("Preset ${index + 1}")
                    }
                SoundFxCapabilities(
                    bandCount = bandCount,
                    bandCenterFreqHz = centerFreqHz,
                    minBandLevelMb = range[0].toInt(),
                    maxBandLevelMb = range[1].toInt(),
                    presetNames = presetNames,
                )
            }.onFailure { Timber.tag(TAG).w(it, "Failed to read equalizer capabilities") }.getOrNull()

        private fun releaseInternal() {
            runCatching { loudnessEnhancer?.release() }
            runCatching { virtualizer?.release() }
            runCatching { bassBoost?.release() }
            runCatching { equalizer?.release() }
            loudnessEnhancer = null
            virtualizer = null
            bassBoost = null
            equalizer = null
            attachedSessionId = SESSION_NONE
            _capabilities.value = null
        }

        private companion object {
            const val TAG = "SoundFxController"
            const val PRIORITY = 0
            const val SESSION_NONE = Int.MIN_VALUE
        }
    }
