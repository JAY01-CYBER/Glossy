import 'package:flutter/material.dart';

/// The collections the library can show, mirroring `LibraryFilter` in the
/// Compose app, plus the Spotify collection it carries locally.
enum LibraryFilter {
  playlists('Playlists'),
  albums('Albums'),
  songs('Songs'),
  artists('Artists'),
  spotify('Spotify'),
  podcasts('Podcasts'),
  mix('Mix');

  const LibraryFilter(this.label);

  final String label;

  /// Albums and mixes read better as an artwork grid than as a row per item.
  bool get usesGrid => this == LibraryFilter.albums || this == LibraryFilter.mix;

  /// Sort that suits the collection, shown next to the item count.
  String get sortLabel => switch (this) {
        LibraryFilter.artists => 'A–Z',
        LibraryFilter.mix => 'Refreshed daily',
        LibraryFilter.albums => 'Artist',
        _ => 'Recently added',
      };
}

/// One row / tile of the library.
///
/// Kept as plain data so the widgets stay presentational and the prototype can
/// later be pointed at the Kotlin engine's `Playlist` / `Album` / `Song` types
/// behind a thin mapping layer.
@immutable
class ShelfEntry {
  const ShelfEntry(
    this.title,
    this.subtitle, {
    this.trailing,
    this.icon,
    this.offline = false,
  });

  final String title;
  final String subtitle;

  /// Muted text at the end of a row (duration, episode count, …).
  final String? trailing;

  /// Leading glyph, for the auto playlists the app pins itself.
  final IconData? icon;

  /// Draws the "downloaded" tick.
  final bool offline;
}

/// Contents of each collection, as the engine would hand them over.
const Map<LibraryFilter, List<ShelfEntry>> libraryShelf =
    <LibraryFilter, List<ShelfEntry>>{
  LibraryFilter.playlists: <ShelfEntry>[
    ShelfEntry('Liked Music', 'Auto playlist · 248 songs',
        icon: Icons.favorite_rounded, offline: true),
    ShelfEntry('Downloaded', 'Auto playlist · 96 songs · 1.2 GB',
        icon: Icons.download_rounded),
    ShelfEntry('Late Night Drive', '42 songs · updated 3d ago', offline: true),
    ShelfEntry('Focus Loops', '31 songs · updated 1w ago'),
    ShelfEntry('Monsoon Evenings', '58 songs · updated 2w ago', offline: true),
    ShelfEntry('Workout Push', '27 songs · updated 1mo ago'),
    ShelfEntry('Sunday Slow', '19 songs · updated 1mo ago'),
  ],
  LibraryFilter.albums: <ShelfEntry>[
    ShelfEntry('In Rainbows', 'Radiohead · 2007', offline: true),
    ShelfEntry('Random Access Memories', 'Daft Punk · 2013'),
    ShelfEntry('Stranger in the Alps', 'Phoebe Bridgers · 2017', offline: true),
    ShelfEntry('Blonde', 'Frank Ocean · 2016'),
    ShelfEntry('Currents', 'Tame Impala · 2015', offline: true),
    ShelfEntry('Melodrama', 'Lorde · 2017'),
  ],
  LibraryFilter.songs: <ShelfEntry>[
    ShelfEntry('Karma Police', 'Radiohead · OK Computer',
        trailing: '4:21', offline: true),
    ShelfEntry('Instant Crush', 'Daft Punk · Random Access Memories',
        trailing: '5:37'),
    ShelfEntry('Motion Sickness', 'Phoebe Bridgers · Stranger in the Alps',
        trailing: '3:58', offline: true),
    ShelfEntry('Nights', 'Frank Ocean · Blonde', trailing: '5:07'),
    ShelfEntry('Weird Fishes', 'Radiohead · In Rainbows',
        trailing: '5:18', offline: true),
    ShelfEntry('The Less I Know', 'Tame Impala · Currents', trailing: '3:36'),
    ShelfEntry('Green Light', 'Lorde · Melodrama', trailing: '3:54'),
    ShelfEntry('Slow Burn', 'Kacey Musgraves · Golden Hour',
        trailing: '3:18'),
  ],
  LibraryFilter.artists: <ShelfEntry>[
    ShelfEntry('Radiohead', 'Artist · 412 songs · 18 albums'),
    ShelfEntry('Daft Punk', 'Artist · 128 songs · 6 albums'),
    ShelfEntry('Phoebe Bridgers', 'Artist · 74 songs · 4 albums'),
    ShelfEntry('Frank Ocean', 'Artist · 96 songs · 3 albums'),
    ShelfEntry('Tame Impala', 'Artist · 88 songs · 5 albums'),
    ShelfEntry('Lorde', 'Artist · 63 songs · 3 albums'),
  ],
  LibraryFilter.spotify: <ShelfEntry>[
    ShelfEntry('Discover Weekly', 'Spotify · 30 songs · new Monday',
        offline: true),
    ShelfEntry('Release Radar', 'Spotify · 30 songs · new Friday'),
    ShelfEntry('Daily Mix 1', 'Spotify · 50 songs'),
    ShelfEntry('Your Time Capsule', 'Spotify · 50 songs'),
  ],
  LibraryFilter.podcasts: <ShelfEntry>[
    ShelfEntry('The Daily', 'The New York Times · 2 new episodes',
        trailing: '24'),
    ShelfEntry('Song Exploder', 'Hrishikesh Hirway · weekly', trailing: '312'),
    ShelfEntry('Twenty Thousand Hertz', 'Defacto Sound · biweekly',
        trailing: '156'),
    ShelfEntry('No Dumb Questions', 'Destin & Matt · weekly', trailing: '214'),
  ],
  LibraryFilter.mix: <ShelfEntry>[
    ShelfEntry('Your Supermix', '50 songs · refreshed daily', offline: true),
    ShelfEntry('Chill Mix', 'Radiohead, Lorde, Phoebe Bridgers'),
    ShelfEntry('Energy Mix', 'Tame Impala, Daft Punk', offline: true),
    ShelfEntry('Rewind Mix', 'Songs you played in 2021'),
    ShelfEntry('Discover Mix', 'Fresh picks for you'),
    ShelfEntry('Replay Mix', 'Your heavy rotation'),
  ],
};

/// Entries of a collection, `const` empty when the engine has nothing yet.
List<ShelfEntry> shelfFor(LibraryFilter filter) =>
    libraryShelf[filter] ?? const <ShelfEntry>[];
