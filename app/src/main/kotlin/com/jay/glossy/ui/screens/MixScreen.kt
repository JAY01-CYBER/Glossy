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
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val isLoadingHome by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    // RAW YTItem list - No type casting, no data loss!
    var ytMixes by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    var isFetching by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    LaunchedEffect(homePage) {
        if (ytMixes.isNotEmpty()) return@LaunchedEffect
        
        isFetching = true
        withContext(Dispatchers.IO) {
            try {
                val fetched = mutableListOf<YTItem>()

                // 1. Dhoondho "Mix" wala chip (HomeScreen logic)
                val mixChip = homePage?.chips?.find { 
                    val t = it.title.lowercase()
                    t.contains("mix") || t.contains("मिक्स") || t.contains("મિક્સ")
                }
                
                val params = mixChip?.endpoint?.params
                if (params != null) {
                    // Browse FEmusic_home with params - This parses Grids perfectly
                    val browseResult = YouTube.browse("FEmusic_home", params).getOrNull()
                    browseResult?.items?.forEach { group ->
                        fetched.addAll(group.items)
                    }
                }

                // 2. Agar chip nahi chala, toh current homePage ke sections scan karo
                if (fetched.isEmpty()) {
                    homePage?.sections?.forEach { section ->
                        val isMixSec = section.title.lowercase().let { it.contains("mix") || it.contains("मिक्स") || it.contains("મિક્સ") }
                        
                        section.items.forEach { item ->
                            val title = when (item) { is PlaylistItem -> item.title; is AlbumItem -> item.title; else -> "" }.lowercase()
                            val id = when (item) { is PlaylistItem -> item.id; is AlbumItem -> item.browseId; else -> "" }
                            
                            val isMixItem = isMixSec || id == "RDMM" || id.startsWith("RDTMAK") || id.startsWith("RDAMPL") || title.contains("mix") || title.contains("मिक्स")
                            
                            if (isMixItem && id != "LM") {
                                fetched.add(item)
                            }
                        }
                    }
                }

                // Store untouched YTItems
                ytMixes = fetched.distinctBy { 
                    when (it) {
                        is PlaylistItem -> it.id
                        is AlbumItem -> it.browseId
                        else -> it.hashCode().toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetching = false
            }
        }
    }

    // Combine Liked Music (from accountPlaylists) with fetched Mixes
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
                    items(finalPlaylists) { item ->
                        
                        // Extract Display Data from Raw YTItem
                        val title = when (item) {
                            is PlaylistItem -> item.title
                            is AlbumItem -> item.title
                            else -> ""
                        }
                        val subtitle = when (item) {
                            is PlaylistItem -> item.author?.name
                            is AlbumItem -> item.artists?.joinToString { it.name }
                            else -> ""
                        }
                        val thumbnail = when (item) {
                            is PlaylistItem -> item.thumbnail
                            is AlbumItem -> item.thumbnail
                            else -> ""
                        }

                        MixCardItem(
                            title = title,
                            subtitle = subtitle ?: "YouTube Music",
                            thumbnail = thumbnail ?: "",
                            onClick = {
                                // EXACT HOME SCREEN NAVIGATION LOGIC - THIS FIXES THE EMPTY PLAYLIST CRASH!
                                when (item) {
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            } else if (isLoadingHome || isFetching) {
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
    title: String,
    subtitle: String,
    thumbnail: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
        )
        
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
            )
        }
    }
}
