/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.playlist

import com.jay.glossy.R

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed

import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.LocalSyncUtils
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.PlaylistEditLockKey
import com.jay.glossy.constants.PlaylistSongSortDescendingKey
import com.jay.glossy.constants.PlaylistSongSortType
import com.jay.glossy.constants.PlaylistSongSortTypeKey
import com.jay.glossy.constants.SwipeToRemoveSongKey
import com.jay.glossy.db.entities.Playlist
import com.jay.glossy.db.entities.PlaylistSong
import com.jay.glossy.extensions.move
import com.jay.glossy.extensions.toMediaItem
import com.metrolist.models.toMediaMetadata
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.playback.queues.ListQueue
import com.jay.glossy.ui.component.ActionPromptDialog
import com.jay.glossy.ui.component.DefaultDialog
import com.jay.glossy.ui.component.DraggableScrollbar
import com.jay.glossy.ui.component.EmptyPlaceholder
import com.jay.glossy.ui.component.ExpandableText
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.OverlayEditButton
import com.jay.glossy.ui.component.SongListItem
import com.jay.glossy.ui.component.SortHeader
import com.jay.glossy.ui.component.TextFieldDialog
import com.jay.glossy.ui.menu.CustomThumbnailMenu
import com.jay.glossy.ui.menu.LocalPlaylistMenu
import com.jay.glossy.ui.menu.SelectionSongMenu
import com.jay.glossy.ui.menu.SongMenu
import com.jay.glossy.ui.screens.settings.DarkMode
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.reportException
import com.jay.glossy.viewmodels.LocalPlaylistViewModel
import com.yalantis.ucrop.UCrop
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.playlistSongs.collectAsStateWithLifecycle()
    val onlinePlaylist by viewModel.onlinePlaylist.collectAsStateWithLifecycle()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSongSortDescendingKey, true)
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val syncUtils = LocalSyncUtils.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else songs.filter { song ->
            song.song.song.title.contains(query.text, ignoreCase = true) ||
                song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    var inSelectMode by remember { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<Int>() }
    var selectionAnchorMapId by remember { mutableStateOf<Int?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorMapId = null
    }

    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }
    else if (inSelectMode) BackHandler(onBack = onExitSelectionMode)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        selection.fastForEachReversed { mapId ->
            if (songs.find { it.map.id == mapId } == null) selection.remove(Integer.valueOf(mapId))
        }
        if (selectionAnchorMapId != null && songs.none { it.map.id == selectionAnchorMapId }) {
            selectionAnchorMapId = songs.firstOrNull { it.map.id in selection }?.map?.id
        }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); addAll(songs) }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all { downloads[it.song.id]?.state == Download.STATE_QUEUED || downloads[it.song.id]?.state == Download.STATE_DOWNLOADING || downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = { Icon(painterResource(R.drawable.edit), null) },
                title = { Text(stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(playlistEntity.name, TextRange(playlistEntity.name.length)),
                onDone = { name ->
                    database.query { update(playlistEntity.copy(name = name, lastUpdateTime = LocalDateTime.now())) }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
            )
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist!!.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) database.transaction { playlist?.id?.let { clearPlaylist(it) } }
                        songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist?.playlist!!.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query { playlist?.let { delete(it.playlist) } }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    val headerItems = if (isSearching || inSelectMode) 1 else 2
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState, scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) (from.index - headerItems) to (to.index - headerItems) else currentDragInfo.first to (to.index - headerItems)
            mutableSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    database.withTransaction { move(viewModel.playlistId, from, to) }
                    val browseId = viewModel.playlist.value?.playlist?.browseId
                    if (browseId != null) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val setVideoId = playlistSongMap.getOrNull(to)?.setVideoId
                        val successorSetVideoId = playlistSongMap.getOrNull(to + 1)?.setVideoId
                        if (setVideoId != null) YouTube.moveSongPlaylist(browseId, setVideoId, successorSetVideoId)
                    }
                }
                dragInfo = null
            }
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || isSearching || inSelectMode } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    // 🔥 FIX: 100% App Theme Background, No extra dark wrapper
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom)
                .union(WindowInsets.ime).asPaddingValues(),
        ) {
            if (isSearching || inSelectMode) {
                item(key = "search_spacer", contentType = "header") {
                    Spacer(modifier = Modifier.height(topPadding))
                }
            }

            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                            modifier = Modifier.animateItem(),
                        )
                    }
                } else {
                    if (!isSearching && !inSelectMode) {
                        item(key = "playlist_header", contentType = "header") {
                            Box(modifier = Modifier.animateItem()) {
                                LocalPlaylistHeader(
                                    playlist = playlist,
                                    songs = songs,
                                    onlinePlaylist = onlinePlaylist,
                                    onShowEditDialog = { showEditDialog = true },
                                    onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                    onshowDeletePlaylistDialog = { showDeletePlaylistDialog = true },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }

                    item(key = "controls_row") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 16.dp, bottom = 8.dp)
                                .animateItem(),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                        PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        PlaylistSongSortType.NAME -> R.string.sort_by_name
                                        PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                        PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (editable) {
                                com.jay.glossy.ui.component.IconButton(
                                    onClick = { locked = !locked },
                                    onLongClick = {},
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val displayedSongs = if (isSearching) filteredSongs else mutableSongs

            itemsIndexed(items = displayedSongs, key = { _, song -> song.map.id }) { index, song ->
                ReorderableItem(state = reorderableState, key = song.map.id) {
                    val currentItem by rememberUpdatedState(song)

                    fun deleteFromPlaylist() {
                        val browseId = playlist?.playlist?.browseId
                        val setVideoId = currentItem.map.setVideoId
                        val songId = currentItem.map.songId
                        val playlistId = currentItem.map.playlistId
                        database.transaction {
                            move(playlistId, currentItem.map.position, Int.MAX_VALUE)
                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                        }
                        if (browseId != null) {
                            syncUtils.scheduleRemoveFromPlaylist(browseId, songId, playlistId) { setVideoId }
                        }
                    }

                    val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = false)
                    val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { totalDistance -> totalDistance })
                    var processedDismiss by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (swipeRemoveEnabled && !processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                            processedDismiss = true
                            deleteFromPlaylist()
                        }
                        if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                    }

                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) selection.add(song.map.id) else selection.remove(Integer.valueOf(song.map.id))
                    }

                    val content: @Composable () -> Unit = {
                        SongListItem(
                            song = song.song,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(checked = selection.contains(song.map.id), onCheckedChange = onCheckedChange)
                                } else {
                                    com.jay.glossy.ui.component.IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(originalSong = song.song, playlistSong = song, playlistBrowseId = playlist?.playlist?.browseId, onDismiss = menuState::dismiss)
                                            }
                                        },
                                        onLongClick = {}
                                    ) {
                                        Icon(painterResource(R.drawable.more_vert), null, tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) {
                                        com.jay.glossy.ui.component.IconButton(
                                            onClick = { }, onLongClick = {}, modifier = Modifier.draggableHandle()
                                        ) {
                                            Icon(painterResource(R.drawable.drag_handle), null, tint = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) onCheckedChange(!selection.contains(song.map.id))
                                        else if (song.song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                        else playerConnection.playQueue(ListQueue(title = playlist!!.playlist.name, items = songs.map { it.song.toMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id }))
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true; onCheckedChange(true); selectionAnchorMapId = song.map.id
                                        } else {
                                            val anchorIndex = selectionAnchorMapId?.let { anchorMapId -> displayedSongs.indexOfFirst { it.map.id == anchorMapId } } ?: -1
                                            if (anchorIndex == -1) {
                                                onCheckedChange(true); selectionAnchorMapId = song.map.id
                                            } else {
                                                val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                for (rangeIndex in range) {
                                                    val rangeMapId = displayedSongs[rangeIndex].map.id
                                                    if (rangeMapId !in selection) selection.add(rangeMapId)
                                                }
                                            }
                                        }
                                    },
                                ),
                        )
                    }

                    if (locked || inSelectMode || !swipeRemoveEnabled) Box(modifier = Modifier.animateItem()) { content() }
                    else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}, modifier = Modifier.animateItem()) { content() }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues())
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = headerItems,
        )

        AnimatedVisibility(
            visible = showTopBarTitle,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showScrolledTopBar) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    if (inSelectMode) {
                        Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                    } else if (isSearching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        )
                    } else {
                        Text(playlist?.playlist?.name.orEmpty())
                    }
                },
                navigationIcon = {
                    if (inSelectMode) {
                        com.jay.glossy.ui.component.IconButton(onClick = onExitSelectionMode, onLongClick = {}) {
                            Icon(painterResource(R.drawable.close), null)
                        }
                    } else {
                        com.jay.glossy.ui.component.IconButton(
                            onClick = {
                                if (isSearching) { isSearching = false; query = TextFieldValue() } else navController.navigateUp()
                            },
                            onLongClick = { if (!isSearching) navController.backToMain() },
                        ) {
                            Icon(painterResource(R.drawable.arrow_back), null)
                        }
                    }
                },
                actions = {
                    if (inSelectMode) {
                        Checkbox(
                            checked = selection.size == songs.size && selection.isNotEmpty(),
                            onCheckedChange = {
                                if (selection.size == songs.size) selection.clear() else { selection.clear(); selection.addAll(songs.map { it.map.id }) }
                            },
                        )
                        com.jay.glossy.ui.component.IconButton(
                            enabled = selection.isNotEmpty(),
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = selection.mapNotNull { mapId -> songs.find { it.map.id == mapId }?.song },
                                        songPosition = selection.mapNotNull { mapId -> songs.find { it.map.id == mapId }?.map },
                                        onDismiss = menuState::dismiss,
                                        clearAction = onExitSelectionMode,
                                    )
                                }
                            },
                            onLongClick = {}
                        ) { Icon(painterResource(R.drawable.more_vert), null) }
                    } else if (!isSearching) {
                        com.jay.glossy.ui.component.IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                            Icon(painterResource(R.drawable.search), null)
                        }
                    }
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter),
        )
    }
}

// 🔥 PURE MATERIAL 3 HEADER (No dark overrides, full shadow buttons)
@Composable
fun LocalPlaylistHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    onlinePlaylist: PlaylistItem?,
    onShowEditDialog: () -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    onshowDeletePlaylistDialog: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    val editPlaylistCoverStr = stringResource(R.string.edit_playlist_cover)
    val playlistSyncedStr = stringResource(R.string.playlist_synced)

    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist.playlist.isEditable

    val overrideThumbnail = remember { mutableStateOf<String?>(null) }
    var isCustomThumbnail: Boolean = playlist.thumbnails.firstOrNull()?.let {
        it.contains("studio_square_thumbnail") || it.contains("content://com.jay.glossy")
    } ?: false

    val result = remember { mutableStateOf<Uri?>(null) }
    var pendingCropDestUri by remember { mutableStateOf<Uri?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            val output = res.data?.let { UCrop.getOutput(it) } ?: pendingCropDestUri
            if (output != null) result.value = output
        }
    }

    val (darkMode, _) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val cropColor = MaterialTheme.colorScheme
    val darkTheme = darkMode == DarkMode.ON || (darkMode == DarkMode.AUTO && isSystemInDarkTheme())

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { sourceUri ->
            val destFile = java.io.File(context.cacheDir, "playlist_cover_crop_${System.currentTimeMillis()}.jpg")
            val destUri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", destFile)
            pendingCropDestUri = destUri

            val options = UCrop.Options().apply {
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                setCompressionQuality(90)
                setHideBottomControls(true)
                setToolbarTitle(editPlaylistCoverStr)
                setStatusBarLight(!darkTheme)
                setToolbarColor(cropColor.surface.toArgb())
                setToolbarWidgetColor(cropColor.inverseSurface.toArgb())
                setRootViewBackgroundColor(cropColor.surface.toArgb())
                setLogoColor(cropColor.surface.toArgb())
            }

            val intent = UCrop
                .of(sourceUri, destUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .getIntent(context)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(intent)
        }
    }

    LaunchedEffect(result.value) {
        val uri = result.value ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            when {
                playlist.playlist.browseId == null -> {
                    overrideThumbnail.value = uri.toString()
                    isCustomThumbnail = true
                    database.query { update(playlist.playlist.copy(thumbnailUrl = uri.toString())) }
                }
                else -> {
                    val bytes = uriToByteArray(context, uri)
                    YouTube.uploadCustomThumbnailLink(playlist.playlist.browseId, bytes!!).onSuccess { newThumbnailUrl ->
                        overrideThumbnail.value = newThumbnailUrl
                        isCustomThumbnail = true
                        database.query { update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl)) }
                    }.onFailure {
                        if (it is ClientRequestException) {
                            snackbarHostState.showSnackbar("${it.response.status.value} ${it.response.status.description}")
                        }
                        reportException(it)
                    }
                }
            }
        }
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all {
                    downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                }
            ) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showEditNoteDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.edit_playlist_cover),
                onDismiss = { showEditNoteDialog = false },
                onConfirm = {
                    showEditNoteDialog = false
                    pickLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onCancel = { showEditNoteDialog = false },
            ) {
                if (playlist.playlist.browseId != null) {
                    Text(text = stringResource(R.string.edit_playlist_cover_note), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                Text(text = stringResource(R.string.edit_playlist_cover_note_wait), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }

        Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp))

        // Center Square Image (M3 Style with shadows)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (playlist.thumbnails.isEmpty() && overrideThumbnail.value == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.queue_music), contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (playlist.thumbnails.size <= 1 || overrideThumbnail.value != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data((overrideThumbnail.value ?: playlist.thumbnails.firstOrNull())?.resize(1080, 1080))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index)?.resize(1080, 1080),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.align(alignment).fillMaxSize(0.5f),
                        )
                    }
                }
            }

            if (editable) {
                OverlayEditButton(
                    visible = true,
                    alignment = Alignment.BottomEnd,
                    onClick = {
                        if (isCustomThumbnail) {
                            menuState.show(
                                {
                                    CustomThumbnailMenu(
                                        onEdit = { pickLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                        onRemove = {
                                            when {
                                                playlist.playlist.browseId == null -> {
                                                    overrideThumbnail.value = null
                                                    database.query { update(playlist.playlist.copy(thumbnailUrl = null)) }
                                                }
                                                else -> {
                                                    scope.launch(Dispatchers.IO) {
                                                        YouTube.removeThumbnailPlaylist(playlist.playlist.browseId).onSuccess { newThumbnailUrl ->
                                                            overrideThumbnail.value = newThumbnailUrl
                                                            database.query { update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl)) }
                                                        }
                                                    }
                                                }
                                            }
                                            isCustomThumbnail = false
                                        },
                                        onDismiss = menuState::dismiss,
                                    )
                                },
                            )
                        } else showEditNoteDialog = true
                    },
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), 
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val onlineAuthor = onlinePlaylist?.author
        if (onlineAuthor != null) {
            Text(
                text = onlineAuthor.name,
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onlineAuthor.id != null) {
                        onlineAuthor.id?.let { authorId -> navController.navigate("artist/$authorId") }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Playlist • 2026",
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), 
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp)) 

        // PURE MATERIAL 3 BUTTONS ROW (With strong shadows and M3 Colors)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.shuffled().map { it.song.toMediaItem() }))
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(24.dp))
                }
            }

            Surface(
                onClick = {
                    playerConnection.playQueue(ListQueue(title = playlist.playlist.name, items = songs.map { it.song.toMediaItem() }))
                },
                shape = CircleShape, // Pill
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.height(56.dp).weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = {
                    menuState.show {
                        LocalPlaylistMenu(
                            playlist = playlist,
                            songs = songs,
                            context = context,
                            downloadState = downloadState,
                            onEdit = onShowEditDialog,
                            onSync = {
                                scope.launch(Dispatchers.IO) {
                                    val playlistPage = YouTube.playlist(playlist.playlist.browseId!!).completed().getOrNull() ?: return@launch
                                    database.transaction {
                                        clearPlaylist(playlist.id)
                                        val songIds = playlistPage.songs.map(SongItem::toMediaMetadata).onEach(::insert).map { it.id to it.setVideoId }
                                        addSongsToPlaylist(playlist, songIds)
                                    }
                                    withContext(Dispatchers.Main) { snackbarHostState.showSnackbar(playlistSyncedStr) }
                                }
                            },
                            onDelete = onshowDeletePlaylistDialog,
                            onDownload = {
                                when (downloadState) {
                                    Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                                    Download.STATE_DOWNLOADING -> {
                                        songs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false) }
                                    }
                                    else -> {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri()).setCustomCacheKey(song.song.id).setData(song.song.song.title.toByteArray()).build()
                                            DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                        }
                                    }
                                }
                            },
                            onQueue = { playerConnection.addToQueue(items = songs.map { it.song.toMediaItem() }) },
                            onDismiss = { menuState.dismiss() },
                        )
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.more_vert), null, modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp)) 

        val description = onlinePlaylist?.description
        if (!description.isNullOrBlank()) {
            ExpandableText(text = description, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), collapsedMaxLines = 3)
            Spacer(Modifier.height(16.dp))
        }

        val songCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) playlist.playlist.remoteSongCount else playlist.songCount
        val nSongs = pluralStringResource(R.plurals.n_song, songCount, songCount)
        val durationText = if (playlistLength > 0) makeTimeString(playlistLength * 1000L) else ""

        Text(
            text = if (durationText.isNotEmpty()) "$nSongs • $durationText" else nSongs,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun uriToByteArray(context: Context, uri: Uri): ByteArray? = try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (_: SecurityException) { null }
