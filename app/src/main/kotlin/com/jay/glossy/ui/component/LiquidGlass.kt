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

// --- MOCK BACKDROP REGISTRATION FOR KYANT V2.0.1 ---
@Composable
fun rememberBackdrop(color: Color = Color.Unspecified): Backdrop {
    return remember { Backdrop() }
}

fun Modifier.layerBackdrop(backdrop: Backdrop): Modifier = this

// --- RESTORED COMPATIBILITY FOR BOTTOM NAVIGATION BARS ---
@Composable
fun rememberGlassInteraction(): InteractiveHighlight {
    val animationScope = rememberCoroutineScope()
    return remember(animationScope) { InteractiveHighlight(animationScope) }
}

// Exactly matches the signature expected by LiquidGlassAppBottomNavigationBar
fun Modifier.drawInteractiveGlass(
    isInteractive: Boolean,
    backdrop: Backdrop,
    layer: androidx.compose.ui.graphics.layer.GraphicsLayer?,
    progress: Float,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset
): Modifier = composed {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { androidx.compose.ui.graphics.RectangleShape },
        effects = {
            vibrancy()
            blur(16f.dp.toPx())
            lens(12f.dp.toPx(), 24f.dp.toPx())
        },
        layerBlock = if (isInteractive) {
            {
                translationX = offset.x
                translationY = offset.y
                scaleX = scale
                scaleY = scale
            }
        } else null,
        onDrawSurface = {
            val tint = Color.White.copy(alpha = 0.15f + (0.05f * progress))
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = 0.35f))
        }
    )
}

fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color = Color.White.copy(alpha = 0.15f), 
    isInteractive: Boolean = true
): Modifier = composed {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    
    if (isInteractive) {
        val progress = interactiveHighlight.pressProgress
        val scale = lerp(1f, 1.05f, progress)
        val offset = interactiveHighlight.offset
        
        this.drawInteractiveGlass(
            isInteractive = true,
            backdrop = backdrop,
            layer = null,
            progress = progress,
            scale = scale,
            offset = offset
        )
        .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
        .then(interactiveHighlight.modifier)
        .then(interactiveHighlight.gestureModifier)
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
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.35f))
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
