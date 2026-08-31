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
import androidx.compose.runtime.remember
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
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem

// UI ko YouTube ke formats se alag rakhne ke liye ek simple Data Class
data class MixUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel() // Direct HomeViewModel use kar rahe hain
) {
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    // TERA IDEA: Home Screen ka data copy karo
    val mixPlaylists = remember(accountPlaylists, homePage) {
        val list = mutableListOf<MixUiItem>()

        // 1. Sabse pehle 'Liked Music' add karo
        accountPlaylists?.find { 
            it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) 
        }?.let { 
            list.add(MixUiItem(it.id, it.title, it.author?.name ?: "Auto playlist", it.thumbnail ?: ""))
        }

        if (homePage != null) {
            // 2. Home page me woh Pura Section dhoondho jiske andar 'RDMM' (Supermix) hai
            val mixSection = homePage!!.sections.find { section ->
                section.items.any { item ->
                    val id = when (item) {
                        is PlaylistItem -> item.id
                        is AlbumItem -> item.playlistId ?: item.browseId
                        else -> null
                    }
                    id == "RDMM" || id?.startsWith("RDTMAK") == true
                }
            }

            // 3. Agar wo section mil gaya, toh uske saare mixes utha kar apni list me daal do!
            if (mixSection != null) {
                mixSection.items.forEach { item ->
                    when (item) {
                        is PlaylistItem -> {
                            list.add(MixUiItem(item.id, item.title, item.author?.name ?: "Auto playlist", item.thumbnail ?: ""))
                        }
                        is AlbumItem -> {
                            val id = item.playlistId ?: item.browseId
                            if (id != null) {
                                list.add(MixUiItem(id, item.title, item.artists?.joinToString { it.name } ?: "Auto playlist", item.thumbnail))
                            }
                        }
                        else -> {}
                    }
                }
            } else {
                // FALLBACK: Agar exact section nahi mila, toh pure home page me jahan bhi mix mile, utha lo
                homePage!!.sections.forEach { section ->
                    section.items.forEach { item ->
                        val id = when (item) {
                            is PlaylistItem -> item.id
                            is AlbumItem -> item.playlistId ?: item.browseId
                            else -> null
                        }
                        if (id == "RDMM" || id?.startsWith("RDTMAK") == true || id?.startsWith("RDAMPL") == true) {
                            when (item) {
                                is PlaylistItem -> list.add(MixUiItem(item.id, item.title, item.author?.name ?: "Auto playlist", item.thumbnail ?: ""))
                                is AlbumItem -> list.add(MixUiItem(id, item.title, item.artists?.joinToString { it.name } ?: "Auto playlist", item.thumbnail))
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        // Duplicates hata kar final list bhejo
        list.filter { it.id.isNotBlank() && it.id != "LM" }.distinctBy { it.id }
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
            if (mixPlaylists.isNotEmpty() || accountPlaylists?.isNotEmpty() == true) {
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
                    // Liked Music manually print kar rahe hain taki wo hamesha rahe
                    accountPlaylists?.find { it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) }?.let { liked ->
                        item {
                            MixCardItem(
                                item = MixUiItem(liked.id, liked.title, liked.author?.name ?: "Auto playlist", liked.thumbnail ?: ""),
                                onClick = { navController.navigate("online_playlist/${liked.id}") }
                            )
                        }
                    }

                    // Fir uske baad Home page se copy kiye hue mixes print karo
                    items(mixPlaylists) { mix ->
                        MixCardItem(
                            item = mix,
                            onClick = {
                                navController.navigate("online_playlist/${mix.id}")
                            }
                        )
                    }
                }
            } else if (isLoading) {
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
    item: MixUiItem, // Ab yeh Playlist model pe depend nahi karta!
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
        
        if (item.subtitle.isNotEmpty()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
            )
        }
    }
}
