package com.jay.glossy.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStoreRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    // Swipe down progress ko 0.0 se 1.0 ke beech limit karna
    val progress = state.progress.coerceIn(0f, 1f)

    // Infinite rotations jab refreshing on ho
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_anim"
    )

    // Bouncing/Pulsing effect
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    // Current state calculate karna (Drag karte waqt ya refresh hote waqt)
    val currentRotation = if (isRefreshing) rotation else progress * 180f
    val currentScale = if (isRefreshing) scaleAnim else progress
    val currentAlpha = if (isRefreshing) 1f else progress

    // Yahan offset set kiya hai taaki khinchne par ye indicator neeche slide ho
    val slideOffset = (progress * 150f).coerceAtMost(200f)

    Box(
        modifier = modifier
            .offset { IntOffset(0, slideOffset.roundToInt()) }
            .size(46.dp)
            .scale(currentScale)
            .alpha(currentAlpha)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface) // Background circle (optional)
            .graphicsLayer {
                rotationZ = currentRotation
                shadowElevation = 8f
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val width = size.width
            val height = size.height

            // 1. Google Blue Circle
            drawCircle(
                color = Color(0xFF4285F4).copy(alpha = 0.8f),
                radius = width * 0.3f,
                center = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.3f)
            )

            // 2. Google Red Triangle
            rotate(if (isRefreshing) -rotation * 1.5f else 0f) { // Triangle thoda alag speed pe ghumega
                val trianglePath = Path().apply {
                    moveTo(width / 2f, height * 0.1f)
                    lineTo(width * 0.9f, height * 0.8f)
                    lineTo(width * 0.1f, height * 0.8f)
                    close()
                }
                drawPath(
                    path = trianglePath,
                    color = Color(0xFFDB4437).copy(alpha = 0.8f)
                )
            }

            // 3. Google Yellow Square/Diamond
            rotate(if (isRefreshing) rotation * 2f else progress * 90f) {
                drawRect(
                    color = Color(0xFFF4B400).copy(alpha = 0.8f),
                    topLeft = androidx.compose.ui.geometry.Offset(width * 0.4f, height * 0.4f),
                    size = androidx.compose.ui.geometry.Size(width * 0.5f, height * 0.5f)
                )
            }
        }
    }
}
