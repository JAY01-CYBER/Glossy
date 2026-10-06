/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The one-tap audio boost chooser, shared by Settings > Player and the player's
 * overflow menu so both surfaces offer the same four levels with the same
 * wording. All it does is pick a level; the level itself is what the audio
 * session reads (see com.jay.glossy.eq.soundfx.AudioBoostLevel).
 */

package com.jay.glossy.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jay.glossy.R
import com.jay.glossy.eq.soundfx.AudioBoostLevel

/**
 * Radio list of the boost levels. [onSelect] receives the chosen level and the
 * caller decides whether to dismiss.
 */
@Composable
fun AudioBoostDialog(
    current: AudioBoostLevel,
    onDismiss: () -> Unit,
    onSelect: (AudioBoostLevel) -> Unit,
) {
    EnumDialog(
        onDismiss = onDismiss,
        onSelect = onSelect,
        title = stringResource(R.string.audio_boost),
        current = current,
        values = AudioBoostLevel.entries.toList(),
        valueText = { getAudioBoostLabel(it) },
        valueDescription = { getAudioBoostDescription(it) },
    )
}

/** Short name of a level, with the gain it asks for: "Strong · +5 dB". */
@Composable
fun getAudioBoostLabel(level: AudioBoostLevel): String =
    when (level) {
        AudioBoostLevel.OFF -> stringResource(R.string.audio_boost_off)
        AudioBoostLevel.LIGHT -> stringResource(R.string.audio_boost_light)
        AudioBoostLevel.STRONG -> stringResource(R.string.audio_boost_strong)
        AudioBoostLevel.MAXIMUM -> stringResource(R.string.audio_boost_maximum)
    }

/** One line on what a level does to the sound, shown under the label. */
@Composable
fun getAudioBoostDescription(level: AudioBoostLevel): String =
    when (level) {
        AudioBoostLevel.OFF -> stringResource(R.string.audio_boost_off_desc)
        AudioBoostLevel.LIGHT -> stringResource(R.string.audio_boost_light_desc)
        AudioBoostLevel.STRONG -> stringResource(R.string.audio_boost_strong_desc)
        AudioBoostLevel.MAXIMUM -> stringResource(R.string.audio_boost_maximum_desc)
    }
