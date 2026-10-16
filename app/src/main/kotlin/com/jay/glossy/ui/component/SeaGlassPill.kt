/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pill-shaped action button whose body is sea glass rather than paint.
 *
 * The tint rides in the glass as Beer-Lambert body absorption, so the colour
 * sits *inside* the surface and the chalky rim picks up whatever is behind the
 * pill instead of drawing a colour on top of it. A pale, see-through body with
 * dark ink is what keeps it readable — sea glass is a surface, not a fill.
 *
 * [fill] is the colour the pill's glass is tinted with. The body comes from
 * [seaGlassSurface], so the pill is cut from the app's one material rather than
 * from a tuning passed in here. A tooling preview has no GPU pass to sample, and
 * a device below API 33 has no `RuntimeShader` to run one on; both get the same
 * body painted directly (see [seaGlassFallback]), tinted with [fill]. Choose
 * [fill] as the colour the frosted body should read as — and keep [ink] legible
 * against it, because on those paths the tint is the body.
 *
 * @param ink the colour of the icon and label. Passed separately rather than
 *   read from `LocalContentColor` because the glass body is composited in a draw
 *   pass and does not tint its children the way a [Surface] does.
 * @param shadowTint the pill's own ink at low alpha is used for the lift, which
 *   keeps the shadow looking like the same material rather than a grey smudge.
 * @param sheen sweeps a highlight across the pill. Reserve it for the one
 *   primary action on a screen: on two pills at once it stops meaning anything.
 * @param enabled false leaves the pill drawn but inert: the touch is refused
 *   rather than accepted and ignored, so talkback and the gesture both report
 *   the truth. The caller still owns the *look* of a disabled pill — usually by
 *   withdrawing the tint it passes as [fill].
 */
@Composable
fun SeaGlassPill(
    fill: Color,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shadowTint: Color = fill,
    height: Dp = 52.dp,
    sheen: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // The press answers on the spring's scale rather than on a ripple: the pill
    // is small and its own colour is the whole point, so shrinking it says
    // "that landed" without washing the colour out.
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )

    // Restarts rather than ping-pongs: the highlight has to leave the pill
    // entirely before the next one starts, or it looks like a stuck band.
    val sheenOffset by rememberInfiniteTransition(label = "sheen").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenSweep",
    )

    // The sea-glass body replaces the flat fill outright: it paints its own
    // tinted, frosted, rim-lit contour, so a fill underneath would only flatten
    // it back out. Where the shader cannot run — a tooling preview, or a device
    // below API 33 — the same body is *drawn* rather than sampled, so the pill
    // is still glass there instead of the flat fill it used to fall back to.
    val shape = RoundedCornerShape(percent = 50)
    val surface = seaGlassSurface(fill = fill, shape = shape, cornerRadius = height / 2)

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // Drawn before the glass so the lift sits behind the pill. Only the
            // API 28+ shadow renderer takes these colours; below that the
            // platform ignores them and casts its own neutral one.
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(percent = 50),
                clip = false,
                ambientColor = shadowTint.copy(alpha = 0.30f),
                spotColor = shadowTint.copy(alpha = 0.42f),
            )
            // Keeps the touch bounds on the pill's outline; the glass contour is
            // the same circle, so nothing the shader draws is cut off.
            .clip(RoundedCornerShape(percent = 50))
            .then(surface)
            .clickable(
                interactionSource = interactionSource,
                // No ripple: the pill is the feedback.
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Provided rather than expected: an icon or label that does not name a
        // colour has to land on [ink], because the glass body is composited in
        // a draw pass and cannot tint its children the way a Surface does.
        CompositionLocalProvider(LocalContentColor provides ink) {
            Row(
                modifier = Modifier.height(height).padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }

        if (sheen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = sheenOffset * size.width
                        // Brightest as it crosses the middle of the pill, gone
                        // by the time it reaches either edge.
                        alpha = (1f - kotlin.math.abs(sheenOffset)).coerceIn(0f, 1f)
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
            )
        }
    }
}
