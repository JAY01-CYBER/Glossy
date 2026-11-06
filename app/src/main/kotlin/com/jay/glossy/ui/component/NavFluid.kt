/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The bottom bar's lava wash.
 *
 * The navigation pill is the one surface in the app that is always on screen,
 * so it is the one place where a slow, ambient gradient earns its keep: the
 * fluid sits behind the tabs, never over them, and gives the bar depth without
 * borrowing the wallpaper's colours. Everything here is painted from the
 * theme's accent colours, so the motion survives a palette change.
 */

package com.jay.glossy.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = 2f * PI.toFloat()

/**
 * The two clocks the wash drifts on.
 *
 * They are kept as [State] rather than floats so the animation is read inside
 * the draw phase: a frame costs three gradients and no recomposition, which
 * matters for a bar that lives on screen for the whole session.
 */
internal class NavFluid(
    internal val drift: State<Float>,
    internal val swirl: State<Float>,
)

/**
 * Creates the wash's clocks, or a still pair when ambient motion is off.
 *
 * The reduced-motion frame is deliberately not blank: the wash is part of the
 * bar's paint, and losing it would make the bar change shape between settings.
 * It simply stops moving, which is what "remove animations" asks for.
 */
@Composable
internal fun rememberNavFluid(enabled: Boolean): NavFluid {
    if (!enabled) {
        val stillDrift = remember { mutableStateOf(0.16f) }
        val stillSwirl = remember { mutableStateOf(0.63f) }
        return remember { NavFluid(stillDrift, stillSwirl) }
    }

    val transition = rememberInfiniteTransition(label = "navFluid")
    // Two periods that never line up quickly: 21 s and 34 s put a repeat over
    // eleven minutes away, long enough that the loop cannot be spotted while
    // scrolling a library.
    val drift =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 21_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "navFluidDrift",
        )
    val swirl =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 34_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "navFluidSwirl",
        )
    return remember(drift, swirl) { NavFluid(drift, swirl) }
}

/**
 * Paints [base], then three oversized soft blobs that slide across it.
 *
 * Every position is a whole-period trig function of a 0..1 clock, so the last
 * frame of a loop matches the first exactly: no visible jump when the
 * animation restarts. Blob radii are a fraction of the bar's width, which
 * keeps the wash looking like one material on both the pill and the round
 * search orb instead of a small circle of colour on each.
 *
 * The caller is expected to clip to the bar's shape first — this draws to the
 * bounds it is given.
 */
internal fun Modifier.navFluidWash(
    base: Color,
    rose: Color,
    violet: Color,
    shade: Color,
    fluid: NavFluid,
): Modifier =
    drawBehind {
        drawRect(base)

        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@drawBehind

        val drift = fluid.drift.value * TWO_PI
        val swirl = fluid.swirl.value * TWO_PI
        val radius = maxOf(width * 0.34f, height * 0.78f)

        // Warm blob, crossing left to right with a slow vertical swell.
        drawFluidBlob(
            color = rose,
            center =
                Offset(
                    x = width * (0.5f + 0.40f * cos(drift)),
                    y = height * (0.34f + 0.22f * sin(2f * drift)),
                ),
            radius = radius,
            alpha = 0.28f,
        )

        // Cool blob, crossing the other way, so the two overlap rather than
        // chase each other around the bar.
        drawFluidBlob(
            color = violet,
            center =
                Offset(
                    x = width * (0.5f - 0.40f * cos(swirl)),
                    y = height * (0.66f + 0.18f * cos(2f * swirl)),
                ),
            radius = radius * 0.86f,
            alpha = 0.22f,
        )

        // A darker core that breathes in place and keeps the wash from looking
        // like two flat spotlights moving on a track.
        drawFluidBlob(
            color = shade,
            center =
                Offset(
                    x = width * (0.5f + 0.28f * sin(swirl)),
                    y = height * (0.5f + 0.16f * sin(drift)),
                ),
            radius = radius * 0.78f,
            alpha = 0.24f,
        )
    }

private fun DrawScope.drawFluidBlob(
    color: Color,
    center: Offset,
    radius: Float,
    alpha: Float,
) {
    if (radius <= 0f) return

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.55f),
                        Color.Transparent,
                    ),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
    )
}
