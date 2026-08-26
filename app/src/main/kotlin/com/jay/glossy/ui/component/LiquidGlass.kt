/**
 * Glossy Project (C) 2026
 * Liquid Glass Physics strictly based on Kyant0 Backdrop Demo
 */
package com.jay.glossy.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Custom Modifier built using Kyant's official Backdrop Engine + Interactive Highlight
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color = Color.White.copy(alpha = 0.15f), 
    isInteractive: Boolean = true
): Modifier = composed {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }

    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(16f.dp.toPx())
            lens(12f.dp.toPx(), 24f.dp.toPx()) // Magic Refraction Effect
        },
        layerBlock = if (isInteractive) {
            {
                // This applies the stretchy rubber physics when dragging
                val width = size.width
                val height = size.height
                val progress = interactiveHighlight.pressProgress
                val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
                
                val maxOffset = size.minDimension
                val initialDerivative = 0.05f
                val offset = interactiveHighlight.offset
                
                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                val maxDragScale = 4f.dp.toPx() / size.height
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
                scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
            }
        } else null,
        onDrawSurface = {
            if (tint.isSpecified) {
                drawRect(tint, blendMode = BlendMode.Hue)
                drawRect(tint.copy(alpha = 0.35f))
            }
        }
    )
    .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
    .then(
        if (isInteractive) {
            Modifier
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
        } else Modifier
    )
}

/**
 * Ready-to-use Glass Icon Button Component (Replaced IconButton to disable default ripple)
 */
@Composable
fun LiquidGlassIconButton(
    backdrop: Backdrop,
    painter: Painter,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(backdrop, shape, isInteractive = true)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Simp Music disables ripple because of glowing shader
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter, contentDescription = null, tint = Color.White)
    }
}
