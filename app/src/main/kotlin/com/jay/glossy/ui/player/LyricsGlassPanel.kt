/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The Apple Music-style frosted-glass panel that sits behind lyrics: the
 * track's artwork, scaled up, heavily blurred and dimmed, with a vertical dark
 * scrim and a hairline border on top. Extracted from the Apple Music lyrics
 * screen so the shared lyrics views (the full-screen inline lyrics and the
 * on-player synced strip) can offer the same look in every player design.
 *
 * The backdrop is purely decorative — callers draw their content in a sibling
 * Box above it — and it renders nothing when there is no artwork to blur, so
 * the surface falls back to whatever background the caller already had.
 */

package com.jay.glossy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Blurred artwork backdrop for a lyrics surface.
 *
 * @param thumbnailUrl artwork to blur; the scrim/border still render without it.
 * @param shape corner radius of the panel.
 * @param modifier apply this to the *container* Box that callers usually also
 *   use to position the panel behind their content (e.g. `fillMaxSize().clip()`).
 * @param artworkAlpha opacity of the blurred artwork layer.
 * @param scrim overlay gradient protecting the text contrast.
 * @param border hairline outline; pass null for none.
 */
@Composable
fun LyricsGlassBackdrop(
    thumbnailUrl: String?,
    shape: Dp,
    modifier: Modifier = Modifier,
    artworkAlpha: Float = 0.6f,
    scrim: Brush? = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = 0.35f),
            Color.Black.copy(alpha = 0.55f),
        ),
    ),
    border: Color? = Color.White.copy(alpha = 0.10f),
) {
    val context = LocalContext.current
    val panelShape = androidx.compose.foundation.shape.RoundedCornerShape(shape)
    val model = remember(thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .crossfade(400)
            .build()
    }

    Box(modifier = modifier.clip(panelShape)) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Scaled up so blurring the edges doesn't pull transparent
                    // pixels into the panel.
                    .graphicsLayer {
                        scaleX = 1.5f
                        scaleY = 1.5f
                    }
                    .blur(240.dp)
                    .alpha(artworkAlpha),
            )
        }
        if (scrim != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrim),
            )
        }
        if (border != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(1.dp, border, panelShape),
            )
        }
    }
}
