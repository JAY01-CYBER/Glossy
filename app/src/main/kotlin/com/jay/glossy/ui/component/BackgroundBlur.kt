/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.constants.BackgroundBlurEnabledKey
import com.jay.glossy.constants.BackgroundBlurStrengthKey
import com.jay.glossy.playback.PlayerConnection
import com.jay.glossy.utils.rememberPreference

/**
 * App backdrop: the current song's artwork, blurred, sitting behind everything,
 * with a strength-driven scrim on top for text legibility. Falls back to the
 * plain theme base when nothing is playing.
 *
 * Deliberately the *still* artwork, never the animated canvas. A canvas is a
 * looping video, and a backdrop is a pane the whole app is read through — the
 * two do not belong together. Running a second decoder, at a quarter of the
 * screen and blurred, behind every screen cost frames and battery to produce
 * something the scrim then hid, and it left the canvas looking like it was
 * showing in two places at once. The canvas has one home: the artwork it
 * belongs to, on the player and on a Featured Spotlight card. Nothing gets
 * resolved, downloaded or decoded for this backdrop any more.
 *
 * The artwork is blurred on the CPU, so it looks the same on every API level.
 */
@Composable
fun BackgroundBlurBackdrop(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
) {
    val (enabled, _) = rememberPreference(BackgroundBlurEnabledKey, defaultValue = true)
    val (strength, _) = rememberPreference(BackgroundBlurStrengthKey, defaultValue = 0.6f)

    if (!enabled || strength <= 0.01f) return

    val s = strength.coerceIn(0f, 1f)
    val scheme = MaterialTheme.colorScheme
    val base = if (pureBlack) Color.Black else scheme.background
    // Denser scrims than a soft frost: the backdrop should read as one hard,
    // opaque pane rather than letting the artwork bleed through it.
    val scrimTop = if (pureBlack) 0.62f else 0.62f - 0.18f * s
    val scrimBottom = if (pureBlack) 0.85f else 0.80f - 0.20f * s

    val metadata = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val artworkUrl = metadata?.thumbnailUrl

    Box(modifier = modifier.fillMaxSize().background(base)) {
        // Blurred artwork underneath.
        if (!artworkUrl.isNullOrBlank()) {
            BlurredArtworkBackdrop(
                url = artworkUrl,
                blurStrength = s,
            )
        }
        // Legibility scrim (strength-driven).
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                base.copy(alpha = scrimTop),
                                base.copy(alpha = scrimBottom),
                            ),
                        ),
                    ),
        )
    }
}
