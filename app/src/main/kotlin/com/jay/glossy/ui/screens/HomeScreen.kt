/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens

import com.jay.glossy.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.LocalNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberDominantColorState
import com.kmpalette.loader.rememberNetworkLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.HomePage
import com.metrolist.innertube.utils.completed
import com.metrolist.innertube.utils.parseCookieString
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.AutoRadioQueueKey
import com.jay.glossy.constants.GridItemSize
import com.jay.glossy.constants.GridItemsSizeKey
import com.jay.glossy.constants.GridThumbnailHeight
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.constants.ListItemHeight
import com.jay.glossy.constants.ListThumbnailSize
import com.jay.glossy.constants.QuickPickShape
import com.jay.glossy.constants.QuickPickShapeKey
import com.jay.glossy.constants.QuickPicksStyle
import com.jay.glossy.constants.QuickPicksStyleKey
import com.jay.glossy.constants.RandomizeHomeOrderKey
import com.jay.glossy.constants.ShowFeaturedCarouselKey
import com.jay.glossy.constants.SmallGridThumbnailHeight
import com.jay.glossy.constants.ThumbnailCornerRadius
import com.jay.glossy.db.entities.Album
import com.jay.glossy.db.entities.Artist
import com.jay.glossy.db.entities.LocalItem
import com.jay.glossy.db.entities.Playlist
import com.jay.glossy.db.entities.PlaylistEntity
import com.jay.glossy.db.entities.PlaylistSongMap
import com.jay.glossy.db.entities.Song
import com.jay.glossy.extensions.toMediaItem
import com.metrolist.models.toMediaMetadata
import com.jay.glossy.playback.queues.ListQueue
import com.jay.glossy.playback.queues.LocalAlbumRadio
import com.jay.glossy.playback.queues.YouTubeAlbumRadio
import com.jay.glossy.playback.queues.YouTubeQueue
import com.jay.glossy.ui.component.AlbumGridItem
import com.jay.glossy.ui.component.ArtistGridItem
import com.jay.glossy.ui.component.ChipsRow
import com.jay.glossy.ui.component.HideOnScrollFAB
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.NavigationTitle
import com.jay.glossy.ui.component.RandomizeGridItem
import com.jay.glossy.ui.component.SongGridItem
import com.jay.glossy.ui.component.SpeedDialGridItem
import com.jay.glossy.ui.component.YouTubeGridItem
import com.jay.glossy.ui.component.YouTubeListItem
import com.jay.glossy.ui.component.shimmer.GridItemPlaceHolder
import com.jay.glossy.ui.component.shimmer.ShimmerHost
import com.jay.glossy.ui.component.shimmer.TextPlaceholder
import com.jay.glossy.ui.menu.AlbumMenu
import com.jay.glossy.ui.menu.ArtistMenu
import com.jay.glossy.ui.menu.SongMenu
import com.jay.glossy.ui.menu.YouTubeAlbumMenu
import com.jay.glossy.ui.menu.YouTubeArtistMenu
import com.jay.glossy.ui.menu.YouTubePlaylistMenu
import com.jay.glossy.ui.menu.YouTubeSongMenu
import com.jay.glossy.ui.utils.SnapLayoutInfoProvider
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.utils.joinByBullet
import com.jay.glossy.utils.joinToArtistString
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.CommunityPlaylistItem
import com.jay.glossy.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.min
import kotlin.random.Random

import androidx.datastore.preferences.core.stringPreferencesKey

sealed class HomeSection(
    val id: String,
    val baseWeight: Int,
) {
    data object SpeedDial : HomeSection("speed_dial", 100)
    data object QuickPicks : HomeSection("quick_picks", 90)
    data object DailyDiscover : HomeSection("daily_discover", 80)
    data object Charts : HomeSection("charts", 75)
    data object KeepListening : HomeSection("keep_listening", 50)
    data object AccountPlaylists : HomeSection("account_playlists", 40)
    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)
    data object FromTheCommunity : HomeSection("from_the_community", 20)
    data class SimilarRecommendation(val index: Int) : HomeSection("similar_recommendation_$index", 10)
    data class HomePageSection(val index: Int) : HomeSection("home_page_section_$index", 10)
    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@Composable
fun SimpTopBar(
    hazeState: HazeState,
    accountName: String?,
    accountImageUrl: String?,
    chips: List<Pair<HomePage.Chip, String>>,
    selectedChip: HomePage.Chip?,
    onChipToggle: (HomePage.Chip?) -> Unit
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val guestNamePref by rememberPreference(stringPreferencesKey("guest_name"), "")
    val finalName = when {
        !accountName.isNullOrBlank() && !accountName.equals("Guest", ignoreCase = true) -> accountName
        guestNamePref.isNotBlank() -> guestNamePref
        else -> "Jay Chaudhary"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.thin(MaterialTheme.colorScheme.surface)
            )
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Glossy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Chips Row
        if (chips.isNotEmpty()) {
            ChipsRow(
                chips = chips,
                currentValue = selectedChip,
                onValueUpdate = onChipToggle,
            )
        }

        // Welcome Back Row
        if (selectedChip == null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(accountImageUrl)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = R.drawable.person),
                        error = painterResource(id = R.drawable.person),
                        contentDescription = "Profile Pic",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = finalName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val explorePage by viewModel.explorePage.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()

    val allLocalItems by viewModel.allLocalItems.collectAsStateWithLifecycle()
    val allYtItems by viewModel.allYtItems.collectAsStateWithLifecycle()
    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()

    val savedPodcastShows by viewModel.savedPodcastShows.collectAsStateWithLifecycle()
    val episodesForLater by viewModel.episodesForLater.collectAsStateWithLifecycle()

    val isLoading: Boolean by viewModel.isLoading.collectAsStateWithLifecycle()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()
    
    val hazeState = remember { HazeState() }

    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, true)
    val autoRadioQueue by rememberPreference(AutoRadioQueueKey, defaultValue = true)
    
    val showFeaturedCarouselPref by rememberPreference(ShowFeaturedCarouselKey, defaultValue = true)

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    val shouldShowWrappedCard by viewModel.showWrappedCard.collectAsStateWithLifecycle()
    val wrappedState by viewModel.wrappedManager.state.collectAsStateWithLifecycle()
    val isWrappedDataReady = wrappedState.isDataReady

    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val url = if (isLoggedIn) accountImageUrl else null

    var cachedPodcasts by remember { mutableStateOf<List<PodcastItem>>(emptyList()) }

    val featuredPodcasts =
        remember(homePage, selectedChip) {
            if (selectedChip == null) {
                cachedPodcasts = emptyList()
                emptyList()
            } else {
                val newPodcasts =
                    homePage
                        ?.sections
                        ?.flatMap { it.items }
                        ?.filterIsInstance<EpisodeItem>()
                        ?.mapNotNull { episode ->
                            episode.podcast?.let { podcast ->
                                PodcastItem(
                                    id = podcast.id,
                                    title = podcast.name,
                                    author = episode.author,
                                    episodeCountText = null,
                                    thumbnail = episode.thumbnail,
                                    playEndpoint = null,
                                    shuffleEndpoint = null,
                                )
                            }
                        }?.distinctBy { it.id }
                        ?.shuffled()
                        ?.take(10)
                        ?: emptyList()

                if (newPodcasts.isNotEmpty()) {
                    cachedPodcasts = newPodcasts
                }
                cachedPodcasts
            }
        }

    val scope = rememberCoroutineScope()
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    val wrappedDismissed by backStackEntry
        ?.savedStateHandle
        ?.getStateFlow("wrapped_seen", false)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    val spotlightItems = remember(homePage, randomSeed) {
        homePage?.sections
            ?.flatMap { it.items }
            ?.filterIsInstance<SongItem>()
            ?.distinctBy { it.id }
            ?.shuffled(Random(randomSeed))
            ?.take(8)
            ?: emptyList()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f

    var topHeaderColor by remember { mutableStateOf(backgroundColor) }
    val animatedColor by animateColorAsState(targetValue = topHeaderColor, animationSpec = tween(500), label = "GradientColor")

    val dominantImageUrl = spotlightItems.firstOrNull()?.thumbnail?.resize(1080, 1080) 

    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState = rememberDominantColorState(
        defaultColor = backgroundColor,
        defaultOnColor = backgroundColor,
        loader = networkLoader
    )

    LaunchedEffect(dominantImageUrl) {
        dominantImageUrl?.let {
            dominantColorState.updateFrom(Url(it))
        }
    }

    LaunchedEffect(dominantColorState.color, isLightTheme) {
        topHeaderColor = if (isLightTheme) {
            lerp(dominantColorState.color, Color.White, 0.85f)
        } else {
            dominantColorState.color.copy(alpha = 0.3f)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    val foundInSettings = stringResource(R.string.found_in_settings_content)
    LaunchedEffect(wrappedDismissed) {
        if (wrappedDismissed) {
            viewModel.markWrappedAsSeen()
            scope.launch {
                snackbarHostState.showSnackbar(foundInSettings)
            }
            backStackEntry?.savedStateHandle?.set("wrapped_seen", false)
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            lazylistState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.collect { lastVisibleIndex ->
            val len = lazylistState.layoutInfo.totalItemsCount
            if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                viewModel.loadMoreYouTubeItems(homePage?.continuation)
            }
        }
    }

    if (selectedChip != null) {
        BackHandler {
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> {
                SongGridItem(
                    song = it,
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = {
                            if (!isListenTogetherGuest) {
                                if (it.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(it.toMediaMetadata()) else ListQueue(title = it.title, items = listOf(it.toMediaItem())))
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { SongMenu(originalSong = it, onDismiss = menuState::dismiss) }
                        },
                    ),
                    isActive = it.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                )
            }
            is Album -> {
                AlbumGridItem(
                    album = it,
                    isActive = it.id == mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = { navController.navigate("album/${it.id}") },
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { AlbumMenu(originalAlbum = it, onDismiss = menuState::dismiss) } },
                    ),
                )
            }
            is Artist -> {
                ArtistGridItem(
                    artist = it,
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = { navController.navigate("artist/${it.id}") },
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { ArtistMenu(originalArtist = it, coroutineScope = scope, onDismiss = menuState::dismiss) } },
                    ),
                )
            }
            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier.combinedClickable(
                onClick = {
                    when (item) {
                        is SongItem -> if (!isListenTogetherGuest) playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()) else ListQueue(title = item.title, items = listOf(item.toMediaItem())))
                        is AlbumItem -> navController.navigate("album/${item.id}")
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                        is EpisodeItem -> if (!isListenTogetherGuest) playerConnection.playQueue(ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem())))
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem -> YouTubeSongMenu(song = item, onDismiss = menuState::dismiss)
                            is AlbumItem -> YouTubeAlbumMenu(albumItem = item, onDismiss = menuState::dismiss)
                            is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = scope, onDismiss = menuState::dismiss)
                            is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), onDismiss = menuState::dismiss)
                        }
                    }
                },
            ),
        )
    }

    val homeSections = remember(randomizeHomeOrder, randomSeed, selectedChip, speedDialItems, quickPicks, dailyDiscover, keepListening, accountPlaylists, forgottenFavorites, communityPlaylists, similarRecommendations, homePage?.sections, explorePage?.moodAndGenres) {
        val list = mutableListOf<HomeSection>()
        val chipActive = selectedChip != null

        if (!chipActive && speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
        if (!chipActive && accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (!chipActive && quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (!chipActive && communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
        if (!chipActive && dailyDiscover?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
        if (!chipActive) list.add(HomeSection.Charts) 
        if (!chipActive && keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (!chipActive && forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        if (!chipActive) {
            similarRecommendations?.indices?.forEach { i -> list.add(HomeSection.SimilarRecommendation(i)) }
        }

        homePage?.sections?.indices?.forEach { i -> list.add(HomeSection.HomePageSection(i)) }

        if (explorePage?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)

        val defaultOrder = mapOf(
            HomeSection.AccountPlaylists to 110,
            HomeSection.SpeedDial to 100,
            HomeSection.QuickPicks to 90,
            HomeSection.FromTheCommunity to 80,
            HomeSection.DailyDiscover to 70,
            HomeSection.Charts to 65, 
            HomeSection.KeepListening to 60,
            HomeSection.ForgottenFavorites to 40,
            HomeSection.MoodAndGenres to 10,
        )

        list.sortedByDescending { section ->
            when (section) {
                is HomeSection.SimilarRecommendation -> 30 - section.index
                is HomeSection.HomePageSection -> 20 - section.index
                else -> defaultOrder[section] ?: 0
            }
        }
    }

    LaunchedEffect(quickPicks) { quickPicksLazyGridState.scrollToItem(0) }
    LaunchedEffect(forgottenFavorites) { forgottenFavoritesLazyGridState.scrollToItem(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            indicator = {
                LoadingIndicator(
                    isRefreshing = isRefreshing,
                    state = pullRefreshState,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 160.dp),
                )
            },
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(brush = Brush.verticalGradient(colors = listOf(animatedColor, backgroundColor)))
                )
                
                val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                LazyColumn(
                    state = lazylistState,
                    modifier = Modifier.haze(state = hazeState),
                    contentPadding = PaddingValues(
                        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 180.dp, 
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    // Loading State
                    if (isLoading && homePage?.sections.isNullOrEmpty()) {
                        item(key = "loading_shimmer") {
                            ShimmerHost {
                                repeat(2) {
                                    TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(250.dp))
                                    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(4) { GridItemPlaceHolder() } }
                                }
                            }
                        }
                    }

                    // Render Sections Based on Order
                    homeSections.forEach { section ->
                        when (section) {
                            HomeSection.AccountPlaylists -> {
                                accountPlaylists?.takeIf { it.isNotEmpty() && selectedChip == null }?.let { accountPlaylists ->
                                    item(key = "account_playlists_title") {
                                        Text(
                                            text = "From your library",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    item(key = "account_playlists_list") {
                                        LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) {
                                            items(items = accountPlaylists.distinctBy { it.id }, key = { "home_account_playlist_${it.id}" }) { item ->
                                                ytGridItem(item)
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.QuickPicks -> {
                                quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                                    item(key = "quick_picks_title") {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            Text(text = "LET'S START WITH A RADIO", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = "Quick picks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                    item(key = "quick_picks_list") {
                                        LazyHorizontalGrid(
                                            state = quickPicksLazyGridState,
                                            rows = GridCells.Fixed(4),
                                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                            modifier = Modifier.fillMaxWidth().height(ListItemHeight * 4),
                                        ) {
                                            itemsIndexed(items = quickPicks.distinctBy { it.id }, key = { _, originalSong -> "home_quickpick_${originalSong.id}" }) { index, originalSong ->
                                                val song by database.song(originalSong.id).collectAsStateWithLifecycle(initialValue = originalSong)
                                                YouTubeListItem(
                                                    item = song!!.song,
                                                    isActive = song!!.id == mediaMetadata?.id,
                                                    isPlaying = isPlaying,
                                                    isSwipeable = false,
                                                    trailingContent = { IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song!!.song, onDismiss = menuState::dismiss) } }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) } },
                                                    modifier = Modifier
                                                        .width(horizontalLazyGridItemWidth)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (!isListenTogetherGuest) {
                                                                    if (song!!.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                                                    else playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(song!!.toMediaMetadata()) else ListQueue(title = song!!.title, items = listOf(song!!.toMediaItem())))
                                                                }
                                                            },
                                                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song!!.song, onDismiss = menuState::dismiss) } }
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HomeSection.Charts -> {
                                item(key = "charts_header") {
                                    var expanded by remember { mutableStateOf(false) }
                                    val countries = listOf("Global", "India", "United States", "United Kingdom", "Japan", "South Korea")
                                    var selectedCountry by rememberSaveable { mutableStateOf(countries[1]) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp), 
                                        horizontalArrangement = Arrangement.SpaceBetween, 
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(text = "WHAT IS BEST CHOICE TODAY", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = "Chart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                        Box {
                                            androidx.compose.material3.Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { expanded = true }) {
                                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = selectedCountry, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                                                countries.forEach { country -> androidx.compose.material3.DropdownMenuItem(text = { Text(country) }, onClick = { selectedCountry = country; expanded = false }) }
                                            }
                                        }
                                    }
                                }
                                item(key = "charts_content") {
                                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(140.dp).clickable { navController.navigate("youtube_browse/FEmusic_charts") }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(text = "Explore Charts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            is HomeSection.HomePageSection -> {
                                if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) return@forEach
                                val sectionData = homePage?.sections?.getOrNull(section.index)
                                sectionData?.let {
                                    val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
                                    val isSongsOnlySection = sectionData.items.isNotEmpty() && sectionData.items.all { it is SongItem }

                                    item(key = "home_section_title_${section.index}") {
                                        Text(
                                            text = sectionData.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }

                                    if (isSongsOnlySection) {
                                        item(key = "home_section_list_${section.index}") {
                                            LazyHorizontalGrid(
                                                state = remember("section_${section.index}_grid") { LazyGridState() },
                                                rows = GridCells.Fixed(4),
                                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                                modifier = Modifier.fillMaxWidth().height(ListItemHeight * 4),
                                            ) {
                                                itemsIndexed(items = sectionSongs.distinctBy { it.id }, key = { _, song -> "home_section_${section.index}_song_${song.id}" }) { _, song ->
                                                    YouTubeListItem(
                                                        item = song,
                                                        isActive = song.id == mediaMetadata?.id,
                                                        isPlaying = isPlaying,
                                                        isSwipeable = false,
                                                        trailingContent = { IconButton(onClick = { menuState.show { YouTubeSongMenu(song = song, onDismiss = menuState::dismiss) } }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) } },
                                                        modifier = Modifier
                                                            .width(horizontalLazyGridItemWidth)
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (!isListenTogetherGuest) {
                                                                        playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata()) else ListQueue(title = song.title, items = listOf(song.toMediaItem())))
                                                                    }
                                                                },
                                                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, onDismiss = menuState::dismiss) } }
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        item(key = "home_section_list_${section.index}") {
                                            LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) {
                                                items(items = sectionData.items.distinctBy { it.id }, key = { "home_section_${section.index}_item_${it.id}" }) { item ->
                                                    ytGridItem(item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Include other sections like DailyDiscover, Community etc. below as needed.
                            else -> {} 
                        }
                    }
                }
            }
        }
        
        // Top Bar Overlaid with Haze
        SimpTopBar(
            hazeState = hazeState,
            accountName = accountName,
            accountImageUrl = url,
            chips = homePage?.chips?.map { it to it.title } ?: emptyList(),
            selectedChip = selectedChip,
            onChipToggle = { viewModel.toggleChip(it) }
        )
        
        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
            lazyListState = lazylistState,
            icon = R.drawable.shuffle,
            onClick = {
                // Shuffle Logic
            },
            onRecognitionClick = { navController.navigate("recognition") },
        )
    }
}
