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
import com.jay.glossy.constants.DesignStyleKey
import com.jay.glossy.constants.SelectedFontKey
import com.jay.glossy.utils.rememberPreference

val DefaultThemeColor = Color(0xFFED5564)

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

    // The design is dark-only: in light mode we keep the Material You scheme so
    // the app still has a proper light look. The style is pinned — see
    // [ActiveDesignStyle].
    val useNewDesign = ActiveDesignStyle != DesignStyle.CLASSIC && darkTheme

    val typography = remember(brandFont, useNewDesign) {
        val base = getTypography(brandFont = brandFont, plainFont = brandFont)
        if (useNewDesign) glossTypography(base) else base
    }

    // The scheme the user's seed colour produces. Only the classic look and the
    // tonal skin paint with it now; the design styles pin their own colours.
    val seedScheme = rememberDynamicColorScheme(
        seedColor = themeColor,
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.Expressive
    )

    // The redesigned surfaces read these tokens instead of hardcoded colours.
    // Which set they read is the listener's choice — the design's own
    // charcoal/rose tokens by default, or the Materialistic skin that derives
    // every role from the seed scheme (and Material You on Android 12+).
    // [ActiveDesignStyle] only decides which layout is drawn, never the paint.
    val (paletteName) = rememberPreference(DesignStyleKey, defaultValue = ActiveDesignStyle.name)
    val materialistic = remember(paletteName) { paletteName == DesignStyle.MATERIALISTIC.name }
    val designColors =
        if (ActiveDesignStyle == DesignStyle.FIGMA) GlossyColors.Figma else GlossyColors.Dark
    val glossyColors = when {
        materialistic -> materialisticColors(seedScheme)
        !darkTheme -> GlossyColors.Light
        pureBlack -> GlossyColors.PureBlack
        else -> designColors
    }

    // A design style pins its own scheme; the classic look and light mode keep
    // the user's seed colour.
    val baseColorScheme = when {
        materialistic -> seedScheme
        useNewDesign -> glossyDarkColorScheme(glossyColors)
        else -> seedScheme
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
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
