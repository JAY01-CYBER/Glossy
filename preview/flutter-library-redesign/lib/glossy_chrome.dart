/// The chrome both redesigned screens share: the floating nav bar, the
/// now-playing bar, artwork placeholders and the small round icon button.
///
/// These used to live inside the library screen. The Together screen needs the
/// same nav bar (tapping *Together* is how you get there) and the same artwork
/// language, so they moved here rather than being copied.
library;

import 'package:flutter/material.dart';

import 'glossy_tokens.dart';

/// One destination in the floating nav bar.
class GlossyDestination {
  const GlossyDestination(this.icon, this.label);

  final IconData icon;
  final String label;
}

const List<GlossyDestination> kGlossyDestinations = <GlossyDestination>[
  GlossyDestination(Icons.home_rounded, 'Home'),
  GlossyDestination(Icons.library_music_rounded, 'Library'),
  GlossyDestination(Icons.auto_awesome_rounded, 'Mix'),
  GlossyDestination(Icons.people_alt_rounded, 'Together'),
  GlossyDestination(Icons.settings_rounded, 'Settings'),
];

/// Glossy's floating nav bar. [activeIndex] indexes [kGlossyDestinations].
class GlossyNavBar extends StatelessWidget {
  const GlossyNavBar({
    super.key,
    required this.activeIndex,
    required this.onSelect,
  });

  final int activeIndex;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
      child: Container(
        height: 60,
        decoration: BoxDecoration(
          color: t.row,
          borderRadius: BorderRadius.circular(26),
          border: Border.all(color: t.border, width: GlossyDimens.hairline),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            for (int i = 0; i < kGlossyDestinations.length; i++)
              _NavItem(
                spec: kGlossyDestinations[i],
                active: i == activeIndex,
                onTap: () => onSelect(i),
              ),
          ],
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({required this.spec, required this.active, required this.onTap});

  final GlossyDestination spec;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 3),
            decoration: active
                ? BoxDecoration(
                    color: t.accentSoft,
                    borderRadius: BorderRadius.circular(GlossyDimens.related),
                  )
                : null,
            child: Icon(
              spec.icon,
              size: 21,
              color: active ? t.accent : t.textLow,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            spec.label,
            style: TextStyle(
              fontSize: 9.5,
              fontWeight: FontWeight.w500,
              color: active ? t.accent : t.textMuted,
            ),
          ),
        ],
      ),
    );
  }
}

/// Now-playing bar, so a screen is judged with the chrome the app keeps above
/// the nav bar. [progress] is 0–1, [onTap] opens the player.
class GlossyMiniPlayer extends StatelessWidget {
  const GlossyMiniPlayer({
    super.key,
    required this.title,
    required this.subtitle,
    required this.progress,
    required this.playing,
    this.onTap,
  });

  final String title;
  final String subtitle;
  final double progress;
  final bool playing;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: t.miniBar,
        border: Border(
          top: BorderSide(color: t.border, width: GlossyDimens.hairline),
        ),
      ),
      child: GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: Column(
          children: <Widget>[
            SizedBox(
              height: 2,
              width: double.infinity,
              child: ColoredBox(
                color: t.chip,
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: FractionallySizedBox(
                    widthFactor: progress.clamp(0.0, 1.0),
                    heightFactor: 1,
                    child: ColoredBox(color: t.accent),
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 9, 12, 9),
              child: Row(
                children: <Widget>[
                  GlossyCover(seed: title, size: 38, radius: 10),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: t.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          subtitle,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontSize: 11, color: t.textLow),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    playing ? Icons.pause_rounded : Icons.play_arrow_rounded,
                    size: 22,
                    color: t.textPrimary,
                  ),
                  const SizedBox(width: 14),
                  Icon(Icons.skip_next_rounded, size: 22, color: t.textPrimary),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Album-art placeholder: a swatch cycled by id, with the leading glyph on the
/// accent gradient for the auto collections.
class GlossyCover extends StatelessWidget {
  const GlossyCover({
    super.key,
    required this.seed,
    this.icon,
    this.size,
    this.radius = GlossyDimens.cornerCard,
    this.iconSize,
    this.circle = false,
    this.pinned = false,
  });

  final String seed;
  final IconData? icon;
  final double? size;
  final double radius;
  final double? iconSize;
  final bool circle;
  final bool pinned;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final BoxDecoration decoration = BoxDecoration(
      color: pinned ? null : GlossyTokens.swatchFor(seed),
      gradient: pinned
          ? LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: <Color>[
                t.accent,
                Color.lerp(t.accent, Colors.black, 0.3)!,
              ],
            )
          : null,
      shape: circle ? BoxShape.circle : BoxShape.rectangle,
      borderRadius: circle ? null : BorderRadius.circular(radius),
    );

    final Widget content = Center(
      child: icon == null
          ? null
          : Icon(
              icon,
              size: iconSize ?? (size ?? 52) * 0.42,
              color: pinned ? t.onAccent : GlossyTokens.onSwatch,
            ),
    );

    if (size == null) {
      return DecoratedBox(decoration: decoration, child: content);
    }
    return Container(
      width: size,
      height: size,
      decoration: decoration,
      child: content,
    );
  }
}

/// The round, hairline-bordered icon button the headers use.
class GlossyIconButton extends StatelessWidget {
  const GlossyIconButton({
    super.key,
    required this.icon,
    required this.onTap,
    this.tooltip,
    this.size = 38,
    this.iconSize = 19,
  });

  final IconData icon;
  final VoidCallback onTap;
  final String? tooltip;
  final double size;
  final double iconSize;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final Widget button = GestureDetector(
      onTap: onTap,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: t.chip,
          shape: BoxShape.circle,
          border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
        ),
        child: Icon(icon, size: iconSize, color: t.textSecondary),
      ),
    );
    if (tooltip == null) return button;
    return Tooltip(message: tooltip!, child: button);
  }
}

/// Screen header: the accent rule, an eyebrow, the title and its subtitle line.
/// Every redesigned screen starts with one, so the hierarchy is identical.
class GlossyHeader extends StatelessWidget {
  const GlossyHeader({
    super.key,
    required this.eyebrow,
    required this.title,
    required this.subtitle,
    this.actions = const <Widget>[],
  });

  final String eyebrow;
  final String title;
  final String subtitle;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        GlossyDimens.screenPadding,
        10,
        12,
        4,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            width: 3,
            height: 46,
            margin: const EdgeInsets.only(top: 2),
            decoration: BoxDecoration(
              color: t.accent,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: GlossyDimens.related),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  eyebrow,
                  style: TextStyle(
                    fontSize: 9.5,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.5,
                    color: t.accent,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 25,
                    fontWeight: FontWeight.w600,
                    height: 1.05,
                    color: t.textPrimary,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  subtitle,
                  style: TextStyle(fontSize: 11.5, color: t.textLow),
                ),
              ],
            ),
          ),
          for (final Widget action in actions) ...<Widget>[
            action,
            const SizedBox(width: 2),
          ],
        ],
      ),
    );
  }
}

/// A card with the prototype's surface, border and radius.
class GlossyCard extends StatelessWidget {
  const GlossyCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin = EdgeInsets.zero,
    this.accent = false,
    this.onTap,
  });

  final Widget child;
  final EdgeInsets padding;
  final EdgeInsets margin;
  final bool accent;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final Widget card = Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: t.row,
        borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
        border: Border.all(
          color: accent ? t.accentSoft : t.border,
          width: GlossyDimens.hairline,
        ),
      ),
      child: child,
    );
    if (onTap == null) return card;
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: card,
    );
  }
}

/// Small uppercase caption above a block, e.g. "MEMBERS · 3".
class GlossyCaption extends StatelessWidget {
  const GlossyCaption(this.text, {super.key, this.trailing});

  final String text;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.fromLTRB(2, 18, 2, 8),
      child: Row(
        children: <Widget>[
          Text(
            text.toUpperCase(),
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w600,
              letterSpacing: 1.2,
              color: t.textMuted,
            ),
          ),
          const Spacer(),
          ?trailing,
        ],
      ),
    );
  }
}

/// The primary action button, full width.
class GlossyButton extends StatelessWidget {
  const GlossyButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.tone = GlossyButtonTone.accent,
    this.busy = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final GlossyButtonTone tone;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final bool enabled = onPressed != null && !busy;
    final Color container = switch (tone) {
      GlossyButtonTone.accent => t.accent,
      GlossyButtonTone.danger => Theme.of(context).colorScheme.error,
      GlossyButtonTone.quiet => t.chip,
    };
    final Color foreground = switch (tone) {
      GlossyButtonTone.accent => t.onAccent,
      GlossyButtonTone.danger => Colors.white,
      GlossyButtonTone.quiet => t.textPrimary,
    };

    return Opacity(
      opacity: enabled ? 1 : 0.45,
      child: GestureDetector(
        onTap: enabled ? onPressed : null,
        behavior: HitTestBehavior.opaque,
        child: Container(
          height: 48,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: container,
            borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
            border: tone == GlossyButtonTone.quiet
                ? Border.all(color: t.chipOutline, width: GlossyDimens.hairline)
                : null,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              if (busy)
                SizedBox(
                  width: 15,
                  height: 15,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: foreground,
                  ),
                )
              else if (icon != null)
                Icon(icon, size: 19, color: foreground),
              if (busy || icon != null) const SizedBox(width: 8),
              Text(
                label,
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w600,
                  color: foreground,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

enum GlossyButtonTone { accent, danger, quiet }
