/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Bitmap blurring that does not depend on the platform's RenderEffect.
 *
 * `Modifier.blur` is a silent no-op below Android 12, which is why the artwork
 * backdrops used to fall back to a stretched low-resolution bitmap — it read as
 * a pixelated mosaic instead of a blur. These helpers blur the pixels
 * themselves, so the result looks the same on every supported version.
 */

package com.jay.glossy.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** How many blurred backdrops are kept decoded (each is a small bitmap). */
private const val BLUR_CACHE_ENTRIES = 6

private val blurCache = LruCache<String, ImageBitmap>(BLUR_CACHE_ENTRIES)

/**
 * Blurred copy of [this], cached by [cacheKey].
 *
 * The cache key must identify the source image and the blur amount, so a track
 * that comes back around does not have to be blurred again.
 */
fun Bitmap.blurredCached(
    radius: Int,
    cacheKey: String,
): ImageBitmap {
    blurCache.get(cacheKey)?.let { return it }
    val blurred = blurred(radius).asImageBitmap()
    blurCache.put(cacheKey, blurred)
    return blurred
}

/**
 * Separable box blur run three times, which is visually indistinguishable from
 * a gaussian of the same radius and linear in the number of pixels.
 *
 * Returns [this] when the radius is not worth the work; the caller owns the
 * result and may recycle it (the source is never modified).
 */
fun Bitmap.blurred(radius: Int): Bitmap {
    if (radius <= 0 || width <= 0 || height <= 0) return this

    val source =
        if (config == Bitmap.Config.ARGB_8888 && isMutable) {
            this
        } else {
            copy(Bitmap.Config.ARGB_8888, false) ?: return this
        }

    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)

    val scratch = IntArray(w * h)
    val passes = 3
    // Three passes of a box of radius r approximate a gaussian of about 2r, so
    // each pass is given a third of the requested radius.
    val boxRadius = (radius / passes).coerceAtLeast(1)

    repeat(passes) {
        boxBlurHorizontal(pixels, scratch, w, h, boxRadius)
        boxBlurVertical(scratch, pixels, w, h, boxRadius)
    }

    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

/** One horizontal box pass: [src] into [dst]. */
private fun boxBlurHorizontal(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val window = radius * 2 + 1
    for (y in 0 until height) {
        val rowStart = y * width
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0

        // Prime the window with the left edge clamped.
        for (i in -radius..radius) {
            val color = src[rowStart + i.coerceIn(0, width - 1)]
            sumA += (color ushr 24) and 0xFF
            sumR += (color ushr 16) and 0xFF
            sumG += (color ushr 8) and 0xFF
            sumB += color and 0xFF
        }

        for (x in 0 until width) {
            dst[rowStart + x] =
                ((sumA / window) shl 24) or
                ((sumR / window) shl 16) or
                ((sumG / window) shl 8) or
                (sumB / window)

            val outIndex = (x - radius).coerceIn(0, width - 1)
            val inIndex = (x + radius + 1).coerceIn(0, width - 1)
            val out = src[rowStart + outIndex]
            val into = src[rowStart + inIndex]
            sumA += ((into ushr 24) and 0xFF) - ((out ushr 24) and 0xFF)
            sumR += ((into ushr 16) and 0xFF) - ((out ushr 16) and 0xFF)
            sumG += ((into ushr 8) and 0xFF) - ((out ushr 8) and 0xFF)
            sumB += (into and 0xFF) - (out and 0xFF)
        }
    }
}

/** One vertical box pass: [src] into [dst]. */
private fun boxBlurVertical(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val window = radius * 2 + 1
    for (x in 0 until width) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0

        for (i in -radius..radius) {
            val color = src[i.coerceIn(0, height - 1) * width + x]
            sumA += (color ushr 24) and 0xFF
            sumR += (color ushr 16) and 0xFF
            sumG += (color ushr 8) and 0xFF
            sumB += color and 0xFF
        }

        for (y in 0 until height) {
            dst[y * width + x] =
                ((sumA / window) shl 24) or
                ((sumR / window) shl 16) or
                ((sumG / window) shl 8) or
                (sumB / window)

            val outIndex = (y - radius).coerceIn(0, height - 1)
            val inIndex = (y + radius + 1).coerceIn(0, height - 1)
            val out = src[outIndex * width + x]
            val into = src[inIndex * width + x]
            sumA += ((into ushr 24) and 0xFF) - ((out ushr 24) and 0xFF)
            sumR += ((into ushr 16) and 0xFF) - ((out ushr 16) and 0xFF)
            sumG += ((into ushr 8) and 0xFF) - ((out ushr 8) and 0xFF)
            sumB += (into and 0xFF) - (out and 0xFF)
        }
    }
}
