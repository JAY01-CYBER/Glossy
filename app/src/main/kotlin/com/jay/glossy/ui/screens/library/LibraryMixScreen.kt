/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.library

import com.jay.glossy.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.AlbumViewTypeKey
import com.jay.glossy.constants.CONTENT_TYPE_HEADER
import com.jay.glossy.constants.CONTENT_TYPE_PLAYLIST
import com.jay.glossy.constants.GridItemSize
import com.jay.glossy.constants.GridItemsSizeKey
import com.jay.glossy.constants.GridThumbnailHeight
import com.jay.glossy.constants.LibraryViewType
import com.jay.glossy.constants.MixSortDescendingKey
import com.jay.glossy.constants.MixSortType
import com.jay.glossy.constants.MixSortTypeKey
import com.jay.glossy.constants.ShowCachedPlaylistKey
import com.jay.glossy.constants.ShowDownloadedPlaylistKey
import com.jay.glossy.constants.ShowLikedPlaylistKey
import com.jay.glossy.constants.ShowTopPlaylistKey
import com.jay.glossy.constants.ShowUploadedPlaylistKey
import com.jay.glossy.constants.YtmSyncKey
import com.jay.glossy.db.entities.Album
import com.jay.glossy.db.entities.Artist
import com.jay.glossy.db.entities.Playlist
import com.jay.glossy.db.entities.PlaylistEntity
import com.jay.glossy.db.entities.Song
import com.jay.glossy.extensions.matchesNormalizedQuery
import com.jay.glossy.extensions.normalizeForSearch
import com.jay.glossy.extensions.reversed
import com.jay.glossy.extensions.toMediaItem
import com.jay.glossy.playback.queues.ListQueue
import com.jay.glossy.ui.component.AlbumGridItem
import com.jay.glossy.ui.component.AlbumListItem
import com.jay.glossy.ui.component.ArtistGridItem
import com.jay.glossy.ui.component.ArtistListItem
import com.jay.glossy.ui.component.CreatePlaylistDialog
import com.jay.glossy.ui.component.LibrarySearchEmptyPlaceholder
import com.jay.glossy.ui.component.LibrarySearchHeader
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.PlayStoreRefreshIndicator
import com.jay.glossy.ui.component.PlaylistGridItem
import com.jay.glossy.ui.component.PlaylistListItem
import com.jay.glossy.ui.component.SongGridItem
import com.jay.glossy.ui.component.SongListItem
import com.jay.glossy.ui.component.SortHeader
import com.jay.glossy.ui.menu.AlbumMenu
import com.jay.glossy.ui.menu.ArtistMenu
import com.jay.glossy.ui.menu.PlaylistMenu
import com.jay.glossy.ui.menu.SongMenu
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val queueSearchedSongsStr = stringResource(R.string.queue_searched_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            MixSortTypeKey,
            MixSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsStateWithLifecycle()
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }
    
    val normalizedQuery = remember(isSearchActive, searchQuery, debouncedSearchQuery) {
        if (isSearchActive) {
            searchQuery.normalizeForSearch()
        } else {
            debouncedSearchQuery.normalizeForSearch()
        }
    }

    val topSize by viewModel.topValue.collectAsStateWithLifecycle(initialValue = 50)
    val likedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.liked),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.offline),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.my_top) + " $topSize",
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.cached_playlist),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val uploadedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.uploaded_playlist),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, true)
    
    val showLikedPlaylist = showLiked && matchesNormalizedQuery(normalizedQuery, likedPlaylist.playlist.name)
    val showDownloadedPlaylist =
        showDownloaded && matchesNormalizedQuery(normalizedQuery, downloadPlaylist.playlist.name)
    val showTopPlaylists = showTop && matchesNormalizedQuery(normalizedQuery, topPlaylist.playlist.name)
    val showUploadedPlaylists =
        showUploaded && matchesNormalizedQuery(normalizedQuery, uploadedPlaylist.playlist.name)
    val showCachedPlaylists = showCached && matchesNormalizedQuery(normalizedQuery, cachedPlaylist.playlist.name)

    val albums = viewModel.albums.collectAsStateWithLifecycle()
    val artist = viewModel.artists.collectAsStateWithLifecycle()
    val songs = viewModel.songs.collectAsStateWithLifecycle()
    val playlist = viewModel.playlists.collectAsStateWithLifecycle()

    var allItems = albums.value + artist.value + playlist.value
    val locale = LocalLocale.current.platformLocale
    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE -> {
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }
            }

            MixSortType.NAME -> {
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )
            }

            MixSortType.LAST_UPDATED -> {
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
            }
        }.reversed(sortDescending)

    val searchableItems = if (normalizedQuery.isBlank()) allItems else allItems + songs.value

    val filteredItems = remember(searchableItems, normalizedQuery, collator) {
        val matchedItems =
            searchableItems.filter { item ->
                when (item) {
                    is Song -> {
                        val artistNames = item.orderedArtists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.song.title, item.song.albumName, *artistNames)
                    }

                    is Album -> {
                        val artistNames = item.artists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.album.title, *artistNames)
                    }

                    is Artist -> matchesNormalizedQuery(normalizedQuery, item.artist.name)
                    is Playlist -> matchesNormalizedQuery(normalizedQuery, item.playlist.name)
                    else -> true
                }
            }

        if (normalizedQuery.isBlank()) {
            matchedItems.distinctBy { it.id }
        } else {
            matchedItems
                .sortedWith { first, second ->
                    val firstPriority =
                        when (first) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                            else -> 4
                        }
                    val secondPriority =
                        when (second) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                            else -> 4
                        }

                    if (firstPriority != secondPriority) {
                        firstPriority.compareTo(secondPriority)
                    } else {
                        val firstName =
                            when (first) {
                                is Playlist -> first.playlist.name
                                is Song -> first.song.title
                                is Artist -> first.artist.name
                                is Album -> first.album.title
                                else -> ""
                            }
                        val secondName =
                            when (second) {
                                is Playlist -> second.playlist.name
                                is Song -> second.song.title
                                is Artist -> second.artist.name
                                is Album -> second.album.title
                                else -> ""
                            }
                        collator.compare(firstName, secondName)
                    }
                }
                .distinctBy { it.id }
        }
    }

    val groupedPlaylists = remember(filteredItems) { filteredItems.filterIsInstance<Playlist>() }
    val groupedAlbums = remember(filteredItems) { filteredItems.filterIsInstance<Album>() }
    val groupedArtists = remember(filteredItems) { filteredItems.filterIsInstance<Artist>() }
    val groupedSongs = remember(filteredItems) { filteredItems.filterIsInstance<Song>() }

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.syncAllLibrary()
            }
        }
    }

    val inactivePillGradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        )
    )

    val headerContent = @Composable {
        LibrarySearchHeader(
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onBack = {
                isSearchActive = false
                viewModel.updateSearchQuery("")
            },
            keyboardController = keyboardController,
            // Yahan top = 12.dp aur bottom = 12.dp padding add ki gayi hai spacing theek karne ke liye
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            // Pill shape for SortHeader
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(inactivePillGradient)
            ) {
                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = { sortType ->
                        when (sortType) {
                            MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                            MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                            MixSortType.NAME -> R.string.sort_by_name
                        }
                    },
                )
            }

            Spacer(Modifier.weight(1f))

            // Pill shape for Search and Grid
            Row(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(inactivePillGradient),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isSearchActive = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                    )
                }

                IconButton(
                    onClick = {
                        viewType = viewType.toggle()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter =
                        painterResource(
                            when (viewType) {
                                LibraryViewType.LIST -> R.drawable.list
                                LibraryViewType.GRID -> R.drawable.grid_view
                            },
                        ),
                        contentDescription = stringResource(
                            when (viewType) {
                                LibraryViewType.LIST -> R.string.switch_to_grid_view
                                LibraryViewType.GRID -> R.string.switch_to_list_view
                            },
                        ),
                    )
                }
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    val activeAutoPlaylists = buildList {
                        if (showLikedPlaylist) add(likedPlaylist to "auto_playlist/liked")
                        if (showDownloadedPlaylist) add(downloadPlaylist to "auto_playlist/downloaded")
                        if (showCachedPlaylists) add(cachedPlaylist to "cache_playlist/cached")
                        if (showTopPlaylists) add(topPlaylist to "top_playlist/$topSize")
                        if (showUploadedPlaylists) add(uploadedPlaylist to "auto_playlist/uploaded")
                    }

                    activeAutoPlaylists.chunked(2).forEach { rowItems ->
                        item(
                            key = "auto_row_${rowItems.first().first.playlist.name}",
                            contentType = CONTENT_TYPE_PLAYLIST
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .animateItem()
                            ) {
                                rowItems.forEach { (playlist, route) ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        AutoPlaylistGridItem(
                                            playlist = playlist,
                                            onClick = { navController.navigate(route) }
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f).padding(8.dp))
                                }
                            }
                        }
                    }

                    if (groupedPlaylists.isNotEmpty()) {
                        item(key = "header_playlists", contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Playlists")
                        }
                        items(
                            items = groupedPlaylists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            PlaylistListItem(
                                playlist = item,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                PlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (!item.playlist.isEditable && item.songCount == 0 && item.playlist.browseId != null) {
                                                navController.navigate("online_playlist/${item.playlist.browseId}")
                                            } else {
                                                navController.navigate("local_playlist/${item.id}")
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (groupedAlbums.isNotEmpty()) {
                        item(key = "header_albums", contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Albums")
                        }
                        items(
                            items = groupedAlbums,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }, 
                        ) { item ->
                            AlbumListItem(
                                album = item,
                                isActive = item.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = { menuState.show { AlbumMenu(originalAlbum = item, onDismiss = menuState::dismiss) } }
                                    ) {
                                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { AlbumMenu(originalAlbum = item, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (groupedArtists.isNotEmpty()) {
                        item(key = "header_artists", contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Artists")
                        }
                        items(
                            items = groupedArtists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            ArtistListItem(
                                artist = item,
                                trailingContent = {
                                    IconButton(
                                        onClick = { menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }
                                    ) {
                                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { navController.navigate("artist/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (groupedSongs.isNotEmpty()) {
                        item(key = "header_songs", contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Songs")
                        }
                        items(
                            items = groupedSongs,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            SongListItem(
                                song = item,
                                showInLibraryIcon = true,
                                isActive = item.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = { menuState.show { SongMenu(originalSong = item, onDismiss = menuState::dismiss) } }
                                    ) {
                                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                val fSongs = filteredItems.filterIsInstance<Song>()
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = queueSearchedSongsStr,
                                                        items = fSongs.map { it.toMediaItem() },
                                                        startIndex = fSongs.indexOfFirst { it.id == item.id },
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { SongMenu(originalSong = item, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (
                        filteredItems.isEmpty() &&
                        !showLikedPlaylist &&
                        !showDownloadedPlaylist &&
                        !showCachedPlaylists &&
                        !showTopPlaylists &&
                        !showUploadedPlaylists &&
                        searchQuery.isNotBlank()
                    ) {
                        item(key = "empty_search_result") {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                }
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                        GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                        ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (showLikedPlaylist) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            AutoPlaylistGridItem(
                                playlist = likedPlaylist,
                                onClick = { navController.navigate("auto_playlist/liked") },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (showDownloadedPlaylist) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            AutoPlaylistGridItem(
                                playlist = downloadPlaylist,
                                onClick = { navController.navigate("auto_playlist/downloaded") },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (showCachedPlaylists) {
                        item(
                            key = "cachedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            AutoPlaylistGridItem(
                                playlist = cachedPlaylist,
                                onClick = { navController.navigate("cache_playlist/cached") },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (showTopPlaylists) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            AutoPlaylistGridItem(
                                playlist = topPlaylist,
                                onClick = { navController.navigate("top_playlist/$topSize") },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (showUploadedPlaylists) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            AutoPlaylistGridItem(
                                playlist = uploadedPlaylist,
                                onClick = { navController.navigate("auto_playlist/uploaded") },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (groupedPlaylists.isNotEmpty()) {
                        item(key = "header_playlists_grid", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Playlists")
                        }
                        items(
                            items = groupedPlaylists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            PlaylistGridItem(
                                playlist = item,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (!item.playlist.isEditable && item.songCount == 0 && item.playlist.browseId != null) {
                                                navController.navigate("online_playlist/${item.playlist.browseId}")
                                            } else {
                                                navController.navigate("local_playlist/${item.id}")
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (groupedAlbums.isNotEmpty()) {
                        item(key = "header_albums_grid", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Albums")
                        }
                        items(
                            items = groupedAlbums,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            AlbumGridItem(
                                album = item,
                                isActive = item.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { AlbumMenu(originalAlbum = item, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (groupedArtists.isNotEmpty()) {
                        item(key = "header_artists_grid", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Artists")
                        }
                        items(
                            items = groupedArtists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            ArtistGridItem(
                                artist = item,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { navController.navigate("artist/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (groupedSongs.isNotEmpty()) {
                        item(key = "header_songs_grid", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                            SectionHeader(title = "Songs")
                        }
                        items(
                            items = groupedSongs,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            SongGridItem(
                                song = item,
                                showInLibraryIcon = true,
                                isActive = item.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                val fSongs = filteredItems.filterIsInstance<Song>()
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = queueSearchedSongsStr,
                                                        items = fSongs.map { it.toMediaItem() },
                                                        startIndex = fSongs.indexOfFirst { it.id == item.id },
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show { SongMenu(originalSong = item, onDismiss = menuState::dismiss) }
                                        },
                                    ).animateItem(),
                            )
                        }
                    }

                    if (
                        filteredItems.isEmpty() &&
                        !showLikedPlaylist &&
                        !showDownloadedPlaylist &&
                        !showCachedPlaylists &&
                        !showTopPlaylists &&
                        !showUploadedPlaylists &&
                        searchQuery.isNotBlank()
                    ) {
                        item(
                            key = "empty_search_result",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreatePlaylistDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = stringResource(R.string.create_playlist),
            )
        }

        PlayStoreRefreshIndicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun AutoPlaylistGridItem(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = playlist.playlist.name
    
    val (iconRes, iconTint, gradientColors) = when {
        title.contains("Liked", ignoreCase = true) || title.contains("पसंद", ignoreCase = true) -> Triple(
            R.drawable.favorite, 
            Color(0xFFD32F2F),
            listOf(Color(0xFFFCE3E3), Color(0xFFF3E5F5))
        )
        title.contains("Offline", ignoreCase = true) || title.contains("Downloaded", ignoreCase = true) -> Triple(
            R.drawable.download, 
            Color(0xFF1976D2),
            listOf(Color(0xFFE3F2FD), Color(0xFFF3E5F5))
        )
        title.contains("Cached", ignoreCase = true) -> Triple(
            R.drawable.sync, 
            Color(0xFF5E35B1),
            listOf(Color(0xFFEDE7F6), Color(0xFFF3E5F5))
        )
        title.contains("Uploaded", ignoreCase = true) -> Triple(
            R.drawable.upload,
            Color(0xFF1976D2),
            listOf(Color(0xFFE3F2FD), Color(0xFFF3E5F5))
        )
        title.contains("Top", ignoreCase = true) -> Triple(
            R.drawable.trending_up, 
            Color(0xFF455A64),
            listOf(Color(0xFFF5F5F5), Color(0xFFE8EAF6))
        )
        else -> Triple(
            R.drawable.playlist_play,
            Color(0xFF1E1E1E),
            listOf(Color(0xFFF5F5F5), Color(0xFFE8EAF6))
        )
    }

    Box(
        modifier = modifier
            .padding(8.dp) 
            .fillMaxWidth()
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Text(
            text = title,
            color = Color(0xFF1E1E1E),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
