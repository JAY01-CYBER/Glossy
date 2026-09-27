/*
 * Sound FX models ported from ArchiveTune (2026) © Rukamori — github.com/rukamori (GPL-3.0).
 * Adapted for Glossy's system audio effect equalizer.
 */

package com.jay.glossy.eq.soundfx

import androidx.datastore.preferences.core.Preferences
import com.jay.glossy.constants.SoundFxBandLevelsMbKey
import com.jay.glossy.constants.SoundFxBassBoostEnabledKey
import com.jay.glossy.constants.SoundFxBassBoostStrengthKey
import com.jay.glossy.constants.SoundFxControlModeKey
import com.jay.glossy.constants.SoundFxEnabledKey
import com.jay.glossy.constants.SoundFxOutputGainEnabledKey
import com.jay.glossy.constants.SoundFxOutputGainMbKey
import com.jay.glossy.constants.SoundFxAutoHeadroomKey
import com.jay.glossy.constants.SoundFxVirtualizerEnabledKey
import com.jay.glossy.constants.SoundFxVirtualizerStrengthKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.floor

enum class SoundFxControlMode(
    val storageValue: String,
) {
    BASIC("basic"),
    ADVANCED("advanced"),
    ;

    companion object {
        fun fromStorage(value: String?): SoundFxControlMode = entries.firstOrNull { it.storageValue == value } ?: BASIC
    }
}

enum class SoundFxTone {
    BASS,
    MIDRANGE,
    TREBLE,
}

/**
 * What the device's system equalizer supports. Populated once the audio effect
 * is attached to a live audio session (i.e. playback has started).
 */
data class SoundFxCapabilities(
    val bandCount: Int,
    val bandCenterFreqHz: List<Int>,
    val minBandLevelMb: Int,
    val maxBandLevelMb: Int,
    val presetNames: List<String>,
)

data class SoundFxSettings(
    val enabled: Boolean = false,
    val bandLevelsMb: List<Int> = emptyList(),
    val outputGainEnabled: Boolean = false,
    val outputGainMb: Int = 0,
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,
    val autoHeadroomEnabled: Boolean = false,
) {
    /**
     * Output gain actually sent to [LoudnessEnhancer]. When auto headroom is on,
     * the positive band and bass boost is subtracted so the boosted signal no
     * longer drives the output into hard clipping.
     */
    fun effectiveOutputGainMb(capabilities: SoundFxCapabilities?): Int {
        if (!autoHeadroomEnabled) return outputGainMb
        val maxBandBoostMb = bandLevelsMb.maxOrNull()?.coerceAtLeast(0) ?: 0
        val bassReserveMb = if (bassBoostEnabled) bassBoostStrength.coerceIn(0, MAX_EFFECT_STRENGTH) else 0
        return (outputGainMb - maxBandBoostMb - bassReserveMb).coerceIn(MIN_OUTPUT_GAIN_MB, MAX_OUTPUT_GAIN_MB)
    }

    companion object {
        const val MIN_OUTPUT_GAIN_MB = -1500
        const val MAX_OUTPUT_GAIN_MB = 1500
        const val MAX_EFFECT_STRENGTH = 1000

        fun fromPreferences(prefs: Preferences): SoundFxSettings =
            SoundFxSettings(
                enabled = prefs[SoundFxEnabledKey] ?: false,
                bandLevelsMb = decodeBandLevels(prefs[SoundFxBandLevelsMbKey]),
                outputGainEnabled = prefs[SoundFxOutputGainEnabledKey] ?: false,
                outputGainMb = (prefs[SoundFxOutputGainMbKey] ?: 0).coerceIn(MIN_OUTPUT_GAIN_MB, MAX_OUTPUT_GAIN_MB),
                bassBoostEnabled = prefs[SoundFxBassBoostEnabledKey] ?: false,
                bassBoostStrength = (prefs[SoundFxBassBoostStrengthKey] ?: 0).coerceIn(0, MAX_EFFECT_STRENGTH),
                virtualizerEnabled = prefs[SoundFxVirtualizerEnabledKey] ?: false,
                virtualizerStrength = (prefs[SoundFxVirtualizerStrengthKey] ?: 0).coerceIn(0, MAX_EFFECT_STRENGTH),
                autoHeadroomEnabled = prefs[SoundFxAutoHeadroomKey] ?: false,
            )
    }
}

/**
 * A saved sound fx setup. Field names match ArchiveTune's exported profile JSON
 * so profiles are interchangeable between the two apps.
 */
@Serializable
data class SoundFxProfile(
    val id: String,
    val name: String,
    val bandCenterFreqHz: List<Int> = emptyList(),
    val bandLevelsMb: List<Int> = emptyList(),
    val outputGainMb: Int = 0,
    val outputGainEnabled: Boolean? = null,
    val bassBoostStrength: Int = 0,
    val bassBoostEnabled: Boolean? = null,
    val virtualizerStrength: Int = 0,
    val virtualizerEnabled: Boolean? = null,
    val autoHeadroomEnabled: Boolean = false,
)

@Serializable
data class SoundFxProfilesPayload(
    val profiles: List<SoundFxProfile>,
)

val SoundFxJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

data class SoundFxConfiguration(
    val controlMode: SoundFxControlMode,
    val selectedProfileId: String,
    val settings: SoundFxSettings,
    val capabilities: SoundFxCapabilities?,
    val profiles: List<SoundFxProfile>,
)

/**
 * Resample stored band levels to the device's actual band count using linear
 * interpolation (ported from ArchiveTune).
 */
internal fun resampleLevels(
    levelsMb: List<Int>,
    targetCount: Int,
): List<Int> {
    if (targetCount <= 0) return emptyList()
    if (levelsMb.isEmpty()) return List(targetCount) { 0 }
    if (levelsMb.size == targetCount) return levelsMb
    if (targetCount == 1) return listOf(levelsMb.average().toInt())
    val lastIndex = levelsMb.lastIndex.toFloat().coerceAtLeast(1f)
    return List(targetCount) { index ->
        val position = index * lastIndex / (targetCount - 1)
        val lower = floor(position).toInt().coerceIn(0, levelsMb.lastIndex)
        val upper = ceil(position).toInt().coerceIn(0, levelsMb.lastIndex)
        (levelsMb[lower] + ((levelsMb[upper] - levelsMb[lower]) * (position - lower))).toInt()
    }
}

/**
 * Map a tone (bass / midrange / treble) to the equalizer band indices it
 * affects, preferring the device's real center frequencies over a positional
 * split (ported from ArchiveTune).
 */
internal fun soundFxToneIndices(
    tone: SoundFxTone,
    frequenciesHz: List<Int>,
    bandCount: Int,
): List<Int> {
    if (bandCount <= 0) return emptyList()
    val frequencyMatches =
        frequenciesHz
            .takeIf { it.size == bandCount && it.any { frequency -> frequency > 0 } }
            ?.indices
            ?.filter { index ->
                when (tone) {
                    SoundFxTone.BASS -> frequenciesHz[index] <= 250
                    SoundFxTone.MIDRANGE -> frequenciesHz[index] in 251..4_000
                    SoundFxTone.TREBLE -> frequenciesHz[index] > 4_000
                }
            }.orEmpty()
    if (frequencyMatches.isNotEmpty()) return frequencyMatches

    val firstBoundary = ceil(bandCount / 3.0).toInt()
    val secondBoundary = ceil(bandCount * 2 / 3.0).toInt()
    val range =
        when (tone) {
            SoundFxTone.BASS -> 0 until firstBoundary
            SoundFxTone.MIDRANGE -> firstBoundary until secondBoundary
            SoundFxTone.TREBLE -> secondBoundary until bandCount
        }.toList()
    if (range.isNotEmpty()) return range
    return listOf(
        when (tone) {
            SoundFxTone.BASS -> 0
            SoundFxTone.MIDRANGE -> (bandCount - 1) / 2
            SoundFxTone.TREBLE -> bandCount - 1
        },
    )
}

/**
 * Reasonable 5-band layout used by the UI before the device's real
 * capabilities become available (they load as soon as playback starts).
 */
fun fallbackSoundFxCapabilities(): SoundFxCapabilities =
    SoundFxCapabilities(
        bandCount = 5,
        bandCenterFreqHz = listOf(60, 230, 910, 3_600, 14_000),
        minBandLevelMb = -1500,
        maxBandLevelMb = 1500,
        presetNames = emptyList(),
    )

fun decodeBandLevels(raw: String?): List<Int> =
    raw
        ?.takeIf(String::isNotBlank)
        ?.let { runCatching { SoundFxJson.decodeFromString<List<Int>>(it) }.getOrNull() }
        .orEmpty()
