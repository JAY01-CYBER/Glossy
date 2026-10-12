/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The design system used by the redesigned screens (Home, Library, Settings,
 * Login, Player, Mini player).
 *
 * The palette has a dark and a light variant and follows the app theme, so the
 * redesigned screens are near-black on dark mode and near-white on light mode.
 * Components must read through [GlossyPalette] instead of hardcoding their own
 * copies, so the whole app stays on one spec. The teal accent and the category
 * colours used for album-art / avatar placeholders are shared by both.
 *
 * Typography is clamped to at most SemiBold(600): the design calls for clean
 * geometric type with no heavy 700+ weights.
 */

package com.jay.glossy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One set of design-system tokens.
 *
 * [Dark] is the original near-black palette, [Light] is its light-mode
 * counterpart and [PureBlack] is [Dark] pushed to true black for AMOLED
 * screens. The accent is deliberately identical in both so the app keeps its
 * identity whichever mode the user runs.
 */
data class GlossyColors(
    /** Page background. */
    val page: Color,
    /** Player (full screen + sheet) background. */
    val player: Color,
    /** List-row / card surface. */
    val row: Color,
    /** Mini-player bar — one step lighter than the page. */
    val miniBar: Color,
    /** Inactive chip / pill fill. */
    val chip: Color,
    /**
     * Hairline around an inactive chip. The Material 3 skin outlines its filter
     * chips; the hand-tuned palettes do too, one step up from the chip fill.
     */
    val chipOutline: Color,
    /** Hairline border and divider. */
    val border: Color,
    /** Brand accent: active states, progress, highlights. */
    val accent: Color,
    /** Text / icons placed on top of the accent. */
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textLow: Color,
    val textMuted: Color,
    /** Slightly translucent accent, for tonal tiles and focused rows. */
    val accentSoft: Color,
    /**
     * Behind the home header. The hand-tuned design washes a gradient of
     * [accent] over the page and leaves this transparent; a tonal skin fills it
     * with a solid container instead — see [materialisticColors].
     */
    val headerBand: Color,
    /** Text and icons sitting on [headerBand]. */
    val onHeaderBand: Color,
) {
    companion object {
        val Dark = GlossyColors(
            page = Color(0xFF121212),
            player = Color(0xFF141212),
            row = Color(0xFF1B1B1B),
            miniBar = Color(0xFF1E1E1E),
            chip = Color(0xFF232323),
            chipOutline = Color(0xFF2A2A2A),
            border = Color(0xFF2A2A2A),
            accent = Color(0xFF1D9E75),
            onAccent = Color(0xFF04342C),
            textPrimary = Color(0xFFF1F1F1),
            textSecondary = Color(0xFFA8A8A8),
            textLow = Color(0xFF7A7A7A),
            textMuted = Color(0xFF5F5F5F),
            accentSoft = Color(0x261D9E75),
            headerBand = Color.Transparent,
            onHeaderBand = Color(0xFF1D9E75),
        )

        val Light = GlossyColors(
            page = Color(0xFFF4F4F7),
            player = Color(0xFFFFFFFF),
            row = Color(0xFFFFFFFF),
            miniBar = Color(0xFFFFFFFF),
            chip = Color(0xFFEDEDF2),
            chipOutline = Color(0xFFE3E3EA),
            border = Color(0xFFE3E3EA),
            accent = Color(0xFF1D9E75),
            onAccent = Color(0xFF04342C),
            textPrimary = Color(0xFF15171C),
            textSecondary = Color(0xFF5A5F6B),
            textLow = Color(0xFF8A8F9A),
            textMuted = Color(0xFFA6ABB6),
            accentSoft = Color(0x1F1D9E75),
            headerBand = Color.Transparent,
            onHeaderBand = Color(0xFF1D9E75),
        )

        val PureBlack = Dark.copy(
            page = Color.Black,
            player = Color.Black,
            row = Color(0xFF0C0C0C),
            miniBar = Color(0xFF0C0C0C),
            chip = Color(0xFF171717),
            chipOutline = Color(0xFF242424),
            border = Color(0xFF242424),
        )
    }
}

/**
 * The "Materialistic" skin: the same token model as [GlossyColors], but every
 * colour is read from the Material 3 scheme rather than hand-picked.
 *
 * Surfaces become the tonal container levels, the accent becomes the primary
 * role, and the home header turns into an opaque `primaryContainer` band
 * instead of a gradient wash. Nothing about the screens' layout changes — they
 * simply repaint from the user's seed colour in both light and dark mode, and
 * pick up Material You wallpaper theming on Android 12+.
 */
fun materialisticColors(scheme: ColorScheme): GlossyColors = GlossyColors(
    page = scheme.surface,
    player = scheme.surface,
    row = scheme.surfaceContainerLow,
    miniBar = scheme.surfaceContainer,
    chip = scheme.surfaceContainerHighest,
    chipOutline = scheme.outline,
    border = scheme.outlineVariant,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    textPrimary = scheme.onSurface,
    textSecondary = scheme.onSurfaceVariant,
    textLow = scheme.onSurfaceVariant.copy(alpha = 0.75f),
    textMuted = scheme.onSurfaceVariant.copy(alpha = 0.55f),
    accentSoft = scheme.primaryContainer,
    headerBand = scheme.primaryContainer,
    onHeaderBand = scheme.onPrimaryContainer,
)

/** Tokens of the design system for the theme currently in composition. */
val LocalGlossyColors = staticCompositionLocalOf { GlossyColors.Dark }

/**
 * Exact tokens of the design system, resolved for the current theme.
 *
 * Provided by the app theme (see `MetrolistTheme`), so every redesigned screen
 * follows light/dark mode without passing colours around. The reads are
 * composition-only: inside `drawBehind`/`Canvas` capture the value first.
 */
object GlossyPalette {
    val Page: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.page

    val Player: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.player

    val Row: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.row

    val MiniBar: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.miniBar

    val Chip: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.chip

    val ChipOutline: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.chipOutline

    val Border: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.border

    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.accent

    val OnAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.onAccent

    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.textPrimary

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.textSecondary

    val TextLow: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.textLow

    val TextMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.textMuted

    val AccentSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.accentSoft

    val HeaderBand: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.headerBand

    val OnHeaderBand: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalGlossyColors.current.onHeaderBand
}

/**
 * The onboarding screens (sign-in and community) use their own violet identity
 * instead of the app's teal: they are drawn over a deep violet gradient and are
 * always dark, whatever the system theme is, so the first impression never
 * changes between light and dark mode.
 */
object GlossyOnboardingPalette {
    val BackdropTop = Color(0xFF2C0B4D)
    val BackdropMid = Color(0xFF3B1367)
    val BackdropBottom = Color(0xFF120A24)

    /** Soft radial light behind the app mark. */
    val Glow = Color(0xFF7C3AED)

    /** Primary action fill on the light bottom card. */
    val Primary = Color(0xFF7C3AED)
    val OnPrimary = Color(0xFFFFFFFF)

    /** The light card that carries the onboarding actions. */
    val Card = Color(0xF7FFFFFF)
    val CardBorder = Color(0x1F1A1226)
    val OnCard = Color(0xFF1A1226)
    val OnCardMuted = Color(0xFF6B6478)

    val CardSecondary = Color(0x14000000)

    /** Translucent pill used for the tagline chip over the backdrop. */
    val ChipFill = Color(0x33FFFFFF)
    val ChipBorder = Color(0x4DFFFFFF)
    val OnChip = Color(0xFFF3EDFF)

    val OnBackdrop = Color(0xFFF1F1F1)
    val OnBackdropMuted = Color(0xFFCFC4E4)

    /** Solid tile behind a row's icon on the light card. */
    val TileFill = Color(0x0F000000)
}

/** Album-art / avatar placeholder colours, cycled by item id. */
val GlossyCategorySwatches = listOf(
    Color(0xFF5DCAA5),
    Color(0xFFD4537E),
    Color(0xFF7F77DD),
    Color(0xFFEF9F27),
    Color(0xFFF0997B),
    Color(0xFF85B7EB),
    Color(0xFF97C459),
    Color(0xFFF09595),
)

/**
 * Stable colour for a given id, so a song or playlist keeps the same swatch
 * every time it is drawn.
 */
fun glossySwatchFor(seed: String?): Color {
    if (seed.isNullOrEmpty()) return GlossyCategorySwatches.first()
    var hash = 0
    for (char in seed) {
        hash = hash * 31 + char.code
    }
    val size = GlossyCategorySwatches.size
    val index = ((hash % size) + size) % size
    return GlossyCategorySwatches[index]
}

/** Colour of the text/icon that reads well on top of [glossySwatchFor]. */
fun glossyOnSwatchFor(color: Color): Color =
    if (color == GlossyCategorySwatches[3] || color == GlossyCategorySwatches[6]) {
        Color(0xFF232323)
    } else {
        Color(0xFF1A1A1A)
    }

/**
 * The dark tokens under the same member names as [GlossyPalette], for the
 * non-composable code that cannot read the theme-aware façade.
 */
private object DarkPaletteTokens {
    val Page = GlossyColors.Dark.page
    val Player = GlossyColors.Dark.player
    val Row = GlossyColors.Dark.row
    val MiniBar = GlossyColors.Dark.miniBar
    val Chip = GlossyColors.Dark.chip
    val Border = GlossyColors.Dark.border
    val Accent = GlossyColors.Dark.accent
    val OnAccent = GlossyColors.Dark.onAccent
    val TextPrimary = GlossyColors.Dark.textPrimary
    val TextSecondary = GlossyColors.Dark.textSecondary
    val TextLow = GlossyColors.Dark.textLow
    val TextMuted = GlossyColors.Dark.textMuted
    val AccentSoft = GlossyColors.Dark.accentSoft
}

/**
 * Material 3 colour scheme built from [GlossyPalette]. Every role used by the
 * app's Material components is mapped so nothing falls back to the default
 * purple baseline scheme.
 */
fun glossyDarkColorScheme(): ColorScheme {
    // Not a composable, so it cannot read the theme-aware [GlossyPalette]: it
    // always builds the dark teal scheme, hence the shadowed name below.
    val GlossyPalette = DarkPaletteTokens
    return darkColorScheme(
    primary = GlossyPalette.Accent,
    onPrimary = GlossyPalette.OnAccent,
    primaryContainer = GlossyPalette.Accent,
    onPrimaryContainer = GlossyPalette.OnAccent,
    inversePrimary = GlossyPalette.Accent,

    secondary = GlossyPalette.TextSecondary,
    onSecondary = Color(0xFF1B1B1B),
    secondaryContainer = GlossyPalette.Chip,
    onSecondaryContainer = GlossyPalette.TextPrimary,

    tertiary = GlossyPalette.Accent,
    onTertiary = GlossyPalette.OnAccent,
    tertiaryContainer = GlossyPalette.Chip,
    onTertiaryContainer = GlossyPalette.TextPrimary,

    background = GlossyPalette.Page,
    onBackground = GlossyPalette.TextPrimary,

    surface = GlossyPalette.Row,
    onSurface = GlossyPalette.TextPrimary,
    surfaceVariant = GlossyPalette.Chip,
    onSurfaceVariant = GlossyPalette.TextSecondary,
    surfaceTint = GlossyPalette.Accent,

    surfaceBright = GlossyPalette.MiniBar,
    surfaceDim = GlossyPalette.Page,
    surfaceContainerLowest = GlossyPalette.Page,
    surfaceContainerLow = GlossyPalette.Row,
    surfaceContainer = GlossyPalette.MiniBar,
    surfaceContainerHigh = GlossyPalette.Chip,
    surfaceContainerHighest = GlossyPalette.Border,

    inverseSurface = GlossyPalette.TextPrimary,
    inverseOnSurface = GlossyPalette.Page,

    error = Color(0xFFF09595),
    onError = Color(0xFF2A0E0E),
    errorContainer = Color(0xFF5C2B2B),
    onErrorContainer = Color(0xFFF1F1F1),

    outline = GlossyPalette.TextLow,
    outlineVariant = GlossyPalette.Border,
    scrim = Color(0xFF000000),

    primaryFixed = GlossyPalette.Accent,
    primaryFixedDim = GlossyPalette.Accent,
    onPrimaryFixed = GlossyPalette.OnAccent,
    onPrimaryFixedVariant = GlossyPalette.OnAccent,
    secondaryFixed = GlossyPalette.Chip,
    secondaryFixedDim = GlossyPalette.Chip,
    onSecondaryFixed = GlossyPalette.TextPrimary,
    onSecondaryFixedVariant = GlossyPalette.TextSecondary,
    tertiaryFixed = GlossyPalette.Chip,
    tertiaryFixedDim = GlossyPalette.Chip,
    onTertiaryFixed = GlossyPalette.TextPrimary,
    onTertiaryFixedVariant = GlossyPalette.TextSecondary,
)
}

/**
 * Same fonts as the rest of the app, but with no weight above SemiBold and a
 * calmer display scale — the design asks for bold/medium headings only.
 */
fun glossTypography(base: Typography): Typography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Medium),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Medium),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Medium),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Medium),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Normal),
    bodySmall = base.bodySmall.copy(fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium),
)

/** The design system's spacing scale, so screens do not invent their own. */
object GlossyDimens {
    /** Screen side padding. */
    val ScreenPadding = 24.dp

    /** Between related elements (icon ↔ label, title ↔ subtitle). */
    val Related = 12.dp

    /** Between sections. */
    val Section = 20.dp

    /** Hairline border / divider thickness. */
    val Hairline = 1.dp

    /** Corner radius for small elements (chips inside rows, tiles). */
    val CornerSmall = 8.dp

    /** Corner radius for cards. */
    val CornerCard = 16.dp

    /** Fully rounded pills. */
    val CornerPill = 999.dp
}
