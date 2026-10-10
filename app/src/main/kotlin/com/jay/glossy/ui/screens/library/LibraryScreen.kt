package com.jay.glossy.ui.screens.library

import com.jay.glossy.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jay.glossy.LocalNavController
import com.jay.glossy.constants.ChipSortTypeKey
import com.jay.glossy.constants.LibraryFilter
import com.jay.glossy.ui.component.CreatePlaylistDialog
import com.jay.glossy.ui.component.GlossyChipRow
import com.jay.glossy.ui.component.GlossyDashedTile
import com.jay.glossy.ui.component.GlossyEyebrow
import com.jay.glossy.ui.component.GlossyIconAction
import com.jay.glossy.ui.component.GlossyQuickTile
import com.jay.glossy.ui.component.GlossySearchDock
import com.jay.glossy.ui.screens.SectionRule
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.utils.rememberEnumPreference

/**
 * One shortcut in the quick-access strip. Kept as data so the strip stays a
 * one-liner and adding a collection later is a single entry.
 */
private data class LibraryShortcut(
    val icon: Int,
    val label: Int,
    val route: String,
)

private val LibraryShortcuts = listOf(
    LibraryShortcut(R.drawable.favorite, R.string.liked, "auto_playlist/liked"),
    LibraryShortcut(R.drawable.download, R.string.glossy_downloaded, "auto_playlist/downloaded"),
    LibraryShortcut(R.drawable.history, R.string.history, "history"),
    LibraryShortcut(R.drawable.stats, R.string.stats, "stats"),
)

/**
 * The library, rebuilt around what people actually open it for: the top of the
 * screen now carries the same accent-rule lockup as the home sections, a strip
 * of raised shortcuts to the saved collections, the floating search dock, and
 * the collections themselves as pills rather than underlined text.
 */
@Composable
fun LibraryScreen() {
    val navController = LocalNavController.current
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.PLAYLISTS)

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }

    val filterContent = @Composable {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Lockup: accent rule, eyebrow and title — the same shape the home
            // sections use, so the library reads as part of the same page.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = GlossyDimens.ScreenPadding, end = 8.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionRule(hasEyebrow = true)
                Column(modifier = Modifier.weight(1f)) {
                    GlossyEyebrow(
                        text = stringResource(R.string.glossy_library_eyebrow),
                        color = GlossyPalette.Accent,
                    )
                    Text(
                        text = stringResource(R.string.filter_library),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GlossyPalette.TextPrimary,
                    )
                }
                Box {
                    GlossyIconAction(
                        icon = R.drawable.more_vert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = GlossyPalette.TextSecondary,
                        onClick = { showOverflowMenu = true },
                    )
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cached_playlist)) },
                            onClick = {
                                showOverflowMenu = false
                                navController.navigate("cache_playlist/cached")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.glossy_downloaded)) },
                            onClick = {
                                showOverflowMenu = false
                                navController.navigate("auto_playlist/downloaded")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history)) },
                            onClick = {
                                showOverflowMenu = false
                                navController.navigate("history")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stats)) },
                            onClick = {
                                showOverflowMenu = false
                                navController.navigate("stats")
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quick access: the four collections worth one tap, as raised cards
            // instead of entries buried in the overflow menu.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlossyDimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(GlossyDimens.Related),
            ) {
                LibraryShortcuts.forEach { shortcut ->
                    GlossyQuickTile(
                        icon = shortcut.icon,
                        label = stringResource(shortcut.label),
                        onClick = { navController.navigate(shortcut.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Floating search pill with the Liked and Downloads shortcuts. It
            // sits above the filters so every collection is one tap from it.
            GlossySearchDock(
                hint = stringResource(R.string.search_library),
                onSearchClick = { navController.navigate("search_input") },
                onLikedClick = { navController.navigate("auto_playlist/liked") },
                onDownloadsClick = { navController.navigate("auto_playlist/downloaded") },
            )

            Spacer(Modifier.height(14.dp))

            // The filters as pills — filled teal when active — rather than
            // underlined tabs, which gained nothing from a rule they shared
            // with the colour already doing the work.
            GlossyChipRow(
                items = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    LibraryFilter.SPOTIFY to "Spotify",
                    LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts),
                    LibraryFilter.LIBRARY to stringResource(R.string.mix),
                ),
                currentValue = filterType,
                onValueUpdate = { filterType = it },
            )

            if (filterType == LibraryFilter.PLAYLISTS) {
                NewPlaylistCard(onClick = { showCreatePlaylistDialog = true })
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.PODCASTS -> LibraryPodcastsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.SPOTIFY -> LibrarySpotifyPlaylistsScreen(
                navController = navController,
                filterContent = filterContent
            )
        }
    }
}

/**
 * "Create a playlist" as a card with the dashed plus tile as its leading slot.
 * It used to be a floating tile over the list, which read as a stray button;
 * giving it a surface makes it the first item of the shelf instead.
 */
@Composable
private fun NewPlaylistCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlossyDimens.ScreenPadding, vertical = 12.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyDashedTile(
            contentDescription = stringResource(R.string.glossy_new_playlist),
            onClick = onClick,
            size = 48.dp,
            corner = 14.dp,
        )
        Spacer(Modifier.width(GlossyDimens.Related))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.glossy_new_playlist),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = GlossyPalette.TextPrimary,
            )
            Text(
                text = stringResource(R.string.glossy_new_playlist_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GlossyPalette.TextMuted,
            )
        }
    }
}
