/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.playlist

import com.jay.glossy.R

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.semantics.Role
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
import timber.log.Timber
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.toBitmap
import com.metrolist.innertube.YouTube

import com.kmpalette.palette.graphics.Palette
import com.kmpalette.rememberPaletteState

import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.HideExplicitKey
import com.jay.glossy.constants.SongSortDescendingKey
import com.jay.glossy.constants.SongSortType
import com.jay.glossy.constants.SongSortTypeKey
import com.jay.glossy.constants.YtmSyncKey
import com.jay.glossy.db.entities.Song
import com.jay.glossy.extensions.toMediaItem
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.playback.queues.ListQueue
import com.jay.glossy.ui.component.DefaultDialog
import com.jay.glossy.ui.component.DraggableScrollbar
import com.jay.glossy.ui.component.EmptyPlaceholder
import com.jay.glossy.ui.component.HideOnScrollFAB
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.SongListItem
import com.jay.glossy.ui.component.SortHeader
import com.jay.glossy.ui.menu.AutoPlaylistMenu
import com.jay.glossy.ui.menu.SelectionSongMenu
import com.jay.glossy.ui.menu.SongMenu
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.ui.utils.isScrollingUp
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.ui.utils.toImmersiveBackground
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.AutoPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.jay.glossy.ui.component.LiquidGlassIconButton
import com.jay.glossy.ui.component.layerBackdrop
import com.jay.glossy.ui.component.liquidGlass
import com.jay.glossy.ui.component.rememberBackdrop

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val uploadUnsupportedFormatStr = stringResource(R.string.upload_unsupported_format)
    val uploadFileTooLargeStr = stringResource(R.string.upload_file_too_large)
    val uploadFailedStr = stringResource(R.string.upload_failed)
    val uploadCompleteStr = stringResource(R.string.upload_complete)
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val playlist =
        when (viewModel.playlist) {
            "liked" -> stringResource(R.string.liked)
            "uploaded" -> stringResource(R.string.uploaded_playlist)
            else -> stringResource(R.string.offline)
        }

    val hazeState = remember { HazeState() }
    val defaultColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(defaultColor) }

    val animatedExtractedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "solidColor"
    )

    //  FIX: Added Missing snackbarHostState variable
    val snackbarHostState = remember { SnackbarHostState() }

    val songs by viewModel.likedSongs.collectAsStateWithLifecycle(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    val playlistId = viewModel.playlist
    val playlistType = when (playlistId) {
        "liked" -> PlaylistType.LIKE
        "downloaded" -> PlaylistType.DOWNLOAD
        "uploaded" -> PlaylistType.UPLOADED
        else -> PlaylistType.OTHER
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val scope = rememberCoroutineScope()

    // Upload state
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var currentUploadIndex by remember { mutableIntStateOf(0) }
    var totalUploads by remember { mutableIntStateOf(0) }
    var currentFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    Timber.w(e, "Could not take persistable permission")
                }
            }
            uploadJob = scope.launch {
                isUploading = true
                showUploadDialog = true
                totalUploads = uris.size
                var successCount = 0

                uris.forEachIndexed { index, uri ->
                    currentUploadIndex = index + 1
                    uploadProgress = 0f

                    try {
                        var fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "unknown"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (displayNameIndex >= 0) {
                                    val name = cursor.getString(displayNameIndex)
                                    if (!name.isNullOrBlank()) {
                                        fileName = name
                                    }
                                }
                            }
                        }
                        currentFileName = fileName
                        val extension = fileName.substringAfterLast('.', "").lowercase()

                        if (extension !in YouTube.SUPPORTED_UPLOAD_TYPES) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, uploadUnsupportedFormatStr, Toast.LENGTH_SHORT).show()
                            }
                            return@forEachIndexed
                        }

                        val inputStream = context.contentResolver.openInputStream(uri)
                        val data = inputStream?.readBytes()
                        inputStream?.close()

                        if (data == null) return@forEachIndexed

                        if (data.size > YouTube.MAX_UPLOAD_SIZE) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, uploadFileTooLargeStr, Toast.LENGTH_SHORT).show()
                            }
                            return@forEachIndexed
                        }

                        val result = YouTube.uploadSong(
                            filename = fileName,
                            data = data,
                            onProgress = { progress -> uploadProgress = progress },
                        )

                        if (result.isSuccess && result.getOrDefault(false)) {
                            successCount++
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, uploadFailedStr + ": ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                isUploading = false

                if (successCount > 0) {
                    uploadProgress = 1f
                    currentFileName = uploadCompleteStr
                    kotlinx.coroutines.delay(1000)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, uploadCompleteStr, Toast.LENGTH_SHORT).show()
                    }

                    showUploadDialog = false
                    viewModel.syncUploadedSongs()
                } else {
                    showUploadDialog = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                if (playlistType == PlaylistType.LIKE) {
                    viewModel.syncLikedSongs()
                }
                if (playlistType == PlaylistType.UPLOADED) {
                    viewModel.syncUploadedSongs()
                }
            }
        }
    }

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
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context, ExoDownloadService::class.java, song.song.id, false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (showUploadDialog) {
        DefaultDialog(
            onDismiss = {
                if (isUploading) {
                    uploadJob?.cancel()
                    isUploading = false
                }
                showUploadDialog = false
            },
            icon = { Icon(painter = painterResource(R.drawable.upload), contentDescription = null) },
            title = { Text(stringResource(R.string.uploading)) },
            buttons = {
                TextButton(
                    onClick = {
                        if (isUploading) {
                            uploadJob?.cancel()
                            isUploading = false
                        }
                        showUploadDialog = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            Text(
                text = stringResource(R.string.upload_progress, currentUploadIndex, totalUploads),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentFileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
        }
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs ?: emptyList()
        else songs?.filter { song ->
            song.song.title.contains(query.text, true) ||
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

    val state = rememberLazyListState()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()
    val canRefresh = playlistType == PlaylistType.LIKE || playlistType == PlaylistType.UPLOADED
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedExtractedColor)
                .then(
                    if (canRefresh) {
                        Modifier.pullToRefresh(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = viewModel::refresh,
                        )
                    } else Modifier
                ),
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
                                    AutoPlaylistHeader(
                                        name = playlist,
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
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            SongSortType.NAME -> R.string.sort_by_name
                                            SongSortType.ARTIST -> R.string.sort_by_artist
                                            SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
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
                                isActive = song.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    if (inSelectMode) {
                                        Checkbox(
                                            checked = song.id in selection,
                                            onCheckedChange = onCheckedChange,
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
                                            Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
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
                                                        title = playlist,
                                                        items = songs!!.map { it.toMediaItem() },
                                                        startIndex = songs!!.indexOfFirst { it.id == song.id },
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
                                    ).animateItem(),
                            )
                        }
                    }
                }
            }

            DraggableScrollbar(
                modifier = Modifier
                    .padding(
                        LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
                    ).align(Alignment.CenterEnd),
                scrollState = state,
                headerItems = if (isSearching || inSelectMode) 1 else 2,
            )

            if (canRefresh) {
                Indicator(
                    isRefreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
                )
            }

            if (playlistType == PlaylistType.UPLOADED) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isScrollingUp(),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        ).padding(16.dp),
                ) {
                    FloatingActionButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "audio/mpeg", "audio/mp4", "audio/x-m4a",
                                    "audio/flac", "audio/ogg", "audio/x-ms-wma",
                                ),
                            )
                        },
                    ) {
                        Icon(painterResource(R.drawable.upload), stringResource(R.string.upload_songs))
                    }
                }
            }

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
                                        .focusRequester(focusRequester),
                                )
                            }
                            else -> {
                                Text(text = playlist, color = Color.White)
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
                                    inSelectMode -> onExitSelectionMode()
                                    else -> navController.navigateUp()
                                }
                            },
                            onLongClick = {
                                if (!isSearching && !inSelectMode) navController.backToMain()
                            },
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
                                },
                            )
                            com.jay.glossy.ui.component.IconButton(
                                enabled = selection.isNotEmpty(),
                                onClick = {
                                    menuState.show {
                                        SelectionSongMenu(
                                            songSelection = filteredSongs.filter { it.id in selection },
                                            onDismiss = menuState::dismiss,
                                            clearAction = onExitSelectionMode,
                                            isUploadedPlaylist = playlistType == PlaylistType.UPLOADED,
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
                    },
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AutoPlaylistHeader(
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
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val artworkBackdrop = rememberBackdrop()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val paletteState = rememberPaletteState()
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    val thumbUrl = songs.firstOrNull()?.song?.thumbnailUrl

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
        horizontalAlignment = Alignment.CenterHorizontally,
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
                                    AutoPlaylistMenu(
                                        downloadState = downloadState,
                                        onQueue = {
                                            playerConnection.addToQueue(
                                                songs.map { it.toMediaItem() },
                                            )
                                        },
                                        onDownload = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    onShowRemoveDownloadDialog()
                                                }
                                                Download.STATE_DOWNLOADING -> {
                                                    songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.song.id,
                                                            false,
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    songs.forEach { song ->
                                                        val downloadRequest =
                                                            DownloadRequest
                                                                .Builder(song.song.id, song.song.id.toUri())
                                                                .setCustomCacheKey(song.song.id)
                                                                .setData(song.song.title.toByteArray())
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
                                        onDismiss = { menuState.dismiss() },
                                        songs = songs,
                                        playlistName = name,
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

enum class PlaylistType {
    LIKE,
    DOWNLOAD,
    UPLOADED,
    OTHER,
}
