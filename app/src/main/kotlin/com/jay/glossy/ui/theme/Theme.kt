/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.theme

import com.jay.glossy.R
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import androidx.compose.runtime.CompositionLocalProvider
import com.jay.glossy.constants.ActiveDesignStyle
import com.jay.glossy.constants.AppFont
import com.jay.glossy.constants.CustomFontPathKey
import com.jay.glossy.constants.DesignStyle
import com.jay.glossy.constants.SelectedFontKey
import com.jay.glossy.utils.rememberPreference

val DefaultThemeColor = Color(0xFFED5564)

/**
 * The light-mode "frost" scheme: the ice-blue look from the reference shot —
 * pale glacial surfaces, glassy blue cards and one deep ice-teal accent.
 *
 * The roles ride the Material 3 names the app already paints from (the
 * Materialistic skin reads every Glossy token out of this scheme), so hour
 * cards land on `primaryContainer`, chips on the container levels, and the
 * accent on `primary` — no screen knows the look changed. Fixed colours, not
 * sampled from the seed: the point of this theme is to look exactly like the
 * reference, and a wallpaper or swatch seed fought the ice.
 */
private fun frostLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF0E7E8D),
    onPrimary = Color.White,
    primaryContainer = Color(0xCBC7E6F2),
    onPrimaryContainer = Color(0xFF0A3F4A),

    secondary = Color(0xFF3E6B78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xB3E4F2F7),
    onSecondaryContainer = Color(0xFF123B46),

    tertiary = Color(0xFF1BA9BF),
    onTertiary = Color.White,

    background = Color(0xFFDBEBF2),
    onBackground = Color(0xFF10262D),

    surface = Color(0xFFE9F4F8),
    onSurface = Color(0xFF10262D),
    surfaceVariant = Color(0xFFE1EEF4),
    onSurfaceVariant = Color(0xFF4E6E7A),
    surfaceTint = Color(0xFF0E7E8D),

    surfaceContainerLowest = Color(0xFFD2E6EE),
    surfaceContainerLow = Color(0xFFF6FBFD),
    surfaceContainer = Color(0xFFEFF8FB),
    surfaceContainerHigh = Color(0xFFE7F2F7),
    surfaceContainerHighest = Color(0xFFDDEDF4),

    outline = Color(0xFF6E8B96),
    outlineVariant = Color(0xFFCBDEE6),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    inverseSurface = Color(0xFF10262D),
    inverseOnSurface = Color(0xFFEFF6F9),
    inversePrimary = Color(0xFF9BD5E3),
    scrim = Color.Black,
)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    
    val (selectedFontValue) = rememberPreference(SelectedFontKey, AppFont.SYSTEM.value)
    // Read here as well as in the picker: the imported font is the one choice
    // whose family comes from a file on the device instead of the app's own
    // resources, so the theme cannot answer it from the stored choice alone.
    val (customFontPath) = rememberPreference(CustomFontPathKey, "")

    val brandFont = remember(selectedFontValue, customFontPath) {
        appFontFamily(AppFont.fromValue(selectedFontValue), customFontPath)
    }

    // The new design is dark-only: in light mode we keep the Material You
    // scheme so the app still has a proper light look. The design style is
    // pinned to classic — see [ActiveDesignStyle].
    val useNewDesign = ActiveDesignStyle == DesignStyle.TEAL && darkTheme

    val typography = remember(brandFont, useNewDesign) {
        val base = getTypography(brandFont = brandFont, plainFont = brandFont)
        if (useNewDesign) glossTypography(base) else base
    }

    // Dynamic colour (Material You wallpaper sampling) was removed: it only ever
    // applied to the classic look, which the design style no longer uses, and a
    // wallpaper palette fought the seed colour every other surface is built
    // from. The scheme now always comes from the chosen seed colour.
    val baseColorScheme = when {
        useNewDesign -> glossyDarkColorScheme()
        // Light mode is the fixed ice-blue frost scheme above; the seed colour
        // keeps driving dark mode only.
        !darkTheme -> frostLightColorScheme()
        else ->
            rememberDynamicColorScheme(
                seedColor = themeColor,
                isDark = darkTheme,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                style = PaletteStyle.Expressive
            )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // The redesigned surfaces (Settings and the Glossy components it is built
    // from) read these tokens instead of hardcoded colours, so they follow the
    // theme: near-black in dark mode, near-white in light mode. Under the
    // Materialistic style they are derived from the Material 3 scheme instead,
    // so every screen repaints tonally from the seed colour.
    val materialistic = ActiveDesignStyle == DesignStyle.MATERIALISTIC
    val glossyColors = when {
        materialistic -> materialisticColors(colorScheme)
        !darkTheme -> GlossyColors.Light
        pureBlack -> GlossyColors.PureBlack
        else -> GlossyColors.Dark
    }

    CompositionLocalProvider(LocalGlossyColors provides glossyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = GlossyShapes,
            content = content
        )
    }
}

/**
 * Global Material You shape system. Every Material component (cards, dialogs,
 * buttons, chips, bottom sheets, text fields, menus...) picks its corner
 * radius from here, giving the whole app one consistent rounded look without
 * touching individual screens.
 */
val GlossyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
