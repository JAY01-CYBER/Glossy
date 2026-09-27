package com.jay.glossy.ui.screens.library

import com.jay.glossy.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jay.glossy.LocalNavController
import com.jay.glossy.constants.ChipSortTypeKey
import com.jay.glossy.constants.LibraryFilter
import com.jay.glossy.ui.component.CreatePlaylistDialog
import com.jay.glossy.ui.component.GlossyDashedTile
import com.jay.glossy.ui.component.GlossyIconAction
import com.jay.glossy.ui.component.GlossySearchDock
import com.jay.glossy.ui.component.GlossyTextTabs
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.utils.rememberEnumPreference

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
            // Header: bold title and overflow. Downloads and Liked live on the
            // dock below, so there is no second download button up here.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = GlossyDimens.ScreenPadding, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_library),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GlossyPalette.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
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

            Spacer(Modifier.height(16.dp))

            // Floating search pill with the Liked and Downloads shortcuts. It
            // sits above the tabs so every filter is one tap from it.
            GlossySearchDock(
                hint = stringResource(R.string.search_library),
                onSearchClick = { navController.navigate("search_input") },
                onLikedClick = { navController.navigate("auto_playlist/liked") },
                onDownloadsClick = { navController.navigate("auto_playlist/downloaded") },
            )

            Spacer(Modifier.height(16.dp))

            // Plain text tabs with a teal underline — the four design tabs first,
            // then the extra collections the app already had.
            GlossyTextTabs(
                items = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    LibraryFilter.SPOTIFY to "Spotify",
                    LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts),
                    LibraryFilter.LIBRARY to stringResource(R.string.mix),
                ),
                selected = filterType,
                onSelect = { filterType = it },
            )

            if (filterType == LibraryFilter.PLAYLISTS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreatePlaylistDialog = true }
                        .padding(
                            horizontal = GlossyDimens.ScreenPadding,
                            vertical = 10.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlossyDashedTile(
                        contentDescription = stringResource(R.string.glossy_new_playlist),
                        onClick = { showCreatePlaylistDialog = true },
                    )
                    Spacer(Modifier.width(GlossyDimens.Related))
                    Text(
                        text = stringResource(R.string.glossy_new_playlist),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GlossyPalette.TextPrimary,
                    )
                }
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
