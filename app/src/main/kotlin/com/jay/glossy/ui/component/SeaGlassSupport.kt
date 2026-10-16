/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Whether this device can run the real SeaGlass pass.
 *
 * The frost is a `RuntimeShader`, and `RuntimeShader` only exists from API 33.
 * Below that the same surfaces are drawn by [seaGlassFallback] instead, which
 * keeps the look — tint, highlight, chalky rim — without the shader, so a pane
 * of sea glass is still *visible* on an older phone rather than collapsing into
 * a flat fill.
 *
 * This was previously pinned off outright, so every call site took its flat
 * branch on every device and the glass never shipped at all. The glass is the
 * whole point of these surfaces, so the tier check is back, and the fallback it
 * leaves for older devices is now a painted surface rather than a fill.
 */
val seaGlassSurfacesAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * The sea-glass body for a device with no shader to run one on.
 *
 * A real pane is a tinted, frosted body with light caught on its lip, so that
 * is what is painted here: the tint densest at the top edge, a highlight thrown
 * back at the light, and a chalky rim around the outline. It is deliberately
 * *not* a flat fill — a flat fill is what made the glass invisible below API 33
 * in the first place.
 *
 * Draw it in place of the shader, not underneath it: it clips itself to [shape]
 * and paints its own rim, so a fill behind it would only flatten it back out.
 */
fun Modifier.seaGlassFallback(
    fill: Color,
    shape: Shape,
): Modifier =
    this
        .clip(shape)
        // The body: denser where the light lands, thinning downwards the way a
        // frosted pane does.
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    fill.copy(alpha = 0.58f),
                    fill.copy(alpha = 0.34f),
                ),
            ),
        )
        // The highlight the frost throws back, and a little bounce along the
        // bottom lip so the body reads as glass rather than as a gradient.
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                ),
            ),
        )
        .border(1.dp, Color.White.copy(alpha = 0.32f), shape)
