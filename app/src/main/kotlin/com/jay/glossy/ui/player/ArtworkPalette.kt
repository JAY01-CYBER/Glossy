/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Per-track colours for the player controls. The artwork is sampled once per
 * song and turned into a small, contrast-safe palette, so the transport
 * buttons, pills and dock take their colour from the music that is playing and
 * crossfade to the next song's colours when it changes.
 */

package com.jay.glossy.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The colours a control needs, derived from the current artwork. Every field is
 * chosen so its matching `on…` colour stays readable.
 */
@Immutable
data class ArtworkColors(
    /** Accent colour taken from the artwork — the primary button. */
    val accent: Color,
    val onAccent: Color,
    /** Secondary (previous / next) buttons. */
    val side: Color,
    val onSide: Color,
    /** Solid pill buttons for the action row and the dock. */
    val surface: Color,
    val onSurface: Color,
)

/** White-ish base used for the pill buttons, tinted with the artwork's accent. */
private fun pillFor(accent: Color): Color = lerp(Color(0xFFF7F3FF), accent, 0.10f)

private fun Color.readableOn(): Color =
    if (luminance() > 0.55f) Color(0xFF191129) else Color.White

/** Cache keyed by media id so a track's palette survives recomposition and revisits. */
private val artworkPaletteCache = LinkedHashMap<String, ArtworkColors>()

private const val PALETTE_CACHE_LIMIT = 48

/**
 * Resolves the artwork palette for [mediaId], falling back to [fallback] while
 * the bitmap is being sampled or when the artwork cannot be used.
 */
@Composable
fun rememberArtworkPalette(
    mediaId: String?,
    thumbnailUrl: String?,
    fallback: ArtworkColors,
): ArtworkColors {
    val context = LocalContext.current
    val key = mediaId?.takeIf { it.isNotBlank() }

    var colors by remember(key) {
        mutableStateOf(key?.let { artworkPaletteCache[it] } ?: fallback)
    }

    LaunchedEffect(key, thumbnailUrl) {
        if (key == null) {
            colors = fallback
            return@LaunchedEffect
        }
        artworkPaletteCache[key]?.let {
            colors = it
            return@LaunchedEffect
        }
        if (thumbnailUrl.isNullOrBlank()) return@LaunchedEffect

        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("playerPalette_$key")
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap()
                    ?: return@runCatching null
                val palette = Palette.from(bitmap)
                    .maximumColorCount(8)
                    .resizeBitmapArea(100 * 100)
                    .generate()
                palette.toArtworkColors(fallback)
            }.getOrNull()
        }

        if (extracted != null) {
            if (artworkPaletteCache.size >= PALETTE_CACHE_LIMIT) {
                artworkPaletteCache.remove(artworkPaletteCache.keys.firstOrNull())
            }
            artworkPaletteCache[key] = extracted
            colors = extracted
        }
    }

    return colors
}

/**
 * Wraps [target] so the controls crossfade into the next track's colours instead
 * of snapping to them the instant the song changes.
 */
@Composable
fun animatedArtworkColors(target: ArtworkColors): ArtworkColors {
    return ArtworkColors(
        accent = animateColorAsState(
            targetValue = target.accent,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "artworkAccent",
        ).value,
        onAccent = animateColorAsState(
            targetValue = target.onAccent,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "artworkOnAccent",
        ).value,
        side = animateColorAsState(
            targetValue = target.side,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "artworkSide",
        ).value,
        onSide = animateColorAsState(
            targetValue = target.onSide,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "artworkOnSide",
        ).value,
        surface = animateColorAsState(
            targetValue = target.surface,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "artworkSurface",
        ).value,
        onSurface = animateColorAsState(
            targetValue = target.onSurface,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "artworkOnSurface",
        ).value,
    )
}

/**
 * Picks a saturated colour for the buttons and derives a readable text colour,
 * a darker secondary and a lightly tinted pill from it. Returns [fallback] when
 * the artwork has no usable colour (monochrome covers, for example).
 */
private fun Palette.toArtworkColors(fallback: ArtworkColors): ArtworkColors {
    val source =
        vibrantSwatch?.rgb?.let { Color(it) }
            ?: lightVibrantSwatch?.rgb?.let { Color(it) }
            ?: mutedSwatch?.rgb?.let { Color(it) }
            ?: dominantSwatch?.rgb?.let { Color(it) }
            ?: return fallback

    // Very dark or washed-out samples read as "no colour" on a dark player.
    if (source.luminance() < 0.04f) return fallback

    val accent = if (source.luminance() > 0.62f) {
        // Pale samples need deepening or the button label loses contrast.
        lerp(source, Color(0xFF120A22), 0.35f)
    } else {
        source
    }

    val side = lerp(accent, Color(0xFFF7F3FF), 0.55f)
    val surface = pillFor(accent)

    return ArtworkColors(
        accent = accent,
        onAccent = accent.readableOn(),
        side = side,
        onSide = side.readableOn(),
        surface = surface,
        onSurface = Color(0xFF191129),
    )
}
