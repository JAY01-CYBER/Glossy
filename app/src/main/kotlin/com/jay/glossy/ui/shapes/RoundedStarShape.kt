/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Creates a perfectly smooth, continuous rounded star (scallop) shape,
 * exactly like the iOS/Vivi active audio device background.
 */
class RoundedStarShape(
    private val sides: Int = 8,
    private val curve: Double = 0.10,
    private val rotation: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rMax = size.width.coerceAtMost(size.height) / 2f

        val rotationRads = rotation * PI / 180.0
        val steps = 120 // Higher steps ensure maximum smoothness

        for (i in 0..steps) {
            val theta = (2 * PI * i) / steps
            // Elegant mathematical formula for a smooth wavy scallop
            val radius = rMax * (1.0 - curve * sin(sides * theta / 2.0).pow(2.0))

            val angle = theta + rotationRads - (PI / 2.0)
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
