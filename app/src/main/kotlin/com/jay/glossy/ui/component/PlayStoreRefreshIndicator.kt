package com.jay.glossy.ui.component

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
// Nayi library ke imports
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
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
    
    // Infinite Animations
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_transition")

    // Rotation Animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Morph Progress (4 shapes ke liye 0 se 4 tak)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morph"
    )

    // Play Store Colors Animation
    val color by infiniteTransition.animateColor(
        initialValue = Color(0xFF4285F4),
        targetValue = Color(0xFF4285F4),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                Color(0xFF4285F4) at 0    // Google Blue
                Color(0xFFDB4437) at 750  // Google Red
                Color(0xFFF4B400) at 1500 // Google Yellow
                Color(0xFF0F9D58) at 2250 // Google Green
                Color(0xFF4285F4) at 3000 // Wapas Blue
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "color"
    )

    // Shapes Define Karna (Image wala starburst bhi yahi hai)
    val starburst = remember { RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.7f, rounding = CornerRounding(0.15f)) }
    val triangle = remember { RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.25f)) }
    val square = remember { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)) }
    val circle = remember { RoundedPolygon.circle() }

    // Morphing sequences
    val morph1 = remember { Morph(starburst, triangle) }
    val morph2 = remember { Morph(triangle, square) }
    val morph3 = remember { Morph(square, circle) }
    val morph4 = remember { Morph(circle, starburst) }

    // Khinchte waqt indicator ko neeche slide karne ka logic
    val slideOffset = (progress * 150f).coerceAtMost(200f)
    val currentScale = if (isRefreshing) 1f else progress
    val currentAlpha = if (isRefreshing) 1f else progress

    Box(
        modifier = modifier
            .offset { IntOffset(0, slideOffset.roundToInt()) }
            .size(46.dp)
            .scale(currentScale)
            .alpha(currentAlpha)
            .graphicsLayer {
                shadowElevation = 6.dp.toPx()
                shape = CircleShape
            }
            .background(MaterialTheme.colorScheme.surface), // White/Dark surface background
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val currentMorphProgress = if (isRefreshing) morphProgress else 0f
            val currentRotation = if (isRefreshing) rotation else progress * 180f
            val currentColor = if (isRefreshing) color else Color(0xFF4285F4) // Khinchte waqt sirf Blue rahega

            // Calculate current shape
            val morph = when (currentMorphProgress.toInt() % 4) {
                0 -> morph1
                1 -> morph2
                2 -> morph3
                else -> morph4
            }
            // Morph ke beech ki timing
            val fraction = currentMorphProgress - currentMorphProgress.toInt()

            // Shape ko Android Path mein convert karna
            val androidPath = morph.toPath(progress = fraction)

            // Shape ko Canvas ke size ke hisaab se scale aur rotate karna
            val matrix = android.graphics.Matrix()
            matrix.setScale(size.width / 2f, size.height / 2f)
            matrix.postTranslate(size.width / 2f, size.height / 2f)
            matrix.postRotate(currentRotation, size.width / 2f, size.height / 2f)
            androidPath.transform(matrix)

            // Compose Canvas par draw karna
            drawPath(
                path = androidPath.asComposePath(),
                color = currentColor
            )
        }
    }
}
