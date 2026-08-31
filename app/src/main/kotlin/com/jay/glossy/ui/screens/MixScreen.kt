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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val isLoadingHome by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    var mixPlaylists by remember { mutableStateOf<List<MixUiItem>>(emptyList()) }
    var isFetching by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    LaunchedEffect(homePage) {
        // Agar already mixes load ho gaye hain toh dubara math fetch karo
        if (mixPlaylists.isNotEmpty()) return@LaunchedEffect
        
        isFetching = true
        withContext(Dispatchers.IO) {
            try {
                val result = mutableListOf<MixUiItem>()
                var foundMixes = false

                // Helper Function: Item extraction ke liye
                fun extract(items: List<Any>) {
                    items.forEach { item ->
                        var id: String? = null
                        var title = ""
                        var thumbnail = ""
                        var author = ""

                        if (item is PlaylistItem) {
                            id = item.id
                            title = item.title
                            thumbnail = item.thumbnail ?: ""
                            author = item.author?.name ?: "Auto playlist"
                        } else if (item is AlbumItem) {
                            id = item.playlistId ?: item.browseId
                            title = item.title
                            thumbnail = item.thumbnail
                            author = item.artists?.joinToString { it.name } ?: "Auto playlist"
                        }

                        if (id != null && id != "LM") {
                            // Check for mix keywords (English, Hindi, Gujarati) OR default mix IDs
                            val isMix = id == "RDMM" || id.startsWith("RDTMAK") || id.startsWith("RDAMPL") || 
                                        title.contains("mix", true) || title.contains("मिक्स", true) || title.contains("મિક્સ", true)
                            if (isMix) {
                                result.add(MixUiItem(id, title, author, thumbnail))
                                foundMixes = true
                            }
                        }
                    }
                }

                // STEP 1: Try to find in currently loaded Home Page Cache
                homePage?.sections?.forEach { section ->
                    extract(section.items)
                }

                // STEP 2: SimpMusic Approach -> Search via Mixes Chip
                if (!foundMixes) {
                    val mixChip = homePage?.chips?.find { 
                        val t = it.title.lowercase()
                        t.contains("mix") || t.contains("मिक्स") || t.contains("મિક્સ")
                    }
                    if (mixChip?.endpoint?.params != null) {
                        val chipPage = YouTube.home(params = mixChip.endpoint.params).getOrNull()
                        chipPage?.sections?.forEach { section ->
                            extract(section.items)
                        }
                    }
                }

                // STEP 3: Deep Scan Pagination -> Load next 3 pages to find hidden mixes
                if (!foundMixes) {
                    var continuation = homePage?.continuation
                    for (i in 1..3) { 
                        if (continuation == null) break
                        val nextPage = YouTube.home(continuation = continuation).getOrNull()
                        nextPage?.sections?.forEach { section ->
                            extract(section.items)
                        }
                        if (foundMixes) break
                        continuation = nextPage?.continuation
                    }
                }

                // STEP 4: Ultimate Fallback -> Hardcode universally working mixes so screen is NEVER blank
                if (result.isEmpty()) {
                    result.add(MixUiItem("RDMM", "My Supermix", "Auto playlist", "https://www.gstatic.com/youtube/media/ytm/images/pbg/supermix-light-v2-active.png"))
                    result.add(MixUiItem("RDAMPLw", "Discover Mix", "Auto playlist", "https://www.gstatic.com/youtube/media/ytm/images/pbg/discover-mix-light-v2-active.png"))
                    result.add(MixUiItem("RDATW", "New Release Mix", "Auto playlist", "https://www.gstatic.com/youtube/media/ytm/images/pbg/new-release-mix-light-v2-active.png"))
                }

                mixPlaylists = result.distinctBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetching = false
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
                    // Liked Music rendering
                    accountPlaylists?.find { it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) }?.let { liked ->
                        item {
                            MixCardItem(
                                item = MixUiItem(liked.id, liked.title, liked.author?.name ?: "Auto playlist", liked.thumbnail ?: ""),
                                onClick = { navController.navigate("online_playlist/${liked.id}") }
                            )
                        }
                    }

                    // YouTube Mixes rendering
                    items(mixPlaylists) { mix ->
                        MixCardItem(
                            item = mix,
                            onClick = {
                                navController.navigate("online_playlist/${mix.id}")
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
    item: MixUiItem,
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
