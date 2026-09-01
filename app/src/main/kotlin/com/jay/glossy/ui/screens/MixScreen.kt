/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.ui.component.GreetingSection
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.PlayStoreRefreshIndicator
import com.jay.glossy.ui.component.YouTubeGridItem
import com.jay.glossy.ui.menu.YouTubeAlbumMenu
import com.jay.glossy.ui.menu.YouTubePlaylistMenu
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.HomeViewModel
import com.jay.glossy.viewmodels.MixViewModel
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MixScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    mixViewModel: MixViewModel = hiltViewModel()
) {
    val accountPlaylists by homeViewModel.accountPlaylists.collectAsStateWithLifecycle()
    val ytMixes by mixViewModel.mixPlaylists.collectAsStateWithLifecycle()
    val isLoading by mixViewModel.isLoading.collectAsStateWithLifecycle()
    
    // Greeting Section ke liye variables
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val guestNamePref by rememberPreference(stringPreferencesKey("guest_name"), "")
    
    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val pullRefreshState = rememberPullToRefreshState()

    // Greeting section ke liye final name decide karna
    val finalName = remember(accountName, guestNamePref) {
        when {
            !accountName.isNullOrBlank() && !accountName.equals("Guest", ignoreCase = true) -> accountName!!
            guestNamePref.isNotBlank() -> guestNamePref
            else -> "J"
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.loadHomeData()
        mixViewModel.loadMixes()
    }

    val finalPlaylists = remember(accountPlaylists, ytMixes) {
        val list = mutableListOf<YTItem>()
        
        accountPlaylists?.find { 
            it.id == "LM" || (it is PlaylistItem && it.title.equals("Liked Music", ignoreCase = true)) 
        }?.let { 
            list.add(it) 
        }
        
        list.addAll(ytMixes)
        
        list.distinctBy { 
            when (it) {
                is PlaylistItem -> it.id
                is AlbumItem -> it.browseId
                else -> it.hashCode().toString()
            }
        }
    }

    Scaffold(
        topBar = {
            // TERA CUSTOM GREETING SECTION WALA TOP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                GreetingSection(userName = finalName)
                
                Text(
                    text = "Mix for you",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { scaffoldPadding ->
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (finalPlaylists.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), 
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = scaffoldPadding.calculateTopPadding(),
                        bottom = insetsPadding.calculateBottomPadding() + 32.dp 
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(finalPlaylists) { item ->
                        val itemId = when (item) {
                            is PlaylistItem -> item.id
                            is AlbumItem -> item.browseId
                            else -> ""
                        }

                        YouTubeGridItem(
                            item = item,
                            isActive = itemId in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                            isPlaying = isPlaying,
                            coroutineScope = scope,
                            thumbnailRatio = 1f,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    when (item) {
                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        is AlbumItem -> navController.navigate("album/${item.browseId}")
                                        else -> {}
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (item) {
                                            is PlaylistItem -> YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = scope,
                                                onDismiss = menuState::dismiss
                                            )
                                            is AlbumItem -> YouTubeAlbumMenu(
                                                albumItem = item,
                                                onDismiss = menuState::dismiss
                                            )
                                            else -> {}
                                        }
                                    }
                                }
                            )
                        )
                    }
                }
            } else if (isLoading) {
                PlayStoreRefreshIndicator(
                    isRefreshing = true,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = "No mixes available right now.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
