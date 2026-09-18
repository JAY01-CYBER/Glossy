/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.jay.glossy.R
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.applecanvas.AppleMusicCanvasProvider
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.canvas.TidalCanvasProvider
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.player.CanvasArtworkPlaybackCache
import com.jay.glossy.ui.player.CanvasArtworkPlayer
import com.jay.glossy.ui.player.normalizeCanvasArtistName
import com.jay.glossy.ui.player.normalizeCanvasSongTitle
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@Immutable
data class AppleMediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

@Stable
internal fun getAppleMediaItems(player: Player): AppleMediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled
    
    val currentMediaItem = try { player.currentMediaItem } catch (e: Exception) { null }
    val previousIndex = if (!timeline.isEmpty) timeline.getPreviousWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled) else C.INDEX_UNSET
    val nextIndex = if (!timeline.isEmpty) timeline.getNextWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled) else C.INDEX_UNSET
    
    val prev = if (previousIndex != C.INDEX_UNSET) try { player.getMediaItemAt(previousIndex) } catch(e: Exception) { null } else null
    val next = if (nextIndex != C.INDEX_UNSET) try { player.getMediaItemAt(nextIndex) } catch(e: Exception) { null } else null
    
    val items = listOfNotNull(prev, currentMediaItem, next)
    val currentIdx = items.indexOf(currentMediaItem)
    
    return AppleMediaItemsData(items, currentIdx)
}

@Composable
fun NowPlayingContentAppleMusic(
    bottomSheetState: BottomSheetState,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    var viewState by rememberSaveable { mutableStateOf(AppleMusicView.MAIN) }
    val typography = rememberAppleMusicTypography()
    val localDensity = LocalDensity.current
    
    var extractedColor by remember { mutableStateOf(Color(0xFF121212)) }
    val animatedSeedColor by animateColorAsState(
        targetValue = extractedColor, 
        animationSpec = tween(800),
        label = "appleMusicDynamicColor"
    )

    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        val url = mediaMetadata?.thumbnailUrl
        if (url != null) {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()
                val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                val bitmap = result?.image?.toBitmap()
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getVibrantColor(palette.getMutedColor(0xFF121212.toInt()))
                    withContext(Dispatchers.Main) {
                        extractedColor = Color(dominant)
                    }
                }
            }
        }
    }

    val activePillContainer = remember(animatedSeedColor) { Color.White.copy(alpha = 0.2f) }
    val activePillContent = remember(animatedSeedColor) { Color.White }

    val backdropBrush = remember(animatedSeedColor) {
        Brush.verticalGradient(
            0f to appleMusicGradientColorAt(animatedSeedColor, 0f),
            0.48f to appleMusicGradientColorAt(animatedSeedColor, 0.48f),
            1f to appleMusicGradientColorAt(animatedSeedColor, 1f),
        )
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.matchParentSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaMetadata?.thumbnailUrl)
                    .crossfade(500)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp)
            )
            Box(modifier = Modifier.fillMaxSize().alpha(0.62f).background(backdropBrush))
        }

        Crossfade(targetState = viewState, animationSpec = tween(300), label = "AppleMusicView") { view ->
            when (view) {
                AppleMusicView.MAIN -> AppleMusicMainView(
                    viewState = view,
                    onSelectView = { viewState = it },
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    typography = typography,
                    bottomSheetState = bottomSheetState,
                    position = position,
                    duration = duration
                )
                AppleMusicView.LYRICS -> AppleMusicLyricsView(
                    viewState = view,
                    onSelectView = { viewState = it },
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    typography = typography,
                    position = position,
                    duration = duration
                )
                AppleMusicView.QUEUE -> AppleMusicQueueView(
                    viewState = view,
                    onSelectView = { viewState = it },
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    typography = typography,
                    bottomSheetState = bottomSheetState,
                    position = position,
                    duration = duration
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() })
                .size(width = 64.dp, height = 28.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { bottomSheetState.collapseSoft() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.35f)),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicMainView(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    typography: AppleMusicTypography,
    bottomSheetState: BottomSheetState,
    position: Long,
    duration: Long
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val localDensity = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    
    var bottomContentHeightDp by remember { mutableIntStateOf(330) }
    val artworkZoneHeightDp = (screenHeight - bottomContentHeightDp).coerceAtLeast(200)

    var hasActiveCanvas by remember { mutableStateOf(false) }
    var showControlLayout by rememberSaveable { mutableStateOf(true) }

    // Removed the 4-second auto-hide delay completely!
    // The controls will stay visible all the time.

    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        mediaMetadata
    ) {
        derivedStateOf { getAppleMediaItems(playerConnection.player) }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    val safeCurrentIndex = maxOf(0, currentMediaIndex)
    val safeQueueSize = maxOf(1, mediaItems.size)

    val pagerState = rememberPagerState(
        initialPage = safeCurrentIndex,
        pageCount = { safeQueueSize }
    )

    LaunchedEffect(currentMediaIndex, mediaItems) {
        if (currentMediaIndex >= 0 && currentMediaIndex < mediaItems.size) {
            pagerState.scrollToPage(currentMediaIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (!pagerState.isScrollInProgress) return@LaunchedEffect
        if (pagerState.currentPage > currentMediaIndex) {
            playerConnection.player.seekToNext()
        } else if (pagerState.currentPage < currentMediaIndex) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { idx -> mediaItems.getOrNull(idx)?.mediaId ?: idx.toString() }
        ) { page ->
            val track = mediaItems.getOrNull(page)
            val isCurrentPage = page == currentMediaIndex
            
            AppleMusicArtworkPage(
                track = track,
                isCurrentPage = isCurrentPage,
                mediaMetadata = mediaMetadata,
                artworkZoneHeightDp = artworkZoneHeightDp,
                onToggleControls = { showControlLayout = !showControlLayout },
                onCanvasReady = { isReady ->
                    if (isCurrentPage && track?.mediaId == mediaMetadata?.id) {
                        hasActiveCanvas = isReady
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            val controlsAlpha by animateFloatAsState(
                targetValue = if (showControlLayout) 1f else 0f,
                animationSpec = tween(if (showControlLayout) 180 else 500),
                label = "appleMusicControlsAlpha"
            )

            Column(
                modifier = Modifier
                    .alpha(controlsAlpha)
                    .onGloballyPositioned { coords ->
                        bottomContentHeightDp = with(localDensity) { coords.size.height.toDp().value.toInt() }
                    }
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                AppleMusicMainTitleRow(typography = typography, bottomSheetState = bottomSheetState)
                Spacer(modifier = Modifier.height(16.dp))
                AppleMusicBottomCluster(
                    viewState = viewState,
                    onSelectView = onSelectView,
                    lyricsAvailable = true, 
                    activeColor = activePillContainer,
                    activeContentColor = activePillContent,
                    position = position,
                    duration = duration
                )
            }

            if (hasActiveCanvas) {
                AnimatedVisibility(
                    visible = !showControlLayout,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bottomContentHeightDp.dp)
                            .clickable(
                                onClick = { showControlLayout = true },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.Black.copy(alpha = 0.5f),
                                    1f to Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        AppleMusicCompactHeader(
                            typography = typography, 
                            modifier = Modifier.padding(bottom = with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleMusicArtworkPage(
    track: MediaItem?,
    isCurrentPage: Boolean,
    mediaMetadata: com.metrolist.models.MediaMetadata?,
    artworkZoneHeightDp: Int,
    onToggleControls: () -> Unit,
    onCanvasReady: (Boolean) -> Unit
) {
    val (canvasThumbnailAnimation) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
    val tryShowCanvas = canvasThumbnailAnimation && isCurrentPage && track?.mediaId == mediaMetadata?.id

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCurrentPage) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(artworkZoneHeightDp.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggleControls() }
            ) {
                val currentArtworkUrl = mediaMetadata?.thumbnailUrl ?: track?.mediaMetadata?.artworkUri

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentArtworkUrl)
                        .crossfade(550)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .appleMusicVerticalFadeEdges(topFade = 0.dp, bottomFade = 300.dp)
                )

                if (tryShowCanvas && track != null) {
                    AppleMusicCanvasLayer(
                        track = track,
                        mediaMetadata = mediaMetadata,
                        onCanvasReady = onCanvasReady,
                        modifier = Modifier
                            .fillMaxSize()
                            .appleMusicVerticalFadeEdges(topFade = 0.dp, bottomFade = 300.dp)
                    )
                } else {
                    LaunchedEffect(Unit) {
                        onCanvasReady(false)
                    }
                }
            }
        } else if (track != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(artworkZoneHeightDp.dp)
                    .padding(24.dp), 
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.mediaMetadata.artworkUri)
                        .crossfade(300)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun AppleMusicCanvasLayer(
    track: MediaItem?,
    mediaMetadata: com.metrolist.models.MediaMetadata?,
    onCanvasReady: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaId = track?.mediaId ?: mediaMetadata?.id ?: return
    var canvasArtwork by remember(mediaId) { mutableStateOf<CanvasArtwork?>(null) }
    var canvasFetchInFlight by remember(mediaId) { mutableStateOf(false) }
    
    val storefront = remember {
        val country = Locale.getDefault().country
        if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }

    LaunchedEffect(mediaId) {
        CanvasArtworkPlaybackCache.get(mediaId)?.let { cached ->
            canvasArtwork = cached
            return@LaunchedEffect
        }

        if (canvasFetchInFlight) return@LaunchedEffect
        canvasFetchInFlight = true

        val fetched = withContext(Dispatchers.IO) {
            val albumName = track?.mediaMetadata?.albumTitle?.toString() ?: mediaMetadata?.album?.title ?: ""
            val songTitleRaw = track?.mediaMetadata?.title?.toString() ?: mediaMetadata?.title ?: ""
            val artistNameRaw = track?.mediaMetadata?.artist?.toString() ?: mediaMetadata?.artists?.joinToString { it.name } ?: ""
            
            val songTitle = normalizeCanvasSongTitle(songTitleRaw)
            val artistName = normalizeCanvasArtistName(artistNameRaw)

            if (songTitle.isBlank() || artistName.isBlank()) return@withContext null

            val tidalDeferred = async {
                TidalCanvasProvider.getBySongArtist(songTitle, artistName, albumName)
                    ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            }
            
            val appleDeferred = async {
                if (albumName.isNotBlank()) {
                    AppleMusicCanvasProvider.getByAlbumArtist(albumName, artistName, storefront)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                } else {
                    null
                } ?: AppleMusicCanvasProvider.getBySongArtist(songTitle, artistName, albumName, storefront)
                    ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            }

            tidalDeferred.await() ?: appleDeferred.await()
        }
        
        canvasArtwork = fetched
        if (fetched != null) {
            CanvasArtworkPlaybackCache.put(mediaId, fetched)
        }
        canvasFetchInFlight = false
    }

    LaunchedEffect(canvasArtwork) {
        onCanvasReady(canvasArtwork != null)
    }

    canvasArtwork?.let { artwork ->
        CanvasArtworkPlayer(
            primaryUrl = artwork.animated,
            fallbackUrl = artwork.videoUrl,
            modifier = modifier
        )
    }
}

@Composable
private fun AppleMusicMainTitleRow(
    typography: AppleMusicTypography,
    bottomSheetState: BottomSheetState
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata?.title ?: "",
                style = typography.mainTitle,
                maxLines = 1,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mediaMetadata?.explicit == true) {
                    Icon(
                        painter = painterResource(R.drawable.explicit), 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    style = typography.mainArtist,
                    maxLines = 1,
                    color = AppleMusicTextSecondary,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppleMusicHeaderActions(viewState = AppleMusicView.MAIN, bottomSheetState = bottomSheetState)
    }
}
