/**
 * Glossy Project (C) 2026
 * 
 * =====================================================================
 * RESPECTFUL ATTRIBUTION:
 * This file contains the original "Liquid Glass" physics and implementation 
 * proudly created by the Simp Music (maxrave-dev) project.
 * 
 * We are using it here with full respect and gratitude to the original 
 * creators for their brilliant work on 3D refraction and touch mechanics.
 * All original credits for this UI component go to Simp Music.
 * =====================================================================
 * 
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign

@Composable
fun Modifier.liquidGlass(
    backdrop: com.kyant.backdrop.Backdrop,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    highlight: Highlight = Highlight.Default,
): Modifier {
    val isDark = true 
    val layer = rememberGraphicsLayer()
    val interaction = rememberGlassInteraction()
    return this.drawInteractiveGlass(
        isDark = isDark,
        backdrop = backdrop,
        layer = layer,
        luminanceAnimation = 0.5f,
        shape = shape,
        interaction = if (interactive) interaction else null,
        highlight = highlight,
    )
}

@Composable
fun Modifier.liquidGlass(
    backdrop: com.kyant.backdrop.Backdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    blurScale: Float = 1f,
    minScrim: Float = 0.12f,
    maxScrim: Float = 0.5f,
): Modifier {
    val isDark = true
    val interaction = rememberGlassInteraction()
    return this.drawInteractiveGlass(
        isDark = isDark,
        backdrop = backdrop,
        layer = layer,
        luminanceAnimation = luminanceAnimation,
        shape = shape,
        interaction = if (interactive) interaction else null,
        pressedScale = 1.04f,
        blurScale = blurScale,
        minScrim = minScrim,
        maxScrim = maxScrim,
    )
}

@Composable
fun LiquidGlassContainer(
    backdrop: com.kyant.backdrop.Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    highlight: Highlight = Highlight.Default,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlass(backdrop, shape, interactive, highlight),
        contentAlignment = contentAlignment,
        content = content,
    )
}

class GlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.001f)
    private val pressAnimation = Animatable(0f, 0.001f)

    val pressProgress: Float get() = pressAnimation.value

    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
                onDragCancel = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberGlassInteraction(): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassInteraction(scope) }
}

fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: com.kyant.backdrop.Backdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    interaction: GlassInteraction?,
    pressedScale: Float = 1.12f,
    highlight: Highlight = Highlight.Default,
    blurScale: Float = 1f,
    minScrim: Float = 0.12f,
    maxScrim: Float = 0.5f,
): Modifier =
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = { highlight },
            effects = {
                val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
                val press = interaction?.pressProgress ?: 0f
                vibrancy()
                colorControls(
                    brightness = 0.05f,
                    contrast = 1f,
                    saturation = 1.5f,
                )
                blur(
                    (if (l > 0f) {
                        lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                    } else {
                        lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                    }) * blurScale + 2f.dp.toPx() * press,
                )
                lens(size.minDimension / 4f + 2f.dp.toPx() * press, size.minDimension / 2f, false)
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
                layer.record { drawBackdrop() }
            },
            onDrawSurface = {
                val darken = lerp(minScrim, maxScrim, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
                drawRect((if (isDark) Color.Black else Color.White).copy(alpha = darken))
                val press = interaction?.pressProgress ?: 0f
                if (press > 0f) {
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f * press),
                                    Color.Transparent,
                                ),
                                center = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 1.2f,
                            ),
                        blendMode = BlendMode.Plus,
                    )
                }
            },
            layerBlock =
                if (interaction != null) {
                    {
                        val scale = lerp(1f, pressedScale, interaction.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    }
                } else {
                    null
                },
        ).then(
            if (interaction != null) {
                Modifier.pointerInput(interaction) { interaction.detectPress(this) }
            } else {
                Modifier
            },
        )

internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        val down = awaitFirstDown(requireUnconsumed = false)

        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent =
            drag(
                pointerId = down.id,
                onDrag = { onDrag(it, it.positionChange()) },
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
