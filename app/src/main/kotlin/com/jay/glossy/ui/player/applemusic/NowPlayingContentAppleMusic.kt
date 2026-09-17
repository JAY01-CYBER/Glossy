/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.jay.glossy.R
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.applecanvas.AppleMusicCanvasProvider
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.canvas.TidalCanvasProvider
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.constants.ListItemHeight
import com.jay.glossy.db.entities.LyricsEntity
import com.jay.glossy.extensions.metadata
import com.jay.glossy.extensions.move
import com.jay.glossy.extensions.toggleRepeatMode
import com.jay.glossy.listentogether.RoomRole
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.Lyrics
import com.jay.glossy.ui.component.MediaMetadataListItem
import com.jay.glossy.ui.component.PlayStoreRefreshIndicator
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.menu.SelectionMediaMetadataMenu
import com.jay.glossy.ui.player.CanvasArtworkPlaybackCache
import com.jay.glossy.ui.player.CanvasArtworkPlayer
import com.jay.glossy.ui.player.normalizeCanvasArtistName
import com.jay.glossy.ui.player.normalizeCanvasSongTitle
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.ui.utils.ShowOffsetDialog
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Locale

@Immutable
data class AppleMediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

@Stable
private fun getAppleMediaItems(player: Player): AppleMediaItemsData {
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

enum class LayoutMode { MAIN, COMPACT, CANVAS_HIDDEN, LYRICS_HIDDEN }

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

    // STATE MACHINE FOR UNIFIED ANIMATION
    var hasActiveCanvas by remember { mutableStateOf(false) }
    var canvasControlsHidden by remember { mutableStateOf(false) }
    var lyricsControlsHidden by remember { mutableStateOf(false) }
    var lyricsInteractionTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(canvasControlsHidden, hasActiveCanvas, viewState) {
        if (!canvasControlsHidden && hasActiveCanvas && viewState == AppleMusicView.MAIN) {
            delay(4000)
            canvasControlsHidden = true
        }
    }

    // Auto-hide Lyrics controls after 5 seconds
    LaunchedEffect(lyricsControlsHidden, lyricsInteractionTick, viewState) {
        if (!lyricsControlsHidden && viewState == AppleMusicView.LYRICS) {
            delay(5000L)
            lyricsControlsHidden = true
        }
    }

    val currentMode = when {
        viewState == AppleMusicView.LYRICS && lyricsControlsHidden -> LayoutMode.LYRICS_HIDDEN
        viewState != AppleMusicView.MAIN -> LayoutMode.COMPACT
        hasActiveCanvas && canvasControlsHidden -> LayoutMode.CANVAS_HIDDEN
        else -> LayoutMode.MAIN
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val screenW = maxWidth
        val screenH = maxHeight
        val statusBarTop = with(localDensity) { WindowInsets.statusBars.getTop(this).toDp() }
        val navBarBottom = with(localDensity) { WindowInsets.systemBars.getBottom(this).toDp() }
        
        val clusterHeight = 230.dp + navBarBottom
        val titleRowHeight = 60.dp
        val artworkH = (screenH - clusterHeight - titleRowHeight).coerceAtLeast(200.dp)

        val transition = updateTransition(targetState = currentMode, label = "layoutTransition")

        // ANIMATION PARAMS
        val titleY by transition.animateDp({ spring(0.75f, 300f) }, label = "titleY") {
            when (it) {
                LayoutMode.MAIN -> artworkH + 16.dp
                LayoutMode.COMPACT, LayoutMode.LYRICS_HIDDEN -> statusBarTop + 12.dp
                LayoutMode.CANVAS_HIDDEN -> screenH - navBarBottom - 76.dp
            }
        }
        val titlePaddingStart by transition.animateDp({ spring(0.75f, 300f) }, label = "titlePad") {
            when (it) {
                LayoutMode.MAIN -> 20.dp
                else -> 87.dp
            }
        }
        val tinyThumbWidth by transition.animateDp({ spring(0.75f, 300f) }, label = "thumbW") {
            when (it) {
                LayoutMode.MAIN -> 0.dp
                else -> 55.dp
            }
        }
        val titleFontSize by transition.animateFloat({ spring(0.75f, 300f) }, label = "titleSize") {
            when (it) {
                LayoutMode.MAIN -> 22f
                else -> 16f
            }
        }
        val artistFontSize by transition.animateFloat({ spring(0.75f, 300f) }, label = "artSize") {
            when (it) {
                LayoutMode.MAIN -> 16f
                else -> 13f
            }
        }
        val mainArtworkAlpha by transition.animateFloat({ tween(300) }, label = "artAlpha") {
            when (it) {
                LayoutMode.MAIN, LayoutMode.CANVAS_HIDDEN -> 1f
                else -> 0f
            }
        }
        val clusterAlpha by transition.animateFloat({ tween(300) }, label = "clusterAlpha") {
            when (it) {
                LayoutMode.MAIN, LayoutMode.COMPACT -> 1f
                else -> 0f
            }
        }
        val titleRowAlpha by transition.animateFloat({ tween(300) }, label = "titleRowAlpha") {
            when (it) {
                LayoutMode.LYRICS_HIDDEN -> 0f
                else -> 1f
            }
        }
        val viewBodyAlpha by transition.animateFloat({ tween(300) }, label = "viewAlpha") {
            when (it) {
                LayoutMode.COMPACT, LayoutMode.LYRICS_HIDDEN -> 1f
                else -> 0f
            }
        }
        val canvasOverlayAlpha by transition.animateFloat({ tween(300) }, label = "overlayAlpha") {
            if (it == LayoutMode.CANVAS_HIDDEN) 1f else 0f
        }

        // BACKGROUND BLUR LAYER
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(mediaMetadata?.thumbnailUrl).crossfade(500).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp)
            )
            Box(modifier = Modifier.fillMaxSize().alpha(0.62f).background(backdropBrush))
        }

        // 1. MAIN ARTWORK PAGER (Stays static, fades out in compact)
        val mediaItemsData by remember(playerConnection.player.currentMediaItemIndex, playerConnection.player.shuffleModeEnabled, mediaMetadata) {
            derivedStateOf { getAppleMediaItems(playerConnection.player) }
        }
        val mediaItems = mediaItemsData.items
        val currentMediaIndex = mediaItemsData.currentIndex
        val safeQueueSize = maxOf(1, mediaItems.size)
        val safeCurrentIndex = maxOf(0, currentMediaIndex)

        val pagerState = rememberPagerState(initialPage = safeCurrentIndex, pageCount = { safeQueueSize })

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkH)
                .alpha(mainArtworkAlpha)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentMode == LayoutMode.MAIN || currentMode == LayoutMode.CANVAS_HIDDEN,
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
                    artworkZoneHeightDp = artworkH.value.toInt(),
                    onCanvasReady = { isReady -> if (isCurrentPage && track?.mediaId == mediaMetadata?.id) hasActiveCanvas = isReady }
                )
            }
            
            // Full screen click to toggle Canvas Controls
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    indication = null, 
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (currentMode == LayoutMode.MAIN || currentMode == LayoutMode.CANVAS_HIDDEN) {
                        canvasControlsHidden = !canvasControlsHidden
                    }
                }
            )
        }

        // 2. QUEUE & LYRICS VIEWS
        if (viewBodyAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = statusBarTop + 12.dp + 55.dp + 16.dp, 
                        bottom = if (viewState == AppleMusicView.QUEUE) clusterHeight else 0.dp
                    )
                    .alpha(viewBodyAlpha)
            ) {
                Crossfade(targetState = viewState, label = "bodyCrossfade") { state ->
                    when (state) {
                        AppleMusicView.LYRICS -> AppleMusicLyricsBody(
                            scrollConnection = remember {
                                object : NestedScrollConnection {
                                    override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                                        if (available.y < 0f) {
                                            lyricsControlsHidden = true
                                        } else if (available.y > 0f) {
                                            lyricsControlsHidden = false
                                            lyricsInteractionTick++
                                        }
                                        return androidx.compose.ui.geometry.Offset.Zero
                                    }
                                }
                            },
                            position = position,
                            duration = duration
                        )
                        AppleMusicView.QUEUE -> AppleMusicQueueBody(bottomSheetState = bottomSheetState)
                        else -> {}
                    }
                }
            }
        }

        // 3. CANVAS HIDDEN OVERLAY GRADIENT
        if (canvasOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .alpha(canvasOverlayAlpha)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )
        }

        // 4. UNIFIED TITLE ROW (Moves and Scales Smoothly)
        Row(
            modifier = Modifier
                .offset(y = titleY)
                .fillMaxWidth()
                .padding(start = titlePaddingStart, end = 20.dp)
                .alpha(titleRowAlpha)
                .clickable(
                    indication = null, 
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (currentMode == LayoutMode.CANVAS_HIDDEN) canvasControlsHidden = false
                    if (currentMode == LayoutMode.LYRICS_HIDDEN) {
                        lyricsControlsHidden = false
                        lyricsInteractionTick++
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tiny Thumbnail (Grows from 0dp to 55dp)
            if (tinyThumbWidth > 0.dp) {
                Box(modifier = Modifier.width(tinyThumbWidth).height(55.dp).clip(RoundedCornerShape(4.dp))) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(mediaMetadata?.thumbnailUrl).crossfade(300).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = mediaMetadata?.title ?: "",
                    color = Color.White,
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mediaMetadata?.explicit == true) {
                        Icon(
                            painter = painterResource(R.drawable.explicit), 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size((artistFontSize + 4).dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                        color = AppleMusicTextSecondary,
                        fontSize = artistFontSize.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            AppleMusicHeaderActions(viewState = viewState, bottomSheetState = bottomSheetState)
        }

        // 5. BOTTOM CLUSTER (Stays Fixed, Fades Out)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = navBarBottom)
                .alpha(clusterAlpha)
        ) {
            AppleMusicBottomCluster(
                viewState = viewState,
                onSelectView = { 
                    viewState = it
                    lyricsControlsHidden = false 
                },
                activeColor = activePillContainer,
                activeContentColor = activePillContent,
                position = position,
                duration = duration
            )
        }
    }
}

@Composable
private fun AppleMusicArtworkPage(
    track: MediaItem?,
    isCurrentPage: Boolean,
    mediaMetadata: com.metrolist.models.MediaMetadata?,
    artworkZoneHeightDp: Int,
    onCanvasReady: (Boolean) -> Unit
) {
    val (canvasThumbnailAnimation) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
    val tryShowCanvas = canvasThumbnailAnimation && isCurrentPage && track?.mediaId == mediaMetadata?.id

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCurrentPage) {
            val currentArtworkUrl = mediaMetadata?.thumbnailUrl ?: track?.mediaMetadata?.artworkUri
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentArtworkUrl)
                    .crossfade(550)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop, // EDGE-TO-EDGE
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
                LaunchedEffect(Unit) { onCanvasReady(false) }
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
                    model = ImageRequest.Builder(LocalContext.current).data(track.mediaMetadata.artworkUri).crossfade(300).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun AppleMusicLyricsBody(
    scrollConnection: NestedScrollConnection,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.jay.glossy.di.LyricsHelperEntryPoint::class.java)
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata!!)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata!!.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> PlayStoreRefreshIndicator(isRefreshing = true, state = refreshState, modifier = Modifier.size(56.dp))
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                val positionProvider = remember { { playerConnection.player.currentPosition } }
                ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, textAlign = TextAlign.Center)) {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp),
                        showLyrics = true,
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicQueueBody(
    bottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)?.value == RoomRole.GUEST

    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    
    val safeCurrentIndex = maxOf(0, currentWindowIndex)
    val mutableQueueWindows = remember { mutableStateListOf<androidx.media3.common.Timeline.Window>() }
    
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) {
            from.index to to.index
        } else {
            currentDragInfo.first to to.index
        }
        val safeFrom = from.index.coerceIn(0, mutableQueueWindows.lastIndex)
        val safeTo = to.index.coerceIn(0, mutableQueueWindows.lastIndex)
        mutableQueueWindows.move(safeFrom, safeTo)
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val actualFrom = (from + safeCurrentIndex).coerceIn(0, queueWindows.lastIndex)
                val actualTo = (to + safeCurrentIndex).coerceIn(0, queueWindows.lastIndex)

                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(actualFrom, actualTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows.map { it.firstPeriodIndex }.toMutableList().move(actualFrom, actualTo).toIntArray(),
                            System.currentTimeMillis(),
                        ),
                    )
                }
                dragInfo = null
            }
        }
    }

    LaunchedEffect(queueWindows, safeCurrentIndex) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows.drop(safeCurrentIndex))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppleMusicQueuePillsRow(modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
        
        Box(modifier = Modifier.weight(1f).appleMusicVerticalFadeEdges(topFade = 24.dp, bottomFade = 48.dp)) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items = mutableQueueWindows, key = { _, item -> item.uid.hashCode() }) { _, window ->
                    ReorderableItem(state = reorderableState, key = window.uid.hashCode()) {
                        val isActive = window.uid == queueWindows.getOrNull(currentWindowIndex)?.uid
                        val trackMeta = window.mediaItem.metadata
                        
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.animateItem()) {
                            if (trackMeta != null) {
                                MediaMetadataListItem(
                                    mediaMetadata = trackMeta,
                                    isActive = isActive,
                                    isPlaying = isActive && playerConnection.player.isPlaying,
                                    trailingContent = {
                                        if (!isListenTogetherGuest) {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        com.jay.glossy.ui.menu.QueueMenu(
                                                            mediaMetadata = trackMeta,
                                                            playerBottomSheetState = bottomSheetState,
                                                            onShowDetailsDialog = {
                                                                trackMeta.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } }
                                                            },
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(painter = painterResource(R.drawable.more_vert), contentDescription = "More", tint = Color.White)
                                            }

                                            IconButton(onClick = { }, modifier = Modifier.draggableHandle()) {
                                                Icon(painter = painterResource(R.drawable.drag_handle), contentDescription = "Drag", tint = Color.White)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (!isListenTogetherGuest) {
                                            if (!isActive) {
                                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                playerConnection.player.playWhenReady = true
                                            } else {
                                                playerConnection.togglePlayPause()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleMusicQueuePillsRow(modifier: Modifier = Modifier) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()

    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.weight(1f).appleMusicPressInflate(pressedScale = 1.08f).height(40.dp).clip(RoundedCornerShape(20.dp))
                .background(if (shuffleModeEnabled) Color.White.copy(alpha = 0.2f) else AppleMusicPillInactive)
                .clickable { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Box(
            modifier = Modifier.weight(1f).appleMusicPressInflate(pressedScale = 1.08f).height(40.dp).clip(RoundedCornerShape(20.dp))
                .background(if (repeatMode != Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.2f) else AppleMusicPillInactive)
                .clickable { playerConnection.player.toggleRepeatMode() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
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
internal fun AppleMusicHeaderActions(
    viewState: AppleMusicView,
    bottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    
    val isEpisode = currentSong?.song?.isEpisode == true
    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .appleMusicPressInflate()
                .size(32.dp)
                .clip(CircleShape)
                .clickable { playerConnection.toggleLike() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        
        AppleMusicGlyphButton(
            icon = R.drawable.more_vert, 
            onClick = {
                if (viewState == AppleMusicView.LYRICS) {
                    menuState.show {
                        com.jay.glossy.ui.menu.LyricsMenu(
                            lyricsProvider = { currentLyrics },
                            songProvider = { currentSong?.song },
                            mediaMetadataProvider = { mediaMetadata },
                            onDismiss = menuState::dismiss,
                            onShowOffsetDialog = {
                                bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) }
                            }
                        )
                    }
                } else {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = bottomSheetState,
                            onShowDetailsDialog = {
                                mediaMetadata?.id?.let {
                                    bottomSheetPageState.show { ShowMediaInfo(it) }
                                }
                            },
                            onDismiss = menuState::dismiss
                        )
                    }
                }
            }
        )
    }
}
