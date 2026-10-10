import 'package:flutter/material.dart';

/// Color tokens mirroring Glossy's Compose design system
/// (`app/src/main/kotlin/com/jay/glossy/ui/theme/GlossyDesign.kt`).
///
/// Nothing in this prototype hardcodes a colour: screens read through
/// [GlossyTokens] so the Flutter canvas and the Compose app can be compared
/// token for token, and so both palettes stay in sync if the app changes.
@immutable
class GlossyTokens extends ThemeExtension<GlossyTokens> {
  const GlossyTokens({
    required this.page,
    required this.player,
    required this.row,
    required this.miniBar,
    required this.chip,
    required this.chipOutline,
    required this.border,
    required this.accent,
    required this.onAccent,
    required this.textPrimary,
    required this.textSecondary,
    required this.textLow,
    required this.textMuted,
    required this.accentSoft,
  });

  /// Page background.
  final Color page;

  /// Full-screen player / bottom-sheet background.
  final Color player;

  /// List-row and card surface.
  final Color row;

  /// Mini-player bar, one step lighter than the page on dark.
  final Color miniBar;

  /// Inactive chip / pill fill.
  final Color chip;

  /// Hairline around an inactive chip.
  final Color chipOutline;

  /// Hairline border and divider.
  final Color border;

  /// Brand accent: active states, progress, highlights.
  final Color accent;

  /// Text / icons placed on top of [accent].
  final Color onAccent;

  final Color textPrimary;
  final Color textSecondary;
  final Color textLow;
  final Color textMuted;

  /// Translucent accent, for tonal tiles and focused rows.
  final Color accentSoft;

  static const GlossyTokens dark = GlossyTokens(
    page: Color(0xFF121212),
    player: Color(0xFF141212),
    row: Color(0xFF1B1B1B),
    miniBar: Color(0xFF1E1E1E),
    chip: Color(0xFF232323),
    chipOutline: Color(0xFF2A2A2A),
    border: Color(0xFF2A2A2A),
    accent: Color(0xFF1D9E75),
    onAccent: Color(0xFF04342C),
    textPrimary: Color(0xFFF1F1F1),
    textSecondary: Color(0xFFA8A8A8),
    textLow: Color(0xFF7A7A7A),
    textMuted: Color(0xFF5F5F5F),
    accentSoft: Color(0x261D9E75),
  );

  static const GlossyTokens light = GlossyTokens(
    page: Color(0xFFF4F4F7),
    player: Color(0xFFFFFFFF),
    row: Color(0xFFFFFFFF),
    miniBar: Color(0xFFFFFFFF),
    chip: Color(0xFFEDEDF2),
    chipOutline: Color(0xFFE3E3EA),
    border: Color(0xFFE3E3EA),
    accent: Color(0xFF1D9E75),
    onAccent: Color(0xFF04342C),
    textPrimary: Color(0xFF15171C),
    textSecondary: Color(0xFF5A5F6B),
    textLow: Color(0xFF8A8F9A),
    textMuted: Color(0xFFA6ABB6),
    accentSoft: Color(0x1F1D9E75),
  );

  /// Album-art / avatar placeholder colours, cycled by item id.
  static const List<Color> swatches = <Color>[
    Color(0xFF5DCAA5),
    Color(0xFFD4537E),
    Color(0xFF7F77DD),
    Color(0xFFEF9F27),
    Color(0xFFF0997B),
    Color(0xFF85B7EB),
    Color(0xFF97C459),
    Color(0xFFF09595),
  ];

  /// Ink used for glyphs drawn on top of a swatch, matching the app.
  static const Color onSwatch = Color(0xFF1A1A1A);

  /// Stable colour for an id, mirroring `glossySwatchFor` in the Compose app,
  /// so an item keeps the same swatch every time it is drawn.
  static Color swatchFor(String seed) {
    if (seed.isEmpty) return swatches.first;
    // Masked to 31 bits each step: web ints are doubles, so an unmasked
    // Kotlin-style hash loses precision after ~11 characters and every id
    // collapses onto the same swatch.
    int hash = 0;
    for (final int unit in seed.codeUnits) {
      hash = (hash * 31 + unit) & 0x7FFFFFFF;
    }
    return swatches[hash % swatches.length];
  }

  @override
  GlossyTokens copyWith({
    Color? page,
    Color? player,
    Color? row,
    Color? miniBar,
    Color? chip,
    Color? chipOutline,
    Color? border,
    Color? accent,
    Color? onAccent,
    Color? textPrimary,
    Color? textSecondary,
    Color? textLow,
    Color? textMuted,
    Color? accentSoft,
  }) {
    return GlossyTokens(
      page: page ?? this.page,
      player: player ?? this.player,
      row: row ?? this.row,
      miniBar: miniBar ?? this.miniBar,
      chip: chip ?? this.chip,
      chipOutline: chipOutline ?? this.chipOutline,
      border: border ?? this.border,
      accent: accent ?? this.accent,
      onAccent: onAccent ?? this.onAccent,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      textLow: textLow ?? this.textLow,
      textMuted: textMuted ?? this.textMuted,
      accentSoft: accentSoft ?? this.accentSoft,
    );
  }

  @override
  GlossyTokens lerp(covariant GlossyTokens? other, double t) {
    if (other == null) return this;
    return GlossyTokens(
      page: Color.lerp(page, other.page, t)!,
      player: Color.lerp(player, other.player, t)!,
      row: Color.lerp(row, other.row, t)!,
      miniBar: Color.lerp(miniBar, other.miniBar, t)!,
      chip: Color.lerp(chip, other.chip, t)!,
      chipOutline: Color.lerp(chipOutline, other.chipOutline, t)!,
      border: Color.lerp(border, other.border, t)!,
      accent: Color.lerp(accent, other.accent, t)!,
      onAccent: Color.lerp(onAccent, other.onAccent, t)!,
      textPrimary: Color.lerp(textPrimary, other.textPrimary, t)!,
      textSecondary: Color.lerp(textSecondary, other.textSecondary, t)!,
      textLow: Color.lerp(textLow, other.textLow, t)!,
      textMuted: Color.lerp(textMuted, other.textMuted, t)!,
      accentSoft: Color.lerp(accentSoft, other.accentSoft, t)!,
    );
  }
}

/// The design system's spacing scale, so the prototype does not invent its own.
abstract final class GlossyDimens {
  /// Screen side padding.
  static const double screenPadding = 24;

  /// Between related elements (icon ↔ label, title ↔ subtitle).
  static const double related = 12;

  /// Between sections.
  static const double section = 20;

  /// Hairline border / divider thickness.
  static const double hairline = 1;

  /// Corner radius for small elements (tiles, chips inside rows).
  static const double cornerSmall = 8;

  /// Corner radius for cards.
  static const double cornerCard = 16;

  /// Fully rounded pills.
  static const double cornerPill = 999;
}

/// Reads the Glossy tokens for the theme currently in the tree.
extension GlossyThemeContext on BuildContext {
  GlossyTokens get glossy =>
      Theme.of(this).extension<GlossyTokens>() ?? GlossyTokens.dark;
}

/// Theme built from the tokens, mapping the roles the widgets use so nothing
/// falls back to the Material baseline purple.
ThemeData glossyTheme(Brightness brightness) {
  final GlossyTokens tokens =
      brightness == Brightness.dark ? GlossyTokens.dark : GlossyTokens.light;
  final ColorScheme scheme =
      ColorScheme.fromSeed(seedColor: tokens.accent, brightness: brightness)
          .copyWith(
    primary: tokens.accent,
    onPrimary: tokens.onAccent,
    secondaryContainer: tokens.chip,
    onSecondaryContainer: tokens.textPrimary,
    surface: tokens.row,
    onSurface: tokens.textPrimary,
    surfaceContainerLow: tokens.row,
    surfaceContainer: tokens.miniBar,
    surfaceContainerHighest: tokens.chip,
    onSurfaceVariant: tokens.textSecondary,
    outline: tokens.textLow,
    outlineVariant: tokens.border,
  );
  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: scheme,
    scaffoldBackgroundColor: tokens.page,
    extensions: <ThemeExtension<dynamic>>[tokens],
  );
}
