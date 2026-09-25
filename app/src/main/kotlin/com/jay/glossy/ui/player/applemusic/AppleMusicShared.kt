/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jay.glossy.R
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.listentogether.RoomRole
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.CastButton
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.ui.utils.ShowOffsetDialog
import com.jay.glossy.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class AppleMusicView { MAIN, LYRICS, QUEUE }

internal val AppleMusicTextSecondary = Color.White.copy(alpha = 0.72f)
internal val AppleMusicPillInactive = Color.White.copy(alpha = 0.24f)
internal val AppleMusicTrackInactive = Color.White.copy(alpha = 0.26f)
internal val AppleMusicTrackActive = Color.White.copy(alpha = 0.92f)

internal fun appleMusicGradientColorAt(seedColor: Color, fraction: Float): Color {
    val top = lerp(seedColor, Color.Black, 0.05f)
    val mid = lerp(seedColor, Color.Black, 0.32f)
    val bottom = lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}

@Immutable
internal data class AppleMusicTypography(
    val mainTitle: TextStyle,
    val mainArtist: TextStyle,
    val compactTitle: TextStyle,
    val compactArtist: TextStyle,
    val queueSectionHeader: TextStyle,
    val queueSectionSubtitle: TextStyle,
    val times: TextStyle,
    val badge: TextStyle,
    val footer: TextStyle,
    val idleLyric: TextStyle,
    val idleTranslated: TextStyle,
)

@Composable
internal fun rememberAppleMusicTypography(): AppleMusicTypography {
    val t = MaterialTheme.typography
    return AppleMusicTypography(
        mainTitle = t.titleMedium,
        mainArtist = t.bodyMedium,
        compactTitle = t.titleMedium.copy(fontSize = t.labelSmall.fontSize),
        compactArtist = t.bodySmall,
        queueSectionHeader = t.titleMedium,
        queueSectionSubtitle = t.bodySmall,
        times = t.bodyMedium,
        badge = t.bodySmall,
        footer = t.bodySmall,
        idleLyric = t.bodyMedium.copy(color = Color.White),
        idleTranslated = t.bodyMedium.copy(color = Color.Yellow),
    )
}

internal fun Modifier.appleMusicVerticalFadeEdges(topFade: Dp, bottomFade: Dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    topStop to Color.Black,
                    bottomStop to Color.Black,
                    1f to Color.Transparent,
                ),
                blendMode = BlendMode.DstIn,
            )
        }

@Composable
internal fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

@Composable
internal fun AppleMusicGlyphButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.appleMusicPressInflate().size(size).clip(CircleShape),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppleMusicThinSlider(
    value: Float,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val trackHeight by animateDpAsState(
        targetValue = if (pressed || dragged) 14.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "appleMusicSliderInflate",
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            interactionSource = interactionSource,
            track = {
                val fraction = value.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppleMusicTrackInactive),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(activeColor),
                    )
                }
            },
            thumb = { Spacer(Modifier.size(0.dp)) },
        )
    }
}

@Composable
internal fun AppleMusicTransportRow(modifier: Modifier = Modifier) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()

    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)?.value == RoomRole.GUEST
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val castHandler = remember(playerConnection) { try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null } }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (canSkipPrevious && !isListenTogetherGuest) playerConnection.seekToPrevious() },
            modifier = Modifier.appleMusicPressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.apple_skip_previous),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (canSkipPrevious && !isListenTogetherGuest) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
        
        Box(
            modifier = Modifier
                .appleMusicPressInflate()
                .size(76.dp)
                .clip(CircleShape)
                .clickable {
                    if (isListenTogetherGuest) {
                        playerConnection.toggleMute()
                    } else if (isCasting) {
                        if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                    } else if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.togglePlayPause()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = effectiveIsPlaying, label = "appleMusicPlayPauseIcon") { playing ->
                Icon(
                    painter = painterResource(
                        if (isListenTogetherGuest) {
                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        } else if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            R.drawable.replay
                        } else if (playing) {
                            R.drawable.pause_applemusic
                        } else {
                            R.drawable.play_applemusic
                        }
                    ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(66.dp),
                )
            }
        }
        
        IconButton(
            onClick = { if (canSkipNext && !isListenTogetherGuest) playerConnection.seekToNext() },
            modifier = Modifier.appleMusicPressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.apple_skip_next),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (canSkipNext && !isListenTogetherGuest) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

@Composable
internal fun AppleMusicVolumeRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    
    val systemVolume by produceState(initialValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        awaitDispose { context.unregisterReceiver(receiver) }
    }

    val volumeInteractionSource = remember { MutableInteractionSource() }
    val isVolumeDragged by volumeInteractionSource.collectIsDraggedAsState()
    val isVolumePressed by volumeInteractionSource.collectIsPressedAsState()
    val isVolumeActive = isVolumeDragged || isVolumePressed

    var dragVolume by remember { mutableFloatStateOf(systemVolume) }
    LaunchedEffect(systemVolume) { if (!isVolumeActive) dragVolume = systemVolume }
    val volume = if (isVolumeActive) dragVolume else systemVolume

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(R.drawable.volume_mute), contentDescription = null, tint = AppleMusicTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(18.dp), contentAlignment = Alignment.Center) {
            AppleMusicThinSlider(
                value = volume,
                activeColor = AppleMusicTrackActive,
                onValueChange = { newVol ->
                    dragVolume = newVol
                    scope.launch(Dispatchers.Default) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (newVol * maxSystemVolume).roundToInt(), 0)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(painter = painterResource(R.drawable.volume_up), contentDescription = null, tint = AppleMusicTextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun AppleMusicDockButton(
    icon: Int,
    active: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .appleMusicPressInflate()
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) activeColor else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = when {
                !enabled -> Color.White.copy(alpha = 0.4f)
                active -> activeContentColor
                else -> Color.White.copy(alpha = 0.85f)
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun AppleMusicDock(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleMusicDockButton(
            icon = R.drawable.lyrics,
            active = viewState == AppleMusicView.LYRICS,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            enabled = lyricsAvailable,
            onClick = {
                onSelectView(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS)
            },
        )
        Box(modifier = Modifier.appleMusicPressInflate().size(40.dp), contentAlignment = Alignment.Center) {
            CastButton(
                modifier = Modifier.size(22.dp),
                tintColor = Color.White
            )
        }
        AppleMusicDockButton(
            icon = R.drawable.queue_music,
            active = viewState == AppleMusicView.QUEUE,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            onClick = {
                onSelectView(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE)
            },
        )
    }
}

@Composable
internal fun AppleMusicBottomCluster(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    withFrost: Boolean = true,
) {
    val localDensity = LocalDensity.current
    val clusterContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        
        val playerConnection = LocalPlayerConnection.current
        var sliderPosition by remember { mutableStateOf<Long?>(null) }
        
        val displayPosition = sliderPosition ?: position
        val safeDuration = if (duration > 0) duration else 1L
        val sliderValue = (displayPosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

        Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
            AppleMusicThinSlider(
                value = sliderValue,
                activeColor = AppleMusicTrackActive,
                onValueChange = { sliderPosition = (it * safeDuration).toLong() },
                onValueChangeFinished = {
                    sliderPosition?.let { playerConnection?.player?.seekTo(it) }
                    sliderPosition = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = makeTimeString(displayPosition), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text(text = "-" + makeTimeString(safeDuration - displayPosition), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        AppleMusicTransportRow()
        Spacer(modifier = Modifier.height(14.dp))
        AppleMusicVolumeRow()
        Spacer(modifier = Modifier.height(14.dp))
        
            AppleMusicDock(
                viewState = viewState,
                onSelectView = onSelectView,
                lyricsAvailable = lyricsAvailable,
                activeColor = activeColor,
                activeContentColor = activeContentColor,
            )
            
            Spacer(
                modifier = Modifier.height(
                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + 12.dp,
                ),
            )
        }
    }

    if (withFrost) {
        FrostedBottomPanel(modifier = modifier.fillMaxWidth()) {
            clusterContent()
        }
    } else {
        clusterContent()
    }
}

/**
 * Premium frosted-glass sheet for the bottom of the Apple Music style player:
 * a heavily blurred, darkened crop of the current artwork sits behind the
 * controls with a hairline glass edge on top.
 */
@Composable
internal fun FrostedBottomPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val context = LocalContext.current
    val artworkUrl = remember(mediaMetadata?.thumbnailUrl) {
        mediaMetadata?.thumbnailUrl?.toHighRes()
    }

    Box(modifier = modifier.clip(shape)) {
        if (artworkUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.4f
                        scaleY = 1.4f
                    }
                    .blur(56.dp)
                    .alpha(0.55f),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.22f),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
        )
        content()
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
                            mediaMetadataProvider = { mediaMetadata!! },
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

@Composable
internal fun AppleMusicCompactHeader(
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaMetadata?.thumbnailUrl)
                .crossfade(300)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata?.title ?: "",
                style = typography.compactTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mediaMetadata?.explicit == true) {
                    Icon(
                        painter = painterResource(R.drawable.explicit), 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    style = typography.compactArtist,
                    color = AppleMusicTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}
