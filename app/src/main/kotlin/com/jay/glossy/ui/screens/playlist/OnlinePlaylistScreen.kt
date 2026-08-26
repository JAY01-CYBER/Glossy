/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.playlist

import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.constants.HideExplicitKey
import com.jay.glossy.db.entities.Playlist
import com.jay.glossy.db.entities.PlaylistEntity
import com.jay.glossy.db.entities.PlaylistSongMap
import com.metrolist.models.toMediaMetadata
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.playback.queues.YouTubePlaylistQueue
import com.jay.glossy.ui.component.ExpandableText
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.YouTubeListItem
import com.jay.glossy.ui.menu.YouTubePlaylistMenu
import com.jay.glossy.ui.menu.YouTubeSelectionSongMenu
import com.jay.glossy.ui.menu.YouTubeSongMenu
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// PURE HAZE IMPORTS
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    var dominantColor by remember { mutableStateOf(Color.Transparent) }
    
    // Core Haze State for blur tracking
    val hazeState = remember { HazeState() }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { i, s -> i to s }
        else songs.mapIndexed { i, s -> i to s }.filter {
            it.second.title.contains(query.text, true) ||
                    it.second.artists.any { a -> a.name.contains(query.text, true) }
        }
    }

    var inSelectMode by remember { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<String>() }
    var selectionAnchorSongId by remember { mutableStateOf<String?>(null) }

    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(filteredSongs) {
        val currentSelection = selection.toList()
        currentSelection.reversed().forEach { songId ->
            if (filteredSongs.find { it.second.id == songId } == null) {
                selection.remove(songId)
            }
        }

        if (selectionAnchorSongId != null && filteredSongs.none { it.second.id == selectionAnchorSongId }) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.second.id in selection }?.second?.id
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val currentPlaylist = playlist

    val fallbackColor = Color(0xFF121212)
    val animatedExtractedColor by animateColorAsState(
        targetValue = if (dominantColor == Color.Transparent) fallbackColor else dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "gradientColor"
    )
    val mutedPaletteBg = lerp(animatedExtractedColor, Color.Black, 0.65f)
    
    val dynamicTopPadding = if (isSearching || inSelectMode) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    } else {
        0.dp
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mutedPaletteBg) 
        ) {
            LazyColumn(
                state = lazyListState,
                // FIXED 1: Only state passed to haze parent, no styles allowed here!
                modifier = Modifier.haze(state = hazeState), 
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom)
                    .union(WindowInsets.ime).asPaddingValues().apply { 
                        androidx.compose.foundation.layout.PaddingValues(
                            top = dynamicTopPadding, 
                            bottom = calculateBottomPadding()
                        )
                    },
            ) {
                if (currentPlaylist == null || songs.isEmpty()) {
                    if (isLoading) {
                        item(key = "loading_placeholder") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    } else if (error != null) {
                        item(key = "error_placeholder") {
                            Column(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = error ?: stringResource(R.string.error_unknown),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.TextButton(onClick = { viewModel.retry() }) {
                                    Text(stringResource(R.string.retry), color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    if (isSearching || inSelectMode) {
                        item {
                            Spacer(
                                modifier = Modifier.height(
                                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
                                )
                            )
                        }
                    } else {
                        item(key = "playlist_header") {
                            OnlinePlaylistHeader(
                                playlist = currentPlaylist,
                                songs = songs,
                                dbPlaylist = dbPlaylist,
                                navController = navController,
                                coroutineScope = coroutineScope,
                                continuation = viewModel.continuation,
                                mutedPaletteBg = mutedPaletteBg,
                                onSearchClick = { isSearching = true },
                                onDominantColorExtracted = { dominantColor = it }
                            )
                        }
                    }

                    itemsIndexed(filteredSongs) { index, (_, songItem) ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(songItem.id)
                            } else {
                                selection.remove(songItem.id)
                            }
                        }

                        YouTubeListItem(
                            item = songItem,
                            isActive = mediaMetadata?.id == songItem.id,
                            isPlaying = isPlaying,
                            isSelected = inSelectMode && songItem.id in selection,
                            modifier = Modifier
                                .padding(top = if (isSearching && index == 0) dynamicTopPadding else 0.dp) 
                                .combinedClickable(
                                    enabled = !hideExplicit || !songItem.explicit,
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(songItem.id !in selection)
                                        } else if (songItem.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubePlaylistQueue(
                                                    playlistId = currentPlaylist.id,
                                                    playlistTitle = currentPlaylist.title,
                                                    initialSongs = filteredSongs.map { it.second },
                                                    initialContinuation = viewModel.continuation,
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                            selectionAnchorSongId = songItem.id
                                        } else {
                                            val anchorIndex =
                                                selectionAnchorSongId?.let { anchorSongId ->
                                                    filteredSongs.indexOfFirst { it.second.id == anchorSongId }
                                                } ?: -1

                                            if (anchorIndex == -1) {
                                                onCheckedChange(true)
                                                selectionAnchorSongId = songItem.id
                                            } else {
                                                val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                for (rangeIndex in range) {
                                                    val rangeSongId = filteredSongs[rangeIndex].second.id
                                                    if (rangeSongId !in selection) {
                                                        selection.add(rangeSongId)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                .animateItem(),
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = songItem.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(songItem, menuState::dismiss)
                                        }
                                    }) {
                                        Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
                                    }
                                }
                            }
                        )
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(Modifier.height(50.dp))
                    }
                }
            }

            if (inSelectMode || isSearching) {
                TopAppBar(
                    title = {
                        if (inSelectMode) {
                            Text(
                                text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        } else if (isSearching) {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.search),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSearching) {
                                    isSearching = false
                                    query = TextFieldValue()
                                } else {
                                    onExitSelectionMode()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (inSelectMode) {
                            Checkbox(
                                checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                                onCheckedChange = {
                                    if (selection.size == filteredSongs.size) {
                                        selection.clear()
                                    } else {
                                        selection.clear()
                                        selection.addAll(filteredSongs.map { it.second.id })
                                    }
                                }
                            )
                            IconButton(
                                enabled = selection.isNotEmpty(),
                                onClick = {
                                    menuState.show {
                                        YouTubeSelectionSongMenu(
                                            songSelection = filteredSongs.filter { it.second.id in selection }.map { it.second },
                                            onDismiss = menuState::dismiss,
                                            clearAction = onExitSelectionMode
                                        )
                                    }
                                }
                            ) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
                            }
                        }
                    },
                    // FIXED 2: Used shape and explicit list for tints to clear ambiguity
                    modifier = Modifier.hazeChild(
                        state = hazeState, 
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        style = HazeStyle(
                            backgroundColor = mutedPaletteBg,
                            tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f))),
                            blurRadius = 24.dp
                        )
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            } else {
                val showScrolledTopBar by remember {
                    derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
                }
                AnimatedVisibility(
                    visible = showScrolledTopBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentPlaylist?.title ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(painterResource(R.drawable.arrow_back), null, tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(painterResource(R.drawable.search), "Search", tint = Color.White)
                            }
                            if (currentPlaylist != null) {
                                IconButton(onClick = {
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = currentPlaylist,
                                            songs = songs,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                }) {
                                    Icon(painterResource(R.drawable.more_vert), "More", tint = Color.White)
                                }
                            }
                        },
                        // FIXED 2: Used shape and explicit list for tints to clear ambiguity
                        modifier = Modifier.hazeChild(
                            state = hazeState, 
                            shape = androidx.compose.ui.graphics.RectangleShape,
                            style = HazeStyle(
                                backgroundColor = mutedPaletteBg,
                                tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f))),
                                blurRadius = 24.dp
                            )
                        ),
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlinePlaylistHeader(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    dbPlaylist: Playlist?,
    navController: NavController,
    coroutineScope: CoroutineScope,
    continuation: String?,
    mutedPaletteBg: Color,
    onSearchClick: () -> Unit,
    onDominantColorExtracted: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val artworkHazeState = remember { HazeState() }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // FIXED 1: Only state passed to haze parent
                    .haze(state = artworkHazeState)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.thumbnail?.resize(1080, 1080))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        try {
                            val coilImage = state.result.image
                            val originalBitmap = (coilImage as? coil3.BitmapImage)?.bitmap
                            if (originalBitmap != null) {
                                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, 1, 1, true)
                                Palette.from(scaledBitmap).generate { palette ->
                                    palette?.dominantSwatch?.rgb?.let { colorInt ->
                                        onDominantColorExtracted(Color(colorInt))
                                    } ?: palette?.vibrantSwatch?.rgb?.let { colorInt ->
                                        onDominantColorExtracted(Color(colorInt))
                                    } ?: palette?.mutedSwatch?.rgb?.let { colorInt ->
                                        onDominantColorExtracted(Color(colorInt))
                                    }
                                }
                                scaledBitmap.recycle()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, mutedPaletteBg),
                                startY = 0f
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist.author?.name ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "Playlist • 2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FIXED 2: Used shape and explicit list for tints
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .hazeChild(
                            state = artworkHazeState, 
                            shape = CircleShape,
                            style = HazeStyle(
                                backgroundColor = Color.Black.copy(alpha = 0.35f),
                                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.35f))),
                                blurRadius = 24.dp
                            )
                        )
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    Icon(painterResource(R.drawable.arrow_back), null, tint = Color.White)
                }

                // FIXED 2: Used shape and explicit list for tints
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .hazeChild(
                            state = artworkHazeState, 
                            shape = RoundedCornerShape(24.dp),
                            style = HazeStyle(
                                backgroundColor = Color.Black.copy(alpha = 0.35f),
                                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.35f))),
                                blurRadius = 24.dp
                            )
                        )
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLiked = dbPlaylist?.playlist?.bookmarkedAt != null
                    IconButton(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (dbPlaylist != null) {
                                database.withTransaction {
                                    val currentPlaylist = dbPlaylist.playlist
                                    update(currentPlaylist, playlist)
                                    update(currentPlaylist.toggleLike())
                                }
                            } else {
                                database.withTransaction {
                                    val playlistEntity = PlaylistEntity(
                                        name = playlist.title,
                                        browseId = playlist.id,
                                        thumbnailUrl = playlist.thumbnail,
                                        isEditable = playlist.isEditable,
                                        remoteSongCount = playlist.songCountText?.let {
                                            Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                        },
                                        playEndpointParams = playlist.playEndpoint?.params,
                                        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = playlist.radioEndpoint?.params
                                    ).toggleLike()
                                    insert(playlistEntity)
                                    songs.map { it.toMediaMetadata() }
                                        .onEach { insert(it) }
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index,
                                                setVideoId = song.setVideoId
                                            )
                                        }
                                        .forEach { insert(it) }
                                }
                            }
                        }
                    }) {
                        Icon(
                            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else Color.White
                        )
                    }

                    IconButton(onClick = onSearchClick) {
                        Icon(painterResource(R.drawable.search), "Search", tint = Color.White)
                    }

                    IconButton(onClick = {
                        menuState.show {
                            YouTubePlaylistMenu(
                                playlist = playlist,
                                songs = songs,
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }) {
                        Icon(painterResource(R.drawable.more_vert), "More", tint = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            if (!isListenTogetherGuest && songs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    YouTubePlaylistQueue(
                                        playlistId = playlist.id,
                                        playlistTitle = playlist.title,
                                        initialSongs = songs.shuffled(),
                                        initialContinuation = continuation
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.shuffle), null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 120.dp) 
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable {
                            if (!isListenTogetherGuest && songs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    YouTubePlaylistQueue(
                                        playlistId = playlist.id,
                                        playlistTitle = playlist.title,
                                        initialSongs = songs,
                                        initialContinuation = continuation
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isThisPlaying = isPlaying && mediaMetadata?.album?.id == playlist.id
                        Icon(
                            painter = painterResource(if (isThisPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isThisPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            songs.forEach { song ->
                                val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                                    .Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.title.toByteArray())
                                    .build()
                                androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.download), null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            val description = playlist.description
            if (!description.isNullOrBlank()) {
                ExpandableText(
                    text = description,
                    collapsedMaxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }

            Text(
                text = "${songs.size} tracks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(Modifier.height(8.dp))
        }
    }
}
