package com.jay.glossy.spotify

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.media3.exoplayer.offline.Download
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.launch
import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.extensions.togglePlayPause
import com.metrolist.models.MediaMetadata
import com.jay.glossy.spotifycore.SpotifyMapper
import com.jay.glossy.spotify.SpotifyDownloadItem
import com.jay.glossy.spotify.SpotifyPlaybackResolver
import com.jay.glossy.spotify.SpotifyPlaylistEvent
import com.jay.glossy.spotify.SpotifyPlaylistQueue
import com.jay.glossy.spotify.SpotifyPlaylistViewModel
import com.jay.glossy.spotifycore.models.SpotifyTrack
import com.jay.glossy.ui.component.DraggableScrollbar
import com.jay.glossy.ui.component.ExpressiveEmptyPlaceholder
import com.jay.glossy.ui.component.ExpressivePullToRefreshBox
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.SpotifyTrackListItem
import com.jay.glossy.ui.utils.HeaderDownloadItem
import com.jay.glossy.ui.utils.HeaderDownloadState
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.ui.utils.headerDownloadState
import com.jay.glossy.ui.utils.sendAddMissingDownloads
import com.jay.glossy.ui.utils.sendRemoveDownloads
import com.jay.glossy.utils.makeTimeString
import kotlin.math.abs

@Composable
fun HeaderDownloadProgressIndicator(
    progress: Float,
    paused: Boolean,
    icon: Int,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SpotifyPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SpotifyPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadUtil = LocalDownloadUtil.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val downloads by downloadUtil.downloads.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val isPlaying by playerConnection?.isPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<MediaMetadata?>(null) }
    val playlist = state.playlist
    val tracks = state.tracks
    val lazyListState = rememberLazyListState()
    val systemBarsTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadActionFailedMessage = "Download failed"
    val latestDownloads by rememberUpdatedState(downloads)

    val downloadState = remember(state.downloadItems, downloads) {
        headerDownloadState(songIds = state.downloadItems.map(SpotifyDownloadItem::id), downloads = downloads)
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    fun handleDownloadAction(items: ImmutableList<SpotifyDownloadItem>, removeCompleted: Boolean = true) {
        val songIds = items.map(SpotifyDownloadItem::id)
        when (headerDownloadState(songIds, latestDownloads)) {
            HeaderDownloadState.Completed -> {
                if (removeCompleted) sendRemoveDownloads(context = navController.context, songIds = songIds)
            }
            is HeaderDownloadState.Partial -> {
                sendRemoveDownloads(context = navController.context, songIds = songIds, downloads = latestDownloads)
            }
            HeaderDownloadState.None -> {
                sendAddMissingDownloads(
                    context = navController.context,
                    songs = items.map { item -> HeaderDownloadItem(id = item.id, title = item.title) },
                    downloads = latestDownloads,
                )
                navController.navigate("auto_playlist/downloaded?tab=progress")
            }
        }
    }

    val showTopBarTitle by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || inSelectMode } }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var resolvingTrackId by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    val filteredTracks = remember(tracks, query.text) {
        if (query.text.isBlank()) tracks else tracks.filter { track ->
            track.name.contains(query.text, ignoreCase = true) ||
                track.artists.any { artist -> artist.name.contains(query.text, ignoreCase = true) }
        }
    }

    val loadedDurationMs = remember(tracks) { tracks.sumOf { track -> track.durationMs.toLong() } }

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SpotifyPlaylistEvent.DownloadResolutionFailed -> snackbarHostState.showSnackbar(downloadActionFailedMessage)
                is SpotifyPlaylistEvent.DownloadsResolved -> handleDownloadAction(items = event.items, removeCompleted = false)
            }
        }
    }

    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    fun playPlaylist(startIndex: Int = 0, shuffled: Boolean = false) {
        val currentPlaylist = playlist ?: return
        val queueTracks = if (shuffled) tracks.shuffled() else tracks
        if (queueTracks.isEmpty()) return
        val boundedStartIndex = startIndex.coerceIn(queueTracks.indices)
        val preloadTrack = queueTracks[boundedStartIndex]
        if (resolvingTrackId != null) return

        coroutineScope.launch {
            resolvingTrackId = preloadTrack.id
            try {
                val preloadItem = SpotifyPlaybackResolver.resolveToMetadata(preloadTrack)
                playerConnection?.playQueue(
                    SpotifyPlaylistQueue(
                        playlistId = currentPlaylist.id,
                        title = currentPlaylist.name,
                        initialTracks = queueTracks,
                        startIndex = boundedStartIndex,
                        preloadItem = preloadItem,
                    ),
                )
            } finally {
                resolvingTrackId = null
            }
        }
    }

    ExpressivePullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = viewModel::reload,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isSearching || inSelectMode) {
                item(key = "search_spacer", contentType = "header") { Spacer(modifier = Modifier.height(systemBarsTopPadding)) }
            }

            if (!isSearching && !inSelectMode) {
                playlist?.let { currentPlaylist ->
                    item(key = "header") {
                        val trackCount = currentPlaylist.tracks?.total ?: tracks.size
                        SpotifyPlaylistHeader(
                            name = currentPlaylist.name,
                            author = currentPlaylist.owner?.displayName,
                            thumbnailUrl = SpotifyMapper.getPlaylistThumbnail(currentPlaylist),
                            trackCount = trackCount,
                            loadedDurationMs = loadedDurationMs,
                            downloadState = downloadState,
                            onSearchClick = { isSearching = true },
                            onPlay = { playPlaylist() },
                            onShuffle = { playPlaylist(shuffled = true) },
                            onDownload = {
                                if (state.downloadItems.isEmpty()) viewModel.resolveDownloads() else handleDownloadAction(state.downloadItems)
                            },
                            navController = navController,
                            menuState = menuState,
                            onReload = viewModel::reload
                        )
                    }
                }
            }

            if (state.isLoading && tracks.isEmpty()) {
                item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { error ->
                item(key = "error") {
                    Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
            }

            if (!state.isLoading && state.errorMessage == null && filteredTracks.isEmpty()) {
                item(key = "empty") {
                    ExpressiveEmptyPlaceholder(icon = R.drawable.music_note, text = if (query.text.isBlank()) "No tracks found in this playlist" else "No results")
                }
            }

            itemsIndexed(items = filteredTracks, key = { index, track -> "spotify_track_${track.id}_$index" }) { index, track ->
                val trackIsActive = remember(track, mediaMetadata) { track.isResolvedAs(mediaMetadata) }
                val trackIsResolving = resolvingTrackId == track.id
                
                val onCheckedChange: (Boolean) -> Unit = { if (it) selection.add(track.id) else selection.remove(track.id) }

                SpotifyTrackListItem(
                    track = track,
                    isActive = trackIsActive || trackIsResolving,
                    isPlaying = isPlaying && !trackIsResolving,
                    trailingContent = {
                        if (trackIsResolving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else if (inSelectMode) {
                            Checkbox(checked = track.id in selection, onCheckedChange = onCheckedChange)
                        } else {
                            IconButton(
                                onClick = {}, 
                                onLongClick = {}, 
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        enabled = resolvingTrackId == null || trackIsActive || inSelectMode,
                        onClick = {
                            if (inSelectMode) onCheckedChange(track.id !in selection)
                            else if (trackIsActive) playerConnection?.player?.togglePlayPause()
                            else {
                                val startIndex = tracks.indexOfFirst { item -> item.id == track.id }.takeIf { it >= 0 } ?: index
                                playPlaylist(startIndex = startIndex)
                            }
                        },
                        onLongClick = {
                            if (!inSelectMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                inSelectMode = true; onCheckedChange(true); selectionAnchorSongId = track.id
                            } else {
                                val anchorIndex = selectionAnchorSongId?.let { anchor -> filteredTracks.indexOfFirst { it.id == anchor } } ?: -1
                                if (anchorIndex == -1) { onCheckedChange(true); selectionAnchorSongId = track.id }
                                else {
                                    val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                    for (rangeIndex in range) {
                                        val rangeSongId = filteredTracks[rangeIndex].id
                                        if (rangeSongId !in selection) selection.add(rangeSongId)
                                    }
                                }
                            }
                        }
                    ),
                )
            }
        }

        DraggableScrollbar(
            modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = if (!isSearching && playlist != null) 1 else 0,
        )

        AnimatedVisibility(
            visible = showTopBarTitle,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showTopBarTitle) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    if (inSelectMode) {
                        Text(text = pluralStringResource(R.plurals.n_song, selection.size, selection.size), style = MaterialTheme.typography.titleLarge)
                    } else if (isSearching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(text = stringResource(R.string.search), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f)) },
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
                        Text(text = playlist?.name ?: "Spotify Playlists", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                isSearching -> { isSearching = false; query = TextFieldValue() }
                                inSelectMode -> onExitSelectionMode()
                                else -> navController.navigateUp()
                            }
                        },
                        onLongClick = { if (!isSearching && !inSelectMode) navController.backToMain() },
                    ) {
                        Icon(painter = painterResource(if (inSelectMode) R.drawable.close else R.drawable.arrow_back), contentDescription = null)
                    }
                },
                actions = {
                    if (inSelectMode) {
                        Checkbox(
                            checked = selection.size == filteredTracks.size && selection.isNotEmpty(),
                            onCheckedChange = {
                                if (selection.size == filteredTracks.size) selection.clear() else { selection.clear(); selection.addAll(filteredTracks.map { it.id }) }
                            }
                        )
                    } else if (!isSearching) {
                        IconButton(onClick = { isSearching = true }, onLongClick = {}) {
                            Icon(painterResource(R.drawable.search), contentDescription = null)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SpotifyPlaylistHeader(
    name: String,
    author: String?,
    thumbnailUrl: String?,
    trackCount: Int,
    loadedDurationMs: Long,
    downloadState: HeaderDownloadState,
    onSearchClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onReload: () -> Unit,
    navController: NavController,
    menuState: com.jay.glossy.ui.component.MenuState,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(screenHeight / 2)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(thumbnailUrl).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            1.0f to MaterialTheme.colorScheme.background
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
                    text = name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), 
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = author ?: "Spotify",
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Spotify Playlist",
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), 
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { navController.navigateUp() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSearchClick, onLongClick = {}) {
                            Icon(painterResource(R.drawable.search), null)
                        }
                        IconButton(onClick = {
                            menuState.show {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.padding(16.dp).width(200.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { menuState.dismiss(); onReload() }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(painterResource(R.drawable.sync), null)
                                            Spacer(Modifier.width(16.dp))
                                            Text("Reload playlist", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { menuState.dismiss(); onDownload() }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(painterResource(R.drawable.download), null)
                                            Spacer(Modifier.width(16.dp))
                                            Text("Download All", style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }, onLongClick = {}) {
                            Icon(painterResource(R.drawable.more_vert), null)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onShuffle,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 6.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(20.dp))
                }
            }

            Surface(
                onClick = onPlay,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier.height(48.dp).weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = onDownload,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 6.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (downloadState) {
                        HeaderDownloadState.Completed -> Icon(painterResource(R.drawable.offline), null, modifier = Modifier.size(20.dp))
                        is HeaderDownloadState.Partial -> HeaderDownloadProgressIndicator(progress = downloadState.progress, paused = downloadState.paused, icon = R.drawable.download)
                        HeaderDownloadState.None -> Icon(painterResource(R.drawable.download), null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        val durationText = if (loadedDurationMs > 0) makeTimeString(loadedDurationMs) else ""
        val trackCountText = pluralStringResource(R.plurals.n_song, trackCount, trackCount)

        Text(
            text = if (durationText.isNotEmpty()) "$trackCountText • $durationText" else trackCountText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Start
        )
    }
}

private fun SpotifyTrack.isResolvedAs(mediaMetadata: MediaMetadata?): Boolean {
    if (mediaMetadata == null) return false
    val titleMatches = name.equals(mediaMetadata.title, ignoreCase = true)
    val durationMatches = durationMs <= 0 || mediaMetadata.duration <= 0 || abs(durationMs.toLong() - mediaMetadata.duration * 1000L) <= 1_000L
    val albumMatches = album?.let { spotifyAlbum ->
        val currentAlbum = mediaMetadata.album ?: return false
        spotifyAlbum.id.isNotBlank() && spotifyAlbum.id == currentAlbum.id || spotifyAlbum.name.equals(currentAlbum.title, ignoreCase = true)
    } ?: true
    val artistMatches = artists.isEmpty() || mediaMetadata.artists.isEmpty() || artists.any { spotifyArtist ->
        mediaMetadata.artists.any { artist -> spotifyArtist.name.equals(artist.name, ignoreCase = true) }
    }
    val thumbnailMatches = SpotifyMapper.getTrackThumbnail(this)?.let { thumbnail -> thumbnail == mediaMetadata.thumbnailUrl } ?: true
    return titleMatches && durationMatches && albumMatches && artistMatches && thumbnailMatches
}
