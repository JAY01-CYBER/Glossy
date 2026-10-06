/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import com.jay.glossy.R

import androidx.compose.animation.AnimatedVisibility
<<<<<<< HEAD
import androidx.compose.animation.core.animateDpAsState
=======
>>>>>>> origin/main
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
<<<<<<< HEAD
=======
import androidx.compose.foundation.layout.aspectRatio
>>>>>>> origin/main
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
<<<<<<< HEAD
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
=======
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
>>>>>>> origin/main
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
<<<<<<< HEAD
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
=======
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
>>>>>>> origin/main
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
<<<<<<< HEAD
import androidx.compose.ui.unit.DpOffset
=======
>>>>>>> origin/main
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.constants.CropAlbumArtKey
import com.jay.glossy.constants.HidePlayerThumbnailKey
import com.jay.glossy.constants.PlayerBackgroundStyle
import com.jay.glossy.constants.PlayerBackgroundStyleKey
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.constants.PlayerStyle
import com.jay.glossy.constants.PlayerStyleKey
import com.jay.glossy.constants.SeekExtraSeconds
import com.jay.glossy.constants.SwipeThumbnailKey
import com.jay.glossy.constants.ThumbnailCornerRadius
<<<<<<< HEAD
import com.jay.glossy.constants.ThumbnailShadowKey
=======
>>>>>>> origin/main
import com.jay.glossy.listentogether.RoomRole
import com.jay.glossy.ui.component.CastButton
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)

@Immutable
data class MediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    val effectiveSize = if (isLandscape) {
        minOf(containerWidth, containerHeight) - (horizontalPadding * 2)
    } else {
        containerWidth - (horizontalPadding * 2)
    }
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = cornerRadius * 2
    )
}

<<<<<<< HEAD
/**
 * Blur radius of the artwork's cast shadow (Appearance → "Thumbnail shadow").
 *
 * A soft halo around a full-square album cover, not a dark band under it. It is
 * deliberately kept below [PlayerHorizontalPadding] — the room the artwork slot
 * reserves around itself, and therefore the room this shadow has to spread into
 * before the player's offscreen layer cuts it off.
 *
 * This is a [dropShadow] and not `Modifier.shadow` on purpose. The
 * elevation-based modifier gets its shadow from the drawn content's alpha, and
 * the artwork sits in a clipped box whose content is an image (or, with
 * canvases on, an `AndroidView` texture) — so the shadow never showed up. A
 * drop shadow is drawn from the shape alone, with its own colour and offset, so
 * it renders on every player background.
 */
internal val ArtworkShadowRadius = 10.dp

/**
 * Vertical offset of the artwork shadow — depth reads downwards, but a small
 * lift keeps the halo even around the top edge instead of puddling below.
 */
internal val ArtworkShadowOffsetY = 4.dp

/**
 * Opacity of the artwork's cast shadow.
 *
 * Plain black at a heavy alpha stops reading as depth the moment it meets the
 * canvas video: the video already carries its own contrast, so the shadow only
 * has to lift the artwork off the background rather than draw an edge around
 * it. Surfaces that really are raised controls — the player's label button —
 * ask for more.
 */
internal val ArtworkShadowAlpha = 0.26f

/**
 * The artwork's cast shadow, shape-matched to the artwork's corner radius.
 *
 * [radius], [offsetY] and [alpha] default to the full-size player artwork; the
 * mini player passes smaller ones so its 48dp tile does not sit in a blurred
 * cloud bigger than the tile itself.
 */
internal fun Modifier.artworkDropShadow(
    shape: Shape,
    radius: Dp = ArtworkShadowRadius,
    offsetY: Dp = ArtworkShadowOffsetY,
    alpha: Float = ArtworkShadowAlpha,
): Modifier =
    dropShadow(
        shape = shape,
        shadow =
            Shadow(
                radius = radius,
                offset = DpOffset(x = 0.dp, y = offsetY),
                color = Color.Black.copy(alpha = alpha),
            ),
    )

=======
>>>>>>> origin/main
@Stable
private fun getMediaItems(
    player: Player,
    swipeThumbnail: Boolean
): MediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled
    
    val currentMediaItem = try {
        player.currentMediaItem
    } catch (e: Exception) { null }
    
    val previousMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(previousIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(nextIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val items = listOfNotNull(previousMediaItem, currentMediaItem, nextMediaItem)
    val currentMediaIndex = items.indexOf(currentMediaItem)
    
    return MediaItemsData(items, currentMediaIndex)
}

@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR,
        PlayerBackgroundStyle.GRADIENT,
        PlayerBackgroundStyle.ANIMATED_MESH -> Color.White
    }
}

// OPTIMIZED PERSISTENT CACHE
object CanvasArtworkPlaybackCache {
    private const val defaultMaxSize = 256
    // v2: the v1 file may hold mismatched canvases from before per-track
    // validation existed, so it is ignored and deleted on init.
    private const val PERSIST_FILE = "canvas_artwork_cache_v2.json"
    private const val LEGACY_PERSIST_FILE = "canvas_artwork_cache.json"
    private const val PERSIST_DEBOUNCE_MS = 2_000L

    private val map = LinkedHashMap<String, CanvasArtwork>(defaultMaxSize, 0.75f, true)
    @Volatile private var maxSize = defaultMaxSize
    @Volatile private var cacheFile: File? = null

    private val persistScope = CoroutineScope(Dispatchers.IO)
    private var persistJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val mapSerializer = MapSerializer(String.serializer(), CanvasArtwork.serializer())

    fun init(context: android.content.Context) {
        cacheFile = File(context.filesDir, PERSIST_FILE)
        // Drop pre-validation entries: they can contain wrong-song canvases.
        runCatching { File(context.filesDir, LEGACY_PERSIST_FILE).delete() }
        loadFromDisk()
    }

    val currentItemCount: Int
        get() = map.size

    @Synchronized
    fun get(mediaId: String): CanvasArtwork? {
        if (maxSize <= 0) return null
        return map[mediaId]
    }

    @Synchronized
    fun put(mediaId: String, artwork: CanvasArtwork) {
        val limit = maxSize
        if (limit <= 0 || mediaId.isBlank()) return
        map[mediaId] = artwork
        trimToSize()
        schedulePersist()
    }

<<<<<<< HEAD
    /**
     * Drops one song's entry. Used when the canvas check refuses what the cache
     * holds — a clip for another song, or a URL that has stopped serving — so
     * the next lookup for that song actually asks the providers instead of
     * being answered by the entry that was just rejected.
     */
    @Synchronized
    fun remove(mediaId: String) {
        if (map.remove(mediaId) != null) schedulePersist()
    }

=======
>>>>>>> origin/main
    @Synchronized
    fun clear() {
        map.clear()
        schedulePersist()
    }

    @Synchronized
    fun setMaxSize(newSize: Int) {
        maxSize = newSize
        if (newSize == 0) {
            map.clear()
            schedulePersist()
        } else {
            trimToSize()
        }
    }

    private fun trimToSize() {
        var evicted = false
        while (map.size > maxSize) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
                evicted = true
            } else {
                break
            }
        }
        if (evicted) schedulePersist()
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        try {
            val raw = file.readText()
            if (raw.isBlank()) return
            val restored = json.decodeFromString(mapSerializer, raw)
            map.clear()
            map.putAll(restored)
            trimToSize()
        } catch (e: Exception) {
            runCatching { file.delete() }
        }
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            writeToDisk()
        }
    }

    private fun writeToDisk() {
        val file = cacheFile ?: return
        try {
            val snapshot: Map<String, CanvasArtwork>
            synchronized(this@CanvasArtworkPlaybackCache) {
                snapshot = LinkedHashMap(map)
            }
            val raw = json.encodeToString(mapSerializer, snapshot)
            file.writeText(raw)
        } catch (e: Exception) {
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()

    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
<<<<<<< HEAD
    val thumbnailShadow by rememberPreference(ThumbnailShadowKey, true)
=======
>>>>>>> origin/main
    
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val (playerStyle) = rememberEnumPreference(
        key = PlayerStyleKey,
        defaultValue = PlayerStyle.MODERN
    )
    
    val textBackgroundColor = getTextColor(playerBackground)
    val thumbnailLazyGridState = rememberLazyGridState()
    
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        swipeThumbnail,
        mediaMetadata
    ) {
        derivedStateOf {
            getMediaItems(playerConnection.player, swipeThumbnail)
        }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        ThumbnailSnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex) {
        val index = mediaItemsData.currentIndex
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isLandscape) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                if (!isLandscape) {
                    if (playerStyle.name == "VIVI_NEW") {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                    
                    ThumbnailHeader(
                        queueTitle = queueTitle, 
                        albumTitle = mediaMetadata?.album?.title, 
                        textColor = textBackgroundColor,
                        playerStyleName = playerStyle.name
                    )
                }
                
                BoxWithConstraints(
                    contentAlignment = if (isLandscape) Alignment.Center else if (playerStyle.name == "VIVI_NEW") Alignment.TopCenter else Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    val dimensions = remember(maxWidth, maxHeight, isLandscape) {
                        calculateThumbnailDimensions(
                            containerWidth = maxWidth,
                            containerHeight = maxHeight,
                            isLandscape = isLandscape
                        )
                    }

                    val onSeekCallback = remember {
                        { direction: String, showEffect: Boolean ->
                            seekDirection = direction
                            showSeekEffect = showEffect
                        }
                    }
                    
                    val isScrollEnabled by remember(swipeThumbnail) {
                        derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                    }
                    
                    if (playerStyle.name == "VIVI_NEW" && !isLandscape) {
                        val currentMedia = mediaItems.getOrNull(currentMediaIndex)
                        val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
                        var skipMultiplier by remember { mutableIntStateOf(1) }
                        var lastTapTime by remember { mutableLongStateOf(0L) }

<<<<<<< HEAD
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = PlayerHorizontalPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            // Sized by the shorter side of the slot, not by its
                            // width alone: the lyrics strip underneath grows when
                            // the active line wraps onto a second row, and a
                            // width-sized square used to hang out of the space
                            // left for it instead of shrinking into it.
                            val fittedSide =
                                if (maxHeight.value.isFinite()) minOf(maxWidth, maxHeight) else maxWidth
                            val artworkSide by animateDpAsState(
                                targetValue = fittedSide,
                                label = "artworkSide",
                            )
                            Box(
                                modifier = Modifier
                                    .size(artworkSide)
                                    .then(
                                        if (thumbnailShadow) {
                                            Modifier.artworkDropShadow(
                                                RoundedCornerShape(dimensions.cornerRadius),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
=======
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center 
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding)
                                    .aspectRatio(1f)
>>>>>>> origin/main
                                    .pointerInput(swipeThumbnail) {
                                        if (!swipeThumbnail) return@pointerInput
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onDragStart = { totalDrag = 0f },
                                            onDragEnd = {
                                                if (totalDrag < -50f && canSkipNext) {
                                                    playerConnection.player.seekToNext()
                                                } else if (totalDrag > 50f && canSkipPrevious) {
                                                    playerConnection.player.seekToPreviousMediaItem()
                                                }
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                totalDrag += dragAmount
                                            }
                                        )
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                if (isListenTogetherGuest) return@detectTapGestures
                                                val currentPosition = playerConnection.player.currentPosition
                                                val songDuration = playerConnection.player.duration
                                                val now = System.currentTimeMillis()
                                                if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) skipMultiplier++ else skipMultiplier = 1
                                                lastTapTime = now
                                                val skipAmount = 5000 * skipMultiplier
                                                val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                                        (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                                                if (isLeftSide) {
                                                    playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                                                    onSeekCallback(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                                                } else {
                                                    playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(songDuration))
                                                    onSeekCallback(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
<<<<<<< HEAD
                                // While the canvas is the layer on show, the slot
                                // takes its shape from Appearance's canvas corner
                                // control; with the canvas off the artwork keeps
                                // its own tighter corners.
                                val canvasThumbnailAnimation = rememberCanvasEnabled()
                                val artworkShape =
                                    RoundedCornerShape(
                                        if (canvasThumbnailAnimation) rememberCanvasCornerSize() else dimensions.cornerRadius,
                                    )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(artworkShape)
                                ) {
=======
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(dimensions.cornerRadius))
                                ) {
                                    val (canvasThumbnailAnimation) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
>>>>>>> origin/main
                                    var canvasVideoReady by remember { mutableStateOf(false) }
                                    val artworkAlpha by animateFloatAsState(
                                        targetValue = if (canvasVideoReady) 0f else 1f,
                                        animationSpec = tween(250),
                                        label = "artworkAlpha",
                                    )

                                    if (canvasThumbnailAnimation && currentMedia?.mediaId == mediaMetadata?.id && currentMedia != null) {
                                        // Static artwork dims out once the canvas video is
                                        // rendering — avoids drawing two full layers every frame.
                                        Box(Modifier.graphicsLayer { alpha = artworkAlpha }) {
                                            if (hidePlayerThumbnail) {
                                                HiddenThumbnailPlaceholder(
                                                    textBackgroundColor = textBackgroundColor,
                                                    playerStyleName = playerStyle.name
                                                )
                                            } else {
                                                val artworkUriToUse = if (currentMedia?.mediaId == mediaMetadata?.id && !mediaMetadata?.thumbnailUrl.isNullOrBlank()) {
                                                    mediaMetadata?.thumbnailUrl
                                                } else {
                                                    currentMedia?.mediaMetadata?.artworkUri?.toString()
                                                }
                                                ThumbnailImage(
                                                    artworkUri = artworkUriToUse,
                                                    cropArtwork = cropAlbumArt,
                                                    playerStyleName = playerStyle.name
                                                )
                                            }
                                        }
                                        CanvasLayer(
                                            item = currentMedia,
                                            modifier = Modifier.fillMaxSize(),
                                            onVideoReady = { canvasVideoReady = it }
                                        )
                                    } else {
                                        if (hidePlayerThumbnail) {
                                            HiddenThumbnailPlaceholder(
                                                textBackgroundColor = textBackgroundColor,
                                                playerStyleName = playerStyle.name
                                            )
                                        } else {
                                            val artworkUriToUse = if (currentMedia?.mediaId == mediaMetadata?.id && !mediaMetadata?.thumbnailUrl.isNullOrBlank()) {
                                                mediaMetadata?.thumbnailUrl
                                            } else {
                                                currentMedia?.mediaMetadata?.artworkUri?.toString()
                                            }
                                            ThumbnailImage(
                                                artworkUri = artworkUriToUse,
                                                cropArtwork = cropAlbumArt,
                                                playerStyleName = playerStyle.name
                                            )
                                        }
                                    }

                                    CastButton(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                        tintColor = textBackgroundColor
                                    )
                                }
                            }
                        }
                    } else {
                        LazyHorizontalGrid(
                            state = thumbnailLazyGridState,
                            rows = GridCells.Fixed(1),
                            flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                            userScrollEnabled = isScrollEnabled,
                            modifier = if (isLandscape) {
                                Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            // YAHAN PAR FIX HAI: Key Collision se bachne ke liye itemsIndexed aur ID_+_Index ka use kiya gaya hai.
                            itemsIndexed(
                                items = mediaItems,
                                key = { index, item -> 
                                    "${item.mediaId}_$index".ifEmpty { "unknown_${item.hashCode()}_$index" }
                                }
                            ) { index, item ->
                                ThumbnailItem(
                                    item = item,
                                    dimensions = dimensions,
                                    hidePlayerThumbnail = hidePlayerThumbnail,
                                    cropAlbumArt = cropAlbumArt,
                                    textBackgroundColor = textBackgroundColor,
                                    layoutDirection = layoutDirection,
                                    onSeek = onSeekCallback,
                                    playerConnection = playerConnection,
                                    context = context,
                                    isLandscape = isLandscape,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    currentMediaId = mediaMetadata?.id,
                                    currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                    playerStyleName = playerStyle.name
                                )
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekEffectOverlay(seekDirection = seekDirection)
        }
    }
}

<<<<<<< HEAD
/**
 * How many times a canvas lookup is attempted while the surface stays on
 * screen, before it settles for its still artwork.
 *
 * A retry is for a lookup that answered nothing *inconclusively* — a provider
 * timing out, a rate limit, a connection that was not up yet, Spotify not
 * connected yet. "This song has no canvas" is a different answer, and
 * CanvasResolver says which one it gave: a definitive miss stops the loop on
 * the spot instead of spending a single retry on it.
 */
private const val MaxCanvasLookupAttempts = 4

/**
 * Pause before the second attempt; every following pause doubles it, so a
 * provider hiccup is not hammered and a slow connection is given time to
 * come up.
 */
private const val CanvasLookupRetryDelayMillis = 2_000L

/** Ceiling for that doubling. */
private const val MaxCanvasLookupRetryDelayMillis = 16_000L

=======
>>>>>>> origin/main
@Composable
private fun CanvasLayer(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onVideoReady: (Boolean) -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
<<<<<<< HEAD
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canvasArtwork =
        rememberCanvasArtwork(
            mediaId = item.mediaId,
            title = item.mediaMetadata.title?.toString() ?: "",
            artist = item.mediaMetadata.artist?.toString() ?: "",
            album = item.mediaMetadata.albumTitle?.toString() ?: "",
            onVideoReady = onVideoReady,
        )
=======
    val context = LocalContext.current
    val isPlaying by playerConnection.isPlaying.collectAsState()

    var canvasArtwork by remember(item.mediaId) { mutableStateOf<CanvasArtwork?>(null) }

    LaunchedEffect(item.mediaId) {
        onVideoReady(false)
        // Playback cache is checked inside CanvasResolver; style-aware providers
        // (Glossy / ArchiveTune / Both) are selected per the user preference.
        canvasArtwork = CanvasResolver.resolve(
            context = context,
            mediaId = item.mediaId,
            songTitle = item.mediaMetadata.title?.toString() ?: "",
            artistName = item.mediaMetadata.artist?.toString() ?: "",
            albumName = item.mediaMetadata.albumTitle?.toString() ?: "",
        )
    }
>>>>>>> origin/main

    canvasArtwork?.let { artwork ->
        CanvasArtworkPlayer(
            primaryUrl = artwork.animated,
            fallbackUrl = artwork.videoUrl,
            isPlaying = isPlaying, // Pause animation when music is paused
            modifier = modifier,
            onVideoReady = onVideoReady
        )
    }
}

<<<<<<< HEAD
/**
 * The same animated canvas for a caller that only holds metadata — the Capsule
 * and Cinematic designs, which draw their own artwork instead of going through
 * [Thumbnail]. Lookup, retry budget, cache and the "pause with the music" rule
 * all come from the shared path below, so every surface agrees.
 */
@Composable
internal fun PlayerCanvasArtwork(
    mediaId: String?,
    title: String,
    artist: String,
    album: String,
    modifier: Modifier = Modifier,
    /**
     * Null follows the music, which is what the player wants. A surface that
     * loops whether or not anything is playing — a Spotlight card — passes its
     * own flag instead.
     */
    isPlaying: Boolean? = null,
    onVideoReady: (Boolean) -> Unit = {},
) {
    if (mediaId.isNullOrBlank()) return
    val playerConnection = LocalPlayerConnection.current
    val playerIsPlaying by playerConnection?.isPlaying?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val canvasArtwork = rememberCanvasArtwork(
        mediaId = mediaId,
        title = title,
        artist = artist,
        album = album,
        onVideoReady = onVideoReady,
    )

    canvasArtwork?.let { artwork ->
        CanvasArtworkPlayer(
            primaryUrl = artwork.animated,
            fallbackUrl = artwork.videoUrl,
            isPlaying = isPlaying ?: playerIsPlaying,
            modifier = modifier,
            onVideoReady = onVideoReady,
        )
    }
}

/** The provider lookup behind both canvas surfaces, with its retry budget. */
@Composable
private fun rememberCanvasArtwork(
    mediaId: String,
    title: String,
    artist: String,
    album: String,
    onVideoReady: (Boolean) -> Unit,
): CanvasArtwork? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canvasArtwork by remember(mediaId) { mutableStateOf<CanvasArtwork?>(null) }
    // Bumped when the app comes back to the front: the phone may have been
    // offline, a provider may have been rate limiting, or the account may have
    // been connected while the app was in the background. Keying the lookup on
    // it starts a fresh attempt budget, so a surface that quietly failed to
    // resolve recovers by itself instead of sitting on still artwork until the
    // next logout/login cycle — which is what it used to take.
    var lookupWindow by remember(mediaId) { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, mediaId) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && canvasArtwork == null) lookupWindow++
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mediaId, lookupWindow) {
        onVideoReady(false)
        canvasArtwork = null
        // One lookup used to be the whole story: a provider that timed out, a
        // rate limit, a connection that was not up yet meant the track showed
        // its still artwork until the player was closed and opened again.
        //
        // A retry is only worth spending on that kind of miss. "No canvas for
        // this song" is a different answer, and CanvasResolver says which one
        // it gave, so a song that genuinely has no canvas is not looked up
        // again while a lookup that came back empty-handed is asked again.
        var retryDelayMillis = CanvasLookupRetryDelayMillis
        repeat(MaxCanvasLookupAttempts) { attempt ->
            // Playback cache is checked inside CanvasResolver; style-aware
            // providers (Glossy / ArchiveTune / Both) come from the preference.
            val resolved =
                CanvasResolver.resolve(
                    context = context,
                    mediaId = mediaId,
                    songTitle = title,
                    artistName = artist,
                    albumName = album,
                )
            if (resolved != null) {
                canvasArtwork = resolved
                return@LaunchedEffect
            }
            if (CanvasResolver.wasDefinitiveMiss(mediaId)) return@LaunchedEffect
            if (attempt < MaxCanvasLookupAttempts - 1) {
                delay(retryDelayMillis)
                retryDelayMillis =
                    (retryDelayMillis * 2).coerceAtMost(MaxCanvasLookupRetryDelayMillis)
            }
        }
    }

    return canvasArtwork
}

=======
>>>>>>> origin/main
@Composable
private fun ThumbnailHeader(
    queueTitle: String?,
    albumTitle: String?,
    textColor: Color,
    playerStyleName: String,
    modifier: Modifier = Modifier
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
        ) {
            if (listenTogetherRoleState?.value != RoomRole.NONE) {
                Text(
                    text = if (listenTogetherRoleState?.value == RoomRole.HOST) "Hosting Listen Together" else "Listening Together",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            } else {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
            
            val playingFrom = queueTitle ?: albumTitle
            if (playerStyleName != "VIVI_NEW" && !playingFrom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playingFrom,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: com.jay.glossy.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    currentMediaId: String? = null,
    currentMediaThumbnail: String? = null,
    playerStyleName: String,
    modifier: Modifier = Modifier,
) {
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
<<<<<<< HEAD
    val thumbnailShadow by rememberPreference(ThumbnailShadowKey, true)
=======
>>>>>>> origin/main
    var skipMultiplier by remember { mutableIntStateOf(1) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                } else {
                    Modifier
                        .width(dimensions.itemWidth)
                        .fillMaxSize()
                }
            )
<<<<<<< HEAD
            // The offscreen layer has to sit *above* the horizontal padding.
            // An offscreen layer is allocated at its node's bounds, so with the
            // padding inside it the buffer came out exactly as wide as the
            // artwork — which left the artwork shadow nowhere to spread, so it
            // was sliced off at the left and right edges. Out here the layer
            // spans the padding too, and those 32dp a side are the halo's room.
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .padding(horizontal = PlayerHorizontalPadding)
=======
            .padding(horizontal = PlayerHorizontalPadding)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
>>>>>>> origin/main
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (isListenTogetherGuest) return@detectTapGestures

                        val currentPosition = playerConnection.player.currentPosition
                        val duration = playerConnection.player.duration
                        val now = System.currentTimeMillis()
                        
                        if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                            skipMultiplier++
                        } else {
                            skipMultiplier = 1
                        }
                        lastTapTime = now

                        val skipAmount = 5000 * skipMultiplier
                        val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                        if (isLeftSide) {
                            playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                            onSeek(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                        } else {
                            playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(duration))
                            onSeek(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
<<<<<<< HEAD
        val canvasThumbnailAnimation = rememberCanvasEnabled()
        val artworkShape =
            RoundedCornerShape(
                if (canvasThumbnailAnimation) rememberCanvasCornerSize() else dimensions.cornerRadius,
            )
        Box(
            modifier = Modifier
                .size(dimensions.thumbnailSize)
                .then(
                    if (thumbnailShadow) {
                        Modifier.artworkDropShadow(artworkShape)
                    } else {
                        Modifier
                    },
                )
                .clip(artworkShape)
        ) {
=======
        Box(
            modifier = Modifier
                .size(dimensions.thumbnailSize)
                .clip(RoundedCornerShape(dimensions.cornerRadius))
        ) {
            val (canvasThumbnailAnimation) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
>>>>>>> origin/main
            var canvasVideoReady by remember { mutableStateOf(false) }
            val artworkAlpha by animateFloatAsState(
                targetValue = if (canvasVideoReady) 0f else 1f,
                animationSpec = tween(250),
                label = "itemArtworkAlpha",
            )

            if (canvasThumbnailAnimation && item.mediaId == currentMediaId) {
                // Fade the static artwork out once the canvas video renders so only
                // one full-screen layer is drawn per frame.
                Box(Modifier.graphicsLayer { alpha = artworkAlpha }) {
                    if (hidePlayerThumbnail) {
                        HiddenThumbnailPlaceholder(
                            textBackgroundColor = textBackgroundColor,
                            playerStyleName = playerStyleName
                        )
                    } else {
                        val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                            currentMediaThumbnail
                        } else {
                            item.mediaMetadata.artworkUri?.toString()
                        }

                        ThumbnailImage(
                            artworkUri = artworkUriToUse,
                            cropArtwork = cropAlbumArt,
                            playerStyleName = playerStyleName
                        )
                    }
                }

                CanvasLayer(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    onVideoReady = { canvasVideoReady = it }
                )
            } else {
                if (hidePlayerThumbnail) {
                    HiddenThumbnailPlaceholder(
                        textBackgroundColor = textBackgroundColor,
                        playerStyleName = playerStyleName
                    )
                } else {
                    val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                        currentMediaThumbnail
                    } else {
                        item.mediaMetadata.artworkUri?.toString()
                    }

                    ThumbnailImage(
                        artworkUri = artworkUriToUse,
                        cropArtwork = cropAlbumArt,
                        playerStyleName = playerStyleName
                    )
                }
            }
            
            CastButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                tintColor = textBackgroundColor
            )
        }
    }
}

@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    playerStyleName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (playerStyleName == "VIVI_NEW") Modifier 
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = stringResource(R.string.hide_player_thumbnail),
            modifier = Modifier.size(120.dp)
        )
    }
}

@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    playerStyleName: String,
    modifier: Modifier = Modifier
) {
    // 1080p High-Resolution Regex logic added here
    val highResUri = remember(artworkUri) {
        artworkUri?.replace(Regex("=[wh]\\d+-[wh]\\d+.*"), "=w1080-h1080-l90-rj")
            ?.replace(Regex("-[wh]\\d+-[wh]\\d+.*"), "-w1080-h1080-l90-rj")
            ?.replace(Regex("=s\\d+.*"), "=s1080-l90-rj")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .then(
                if (playerStyleName == "VIVI_NEW") Modifier 
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(highResUri ?: artworkUri) // Use the high-res link
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = if (cropArtwork || playerStyleName == "VIVI_NEW") ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = seekDirection,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}

internal fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}
