/*
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * One-tap audio boost.
 *
 * The boost is deliberately *not* a second audio effect: it is a level that is
 * folded into [SoundFxSettings] as those settings are handed to the live audio
 * session (see SoundFxSettings.fromPreferences). That means it drives the very
 * same LoudnessEnhancer the equalizer screen's output-gain slider drives, it
 * needs no extra plumbing to reach the player, and it costs nothing when it is
 * off.
 *
 * Because it is layered on rather than stored, turning it off restores the
 * hand-tuned equalizer exactly as it was, and the output-gain slider keeps
 * winning whenever it is already set higher than the level asks for.
 */

package com.jay.glossy.eq.soundfx

import androidx.datastore.preferences.core.Preferences
import com.jay.glossy.constants.AudioBoostLevelKey

/**
 * How much louder the output is pushed, in millibels of LoudnessEnhancer gain
 * (1000 mB = 10 dB — a doubling of perceived loudness is ~10 dB).
 *
 * The steps stop at +9 dB on purpose: below that a phone speaker or cheap
 * earbuds still have headroom, and anything above it mostly buys clipping. For
 * anything more extreme the equalizer screen's own gain slider, with auto
 * headroom, is the tool.
 */
enum class AudioBoostLevel(
    /** Extra LoudnessEnhancer gain, in millibels. */
    val outputGainMb: Int,
) {
    OFF(0),
    LIGHT(250),
    STRONG(500),
    MAXIMUM(900),
    ;

    val isOn: Boolean get() = this != OFF

    companion object {
        fun fromStorage(value: String?): AudioBoostLevel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OFF

        fun fromPreferences(prefs: Preferences): AudioBoostLevel = fromStorage(prefs[AudioBoostLevelKey])
    }
}
