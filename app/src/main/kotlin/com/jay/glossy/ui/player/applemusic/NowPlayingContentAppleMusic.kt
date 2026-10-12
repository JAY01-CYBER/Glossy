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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.constants.MiniLyricsStyle
import com.jay.glossy.constants.MiniLyricsStyleKey
import com.jay.glossy.constants.ShowLyricsOnPlayerKey
import com.jay.glossy.extensions.metadata
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.player.CanvasArtworkPlaybackCache
import com.jay.glossy.ui.player.CanvasArtworkPlayer
import com.jay.glossy.ui.player.CanvasResolver
import com.jay.glossy.ui.player.PlayerCanvasGlowLyrics
import com.jay.glossy.ui.player.PlayerSyncedLyricsView
import com.jay.glossy.ui.player.rememberCanvasEnabled
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.utils.rememberEnumPreference
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
                modifier = Modifier.fillMaxSize().blur(180.dp)
            )
            Box(modifier = Modifier.fillMaxSize().alpha(0.72f).background(backdropBrush))
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
    val showLyricsOnPlayer by rememberPreference(ShowLyricsOnPlayerKey, defaultValue = false)
    val (miniLyricsStyle) = rememberEnumPreference(MiniLyricsStyleKey, defaultValue = MiniLyricsStyle.CLASSIC)
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
                // The glow style paints the lyric on the artwork, so the page
                // that owns the artwork draws it — and only the current page,
                // so a swipe never leaves a stale line on a neighbouring cover.
                showGlowLyrics =
                    showLyricsOnPlayer && miniLyricsStyle == MiniLyricsStyle.CANVAS_GLOW,
                positionProvider = { position },
                onExpandLyrics = { onSelectView(AppleMusicView.LYRICS) },
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
                // The glow line is anchored to the artwork's lower edge and is
                // meant to sit right on top of the title, so the gap above the
                // title row collapses to almost nothing for that style; the
                // other two need the room this used to be.
                Spacer(
                    modifier = Modifier.height(
                        if (showLyricsOnPlayer && miniLyricsStyle == MiniLyricsStyle.CANVAS_GLOW) 2.dp
                        else 20.dp
                    )
                )
                // 🛠️ FIX: Passed viewState here so it reaches AppleMusicHeaderActions
                AppleMusicMainTitleRow(
                    viewState = viewState, 
                    typography = typography, 
                    bottomSheetState = bottomSheetState
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (showLyricsOnPlayer && miniLyricsStyle == MiniLyricsStyle.CLASSIC) {
                    PlayerSyncedLyricsView(
                        mediaMetadata = mediaMetadata,
                        positionProvider = { position },
                        onExpand = { onSelectView(AppleMusicView.LYRICS) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
    /** True when the chosen mini lyrics style draws on the artwork itself. */
    showGlowLyrics: Boolean,
    positionProvider: () -> Long,
    onExpandLyrics: () -> Unit,
    onCanvasReady: (Boolean) -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    // The animated canvas follows the single "Canvas Background" switch, the
    // same one that drives the player artwork and the app backdrop, so turning
    // it off never leaves a canvas playing on this screen. CanvasResolver still
    // honours the global Canvas Style setting, so the engine stays selectable.
    val canvasEnabled = rememberCanvasEnabled()
    val tryShowCanvas = canvasEnabled && isCurrentPage && track?.mediaId == mediaMetadata?.id

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

                // Canvas-glow mini lyrics, low on the artwork itself: this
                // design's artwork already runs to the title row, so the line
                // has the room and the layout keeps its height.
                if (showGlowLyrics) {
                    PlayerCanvasGlowLyrics(
                        mediaMetadata = mediaMetadata,
                        positionProvider = positionProvider,
                        accent = Color.White,
                        textSize = 26.sp,
                        // Bottom of the cover, on the title row's own edge. The
                        // title starts the moment the artwork ends, so anchoring
                        // here puts the words directly above the song name, reading
                        // as part of the title block rather than drifting over the
                        // middle of the cover.
                        alignment = Alignment.BottomStart,
                        // The cover here is full-bleed — it has no side padding of
                        // its own — so this inset is what lines the words up with
                        // the song name. It matches the 20dp AppleMusicMainTitleRow
                        // insets by: with none, the words sat flush against the
                        // screen edge while the name floated 20dp inside it, and
                        // the two no longer read as one block. The vertical values
                        // stay near zero for the same reason — the name sits just
                        // below this block, and the default's 16dp would hold the
                        // words a whole pocket away from it.
                        contentPadding = AppleMusicGlowInset,
                        alignToStart = true,
                        onExpand = onExpandLyrics,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                    )
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
        // One retry: a lookup that lands while a provider is still minting its
        // credentials otherwise leaves the track without a canvas for good.
        var found: CanvasArtwork? = null
        var attempt = 0
        while (found == null && attempt < 2) {
            if (attempt > 0) delay(2_500)
            found = try {
                CanvasResolver.resolve(
                    context = context,
                    mediaId = mediaId,
                    songTitle = track?.mediaMetadata?.title?.toString() ?: mediaMetadata?.title ?: "",
                    artistName = track?.mediaMetadata?.artist?.toString()
                        ?: mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    albumName = track?.mediaMetadata?.albumTitle?.toString() ?: mediaMetadata?.album?.title ?: "",
                )
            } catch (e: Exception) {
                null
            }
            attempt++
        }
        canvasArtwork = found
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
    val navController = LocalNavController.current

    // The artist line is the only way into an artist page from this design, so
    // every name in it is its own link. A track can credit several artists and
    // the metadata can carry a credit with no id at all, so each name is
    // annotated with the id of the credit it belongs to, and a credit without
    // one simply is not a link. The row used to carry a single click target
    // pointing at the first credit, so tapping any name opened the same
    // artist's page regardless of which name was pressed.
    val artists = mediaMetadata?.artists.orEmpty()
    val artistsLine =
        buildAnnotatedString {
            artists.forEachIndexed { index, artist ->
                pushStringAnnotation(tag = "artist", annotation = artist.id.orEmpty())
                withStyle(SpanStyle()) { append(artist.name) }
                pop()
                if (index != artists.lastIndex) append(", ")
            }
        }
    var artistLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        // AppleMusicGutter, not a literal: the canvas-glow mini lyrics above this
        // row are drawn over full-bleed artwork and pad themselves by the same
        // value to line their words up with the name. Keeping both on the one
        // constant is what makes them share an edge.
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppleMusicGutter),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)),
            ) {
                if (mediaMetadata?.explicit == true) {
                    Icon(
                        painter = painterResource(R.drawable.explicit), 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = artistsLine,
                    style = typography.mainArtist,
                    maxLines = 1,
                    color = AppleMusicTextSecondary,
                    onTextLayout = { artistLayoutResult = it },
                    modifier = Modifier
                        .basicMarquee()
                        .pointerInput(artistsLine) {
                            detectTapGestures { position ->
                                val layout = artistLayoutResult ?: return@detectTapGestures
                                val offset = layout.getOffsetForPosition(position)
                                artistsLine
                                    .getStringAnnotations("artist", offset, offset)
                                    .firstOrNull()
                                    ?.let { annotation ->
                                        if (annotation.item.isNotBlank()) {
                                            navController.navigate("artist/${annotation.item}")
                                            bottomSheetState.collapseSoft()
                                        }
                                    }
                            }
                        },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // 🛠️ FIX: Passing viewState here to the shared function
        AppleMusicHeaderActions(viewState = viewState, bottomSheetState = bottomSheetState)
    }
}
