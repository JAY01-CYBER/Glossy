/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The design system used by the redesigned screens (Home, Library, Settings,
 * Login, Player, Mini player).
 *
 * [Figma] is the design's own palette — deep charcoal surfaces with the soft
 * rose accent — and is the one the redesigned screens paint with. [Dark] and
 * [Light] are the earlier near-black / teal and light variants, kept for the
 * code paths that still describe them; [PureBlack] is [Figma] pushed to true
 * black for AMOLED screens. Components must read through [GlossyPalette]
 * instead of hardcoding their own copies, so the whole app stays on one spec.
 * The category colours used for album-art / avatar placeholders are shared by
 * all of them.
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One set of design-system tokens.
 *
 * [Figma] is the design's palette (charcoal #101114, rose #EDB8C3). [Dark] is
 * the earlier near-black/teal palette, [Light] its light-mode counterpart and
 * [PureBlack] is [Figma] pushed to true black for AMOLED screens. The accent
 * is deliberately identical across the dark variants so the app keeps its
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
        /**
         * The design's palette, taken straight from its tokens: page and player
         * #101114, nav and rows #1B1D22, mini player #24262C, borders #34353C,
         * text #F6F2F3 / #A1A0AA, accent #EDB8C3, selected surface #382B33.
         * Nothing here is derived from the user's seed colour: the brief is a
         * fixed after-dark look, not a tonal one.
         */
        val Figma = GlossyColors(
            page = Color(0xFF101114),
            player = Color(0xFF101114),
            row = Color(0xFF1B1D22),
            miniBar = Color(0xFF24262C),
            chip = Color(0xFF1B1D22),
            chipOutline = Color(0xFF34353C),
            border = Color(0xFF34353C),
            accent = Color(0xFFEDB8C3),
            onAccent = Color(0xFF2A1520),
            textPrimary = Color(0xFFF6F2F3),
            textSecondary = Color(0xFFA1A0AA),
            textLow = Color(0xFF6F6E77),
            textMuted = Color(0xFF55545C),
            accentSoft = Color(0xFF382B33),
            headerBand = Color.Transparent,
            onHeaderBand = Color(0xFFEDB8C3),
        )

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

        val PureBlack = Figma.copy(
            page = Color.Black,
            player = Color.Black,
            row = Color(0xFF121316),
            miniBar = Color(0xFF15161A),
            chip = Color(0xFF121316),
            chipOutline = Color(0xFF2A2B31),
            border = Color(0xFF2A2B31),
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

/**
 * Album-art / avatar placeholder colours, cycled by item id. Deep and
 * desaturated on purpose: they stand in for a sleeve on the charcoal page, so
 * the shelf keeps its rhythm without a single bright tile breaking the
 * after-dark look.
 */
val GlossyCategorySwatches = listOf(
    Color(0xFF7A4E5A),
    Color(0xFF4E5A66),
    Color(0xFF6A5570),
    Color(0xFF6E5B45),
    Color(0xFF755443),
    Color(0xFF475C63),
    Color(0xFF58624A),
    Color(0xFF6B4F56),
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
    if (color.luminance() > 0.45f) Color(0xFF1A1A1A) else Color(0xFFF6F2F3)

/**
 * Material 3 colour scheme built from a [GlossyColors] set. Every role used by
 * the app's Material components is mapped so nothing falls back to the default
 * purple baseline scheme. Defaults to the design palette; [Theme] passes the
 * set the current design style resolved.
 */
fun glossyDarkColorScheme(colors: GlossyColors = GlossyColors.Figma): ColorScheme {
    return darkColorScheme(
    primary = colors.accent,
    onPrimary = colors.onAccent,
    primaryContainer = colors.accentSoft,
    onPrimaryContainer = colors.accent,
    inversePrimary = colors.accent,

    secondary = colors.textSecondary,
    onSecondary = colors.page,
    secondaryContainer = colors.chip,
    onSecondaryContainer = colors.textPrimary,

    tertiary = colors.accent,
    onTertiary = colors.onAccent,
    tertiaryContainer = colors.chip,
    onTertiaryContainer = colors.textPrimary,

    background = colors.page,
    onBackground = colors.textPrimary,

    surface = colors.row,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.chip,
    onSurfaceVariant = colors.textSecondary,
    surfaceTint = colors.accent,

    surfaceBright = colors.miniBar,
    surfaceDim = colors.page,
    surfaceContainerLowest = colors.page,
    surfaceContainerLow = colors.row,
    surfaceContainer = colors.miniBar,
    surfaceContainerHigh = colors.chip,
    surfaceContainerHighest = colors.border,

    inverseSurface = colors.textPrimary,
    inverseOnSurface = colors.page,

    error = Color(0xFFF09595),
    onError = Color(0xFF2A0E0E),
    errorContainer = Color(0xFF5C2B2B),
    onErrorContainer = Color(0xFFF1F1F1),

    outline = colors.textLow,
    outlineVariant = colors.border,
    scrim = Color(0xFF000000),

    primaryFixed = colors.accent,
    primaryFixedDim = colors.accent,
    onPrimaryFixed = colors.onAccent,
    onPrimaryFixedVariant = colors.onAccent,
    secondaryFixed = colors.chip,
    secondaryFixedDim = colors.chip,
    onSecondaryFixed = colors.textPrimary,
    onSecondaryFixedVariant = colors.textSecondary,
    tertiaryFixed = colors.chip,
    tertiaryFixedDim = colors.chip,
    onTertiaryFixed = colors.textPrimary,
    onTertiaryFixedVariant = colors.textSecondary,
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

    /** Corner radius of the home's editorial hero, per the design. */
    val CornerHero = 24.dp

    /** Corner radius of the floating mini player. */
    val CornerMiniPlayer = 18.dp

    /** Height of the mini player, excluding its surrounding gap. */
    val MiniPlayerHeight = 60.dp

    /** Height of the floating navigation pill. */
    val NavPillHeight = 70.dp

    /** Fully rounded pills. */
    val CornerPill = 999.dp
}
