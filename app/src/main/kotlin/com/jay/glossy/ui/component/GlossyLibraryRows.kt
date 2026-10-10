/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Library rows in the new design system: a 46dp colour swatch, a bold title,
 * the song count and an overflow menu, with a hairline between rows.
 */

package com.jay.glossy.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jay.glossy.LocalNavController
import com.jay.glossy.R
import com.jay.glossy.db.entities.Playlist
import com.jay.glossy.ui.menu.PlaylistMenu
import com.jay.glossy.ui.menu.YouTubePlaylistMenu
import com.jay.glossy.ui.theme.GlossyPalette
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.WatchEndpoint
import kotlinx.coroutines.CoroutineScope

/**
 * One playlist row. Pass [route] for the virtual collections (Liked, Downloaded,
 * Cached…) which navigate to their own screens; local and online playlists fall
 * back to the usual id based routes. Passing [menuState] adds the overflow
 * button, exactly as the previous rows did.
 */
@Composable
fun GlossyPlaylistListRow(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    route: String? = null,
    subtitle: String? = null,
    menuState: MenuState? = null,
    coroutineScope: CoroutineScope? = null,
    showDivider: Boolean = true,
) {
    val navController = LocalNavController.current

    GlossyListRow(
        title = playlist.playlist.name,
        subtitle = subtitle ?: stringResource(R.string.glossy_playlist_songs, playlist.songCount),
        leading = {
            GlossyThumb(
                seed = playlist.id,
                size = 46.dp,
                corner = 12.dp,
                imageUrl = playlist.thumbnails.firstOrNull(),
            )
        },
        trailing = {
            if (menuState != null && coroutineScope != null) {
                GlossyIconAction(
                    icon = R.drawable.more_vert,
                    contentDescription = null,
                    tint = GlossyPalette.TextLow,
                    buttonSize = 36.dp,
                    iconSize = 20.dp,
                    onClick = {
                        menuState.show {
                            if (playlist.playlist.isEditable || playlist.songCount != 0) {
                                PlaylistMenu(
                                    playlist = playlist,
                                    coroutineScope = coroutineScope,
                                    onDismiss = menuState::dismiss,
                                )
                            } else {
                                playlist.playlist.browseId?.let { browseId ->
                                    YouTubePlaylistMenu(
                                        playlist = PlaylistItem(
                                            id = browseId,
                                            title = playlist.playlist.name,
                                            author = null,
                                            songCountText = null,
                                            thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                            playEndpoint = WatchEndpoint(
                                                playlistId = browseId,
                                                params = playlist.playlist.playEndpointParams
                                            ),
                                            shuffleEndpoint = WatchEndpoint(
                                                playlistId = browseId,
                                                params = playlist.playlist.shuffleEndpointParams
                                            ),
                                            radioEndpoint = WatchEndpoint(
                                                playlistId = "RDAMPL$browseId",
                                                params = playlist.playlist.radioEndpointParams
                                            ),
                                            isEditable = false
                                        ),
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        showDivider = showDivider,
        modifier = modifier,
        onClick = {
            when {
                route != null -> navController.navigate(route)
                !playlist.playlist.isEditable && playlist.songCount == 0 &&
                    playlist.playlist.browseId != null ->
                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                else -> navController.navigate("local_playlist/${playlist.id}")
            }
        },
    )
}
