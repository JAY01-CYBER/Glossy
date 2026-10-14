/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * The player's opening animation: a bloom of light that grows out of the mini
 * player's artwork as the bar rises into the full player — the popup that the
 * sea-glass surfaces cannot provide on a phone below API 33.
 *
 * The bloom is driven by the sheet's own [progress] rather than by its own
 * timer, so it is the same motion the sheet is already making: dragging the bar
 * upwards scrubs the bloom by hand, letting go half way leaves it half drawn,
 * and collapsing reverses it. A promise like this is worth keeping — the
 * animation can never disagree with the sheet, or arrive after it.
 *
 * Draws nothing at either end of the gesture (collapsed, and fully open), so an
 * already-open player costs no drawing at all.
 */
@Composable
fun PlayerOpenBloom(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (progress <= 0.01f || progress >= 0.999f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val eased = 1f - (1f - progress.coerceIn(0f, 1f)) * (1f - progress.coerceIn(0f, 1f))

        // The mini player's artwork sits a quarter of the way in from the left,
        // just above the bottom edge — the bloom starts where the eye already
        // is, so the light reads as having come out of that thumbnail.
        val origin = Offset(size.width * 0.25f, size.height * 0.94f)
        val start = 24.dp.toPx()
        val radius = start + (size.maxDimension * 1.2f - start) * eased
        // Brightest at the moment of the tap and gone by the time the player has
        // arrived: the bloom announces the transition, it does not decorate it.
        val fade = (1f - eased).coerceIn(0f, 1f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.40f * fade), Color.Transparent),
                center = origin,
                radius = radius,
            ),
            radius = radius,
            center = origin,
        )
        drawCircle(
            color = accent.copy(alpha = 0.30f * fade),
            radius = radius,
            center = origin,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
