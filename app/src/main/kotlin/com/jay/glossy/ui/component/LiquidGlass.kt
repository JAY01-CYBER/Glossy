/**
 * Glossy Project (C) 2026
 * Liquid Glass Physics officially based on Kyant's Catalog Demo
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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost

import com.kyant.backdrop.*
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun rememberBackdrop(color: Color = Color.Unspecified): Backdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { 
        LayerBackdrop(graphicsLayer = graphicsLayer, onDraw = {}) 
    }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun Modifier.layerBackdrop(backdrop: Backdrop): Modifier {
    return if (backdrop is LayerBackdrop) {
        this.then(Modifier.kyantLayerBackdrop(backdrop))
    } else {
        this
    }
}

@Composable
fun rememberGlassInteraction(): InteractiveHighlight {
    val animationScope = rememberCoroutineScope()
    return remember(animationScope) { InteractiveHighlight(animationScope) }
}

fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: Backdrop,
    layer: GraphicsLayer?,
    luminance: Float,
    shape: Shape,
    interaction: InteractiveHighlight
): Modifier = composed {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(16f.dp.toPx())
            lens(12f.dp.toPx(), 24f.dp.toPx())
        },
        layerBlock = {
            val width = size.width
            val height = size.height
            val progress = interaction.pressProgress
            val scale = androidx.compose.ui.util.lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
            
            val maxOffset = size.minDimension
            val initialDerivative = 0.05f
            val offset = interaction.offset
            
            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
            
            val maxDragScale = 4f.dp.toPx() / size.height
            val offsetAngle = atan2(offset.y, offset.x)
            scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
            scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
        },
        onDrawSurface = {
            // FIX: Made the glass highly transparent and glowy 
            val glow = Color.White.copy(alpha = 0.05f + (0.05f * interaction.pressProgress))
            drawRect(glow, blendMode = BlendMode.Plus)
            drawRect(Color.White.copy(alpha = 0.10f)) // Reduced from 0.35f to 0.10f
        }
    )
    .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
    .then(interaction.modifier)
    .then(interaction.gestureModifier)
}

fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color = Color.White.copy(alpha = 0.10f), 
    isInteractive: Boolean = true
): Modifier = composed {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    
    if (isInteractive) {
        this.drawInteractiveGlass(
            isDark = true,
            backdrop = backdrop,
            layer = null,
            luminance = 0.5f,
            shape = shape,
            interaction = interactiveHighlight
        )
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(16f.dp.toPx())
                lens(12f.dp.toPx(), 24f.dp.toPx())
            },
            onDrawSurface = {
                if (tint.isSpecified) {
                    drawRect(tint, blendMode = BlendMode.Plus)
                    drawRect(tint)
                }
            }
        ).border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
    }
}

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
                indication = null, 
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter, contentDescription = null, tint = Color.White)
    }
}
