package com.jay.glossy.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.spotify.SpotifyAccountViewModel
import com.jay.glossy.ui.component.ExpressivePullToRefreshBox

@Composable
fun LibrarySpotifyPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: SpotifyAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    ExpressivePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refreshPlaylists,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "filter") {
                filterContent()
            }

            if (!uiState.isAuthenticated) {
                item(key = "unauthenticated") {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Spotify is not connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { navController.navigate("spotify_login") }) {
                                Text("Login to Spotify")
                            }
                        }
                    }
                }
            } else if (playlists.isEmpty() && (uiState.isLoading || isRefreshing)) {
                // Agar playlist khali hai aur load ho raha hai, toh Loading spinner dikhao
                item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (playlists.isEmpty()) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No Spotify playlists found", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                itemsIndexed(
                    items = playlists,
                    key = { index, playlist -> "${playlist.id}_$index" }
                ) { index, playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("spotify_playlist/${playlist.id}")
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val thumbnailUrl = playlist.images.firstOrNull()?.url
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(4.dp))
                            
                            val totalTracks = playlist.tracks?.total ?: 0
                            val ownerName = playlist.owner?.displayName
                            
                            val subtitleText = when {
                                totalTracks > 0 -> "$totalTracks songs • Spotify"
                                !ownerName.isNullOrBlank() -> "Playlist • $ownerName"
                                else -> "Spotify Playlist"
                            }

                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
