/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The one way to draw a frosted-glass panel: the current artwork, scaled to
 * fill, blurred on the CPU, dimmed by a scrim and edged with a hairline border,
 * all clipped to [shape] and faded in from the panel's top edge.
 *
 * The fade is what makes the panel read as glass instead of a pasted-on crop.
 * A panel that starts at full strength meets whatever is above it at a hard
 * seam; ramping the whole backdrop as a single layer means the blur and the
 * darkening build up across the panel, so it comes out of the artwork rather
 * than sitting on top of it. `fade` is applied by a mask over the whole stack
 * rather than by an alpha per child, so the seam between the blur, the scrim
 * and the border never shows.
 *
 * The backdrop is purely decorative — callers draw their content in a sibling
 * Box above it — and it renders nothing when there is no artwork to blur, so
 * the surface falls back to whatever background the caller already had.
 */

package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Default scrim: light at the top edge, heavier where the panel's text sits. */
private val GlassScrim = Brush.verticalGradient(
    listOf(
        Color.Black.copy(alpha = 0.35f),
        Color.Black.copy(alpha = 0.55f),
    ),
)

/**
 * How far down the panel the fade reaches full strength. The rest of the panel
 * stays there, so the dissolve happens against the edge the panel meets while
 * the text inside it keeps the contrast it had before.
 */
private const val FadeKnee = 0.35f

/**
 * Blurred artwork backdrop for a glass panel.
 *
 * @param thumbnailUrl artwork to blur; the scrim/border still render without it.
 * @param shape the panel's outline, used for both the clip and the border.
 * @param modifier apply this to the *container* Box that callers usually also
 *   use to position the panel behind their content (e.g. `matchParentSize()`).
 * @param blurStrength 0..1; higher is softer and more diffuse.
 * @param artworkAlpha opacity of the blurred artwork layer.
 * @param fade the backdrop's opacity at its top edge and from [FadeKnee] down.
 *   The default dissolves into the content above; pass `1f to 1f` for a flat
 *   panel.
 * @param scrim overlay gradient protecting the text contrast.
 * @param border hairline outline; pass null for none.
 */
@Composable
fun GlassBackdrop(
    thumbnailUrl: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    blurStrength: Float = 0.85f,
    artworkAlpha: Float = 0.6f,
    fade: Pair<Float, Float> = 0f to 1f,
    scrim: Brush? = GlassScrim,
    border: Color? = Color.White.copy(alpha = 0.10f),
) {
    val (fadeTop, fadeBottom) = fade

    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // DstIn keeps the backdrop's own alpha where the gradient is
                    // opaque, so one ramp multiplies the blur, the scrim and the
                    // border at once.
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = fadeTop),
                            FadeKnee to Color.Black.copy(alpha = fadeBottom),
                            1f to Color.Black.copy(alpha = fadeBottom),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                BlurredArtworkBackdrop(
                    url = thumbnailUrl,
                    blurStrength = blurStrength,
                    modifier = Modifier.fillMaxSize().alpha(artworkAlpha),
                )
            }
            if (scrim != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(scrim),
                )
            }
            if (border != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, border, shape),
                )
            }
        }
    }
}
