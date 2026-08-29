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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.toBitmap

import com.kmpalette.palette.graphics.Palette
import com.kmpalette.rememberPaletteState

import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.MyTopFilter
import com.jay.glossy.db.entities.Song
import com.jay.glossy.extensions.toMediaItem
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.playback.queues.ListQueue
import com.jay.glossy.ui.component.DefaultDialog
import com.jay.glossy.ui.component.DraggableScrollbar
import com.jay.glossy.ui.component.EmptyPlaceholder
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.SongListItem
import com.jay.glossy.ui.component.SortHeader
import com.jay.glossy.ui.menu.SelectionSongMenu
import com.jay.glossy.ui.menu.SongMenu
import com.jay.glossy.ui.menu.TopPlaylistMenu
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.ui.utils.toImmersiveBackground
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.viewmodels.TopPlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.jay.glossy.ui.component.LiquidGlassIconButton
import com.jay.glossy.ui.component.layerBackdrop
import com.jay.glossy.ui.component.liquidGlass
import com.jay.glossy.ui.component.rememberBackdrop

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopPlaylistScreen(
    navController: NavController,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val maxSize = viewModel.top

    val hazeState = remember { HazeState() }
    var dominantColor by remember { mutableStateOf(Color.Black) }
    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    val songs by viewModel.topSongs.collectAsStateWithLifecycle(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs ?: emptyList()
        else songs?.filter { song ->
            song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        } ?: emptyList()
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }

        if (selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id
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

    val sortType by viewModel.topPeriod.collectAsStateWithLifecycle()
    val name = stringResource(R.string.my_top) + " $maxSize"

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val state = rememberLazyListState()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedExtractedColor)
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier.haze(state = hazeState),
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom)
                    .union(WindowInsets.ime).asPaddingValues(),
            ) {
                if (isSearching || inSelectMode || songs?.isEmpty() == true) {
                    item(key = "search_spacer", contentType = "header") {
                        Spacer(modifier = Modifier.height(topPadding))
                    }
                }

                if (songs != null) {
                    if (songs!!.isEmpty()) {
                        item(key = "empty_placeholder") {
                            EmptyPlaceholder(
                                icon = R.drawable.music_note,
                                text = stringResource(R.string.playlist_is_empty),
                                modifier = Modifier.animateItem()
                            )
                        }
                    } else {
                        if (!isSearching && !inSelectMode) {
                            item(key = "playlist_header", contentType = "header") {
                                Box(modifier = Modifier.animateItem()) {
                                    TopPlaylistHeader(
                                        name = name,
                                        songs = songs!!,
                                        likeLength = likeLength,
                                        downloadState = downloadState,
                                        onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                        navController = navController,
                                        menuState = menuState,
                                        solidBgColor = animatedExtractedColor,
                                        onSearchClick = { isSearching = true },
                                        onDominantColorExtracted = { dominantColor = it }
                                    )
                                }
                            }
                        }

                        item(key = "songs_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 8.dp)
                                    .animateItem(),
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = false,
                                    onSortTypeChange = {
                                        viewModel.topPeriod.value = it
                                    },
                                    onSortDescendingChange = {},
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            MyTopFilter.ALL_TIME -> R.string.all_time
                                            MyTopFilter.DAY -> R.string.past_24_hours
                                            MyTopFilter.WEEK -> R.string.past_week
                                            MyTopFilter.MONTH -> R.string.past_month
                                            MyTopFilter.YEAR -> R.string.past_year
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    showDescending = false,
                                )
                            }
                        }
                    }

                    if (filteredSongs.isNotEmpty()) {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { index, song -> "${song.id}_$index" },
                        ) { index, song ->
                            val onCheckedChange: (Boolean) -> Unit = {
                                if (it) {
                                    selection.add(song.id)
                                } else {
                                    selection.remove(song.id)
                                }
                            }

                            SongListItem(
                                song = song,
                                albumIndex = index + 1,
                                isActive = song.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    if (inSelectMode) {
                                        Checkbox(
                                            checked = song.id in selection,
                                            onCheckedChange = onCheckedChange
                                        )
                                    } else {
                                        com.jay.glossy.ui.component.IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                            onLongClick = {}
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (inSelectMode) {
                                                onCheckedChange(song.id !in selection)
                                            } else if (song.song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = name,
                                                        items = songs!!.map { it.toMediaItem() },
                                                        startIndex = songs!!.indexOfFirst { it.id == song.id }
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                                selectionAnchorSongId = song.id
                                            } else {
                                                val anchorIndex = selectionAnchorSongId?.let { anchorSongId ->
                                                    filteredSongs.indexOfFirst { it.id == anchorSongId }
                                                } ?: -1

                                                if (anchorIndex == -1) {
                                                    onCheckedChange(true)
                                                    selectionAnchorSongId = song.id
                                                } else {
                                                    val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                    for (rangeIndex in range) {
                                                        val rangeSongId = filteredSongs[rangeIndex].id
                                                        if (rangeSongId !in selection) {
                                                            selection.add(rangeSongId)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }
            }

            DraggableScrollbar(
                modifier = Modifier
                    .padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues())
                    .align(Alignment.CenterEnd),
                scrollState = state,
                headerItems = if (isSearching || inSelectMode) 1 else 2
            )

            val showScrolledTopBar by remember {
                derivedStateOf { state.firstVisibleItemIndex > 0 || isSearching || inSelectMode }
            }

            AnimatedVisibility(
                visible = showScrolledTopBar,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
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
                        when {
                            inSelectMode -> {
                                Text(
                                    text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                            isSearching -> {
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
                            else -> {
                                Text(text = name, color = Color.White)
                            }
                        }
                    },
                    navigationIcon = {
                        com.jay.glossy.ui.component.IconButton(
                            onClick = {
                                when {
                                    isSearching -> {
                                        isSearching = false
                                        query = TextFieldValue()
                                        focusManager.clearFocus()
                                    }
                                    inSelectMode -> {
                                        onExitSelectionMode()
                                    }
                                    else -> {
                                        navController.navigateUp()
                                    }
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
                                        selection.addAll(filteredSongs.map { it.id })
                                    }
                                }
                            )
                            com.jay.glossy.ui.component.IconButton(
                                enabled = selection.isNotEmpty(),
                                onClick = {
                                    menuState.show {
                                        SelectionSongMenu(
                                            songSelection = filteredSongs.filter { it.id in selection },
                                            onDismiss = menuState::dismiss,
                                            clearAction = onExitSelectionMode,
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
                                Icon(painterResource(R.drawable.search), null, tint = Color.White)
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    downloadState: Int,
    onShowRemoveDownloadDialog: () -> Unit,
    navController: NavController,
    menuState: com.jay.glossy.ui.component.MenuState,
    solidBgColor: Color,
    onSearchClick: () -> Unit,
    onDominantColorExtracted: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    
    val artworkBackdrop = rememberBackdrop()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val paletteState = rememberPaletteState()
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    val thumbUrl = songs.firstOrNull()?.thumbnailUrl

    LaunchedEffect(imageBitmap) {
        val bm = imageBitmap
        if (bm != null && thumbUrl != null && paletteGeneratedFor != thumbUrl) {
            paletteState.generate(bm)
            paletteGeneratedFor = thumbUrl
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
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight / 2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(artworkBackdrop) 
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbUrl?.resize(1080, 1080))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        imageBitmap = state.result.image.toBitmap().asImageBitmap()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.4f to solidBgColor.copy(alpha = 0.4f),
                                0.8f to solidBgColor.copy(alpha = 0.9f),
                                1.0f to solidBgColor
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp) 
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                        color = Color.White,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall, 
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "Playlist • 2026",
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color(0xC4FFFFFF), 
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
                LiquidGlassIconButton(
                    backdrop = artworkBackdrop,
                    painter = painterResource(R.drawable.arrow_back),
                    modifier = Modifier.size(48.dp), 
                    onClick = { navController.navigateUp() }
                )

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.jay.glossy.ui.component.IconButton(
                        onClick = onSearchClick,
                        onLongClick = {}
                    ) {
                        Icon(painterResource(R.drawable.search), "Search", tint = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp)) 

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button (Liquid Glass)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlass(artworkBackdrop, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = name,
                                        items = songs.shuffled().map { it.toMediaItem() },
                                    ),
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.shuffle), null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // Play Button (White Pill)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 110.dp) 
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = name,
                                    items = songs.map { it.toMediaItem() },
                                ),
                            )
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.play),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                // Menu Button (Liquid Glass)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlass(artworkBackdrop, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                menuState.show {
                                    TopPlaylistMenu(
                                        downloadState = downloadState,
                                        onQueue = {
                                            playerConnection.addToQueue(
                                                songs.map { it.toMediaItem() }
                                            )
                                        },
                                        onDownload = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                                                Download.STATE_DOWNLOADING -> {
                                                    songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.id,
                                                            false,
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    songs.forEach { song ->
                                                        val downloadRequest = DownloadRequest
                                                            .Builder(song.id, song.id.toUri())
                                                            .setCustomCacheKey(song.id)
                                                            .setData(song.title.toByteArray())
                                                            .build()
                                                        DownloadService.sendAddDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            downloadRequest,
                                                            false,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onDismiss = { menuState.dismiss() }
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.more_vert), null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(20.dp)) 

            val durationText = if (likeLength > 0) makeTimeString(likeLength * 1000L) else ""
            val trackCountText = pluralStringResource(R.plurals.n_song, songs.size, songs.size)

            Text(
                text = if (durationText.isNotEmpty()) "$trackCountText • $durationText" else trackCountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(Modifier.height(8.dp))
        }
    }
}
