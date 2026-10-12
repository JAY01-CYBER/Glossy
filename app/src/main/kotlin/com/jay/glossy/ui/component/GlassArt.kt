/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The floating glass props of the app's key art — a play tile, a spiral disc, a
 * lightning bolt and a cassette — together with the idle drift they share.
 *
 * Two screens draw the same four props: the sign-in screen (full height) and the
 * Settings header (a short banner), so both the arrangement and the animation
 * live here. A prop is placed by fraction of whatever space the caller hands
 * over, which is why one renderer covers a whole screen and a shallow band.
 */

package com.jay.glossy.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jay.glossy.R

/**
 * One glass prop: where it sits, how big it is and how it moves while it idles.
 *
 * [periodMillis] differs per prop on purpose — four props on the same cycle
 * would read as one mechanical block instead of objects drifting on their own.
 */
data class GlassProp(
    val resId: Int,
    /** Horizontal anchor, as a fraction of the host's width. */
    val xFraction: Float,
    /** Vertical anchor, as a fraction of the host's height. */
    val yFraction: Float,
    /** Anchor to the right edge instead of the left one. */
    val anchorEnd: Boolean,
    val width: Dp,
    val height: Dp,
    /** Resting tilt, in degrees. */
    val rotation: Float,
    /** How far the prop drifts up and down from its resting place. */
    val floatDistance: Dp,
    /** Extra tilt the drift adds on either side of [rotation]. */
    val sway: Float,
    /** One half of the drift cycle. */
    val periodMillis: Int,
)

/**
 * The key art laid out for a full-height screen: the play tile and the cassette
 * on the left, the disc and the bolt on the right, matching the onboarding
 * reference.
 */
val GlassFullScreenProps = listOf(
    GlassProp(
        resId = R.drawable.glossy_glass_play,
        xFraction = 0.07f,
        yFraction = 0.04f,
        anchorEnd = false,
        width = 128.dp,
        height = 101.dp,
        rotation = -13f,
        floatDistance = 16.dp,
        sway = 4f,
        periodMillis = 5400,
    ),
    GlassProp(
        resId = R.drawable.glossy_glass_spiral,
        xFraction = 0.12f,
        yFraction = 0.15f,
        anchorEnd = true,
        width = 72.dp,
        height = 72.dp,
        rotation = 10f,
        floatDistance = 12.dp,
        sway = 6f,
        periodMillis = 6300,
    ),
    GlassProp(
        resId = R.drawable.glossy_glass_lightning,
        xFraction = 0.12f,
        yFraction = 0.40f,
        anchorEnd = true,
        width = 64.dp,
        height = 85.dp,
        rotation = -14f,
        floatDistance = 18.dp,
        sway = 5f,
        periodMillis = 4700,
    ),
    GlassProp(
        resId = R.drawable.glossy_glass_cassette,
        xFraction = 0.02f,
        yFraction = 0.53f,
        anchorEnd = false,
        width = 132.dp,
        height = 92.dp,
        rotation = 7f,
        floatDistance = 14.dp,
        sway = 3f,
        periodMillis = 6900,
    ),
)

/**
 * The same four props, tightened for a wide, shallow band — the Settings header
 * — where a full screen's worth of spacing would push them off the edges.
 */
val GlassBannerProps = listOf(
    GlassProp(
        resId = R.drawable.glossy_glass_play,
        xFraction = 0.06f,
        yFraction = 0.06f,
        anchorEnd = false,
        width = 92.dp,
        height = 73.dp,
        rotation = -13f,
        floatDistance = 11.dp,
        sway = 4f,
        periodMillis = 5400,
    ),
    GlassProp(
        resId = R.drawable.glossy_glass_spiral,
        xFraction = 0.10f,
        yFraction = 0.16f,
        anchorEnd = true,
        width = 52.dp,
        height = 52.dp,
        rotation = 10f,
        floatDistance = 9.dp,
        sway = 6f,
        periodMillis = 6300,
    ),
    GlassProp(
        resId = R.drawable.glossy_glass_lightning,
        xFraction = 0.13f,
        yFraction = 0.46f,
        anchorEnd = true,
        width = 46.dp,
        height = 61.dp,
        rotation = -14f,
        floatDistance = 13.dp,
        sway = 5f,
        periodMillis = 4700,
    ),
    GlassProp(
        resId = R.drawable.glossy_glass_cassette,
        xFraction = 0.03f,
        yFraction = 0.58f,
        anchorEnd = false,
        width = 96.dp,
        height = 67.dp,
        rotation = 7f,
        floatDistance = 10.dp,
        sway = 3f,
        periodMillis = 6900,
    ),
)

/**
 * Draws [props] over the caller's layout, each drifting up and down on its own
 * slow cycle and tilting a little with it, so the group breathes instead of
 * sitting still.
 *
 * Positions are fractions of the space this composable is given, so the same
 * list reads correctly on a full screen and in a short banner.
 */
@Composable
fun FloatingGlassArt(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    scale: Float = 1f,
    props: List<GlassProp> = GlassFullScreenProps,
) {
    BoxWithConstraints(modifier = modifier) {
        props.forEach { prop ->
            GlassPropItem(
                prop = prop,
                hostWidth = maxWidth,
                hostHeight = maxHeight,
                alpha = alpha,
                scale = scale,
            )
        }
    }
}

@Composable
private fun BoxScope.GlassPropItem(
    prop: GlassProp,
    hostWidth: Dp,
    hostHeight: Dp,
    alpha: Float,
    scale: Float,
) {
    val transition = rememberInfiniteTransition(label = "glassArt")
    // Ping-pongs 0 → 1 → 0 over the prop's own cycle; `drift` turns that into a
    // signed -1..1 offset from the resting position.
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = prop.periodMillis,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glassDrift",
    )
    val drift = (cycle - 0.5f) * 2f

    Image(
        painter = painterResource(prop.resId),
        contentDescription = null,
        modifier = Modifier
            .align(if (prop.anchorEnd) Alignment.TopEnd else Alignment.TopStart)
            .offset(
                x = hostWidth * prop.xFraction * (if (prop.anchorEnd) -1f else 1f),
                y = hostHeight * prop.yFraction + prop.floatDistance * drift,
            )
            .size(width = prop.width * scale, height = prop.height * scale)
            .graphicsLayer {
                rotationZ = prop.rotation + prop.sway * drift
                val breath = 1f + 0.03f * drift
                scaleX = breath
                scaleY = breath
                this.alpha = alpha
            },
    )
}
