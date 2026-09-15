/**
 * ==============================================================================
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * File: AppleStylePlayer.kt
 * Description: Premium 3-State Apple Music Style Player (Main, Lyrics, Queue)
 * Variant Support: GMS (Cast Enabled) & Normal/FOSS (Lyrics & Queue Only)
 * Lead Developer: Jay Chaudhary
 * ==============================================================================
 */

package com.jay.glossy.ui.player

import com.jay.glossy.R
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.extensions.metadata
import com.jay.glossy.extensions.togglePlayPause
import com.jay.glossy.extensions.toggleRepeatMode
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.PlayerSliderTrack
import com.jay.glossy.utils.makeTimeString
import com.metrolist.models.MediaMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// 3 States for the Premium Player
enum class ApplePlayerState {
    MAIN_CONTROLS,
    LYRICS,
    QUEUE
}

@Composable
fun AppleStylePlayer(
    mediaMetadata: MediaMetadata,
    playerBottomSheetState: BottomSheetState,
    pureBlack: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    var currentState by remember { mutableStateOf(ApplePlayerState.MAIN_CONTROLS) }

    // GMS Variant Check: Safe casting handler check
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    // If castHandler is not null, it's the GMS variant. If null, it's the Normal variant.
    val isGmsVariant = castHandler != null

    // Dynamic blur radius based on state (Heavy blur for Lyrics/Queue, slight/no blur for Main)
    val blurRadius by animateDpAsState(
        targetValue = if (currentState == ApplePlayerState.MAIN_CONTROLS) 50.dp else 120.dp,
        animationSpec = tween(600),
        label = "BlurAnimation"
    )

    // Handle back press to return to MAIN_CONTROLS before closing the bottom sheet
    BackHandler(enabled = currentState != ApplePlayerState.MAIN_CONTROLS) {
        currentState = ApplePlayerState.MAIN_CONTROLS
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
    ) {
        // --- 1. BACKGROUND LAYER ---
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaMetadata.thumbnailUrl)
                .crossfade(1000)
                .build(),
            contentDescription = "Blurred Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (currentState == ApplePlayerState.MAIN_CONTROLS) 0.8f else 0.5f
                }
                .blur(blurRadius)
        )

        // Gradient Overlay for text readability (Dark at bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // --- 2. FOREGROUND CONTENT LAYER ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Top Drag Handle (Pill)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Animated Content Switcher between Main, Lyrics, and Queue
            AnimatedContent(
                targetState = currentState,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                modifier = Modifier.weight(1f),
                label = "PlayerStateTransition"
            ) { targetState ->
                when (targetState) {
                    ApplePlayerState.MAIN_CONTROLS -> {
                        AppleMainControls(mediaMetadata = mediaMetadata)
                    }
                    ApplePlayerState.LYRICS -> {
                        val positionProvider = remember { { playerConnection.player.currentPosition } }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            InlineLyricsView(
                                mediaMetadata = mediaMetadata,
                                showLyrics = true,
                                positionProvider = positionProvider
                            )
                        }
                    }
                    ApplePlayerState.QUEUE -> {
                        AppleQueueView(
                            playerConnection = playerConnection,
                            mediaMetadata = mediaMetadata
                        )
                    }
                }
            }

            // --- 3. BOTTOM NAVIGATION BAR ---
            AppleBottomNavigationBar(
                currentState = currentState,
                isGmsVariant = isGmsVariant,
                onStateSelected = { newState ->
                    // Toggle state: if clicked again, return to main controls
                    currentState = if (currentState == newState) ApplePlayerState.MAIN_CONTROLS else newState
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMainControls(mediaMetadata: MediaMetadata) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    // Listen Together State for Mute logic
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isPlaying, playbackState) {
        while (isActive) {
            if (sliderPosition == null) {
                positionState.longValue = playerConnection.player.currentPosition
                durationState.longValue = playerConnection.player.duration.coerceAtLeast(0L)
            }
            delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        // Big Artwork with Shadow
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(elevation = 32.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Title and Artist Info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = mediaMetadata.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                Text(
                    text = mediaMetadata.artists.joinToString(", ") { it.name },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Apple Style Slim Slider
        val trackInteractionSource = remember { MutableInteractionSource() }
        val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
        val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
        val isTrackActive = isTrackDragged || isTrackPressed

        val trackHeight by animateDpAsState(
            targetValue = if (isTrackActive) 10.dp else 4.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "trackHeight"
        )

        Slider(
            value = (sliderPosition ?: positionState.longValue).toFloat(),
            valueRange = 0f..(if (durationState.longValue == C.TIME_UNSET) 0f else durationState.longValue.toFloat()),
            onValueChange = { value -> 
                if (!isListenTogetherGuest) {
                    sliderPosition = value.toLong() 
                }
            },
            onValueChangeFinished = {
                if (!isListenTogetherGuest) {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        positionState.longValue = it
                        sliderPosition = null
                    }
                }
            },
            enabled = !isListenTogetherGuest,
            interactionSource = trackInteractionSource,
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                PlayerSliderTrack(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White.copy(alpha = 0.9f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    trackHeight = trackHeight
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(sliderPosition ?: positionState.longValue),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (durationState.longValue != C.TIME_UNSET) makeTimeString(durationState.longValue) else "",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Previous Button
            val prevInteractionSource = remember { MutableInteractionSource() }
            val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
            val prevScale by animateFloatAsState(if (isPrevPressed) 0.7f else 1f, spring(0.6f, 500f), label = "prevScale")

            IconButton(
                onClick = playerConnection::seekToPrevious,
                enabled = canSkipPrevious && !isListenTogetherGuest,
                interactionSource = prevInteractionSource,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = prevScale, scaleY = prevScale)
            ) {
                Icon(
                    painter = painterResource(R.drawable.apple_skip_previous), 
                    contentDescription = "Previous",
                    tint = if (canSkipPrevious && !isListenTogetherGuest) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(42.dp)
                )
            }

            // Play/Pause Button
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(if (isPlayPressed) 0.75f else 1f, spring(0.6f, 500f), label = "playScale")

            IconButton(
                onClick = {
                    if (isListenTogetherGuest) {
                        playerConnection.toggleMute()
                    } else if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.togglePlayPause()
                    }
                },
                interactionSource = playInteractionSource,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer(scaleX = playScale, scaleY = playScale)
            ) {
                Icon(
                    painter = painterResource(
                        if (isListenTogetherGuest) {
                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        } else if (playbackState == Player.STATE_ENDED) {
                            R.drawable.replay
                        } else if (isPlaying) {
                            R.drawable.pause_applemusic
                        } else {
                            R.drawable.play_applemusic
                        }
                    ),
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            // Next Button
            val nextInteractionSource = remember { MutableInteractionSource() }
            val isNextPressed by nextInteractionSource.collectIsPressedAsState()
            val nextScale by animateFloatAsState(if (isNextPressed) 0.7f else 1f, spring(0.6f, 500f), label = "nextScale")

            IconButton(
                onClick = playerConnection::seekToNext,
                enabled = canSkipNext && !isListenTogetherGuest,
                interactionSource = nextInteractionSource,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = nextScale, scaleY = nextScale)
            ) {
                Icon(
                    painter = painterResource(R.drawable.apple_skip_next), 
                    contentDescription = "Next",
                    tint = if (canSkipNext && !isListenTogetherGuest) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppleBottomNavigationBar(
    currentState: ApplePlayerState,
    isGmsVariant: Boolean,
    onStateSelected: (ApplePlayerState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = if (isGmsVariant) Arrangement.SpaceBetween else Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Lyrics Button (Always visible)
        BottomNavButton(
            iconRes = R.drawable.lyrics, 
            isActive = currentState == ApplePlayerState.LYRICS,
            onClick = { onStateSelected(ApplePlayerState.LYRICS) }
        )

        // 2. Cast Button (Only visible in GMS Variant)
        if (isGmsVariant) {
            BottomNavButton(
                iconRes = R.drawable.cast_connected,
                isActive = false, 
                onClick = { /* Handle Cast Logic */ }
            )
        }

        // 3. Queue Button (Always visible)
        BottomNavButton(
            iconRes = R.drawable.queue_music, 
            isActive = currentState == ApplePlayerState.QUEUE,
            onClick = { onStateSelected(ApplePlayerState.QUEUE) }
        )
    }
}

@Composable
fun BottomNavButton(
    iconRes: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isPressed by buttonInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f, spring(0.6f, 500f), label = "navBtnScale")

    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        label = "navBgColor"
    )
    val tintColor by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
        label = "navTintColor"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = buttonInteractionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleQueueView(
    playerConnection: com.jay.glossy.playback.PlayerConnection,
    mediaMetadata: MediaMetadata
) {
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(currentWindowIndex) {
        if (currentWindowIndex != -1 && currentWindowIndex < queueWindows.size) {
            lazyListState.scrollToItem(currentWindowIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playing Next",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (shuffleModeEnabled) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                        .clickable { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (repeatMode != Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                        .clickable { playerConnection.player.toggleRepeatMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(
                items = queueWindows,
                key = { _, item -> item.uid.hashCode() }
            ) { index, window ->
                val isActive = index == currentWindowIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem() //  using animateItem() instead of animateItemPlacement()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            if (index == currentWindowIndex) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                playerConnection.player.playWhenReady = true
                            }
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = window.mediaItem.metadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = window.mediaItem.metadata?.title ?: "Unknown",
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        //  Using safe call correctly here
                        Text(
                            text = window.mediaItem.metadata?.artists?.joinToString { it.name } ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
