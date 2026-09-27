/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The one way to draw "the current artwork, heavily blurred" as a backdrop.
 *
 * The bitmap is decoded at a sane resolution and then blurred on the CPU, so
 * the result is a real blur on every supported Android version — `Modifier.blur`
 * alone is a no-op below Android 12, which is what used to leave these
 * backdrops looking like a stretched, pixelated thumbnail.
 */

package com.jay.glossy.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jay.glossy.utils.blurredCached
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Full-bleed blurred artwork.
 *
 * [blurStrength] is the backdrop's 0..1 strength: higher means a softer, more
 * diffuse image. Nothing is drawn while the artwork is missing or still
 * decoding, so the caller's own background stays visible.
 */
@Composable
fun BlurredArtworkBackdrop(
    url: String?,
    modifier: Modifier = Modifier,
    blurStrength: Float = 0.6f,
    placeholderColor: Color? = null,
) {
    val strength = blurStrength.coerceIn(0f, 1f)
    val bitmap = rememberBlurredArtwork(url = url, strength = strength)

    Box(modifier = modifier.fillMaxSize()) {
        if (placeholderColor != null) {
            Box(modifier = Modifier.fillMaxSize().background(placeholderColor))
        }
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Decodes [url] and returns a blurred [ImageBitmap], cached by url and
 * strength so returning to a track does not blur it again.
 */
@Composable
private fun rememberBlurredArtwork(
    url: String?,
    strength: Float,
): ImageBitmap? {
    val context = LocalContext.current

    // A generous source: at 400px+ the upscale to a phone screen stays smooth
    // and no visible block structure survives the blur. The old implementation
    // decoded at 48-128px, which is exactly what read as "pixelated".
    val sourcePx = when {
        strength < 0.33f -> 560
        strength < 0.66f -> 480
        else -> 420
    }
    val radius = (sourcePx * (0.04f + 0.16f * strength)).roundToInt().coerceIn(2, sourcePx / 4)
    val cacheKey = "$url#$sourcePx#$radius"

    var result by remember(url, sourcePx, radius) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url, sourcePx, radius) {
        if (url.isNullOrBlank()) {
            result = null
            return@LaunchedEffect
        }
        result =
            withContext(Dispatchers.Default) {
                try {
                    val loaded = context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(url)
                            .size(sourcePx, sourcePx)
                            .allowHardware(false)
                            .build(),
                    )
                    val source: Bitmap = loaded.image?.toBitmap() ?: return@withContext null
                    source.blurredCached(radius = radius, cacheKey = cacheKey)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
    }

    return result
}
