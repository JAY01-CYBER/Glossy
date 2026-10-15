/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.theme

import com.jay.glossy.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jay.glossy.constants.AppFont
import java.io.File

val GoogleSansFontFamily = FontFamily(Font(R.font.google_sans, FontWeight.Normal))
val GoogleSansFlexFontFamily = FontFamily(Font(R.font.google_sans_flex, FontWeight.Normal))
val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val ManropeFontFamily = FontFamily(Font(R.font.manrope, FontWeight.Normal))
val OutfitFontFamily = FontFamily(Font(R.font.outfit, FontWeight.Normal))
val PlusJakartaSansFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val PoppinsFontFamily = FontFamily(Font(R.font.poppins, FontWeight.Normal))
val RoundexFontFamily = FontFamily(Font(R.font.roundex, FontWeight.Normal))

/**
 * A family built from a font file on the device — the "bring your own font"
 * option behind [AppFont.CUSTOM].
 *
 * The text stack loads the file itself, so nothing has to be bundled or
 * re-installed with the app. A path that is missing, or a file that turns out
 * not to be a font at all, returns null and the caller draws the system
 * typeface instead: a preference set once must not be able to break the app
 * after the file it points at has been cleared or moved.
 */
fun fontFamilyFromFile(path: String?): FontFamily? {
    val file = path?.takeIf(String::isNotBlank)?.let(::File) ?: return null
    if (!file.isFile) return null
    return runCatching { FontFamily(Font(file)) }.getOrNull()
}

/**
 * The family one of the app's font choices draws with, in one place so the
 * theme and the picker cannot disagree about what a choice means.
 *
 * [customFontPath] is only read for [AppFont.CUSTOM]; every other choice is a
 * typeface that ships with the app.
 */
fun appFontFamily(appFont: AppFont, customFontPath: String? = null): FontFamily =
    when (appFont) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.GOOGLE_SANS -> GoogleSansFontFamily
        AppFont.GOOGLE_SANS_FLEX -> GoogleSansFlexFontFamily
        AppFont.INTER -> InterFontFamily
        AppFont.MANROPE -> ManropeFontFamily
        AppFont.OUTFIT -> OutfitFontFamily
        AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
        AppFont.POPPINS -> PoppinsFontFamily
        AppFont.ROUNDEX -> RoundexFontFamily
        AppFont.CUSTOM -> fontFamilyFromFile(customFontPath) ?: FontFamily.Default
    }

fun getTypography(brandFont: FontFamily, plainFont: FontFamily = FontFamily.Default): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = brandFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = plainFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp 
    ),
    bodyMedium = TextStyle(
        fontFamily = plainFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = plainFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = plainFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = plainFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = plainFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

val AppTypography = getTypography(FontFamily.Default)
