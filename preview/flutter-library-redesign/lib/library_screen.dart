import 'package:flutter/material.dart';

import 'glossy_chrome.dart';
import 'glossy_tokens.dart';
import 'mock_data.dart';

/// The Library tab, rebuilt.
///
/// The Compose screen's four jobs stay: quick access to the pinned collections,
/// a way into search, a filter row, and the collections themselves. What
/// changes is that each job happens exactly once — the current layout reaches
/// Liked / Downloads / History / Stats three separate ways (quick tiles, search
/// dock buttons, overflow menu) — and that the collection body adapts to its
/// content instead of rendering every filter as the same row.
class LibraryScreen extends StatefulWidget {
  const LibraryScreen({super.key, this.onNavigate});

  /// Switches the prototype's screen, so the nav bar works from here too.
  final ValueChanged<int>? onNavigate;

  @override
  State<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends State<LibraryScreen> {
  /// Playlists by default; `?filter=albums` (any collection name) opens the
  /// preview straight on that collection.
  LibraryFilter _filter = _initialFilter();

  static LibraryFilter _initialFilter() {
    final String? name = Uri.base.queryParameters['filter'];
    return LibraryFilter.values.firstWhere(
      (LibraryFilter filter) => filter.name == name,
      orElse: () => LibraryFilter.playlists,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        const _LibraryHeader(),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.only(bottom: GlossyDimens.section),
            physics: const ClampingScrollPhysics(),
            children: <Widget>[
              const _QuickAccess(),
              const _SearchPill(),
              _FilterRow(
                active: _filter,
                onSelect: (LibraryFilter filter) =>
                    setState(() => _filter = filter),
              ),
              _ShelfCaption(filter: _filter),
              _ShelfBody(filter: _filter),
            ],
          ),
        ),
        const GlossyMiniPlayer(
          title: 'Karma Police',
          subtitle: 'Radiohead · OK Computer',
          progress: 0.38,
          playing: true,
        ),
        GlossyNavBar(
          activeIndex: 1,
          onSelect: (int index) => widget.onNavigate?.call(index),
        ),
      ],
    );
  }
}

/// Accent rule, eyebrow, title and the two actions that are not a collection:
/// search, and the overflow for the secondary entries.
class _LibraryHeader extends StatelessWidget {
  const _LibraryHeader();

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
                  'YOUR LIBRARY',
                  style: TextStyle(
                    fontSize: 9.5,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.5,
                    color: t.accent,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Your Library',
                  style: TextStyle(
                    fontSize: 25,
                    fontWeight: FontWeight.w600,
                    height: 1.05,
                    color: t.textPrimary,
                  ),
                ),
                const SizedBox(height: 5),
                // The tab can say how much is in it before you scroll.
                Text(
                  '84 playlists · 1,204 songs · 312 offline',
                  style: TextStyle(fontSize: 11.5, color: t.textLow),
                ),
              ],
            ),
          ),
          GlossyIconButton(icon: Icons.search_rounded, onTap: () {}),
          const SizedBox(width: 2),
          PopupMenuButton<String>(
            tooltip: 'More options',
            color: t.row,
            position: PopupMenuPosition.under,
            icon: Icon(Icons.more_vert_rounded, size: 20, color: t.textSecondary),
            onSelected: (String value) {},
            itemBuilder: (BuildContext context) => const <PopupMenuEntry<String>>[
              PopupMenuItem<String>(
                value: 'cached',
                child: Text('Cached songs'),
              ),
              PopupMenuItem<String>(
                value: 'recap',
                child: Text('Your recap'),
              ),
              PopupMenuItem<String>(
                value: 'import',
                child: Text('Import a playlist'),
              ),
              PopupMenuItem<String>(
                value: 'settings',
                child: Text('Library settings'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QuickTileSpec {
  const _QuickTileSpec(this.icon, this.label);

  final IconData icon;
  final String label;
}

/// The four collections worth one tap. This is the only place they appear now —
/// the search dock's duplicate buttons and the overflow duplicates are gone.
class _QuickAccess extends StatelessWidget {
  const _QuickAccess();

  static const List<_QuickTileSpec> _items = <_QuickTileSpec>[
    _QuickTileSpec(Icons.favorite_rounded, 'Liked'),
    _QuickTileSpec(Icons.download_rounded, 'Downloads'),
    _QuickTileSpec(Icons.history_rounded, 'History'),
    _QuickTileSpec(Icons.bar_chart_rounded, 'Stats'),
  ];

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        GlossyDimens.screenPadding,
        14,
        GlossyDimens.screenPadding,
        0,
      ),
      child: Row(
        children: <Widget>[
          for (int i = 0; i < _items.length; i++) ...<Widget>[
            if (i > 0) const SizedBox(width: GlossyDimens.related),
            Expanded(
              child: _QuickTile(spec: _items[i]),
            ),
          ],
        ],
      ),
    );
  }
}

class _QuickTile extends StatelessWidget {
  const _QuickTile({required this.spec});

  final _QuickTileSpec spec;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: () {},
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: t.row,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerCard),
          border: Border.all(color: t.border, width: GlossyDimens.hairline),
        ),
        child: Column(
          children: <Widget>[
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                color: t.accentSoft,
                borderRadius: BorderRadius.circular(11),
              ),
              child: Icon(spec.icon, size: 18, color: t.accent),
            ),
            const SizedBox(height: 8),
            Text(
              spec.label,
              style: TextStyle(
                fontSize: 10.5,
                fontWeight: FontWeight.w500,
                color: t.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// One search entry for the whole tab, replacing the dock that also carried two
/// buttons the quick tiles already own.
class _SearchPill extends StatelessWidget {
  const _SearchPill();

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        GlossyDimens.screenPadding,
        GlossyDimens.section,
        GlossyDimens.screenPadding,
        0,
      ),
      child: GestureDetector(
        onTap: () {},
        child: Container(
          height: 46,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          decoration: BoxDecoration(
            color: t.chip,
            borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
            border: Border.all(color: t.chipOutline, width: GlossyDimens.hairline),
          ),
          child: Row(
            children: <Widget>[
              Icon(Icons.search_rounded, size: 18, color: t.textSecondary),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Search your library, artists, albums',
                  style: TextStyle(fontSize: 13, color: t.textMuted),
                ),
              ),
              Icon(Icons.tune_rounded, size: 17, color: t.textSecondary),
            ],
          ),
        ),
      ),
    );
  }
}

class _FilterRow extends StatelessWidget {
  const _FilterRow({required this.active, required this.onSelect});

  final LibraryFilter active;
  final ValueChanged<LibraryFilter> onSelect;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: GlossyDimens.section),
      child: SizedBox(
        height: 34,
        child: ListView.separated(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(
            horizontal: GlossyDimens.screenPadding,
          ),
          itemCount: LibraryFilter.values.length,
          separatorBuilder: (BuildContext context, int index) =>
              const SizedBox(width: 8),
          itemBuilder: (BuildContext context, int index) {
            final LibraryFilter filter = LibraryFilter.values[index];
            return _FilterPill(
              label: filter.label,
              selected: filter == active,
              onTap: () => onSelect(filter),
            );
          },
        ),
      ),
    );
  }
}

class _FilterPill extends StatelessWidget {
  const _FilterPill({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 15),
        decoration: BoxDecoration(
          color: selected ? t.accent : t.chip,
          borderRadius: BorderRadius.circular(GlossyDimens.cornerPill),
          border: Border.all(
            color: selected ? t.accent : t.chipOutline,
            width: GlossyDimens.hairline,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
            color: selected ? t.onAccent : t.textSecondary,
          ),
        ),
      ),
    );
  }
}

/// Item count plus the sort that suits the collection, instead of a sort buried
/// in the overflow menu.
class _ShelfCaption extends StatelessWidget {
  const _ShelfCaption({required this.filter});

  final LibraryFilter filter;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    final int count = shelfFor(filter).length;
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        GlossyDimens.screenPadding,
        18,
        GlossyDimens.screenPadding,
        8,
      ),
      child: Row(
        children: <Widget>[
          Text(
            '$count ${filter.label.toUpperCase()}',
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w600,
              letterSpacing: 1.2,
              color: t.textMuted,
            ),
          ),
          const Spacer(),
          Icon(Icons.swap_vert_rounded, size: 14, color: t.textSecondary),
          const SizedBox(width: 4),
          Text(
            filter.sortLabel,
            style: TextStyle(fontSize: 11.5, color: t.textSecondary),
          ),
        ],
      ),
    );
  }
}

class _ShelfBody extends StatelessWidget {
  const _ShelfBody({required this.filter});

  final LibraryFilter filter;

  @override
  Widget build(BuildContext context) {
    final List<ShelfEntry> entries = shelfFor(filter);
    if (filter.usesGrid) {
      return _TileGrid(entries: entries);
    }
    return Column(
      children: <Widget>[
        if (filter == LibraryFilter.playlists) const _AddPlaylistCard(),
        for (final ShelfEntry entry in entries)
          _EntryRow(
            entry: entry,
            circle: filter == LibraryFilter.artists,
          ),
      ],
    );
  }
}

class _AddPlaylistCard extends StatelessWidget {
  const _AddPlaylistCard();

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: () {},
      child: Container(
        margin: const EdgeInsets.fromLTRB(
          GlossyDimens.screenPadding,
          4,
          GlossyDimens.screenPadding,
          10,
        ),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: t.row,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: t.accentSoft, width: GlossyDimens.hairline),
        ),
        child: Row(
          children: <Widget>[
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: t.accentSoft,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(Icons.add_rounded, size: 22, color: t.accent),
            ),
            const SizedBox(width: GlossyDimens.related),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    'New playlist',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: t.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    'Start one from a song, album or artist',
                    style: TextStyle(fontSize: 11.5, color: t.textLow),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EntryRow extends StatelessWidget {
  const _EntryRow({required this.entry, this.circle = false});

  final ShelfEntry entry;
  final bool circle;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: () {},
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: GlossyDimens.screenPadding,
          vertical: 6,
        ),
        child: Row(
          children: <Widget>[
            GlossyCover(
              seed: entry.title,
              icon: entry.icon ?? (circle ? Icons.person_rounded : null),
              pinned: entry.icon != null,
              size: 52,
              radius: 14,
              circle: circle,
            ),
            const SizedBox(width: GlossyDimens.related),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    entry.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: t.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    entry.subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 11.5, color: t.textLow),
                  ),
                ],
              ),
            ),
            if (entry.offline)
              Padding(
                padding: const EdgeInsets.only(left: 8),
                child: Icon(
                  Icons.download_done_rounded,
                  size: 15,
                  color: t.accent,
                ),
              ),
            if (entry.trailing != null)
              Padding(
                padding: const EdgeInsets.only(left: 8),
                child: Text(
                  entry.trailing!,
                  style: TextStyle(fontSize: 11.5, color: t.textMuted),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _TileGrid extends StatelessWidget {
  const _TileGrid({required this.entries});

  final List<ShelfEntry> entries;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      padding: const EdgeInsets.fromLTRB(
        GlossyDimens.screenPadding,
        6,
        GlossyDimens.screenPadding,
        6,
      ),
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: entries.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 14,
        mainAxisSpacing: 18,
        childAspectRatio: 0.74,
      ),
      itemBuilder: (BuildContext context, int index) =>
          _TileCard(entry: entries[index]),
    );
  }
}

class _TileCard extends StatelessWidget {
  const _TileCard({required this.entry});

  final ShelfEntry entry;

  @override
  Widget build(BuildContext context) {
    final GlossyTokens t = context.glossy;
    return GestureDetector(
      onTap: () {},
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Expanded(
            child: Stack(
              children: <Widget>[
                Positioned.fill(
                  child: GlossyCover(
                    seed: entry.title,
                    icon: Icons.music_note_rounded,
                    radius: 16,
                    iconSize: 30,
                  ),
                ),
                if (entry.offline)
                  Positioned(
                    right: 8,
                    bottom: 8,
                    child: Container(
                      width: 24,
                      height: 24,
                      decoration: BoxDecoration(
                        color: t.page.withValues(alpha: 0.72),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.download_done_rounded,
                        size: 14,
                        color: t.accent,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            entry.title,
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
            entry.subtitle,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 11, color: t.textLow),
          ),
        ],
      ),
    );
  }
}
