/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.playlist

import com.jay.glossy.R

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.toBitmap

// KMPalette Imports
import com.kmpalette.palette.graphics.Palette
import com.kmpalette.rememberPaletteState

import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.HideExplicitKey
import com.jay.glossy.db.entities.Playlist
import com.jay.glossy.db.entities.PlaylistEntity
import com.jay.glossy.db.entities.PlaylistSongMap
import com.metrolist.models.toMediaMetadata
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.playback.queues.YouTubePlaylistQueue
import com.jay.glossy.ui.component.ExpandableText
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.YouTubeListItem
import com.jay.glossy.ui.menu.YouTubePlaylistMenu
import com.jay.glossy.ui.menu.YouTubeSelectionSongMenu
import com.jay.glossy.ui.menu.YouTubeSongMenu
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.ui.utils.toImmersiveBackground
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.jay.glossy.ui.component.LiquidGlassIconButton
import com.jay.glossy.ui.component.layerBackdrop
import com.jay.glossy.ui.component.liquidGlass
import com.jay.glossy.ui.component.rememberBackdrop

// Haze Imports
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

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.playlistSongs.collectAsStateWithLifecycle()
    val dbPlaylist by viewModel.dbPlaylist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    var dominantColor by remember { mutableStateOf(Color.Black) }

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
    
    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedExtractedColor)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.haze(state = hazeState),
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom)
                    .union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (currentPlaylist == null) {
                    if (isLoading) {
                        item(key = "full_loading") {
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
                        item(key = "full_error") {
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
                    // FIX: Search करते वक़्त Header छिप जाएगा और उसकी जगह Spacer आ जायेगा
                    // लेकिन जब सर्च नहीं हो रहा, तो Spacer नहीं होगा और फोटो Edge-to-Edge आएगी!
                    if (isSearching || inSelectMode) {
                        item(key = "search_spacer", contentType = "header") {
                            Spacer(modifier = Modifier.height(topPadding))
                        }
                    } else {
                        item(key = "playlist_header", contentType = "header") {
                            Box(modifier = Modifier.animateItem()) {
                                OnlinePlaylistHeader(
                                    playlist = currentPlaylist,
                                    songs = songs,
                                    dbPlaylist = dbPlaylist,
                                    navController = navController,
                                    coroutineScope = coroutineScope,
                                    continuation = viewModel.continuation,
                                    solidBgColor = animatedExtractedColor,
                                    onSearchClick = { isSearching = true },
                                    onDominantColorExtracted = { dominantColor = it }
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = filteredSongs,
                        key = { index, item -> item.second.id + "item_$index" }
                    ) { index, (_, songItem) ->
                        Column(modifier = Modifier.animateItem()) {
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
                                    ),
                                trailingContent = {
                                    if (inSelectMode) {
                                        Checkbox(
                                            checked = songItem.id in selection,
                                            onCheckedChange = onCheckedChange
                                        )
                                    } else {
                                        com.jay.glossy.ui.component.IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(songItem, menuState::dismiss)
                                                }
                                            },
                                            onLongClick = {}
                                        ) {
                                            Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
                                        }
                                    }
                                }
                            )
                            
                            if (index < filteredSongs.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }

                    if (isLoading && songs.isEmpty()) {
                        item(key = "tracks_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                                    .animateItem(), 
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    } else if (error != null && songs.isEmpty()) {
                        item(key = "tracks_error") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp).animateItem(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = error ?: stringResource(R.string.error_unknown), color = Color.White)
                            }
                        }
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .animateItem(),
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

            val showScrolledTopBar by remember {
                derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || isSearching || inSelectMode }
            }

            AnimatedVisibility(
                visible = showScrolledTopBar,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    // FIX: Top Bar Color - अब डार्क सरफेस की जगह तुम्हारा कलरफुल ग्रेडिएंट ब्लर होगा
                    modifier = Modifier.hazeChild(
                        state = hazeState, 
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        style = HazeStyle(
                            backgroundColor = animatedExtractedColor.copy(alpha = 0.6f),
                            tints = listOf(HazeTint(animatedExtractedColor.copy(alpha = 0.6f))),
                            blurRadius = 24.dp
                        )
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                        } else {
                            Text(
                                text = currentPlaylist?.title ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        com.jay.glossy.ui.component.IconButton(
                            onClick = {
                                if (isSearching) {
                                    isSearching = false
                                    query = TextFieldValue()
                                } else if (inSelectMode) {
                                    onExitSelectionMode()
                                } else {
                                    navController.navigateUp()
                                }
                            },
                            onLongClick = {
                                if (!isSearching && !inSelectMode) {
                                    navController.backToMain()
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
                            com.jay.glossy.ui.component.IconButton(
                                enabled = selection.isNotEmpty(),
                                onClick = {
                                    menuState.show {
                                        YouTubeSelectionSongMenu(
                                            songSelection = filteredSongs.filter { it.second.id in selection }.map { it.second },
                                            onDismiss = menuState::dismiss,
                                            clearAction = onExitSelectionMode
                                        )
                                    }
                                },
                                onLongClick = {}
                            ) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
                            }
                        } else if (!isSearching) {
                            com.jay.glossy.ui.component.IconButton(
                                onClick = { isSearching = true },
                                onLongClick = {}
                            ) {
                                Icon(painterResource(R.drawable.search), "Search", tint = Color.White)
                            }
                            currentPlaylist?.let { nonNullPlaylist ->
                                com.jay.glossy.ui.component.IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubePlaylistMenu(
                                                playlist = nonNullPlaylist,
                                                songs = songs,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    onLongClick = {}
                                ) {
                                    Icon(painterResource(R.drawable.more_vert), "More", tint = Color.White)
                                }
                            }
                        }
                    }
                )
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
    solidBgColor: Color,
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

    val artworkBackdrop = rememberBackdrop()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val paletteState = rememberPaletteState()
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageBitmap) {
        val bm = imageBitmap
        if (bm != null && playlist.thumbnail != null && paletteGeneratedFor != playlist.thumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = playlist.thumbnail
        }
    }

    LaunchedEffect(paletteState.palette) {
        val palette = paletteState.palette
        if (palette != null) {
            onDominantColorExtracted(palette.toImmersiveBackground())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(240.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(3.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                ),
            shape = RoundedCornerShape(3.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playlist.thumbnail?.resize(1080, 1080))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    imageBitmap = state.result.image.toBitmap().asImageBitmap()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = playlist.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        val author = playlist.author
        if (author != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.combinedClickable(
                    onClick = {
                        if (author.id != null) {
                            navController.navigate("artist/${author.id}")
                        }
                    },
                ),
            ) {
                if (playlist.authorAvatarUrl != null) {
                    AsyncImage(
                        model = playlist.authorAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                    )
                }
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val totalDuration = songs.sumOf { it.duration ?: 0 }
        val nSongs = pluralStringResource(R.plurals.n_song, songs.size, songs.size)
        val durationText = if (totalDuration > 0) makeTimeString(totalDuration * 1000L) else null
        val metadataText = buildString {
            append(playlist.songCountText ?: nSongs)
            if (durationText != null) {
                append(" • ")
                append(durationText)
            }
        }
        Text(
            text = metadataText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xC4FFFFFF),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        val description = playlist.description
        if (!description.isNullOrBlank()) {
            ExpandableText(
                text = description,
                modifier = Modifier.padding(horizontal = 32.dp),
                collapsedMaxLines = 3,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = {
                    if (dbPlaylist != null) {
                        database.transaction {
                            val currentPlaylist = dbPlaylist.playlist
                            update(currentPlaylist, playlist)
                            update(currentPlaylist.toggleLike())
                        }
                    } else {
                        coroutineScope.launch(Dispatchers.IO) {
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
                                radioEndpointParams = playlist.radioEndpoint?.params,
                            ).toggleLike()
                            val songMetadata = songs.map { it.toMediaMetadata() }
                            database.withTransaction {
                                insert(playlistEntity)
                                songMetadata.onEach { insert(it) }
                                val songIds = songMetadata.map { it.id to it.setVideoId }
                                val createdPlaylist = database.playlistBlocking(playlistEntity.id)
                                    ?: throw IllegalStateException("Failed to create playlist")
                                database.addSongsToPlaylist(createdPlaylist, songIds)
                            }
                        }
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border,
                        ),
                        contentDescription = null,
                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Surface(
                onClick = {
                    if (!isListenTogetherGuest && songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            YouTubePlaylistQueue(
                                playlistId = playlist.id,
                                playlistTitle = playlist.title,
                                initialSongs = songs,
                                initialContinuation = continuation,
                            ),
                        )
                    }
                },
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Surface(
                onClick = {
                    menuState.show {
                        YouTubePlaylistMenu(
                            playlist = playlist,
                            songs = songs,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
