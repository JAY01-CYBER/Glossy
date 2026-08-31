/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.viewmodels.HomeViewModel
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel() // Sirf purana aur trusted HomeViewModel
) {
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val isLoadingHome by viewModel.isLoading.collectAsStateWithLifecycle()
    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    var mixPlaylists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var isFetchingMixes by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 1. HomeData load karna ZARURI hai, taaki 'Liked Music' mil sake
        viewModel.loadHomeData()

        // 2. Mixes ko background me safely dhoondhna
        if (mixPlaylists.isEmpty()) {
            isFetchingMixes = true
            withContext(Dispatchers.IO) {
                try {
                    val homePage = YouTube.home().getOrNull()
                    val params = homePage?.chips?.find { it.title.contains("mix", ignoreCase = true) }?.endpoint?.params
                    
                    // Agar API ne Mix chip bheja hai, toh direct load karo. Warna Home page ke multiple pages scan karo.
                    val mixSections = if (params != null) {
                        YouTube.home(params = params).getOrNull()?.sections
                    } else {
                        var currentSections = homePage?.sections
                        var continuation = homePage?.continuation
                        
                        // YouTube kabhi kabhi mixes 2nd ya 3rd page par chhupa deta hai, toh hum aage tak search karenge
                        for (i in 0..2) {
                            if (currentSections?.any { it.title.contains("mix", ignoreCase = true) } == true) {
                                break
                            }
                            if (continuation == null) break
                            
                            val nextHome = YouTube.home(continuation = continuation).getOrNull()
                            currentSections = currentSections.orEmpty() + nextHome?.sections.orEmpty()
                            continuation = nextHome?.continuation
                        }
                        currentSections
                    }

                    val fetched = mutableListOf<PlaylistItem>()
                    mixSections?.forEach { section ->
                        val isMixSection = section.title.contains("mix", ignoreCase = true)
                        
                        section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                            val title = playlist.title ?: ""
                            val isMix = isMixSection || 
                                        title.contains("mix", ignoreCase = true) || 
                                        playlist.id == "RDMM" || 
                                        playlist.id.startsWith("RDTMAK") ||
                                        playlist.id.startsWith("RDAMPL")
                                        
                            if (isMix && playlist.id != "LM") {
                                fetched.add(playlist)
                            }
                        }
                    }
                    mixPlaylists = fetched.distinctBy { it.id }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isFetchingMixes = false
                }
            }
        }
    }

    // Liked Music aur Youtube Mixes ko combine karna
    val finalPlaylists = remember(accountPlaylists, mixPlaylists) {
        val list = mutableListOf<PlaylistItem>()
        
        accountPlaylists?.find { 
            it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) 
        }?.let { 
            list.add(it) 
        }
        
        list.addAll(mixPlaylists)
        list.distinctBy { it.id }
    }

    val isCurrentlyLoading = isLoadingHome || isFetchingMixes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Mix for you", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
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
                        top = scaffoldPadding.calculateTopPadding() + 8.dp,
                        bottom = insetsPadding.calculateBottomPadding() + 32.dp 
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(finalPlaylists) { playlist ->
                        MixCardItem(
                            item = playlist,
                            onClick = {
                                navController.navigate("online_playlist/${playlist.id}")
                            }
                        )
                    }
                }
            } else if (isCurrentlyLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
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

@Composable
fun MixCardItem(
    item: PlaylistItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
        )
        
        if (!item.author?.name.isNullOrEmpty()) {
            Text(
                text = item.author!!.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
            )
        }
    }
}
