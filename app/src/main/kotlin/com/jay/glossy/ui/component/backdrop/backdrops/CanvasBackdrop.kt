/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 */
package com.jay.glossy.ui.component.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.jay.glossy.ui.component.backdrop.Backdrop

@Composable
fun rememberCanvasBackdrop(
    onDraw: DrawScope.() -> Unit
): Backdrop {
    return remember(onDraw) {
        CanvasBackdropImpl(onDraw)
    }
}

@Immutable
private class CanvasBackdropImpl(
    val onDraw: DrawScope.() -> Unit
) : Backdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw()
    }
}
