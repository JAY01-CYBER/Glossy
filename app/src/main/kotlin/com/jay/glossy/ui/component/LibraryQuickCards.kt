/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The library's quick-access strip: three coloured cards that jump straight to
 * the Liked, Downloads and Uploaded collections. They sit under the filter row
 * so the three collections people actually use are one tap away instead of
 * being buried in the playlist grid, and they pop in with a springy entrance
 * like the rest of the cards.
 */

package com.jay.glossy.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.R

private data class QuickCard(
    val label: String,
    val caption: String,
    val icon: Int,
    val start: Color,
    val end: Color,
)

private val LikedCard = QuickCard(
    label = "Liked",
    caption = "Songs you love",
    icon = R.drawable.favorite,
    start = Color(0xFFE9628A),
    end = Color(0xFFA82E5A),
)

private val DownloadsCard = QuickCard(
    label = "Downloads",
    caption = "Offline",
    icon = R.drawable.download,
    start = Color(0xFF7B6BF0),
    end = Color(0xFF3B2C93),
)

private val UploadsCard = QuickCard(
    label = "Uploads",
    caption = "Added by you",
    icon = R.drawable.upload,
    start = Color(0xFF37B3A6),
    end = Color(0xFF1C6B72),
)

@Composable
fun LibraryQuickCards(
    onLiked: () -> Unit,
    onDownloads: () -> Unit,
    onUploads: () -> Unit,
    modifier: Modifier = Modifier,
    showLiked: Boolean = true,
    showDownloads: Boolean = true,
    showUploads: Boolean = true,
) {
    if (!showLiked && !showDownloads && !showUploads) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showLiked) QuickAccessCard(LikedCard, Modifier.weight(1f), onLiked)
        if (showDownloads) QuickAccessCard(DownloadsCard, Modifier.weight(1f), onDownloads)
        if (showUploads) QuickAccessCard(UploadsCard, Modifier.weight(1f), onUploads)
    }
}

@Composable
private fun QuickAccessCard(
    card: QuickCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Springy pop when the card first appears, bouncier squish while pressed.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.94f
            appeared -> 1f
            else -> 0.9f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "quickCardScale",
    )

    Column(
        modifier = modifier
            .height(96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(card.start, card.end),
                ),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(card.icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }

        Column {
            Text(
                text = card.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = card.caption,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
