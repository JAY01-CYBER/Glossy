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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.extensions.metadata
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.player.CanvasArtworkPlaybackCache
import com.jay.glossy.ui.player.CanvasArtworkPlayer
import com.jay.glossy.ui.player.CanvasResolver
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
internal fun String.toHighRes(): String {
    return this.replace(Regex("=[wh]\\d+-[wh]\\d+.*"), "=w1080-h1080-l90-rj")
        .replace(Regex("-[wh]\\d+-[wh]\\d+.*"), "-w1080-h1080-l90-rj")
        .replace(Regex("=s\\d+.*"), "=s1080-l90-rj")
}

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

    val highResThumbnailUrl = remember(mediaMetadata?.thumbnailUrl) {
        mediaMetadata?.thumbnailUrl?.toHighRes()
    }

    LaunchedEffect(highResThumbnailUrl) {
        if (highResThumbnailUrl != null) {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(highResThumbnailUrl)
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
                    .data(highResThumbnailUrl)
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
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled
    ) {
        derivedStateOf { getAppleMediaItems(playerConnection.player) }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex
    val localDensity = LocalDensity.current
    var bottomContentHeightDp by remember { mutableIntStateOf(330) }
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val artworkZoneHeightDp = (screenHeight - bottomContentHeightDp).coerceAtLeast(200)

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
            // 🛠️ FIX: Unique key for each page to prevent "Key already used" crash
            key = { idx -> "${mediaItems.getOrNull(idx)?.mediaId}_$idx" } 
        ) { page ->
            val track = mediaItems.getOrNull(page)
            val isCurrentPage = page == currentMediaIndex
            
            AppleMusicArtworkPage(
                track = track,
                isCurrentPage = isCurrentPage,
                artworkZoneHeightDp = artworkZoneHeightDp,
                onCanvasReady = { /* Managed internally */ }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        bottomContentHeightDp = with(localDensity) { coords.size.height.toDp().value.toInt() }
                    }
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                // 🛠️ FIX: Passed viewState here so it reaches AppleMusicHeaderActions
                AppleMusicMainTitleRow(
                    viewState = viewState, 
                    typography = typography, 
                    bottomSheetState = bottomSheetState
                )
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
        }
    }
}

@Composable
private fun AppleMusicArtworkPage(
    track: MediaItem?,
    isCurrentPage: Boolean,
    artworkZoneHeightDp: Int,
    onCanvasReady: (Boolean) -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    val (canvasThumbnailAnimation) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
    val tryShowCanvas = canvasThumbnailAnimation && isCurrentPage && track?.mediaId == mediaMetadata?.id

    var isVideoPlaying by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCurrentPage) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(artworkZoneHeightDp.dp)
            ) {
                val rawUrl = mediaMetadata?.thumbnailUrl ?: track?.mediaMetadata?.artworkUri?.toString()
                val currentArtworkUrl = remember(rawUrl) { rawUrl?.toHighRes() }

                val imageAlpha by animateFloatAsState(
                    targetValue = if (isVideoPlaying) 0f else 1f,
                    animationSpec = tween(250),
                    label = "imageAlpha"
                )

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentArtworkUrl)
                        .crossfade(550)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = imageAlpha
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black,
                                        0.65f to Color.Black,
                                        0.92f to Color.Black.copy(alpha = 0.4f),
                                        1.00f to Color.Transparent,
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )

                if (tryShowCanvas && track != null) {
                    AppleMusicCanvasLayer(
                        track = track,
                        onCanvasReady = { isReady ->
                            isVideoPlaying = isReady
                            onCanvasReady(isReady)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.Black,
                                            0.65f to Color.Black,
                                            0.92f to Color.Black.copy(alpha = 0.4f),
                                            1.00f to Color.Transparent,
                                        )
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        isVideoPlaying = false
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
                val rawUrl = track.mediaMetadata.artworkUri?.toString()
                val nextArtworkUrl = remember(rawUrl) { rawUrl?.toHighRes() }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(nextArtworkUrl)
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
    onCanvasReady: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val mediaId = track?.mediaId ?: mediaMetadata?.id ?: return
    var canvasArtwork by remember(mediaId) { mutableStateOf<CanvasArtwork?>(null) }

    LaunchedEffect(mediaId) {
        canvasArtwork = CanvasResolver.resolve(
            context = context,
            mediaId = mediaId,
            songTitle = track?.mediaMetadata?.title?.toString() ?: mediaMetadata?.title ?: "",
            artistName = track?.mediaMetadata?.artist?.toString()
                ?: mediaMetadata?.artists?.joinToString { it.name } ?: "",
            albumName = track?.mediaMetadata?.albumTitle?.toString() ?: mediaMetadata?.album?.title ?: "",
        )
    }

    canvasArtwork?.let { artwork ->
        CanvasArtworkPlayer(
            primaryUrl = artwork.animated,
            fallbackUrl = artwork.videoUrl,
            isPlaying = isPlaying, 
            modifier = modifier,
            onVideoReady = onCanvasReady 
        )
    } ?: LaunchedEffect(Unit) {
        onCanvasReady(false)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicMainTitleRow(
    viewState: AppleMusicView,
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
        // 🛠️ FIX: Passing viewState here to the shared function
        AppleMusicHeaderActions(viewState = viewState, bottomSheetState = bottomSheetState)
    }
}
