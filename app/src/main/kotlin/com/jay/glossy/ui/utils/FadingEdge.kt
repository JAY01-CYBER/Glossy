/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.utils

import com.jay.glossy.R

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Feathers the named edges of whatever this is applied to.
 *
 * The gradients cannot be plain constants, because where each one ends is the
 * drawn size — a fade is measured in dp from an edge of the thing itself. So
 * they are built in [drawWithCache] instead, which is the one hook that hands
 * back the size *and* only re-runs when that size changes. Building them inside
 * the draw pass instead would mean four new brushes, and four new Skia shaders
 * behind them, on every single frame a row is drawn — which is every frame of
 * every scroll, on a screen's worth of rows at once.
 */
fun Modifier.fadingEdge(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
) = graphicsLayer(alpha = 0.99f)
    .drawWithCache {
        val topBrush =
            top?.let {
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = it.toPx(),
                )
            }
        val bottomBrush =
            bottom?.let {
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - it.toPx(),
                    endY = size.height,
                )
            }
        val leftBrush =
            left?.let {
                Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = it.toPx(),
                )
            }
        val rightBrush =
            right?.let {
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = size.width - it.toPx(),
                    endX = size.width,
                )
            }
        onDrawWithContent {
            drawContent()
            topBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
            bottomBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
            leftBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
            rightBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
        }
    }

fun Modifier.fadingEdge(
    horizontal: Dp? = null,
    vertical: Dp? = null,
) = fadingEdge(
    left = horizontal,
    right = horizontal,
    top = vertical,
    bottom = vertical,
)
