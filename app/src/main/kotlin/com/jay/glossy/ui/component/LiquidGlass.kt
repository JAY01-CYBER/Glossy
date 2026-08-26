/**
 * Glossy Project (C) 2026
 * Liquid Glass Physics strictly based on Kyant0 Backdrop Demo
 */
package com.jay.glossy.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Custom Modifier built using Kyant's official Backdrop Engine
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color = Color.White.copy(alpha = 0.15f) // Default Frosted White Tint
): Modifier = composed {
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            // As per Kyant Docs: Vibrancy + Brightness + Blur = True Glass
            vibrancy()
            colorControls(brightness = 0.1f, contrast = 1.1f, saturation = 1.5f)
            blur(24f.dp.toPx())
        },
        // Highlight gives that 3D top-edge lighting reflection
        highlight = { Highlight.Default.copy(alpha = 0.5f) },
        // Shadow separates the glass from the background
        shadow = { Shadow(radius = 6f.dp, alpha = 0.25f) },
        onDrawSurface = {
            drawRect(tint) 
        }
    )
    // Extra 3D glowing rim for Apple Music vibe
    .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
}

/**
 * Ready-to-use Glass Icon Button Component
 */
@Composable
fun LiquidGlassIconButton(
    backdrop: Backdrop,
    painter: Painter,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.liquidGlass(backdrop, shape)
    ) {
        Icon(painter, contentDescription = null, tint = Color.White)
    }
}
