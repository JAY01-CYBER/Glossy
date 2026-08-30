/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.utils

import androidx.compose.ui.graphics.Color
import com.kmpalette.palette.graphics.Palette

/**
 * Apple-Music-style immersive page background derived from the artwork.
 * Uses the DOMINANT swatch and darkens adaptively so white text is always readable.
 */
fun Palette?.toImmersiveBackground(): Color {
    val p = this ?: return Color.Black
    
    val rgb = p.getDominantColor(0).takeIf { it != 0 }
        ?: p.getMutedColor(0).takeIf { it != 0 }
        ?: p.getVibrantColor(0).takeIf { it != 0 }
        ?: return Color.Black
        
    val base = Color(rgb)
    
    val luminance = 0.299f * base.red + 0.587f * base.green + 0.114f * base.blue
    val darkenFactor = 0.35f + 0.45f * luminance
    
    return androidx.compose.ui.graphics.lerp(base, Color.Black, darkenFactor)
}
