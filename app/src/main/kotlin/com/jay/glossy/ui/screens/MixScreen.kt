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
import com.metrolist.innertube.models.PlaylistItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    val mixPlaylists = remember(accountPlaylists, homePage) {
        val list = mutableListOf<PlaylistItem>()

        // 1. Sabse pehle 'Liked Music' add karenge taaki wo hamesha Top par rahe
        val likedMusic = accountPlaylists?.find { 
            it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) 
        }
        if (likedMusic != null) {
            list.add(likedMusic)
        }

        // 2. Home Page se sirf "RDTMAK" ID wale asli YouTube Mixes nikalenge
        // (RDTMAK = YouTube Music Auto-Generated Personalized Mixes)
        homePage?.sections?.forEach { section ->
            section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                if (playlist.id.startsWith("RDTMAK")) {
                    if (playlist.id != "LM") { // Duplicate check
                        list.add(playlist)
                    }
                }
            }
        }
        
        // 3. Fallback: Agar RDTMAK ID change ho gayi ho, toh section title se nikal lo
        if (list.size <= 1) {
            homePage?.sections?.forEach { section ->
                if (section.title.equals("Mixed for you", ignoreCase = true) || 
                    section.title.equals("Your mixes", ignoreCase = true)) {
                    section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                        if (playlist.id != "LM") list.add(playlist)
                    }
                }
            }
        }

        list.distinctBy { it.id }
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
            if (mixPlaylists.isNotEmpty()) {
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
                    items(mixPlaylists) { playlist ->
                        MixCardItem(
                            item = playlist,
                            onClick = {
                                navController.navigate("online_playlist/${playlist.id}")
                            }
                        )
                    }
                }
            } else if (isLoading) {
                // Loading Spinner
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
