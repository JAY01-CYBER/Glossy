/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.jay.glossy.ui.player

import com.jay.glossy.R

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
<<<<<<< HEAD
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
=======
>>>>>>> origin/main
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
<<<<<<< HEAD
import androidx.compose.foundation.Canvas
=======
>>>>>>> origin/main
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
<<<<<<< HEAD
import androidx.compose.foundation.layout.fillMaxHeight

=======
>>>>>>> origin/main
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.CropAlbumArtKey
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.MiniPlayerBackgroundStyle
import com.jay.glossy.constants.MiniPlayerBackgroundStyleKey
import com.jay.glossy.constants.MiniPlayerHeight
import com.jay.glossy.constants.PureBlackMiniPlayerKey
import com.jay.glossy.constants.SwipeSensitivityKey
import com.jay.glossy.constants.SwipeThumbnailKey
import com.jay.glossy.constants.ThumbnailCornerRadius
<<<<<<< HEAD
import com.jay.glossy.constants.ThumbnailShadowKey
import com.jay.glossy.constants.ActiveDesignStyle
import com.jay.glossy.constants.DesignStyle
import com.jay.glossy.constants.MiniPlayerStyle
import com.jay.glossy.constants.MiniPlayerStyleKey
import com.jay.glossy.constants.MiniPlayerPlayingAnimation
import com.jay.glossy.constants.MiniPlayerPlayingAnimationKey
=======
import com.jay.glossy.constants.MiniPlayerStyle
import com.jay.glossy.constants.MiniPlayerStyleKey
>>>>>>> origin/main
import com.jay.glossy.db.entities.ArtistEntity
import com.jay.glossy.listentogether.ListenTogetherManager
import com.metrolist.models.MediaMetadata
import com.jay.glossy.playback.CastConnectionHandler
import com.jay.glossy.playback.PlayerConnection
import com.jay.glossy.ui.screens.settings.DarkMode
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.utils.joinToArtistString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.ui.component.Icon as MIcon
import androidx.compose.ui.draw.blur
import com.jay.glossy.ui.theme.PlayerColorExtractor
<<<<<<< HEAD
import com.jay.glossy.ui.component.BlurredArtworkBackdrop
import com.jay.glossy.ui.component.GlossyIconAction
import com.jay.glossy.ui.component.GlossyThumb
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.ArtworkNotesOverlay
import com.jay.glossy.ui.component.NowPlayingAnimationIndicator
import com.jay.glossy.ui.component.rememberAmbientMotionEnabled
import com.jay.glossy.ui.theme.GlossyPalette
=======
import com.jay.glossy.ui.component.LocalMenuState
>>>>>>> origin/main
import com.jay.glossy.ui.menu.AddToPlaylistDialog

@Stable
class ProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
<<<<<<< HEAD
    val miniPlayerStylePreference by rememberEnumPreference(
        MiniPlayerStyleKey,
        defaultValue = MiniPlayerStyle.MODERN
    )

    // The design style is pinned to classic, so the stored mini player style
    // always wins. While the new design was selectable it forced Studio.
    val miniPlayerStyle = if (ActiveDesignStyle == DesignStyle.TEAL) {
        MiniPlayerStyle.STUDIO
    } else {
        miniPlayerStylePreference
    }
=======
    val miniPlayerStyle by rememberEnumPreference(MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.MODERN)
>>>>>>> origin/main
    val pureBlack by rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)

    val progressState = remember { ProgressState(positionState, durationState) }

    when (miniPlayerStyle) {
<<<<<<< HEAD
        MiniPlayerStyle.STUDIO -> {
            GlossyBarMiniPlayer(
                positionState = positionState,
                durationState = durationState,
                modifier = modifier,
                pureBlack = pureBlack,
                onClick = onClick,
            )
        }
=======
>>>>>>> origin/main
        MiniPlayerStyle.GLOSSY_SPECIAL -> {
            GlossySpecialEditionMiniPlayer(
                progressState = progressState,
                modifier = modifier,
                pureBlack = pureBlack,
                expandProgress = 0f,
                onClick = onClick
            )
        }
        MiniPlayerStyle.MODERN -> {
            NewMiniPlayer(
                progressState = progressState,
                modifier = modifier,
                onClick = onClick,
            )
        }
        MiniPlayerStyle.LEGACY -> {
            Box(modifier = modifier.fillMaxWidth()) {
                LegacyMiniPlayer(
                    progressState = progressState,
                    modifier = Modifier.align(Alignment.Center),
                    onClick = onClick,
                )
            }
        }
    }
}

// ============================================================================
<<<<<<< HEAD
// New design system: compact bar with a teal progress hairline
// ============================================================================

@Composable
private fun GlossyBarMiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)

    // The bar used to paint a flat [GlossyPalette.MiniBar] and ignore the
    // mini player background setting entirely. It now honours the same styles
    // as the Modern bar.
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val (gradientColors, onGradientColorsChange) = remember { mutableStateOf<List<Color>>(emptyList()) }
    MiniPlayerColorExtractor(
        mediaMetadata = mediaMetadata,
        miniPlayerBackground = miniPlayerBackground,
        onGradientColorsChange = onGradientColorsChange,
    )

    val isFavorite = currentSong?.song?.liked == true
    val duration = durationState.longValue
    val progress = if (duration > 0) {
        (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }
    val artistText = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty()

    // Every one of these backgrounds is dark, so the bar switches to white
    // content whenever a style other than the theme surface is painted.
    val styledBackground = miniPlayerBackground != MiniPlayerBackgroundStyle.DEFAULT || pureBlack
    val containerColor = when (miniPlayerBackground) {
        MiniPlayerBackgroundStyle.DEFAULT -> if (pureBlack) Color.Black else GlossyPalette.MiniBar
        MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.25f)
        MiniPlayerBackgroundStyle.PURE_BLACK -> Color.Black
        else -> Color.Transparent
    }
    val primaryTextColor = if (styledBackground) Color.White else GlossyPalette.TextPrimary
    val secondaryTextColor = if (styledBackground) Color.White.copy(alpha = 0.75f) else GlossyPalette.TextSecondary

    // Swipe engine shared with the Modern bar: drag offset + skip cues +
    // velocity/distance song change. Studio previously never applied its
    // offset anywhere, so swipes did nothing.
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false,
    ) { offsetX ->
        val studioPlayingAnimation by rememberEnumPreference(
            MiniPlayerPlayingAnimationKey,
            defaultValue = MiniPlayerPlayingAnimation.BARS,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .clickable(onClick = onClick),
        ) {
            MiniPlayerStyleBackground(
                style = miniPlayerBackground,
                colors = gradientColors,
                artworkUrl = mediaMetadata?.thumbnailUrl,
                modifier = Modifier.matchParentSize(),
            )

            // Thin progress hairline pinned to the top edge of the card. It is a
            // sibling of the content row rather than its parent, so it never
            // pushes the row down or indents it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(if (styledBackground) Color.White else GlossyPalette.Accent),
                )
            }

            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        GlossyThumb(
                            seed = mediaMetadata?.id,
                            size = 48.dp,
                            corner = 12.dp,
                            imageUrl = mediaMetadata?.thumbnailUrl,
                        )
                        if (studioPlayingAnimation == MiniPlayerPlayingAnimation.NOTES) {
                            ArtworkNotesOverlay(
                                isPlaying = isPlaying,
                                color = Color.White,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (studioPlayingAnimation == MiniPlayerPlayingAnimation.BARS) {
                                NowPlayingAnimationIndicator(
                                    animation = studioPlayingAnimation,
                                    isPlaying = isPlaying,
                                    color = primaryTextColor,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            }
                            Text(
                                text = mediaMetadata?.title.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (artistText.isNotBlank()) {
                            Text(
                                text = artistText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    GlossyIconAction(
                        icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                        contentDescription = null,
                        tint = if (isFavorite) {
                            if (styledBackground) Color(0xFFFF7A9A) else GlossyPalette.Accent
                        } else {
                            primaryTextColor
                        },
                        buttonSize = 36.dp,
                        iconSize = 20.dp,
                        onClick = { playerConnection.toggleLike() },
                    )
                    GlossyIconAction(
                        icon = if (isPlaying) R.drawable.pause else R.drawable.play,
                        contentDescription = null,
                        tint = primaryTextColor,
                        buttonSize = 36.dp,
                        iconSize = 20.dp,
                        onClick = { playerConnection.togglePlayPause() },
                    )
                    GlossyIconAction(
                        icon = R.drawable.skip_next,
                        contentDescription = null,
                        tint = primaryTextColor,
                        buttonSize = 36.dp,
                        iconSize = 20.dp,
                        onClick = { playerConnection.seekToNext() },
                    )
                }
            }
    }
}

/**
 * Paints a [MiniPlayerBackgroundStyle] behind a mini player bar. Only the
 * artwork-derived styles are drawn here ([MiniPlayerBackgroundStyle.BLUR],
 * [MiniPlayerBackgroundStyle.GRADIENT] and
 * [MiniPlayerBackgroundStyle.GLOW]); the flat fills are the caller's job, so
 * the bar keeps its own base colour for those.
 */
@Composable
private fun MiniPlayerStyleBackground(
    style: MiniPlayerBackgroundStyle,
    colors: List<Color>,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    when (style) {
        MiniPlayerBackgroundStyle.BLUR -> {
            // CPU blur, so this is a real Gaussian blur on every Android
            // version instead of nothing below API 31.
            artworkUrl?.let { url ->
                BlurredArtworkBackdrop(
                    url = url,
                    blurStrength = 0.82f,
                    modifier = modifier,
                )
            }
            // Scrim for the white content — and the whole background when the
            // track has no artwork to blur.
            Box(
                modifier = modifier.background(
                    Color.Black.copy(alpha = if (artworkUrl != null) 0.5f else 0.72f),
                ),
            )
        }

        MiniPlayerBackgroundStyle.GRADIENT -> {
            val palette = colors.ifEmpty {
                listOf(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.surfaceContainer,
                )
            }
            Box(
                modifier = modifier
                    .background(Brush.horizontalGradient(palette))
                    .background(Color.Black.copy(alpha = 0.15f)),
            )
        }

        MiniPlayerBackgroundStyle.GLOW -> {
            GlowAnimatedBackground(colors = colors, modifier = modifier)
            // Scrim so the title and controls stay readable over the glow.
            Box(modifier = modifier.background(Color.Black.copy(alpha = 0.32f)))
        }

        else -> Unit
    }
}

// ============================================================================
=======
>>>>>>> origin/main
// EXACT M3-PLAY APPLE MUSIC STYLE UI & LOGIC
// ============================================================================

@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    content: @Composable (Float) -> Unit
) {
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier.background(
                        if (pureBlack) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    baseModifier.padding(horizontal = 12.dp)
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                    velocity > velocityThreshold
                                ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    val isRightSwipe = currentOffset > 0
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        content(offsetXAnimatable.value)

        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.apple_skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayerColorExtractor(
    mediaMetadata: MediaMetadata?,
    miniPlayerBackground: MiniPlayerBackgroundStyle,
    onGradientColorsChange: (List<Color>) -> Unit
) {
    val context = LocalContext.current
    val fallbackColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground) {
<<<<<<< HEAD
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT ||
            miniPlayerBackground == MiniPlayerBackgroundStyle.GLOW) {
=======
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT || 
            miniPlayerBackground == MiniPlayerBackgroundStyle.ANIMATED_MESH) {
>>>>>>> origin/main
            
            val currentMetadata = mediaMetadata
            if (currentMetadata?.thumbnailUrl != null) {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(8)
                                    .resizeBitmapArea(100 * 100)
                                    .generate()
                            }
                            val extractedColors = PlayerColorExtractor.extractGradientColors(
                                palette = palette,
                                fallbackColor = fallbackColor
                            )
                            withContext(Dispatchers.Main) { onGradientColorsChange(extractedColors) }
                        }
                    }
                }
            }
        } else {
            onGradientColorsChange(emptyList())
        }
    }
}

@Composable
private fun GlossySpecialEditionMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    expandProgress: Float,
    onClick: () -> Unit = {}
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val isLoading = playbackState == Player.STATE_BUFFERING
    val haptic = LocalHapticFeedback.current
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()

    val castHandler = remember(playerConnection) { try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null } }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey, 
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT
    )
    val (gradientColors, onGradientColorsChange) = remember { mutableStateOf<List<Color>>(emptyList()) }

    MiniPlayerColorExtractor(
        mediaMetadata = mediaMetadata,
        miniPlayerBackground = miniPlayerBackground,
        onGradientColorsChange = onGradientColorsChange
    )

    val dominantColor = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.surfaceVariant

    val isDarkBg = miniPlayerBackground != MiniPlayerBackgroundStyle.DEFAULT || pureBlack
    val textColor = if (isDarkBg) Color.White else Color.Black
    val secondaryTextColor = if (isDarkBg) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)

    val infiniteTransition = rememberInfiniteTransition(label = "reflection")
    val reflectionOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reflectionOffset"
    )

    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "playScaleAnim"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progressState.progress,
        animationSpec = tween(500, easing = LinearEasing),
        label = "progressAnim"
    )

    val boxInteractionSource = remember { MutableInteractionSource() }
    val isBoxPressed by boxInteractionSource.collectIsPressedAsState()
    val boxScale by animateFloatAsState(
        targetValue = if (isBoxPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "boxScaleAnim"
    )

    SwipeableMiniPlayerBox(
        modifier = modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
<<<<<<< HEAD
        val thumbnailShadow by rememberPreference(ThumbnailShadowKey, true)
=======
>>>>>>> origin/main
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) 
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .shadow(
                    elevation = 20.dp, 
                    shape = RoundedCornerShape(32.dp), 
                    spotColor = dominantColor.copy(alpha = 0.5f),
                    ambientColor = dominantColor.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(32.dp)
                )
                .graphicsLayer {
                    alpha = 1f - (expandProgress * 2f).coerceIn(0f, 1f)
                    scaleX = boxScale
                    scaleY = boxScale
                }
                .clickable(
                    interactionSource = boxInteractionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            // BACKGROUND LAYER
            Box(modifier = Modifier.matchParentSize()) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect
                                    .createBlurEffect(50f, 50f, android.graphics.Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (pureBlack) Color(0xFF1C1C1E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f))
                        .background(dominantColor.copy(alpha = 0.15f)) 
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                                    startY = 0f,
                                    endY = size.height * 0.4f
                                )
                            )
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.2f), Color.Transparent),
                                    start = Offset(reflectionOffset, reflectionOffset),
                                    end = Offset(reflectionOffset + 400f, reflectionOffset + 400f)
                                )
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f)) 
                        }
                )
            }

            // FOREGROUND LAYER
            Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                translationY = expandProgress * 50.dp.toPx()
                alpha = 1f - (expandProgress * 3f).coerceIn(0f, 1f)
                val scale = 1f - (0.05f * expandProgress)
                scaleX = scale
                scaleY = scale
            }) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).alpha(0.5f),
                    color = textColor,
                    trackColor = Color.Transparent,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                ) {
<<<<<<< HEAD

                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaMetadata?.thumbnailUrl)
                                .crossfade(500)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (thumbnailShadow) {
                                        // Scaled down for a 50dp tile: the full-size
                                        // player shadow would swamp it.
                                        Modifier.artworkDropShadow(
                                            shape = RoundedCornerShape(14.dp),
                                            radius = 8.dp,
                                            offsetY = 3.dp,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clip(RoundedCornerShape(14.dp))
                        )
                        val specialArtworkNotes by rememberEnumPreference(
                            MiniPlayerPlayingAnimationKey,
                            defaultValue = MiniPlayerPlayingAnimation.BARS,
                        )
                        if (specialArtworkNotes == MiniPlayerPlayingAnimation.NOTES) {
                            ArtworkNotesOverlay(
                                isPlaying = effectiveIsPlaying,
                                color = Color.White,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    }
=======
        
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mediaMetadata?.thumbnailUrl)
                            .crossfade(500)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                            .clip(RoundedCornerShape(14.dp))
                    )
>>>>>>> origin/main
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    AnimatedContent(
<<<<<<< HEAD
                        // Key on the song ID so the slide animation only fires when
                        // the track actually changes — not when metadata is re-emitted
                        // unchanged after a seek or position update.
                        targetState = mediaMetadata?.id,
                        contentKey = { it },
=======
                        targetState = mediaMetadata,
>>>>>>> origin/main
                        transitionSpec = {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        label = "trackInfoAnimation"
<<<<<<< HEAD
                    ) { _ ->
                        val metadata = mediaMetadata
                        val specialPlayingAnimation by rememberEnumPreference(
                            MiniPlayerPlayingAnimationKey,
                            defaultValue = MiniPlayerPlayingAnimation.BARS,
                        )
=======
                    ) { metadata ->
>>>>>>> origin/main
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0f to Color.Transparent,
                                            0.05f to Color.Black,
                                            0.95f to Color.Black,
                                            1f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
<<<<<<< HEAD
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (specialPlayingAnimation == MiniPlayerPlayingAnimation.BARS) {
                                    NowPlayingAnimationIndicator(
                                        animation = specialPlayingAnimation,
                                        isPlaying = effectiveIsPlaying,
                                        color = textColor,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                }
                                Text(
                                    text = metadata?.title ?: "Unknown",
                                    color = textColor,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).basicMarquee()
                                )
                            }
=======
                            Text(
                                text = metadata?.title ?: "Unknown",
                                color = textColor,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
>>>>>>> origin/main
                            val artistText = metadata?.artists?.filter { it.name.isNotBlank() }?.joinToString(", ") { it.name } ?: "Unknown Artist"
                            Text(
                                text = artistText,
                                color = secondaryTextColor,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(playScale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isListenTogetherGuest) {
                                    playerConnection.toggleMute()
                                } else if (isCasting) {
                                    if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                } else if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.togglePlayPause()
                                }
                            }
                    ) {
                        Crossfade(targetState = isLoading, label = "playPauseCrossfade") { loading ->
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textColor, strokeWidth = 2.dp)
                            } else {
                                val iconRes = if (isListenTogetherGuest) {
                                    if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                } else if (playbackState == Player.STATE_ENDED) {
                                    R.drawable.replay
                                } else if (effectiveIsPlaying) {
                                    R.drawable.pause_applemusic
                                } else {
                                    R.drawable.play_applemusic
                                }

                                AnimatedContent(
                                    targetState = iconRes,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.7f) + fadeIn(tween(150))).togetherWith(
                                            scaleOut(targetScale = 0.7f) + fadeOut(tween(150))
                                        )
                                    },
                                    label = "playPauseAnimation"
                                ) { targetIcon ->
                                    Icon(
                                        painter = painterResource(targetIcon),
                                        contentDescription = "Play/Pause",
                                        tint = textColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        enabled = canSkipNext && !isListenTogetherGuest,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            playerConnection.seekToNext()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.apple_skip_next),
                            contentDescription = "Next",
                            tint = textColor.copy(alpha = if (canSkipNext && !isListenTogetherGuest) 1f else 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// NEW MINI PLAYER DESIGN (GLOSSY ORIGINAL)
// ============================================================================

@Composable
private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current

    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)

    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground) {
        gradientColors = emptyList()
<<<<<<< HEAD
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT ||
            miniPlayerBackground == MiniPlayerBackgroundStyle.GLOW) {
=======
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT || 
            miniPlayerBackground == MiniPlayerBackgroundStyle.ANIMATED_MESH) {
>>>>>>> origin/main
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
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                        val extracted = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = 0xFF000000.toInt(),
                        )
                        withContext(Dispatchers.Main) {
                            gradientColors = extracted
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            gradientColors = emptyList()
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val backgroundColor = when (miniPlayerBackground) {
        MiniPlayerBackgroundStyle.DEFAULT    -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.25f)
        MiniPlayerBackgroundStyle.BLUR       -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.GRADIENT   -> MaterialTheme.colorScheme.surfaceContainer
<<<<<<< HEAD
        MiniPlayerBackgroundStyle.GLOW       -> MaterialTheme.colorScheme.surfaceContainer
=======
        MiniPlayerBackgroundStyle.ANIMATED_MESH -> MaterialTheme.colorScheme.surfaceContainer
>>>>>>> origin/main
        MiniPlayerBackgroundStyle.PURE_BLACK -> Color.Black
    }
    val forceLightColors = !useDarkTheme && (miniPlayerBackground == MiniPlayerBackgroundStyle.PURE_BLACK ||
            miniPlayerBackground == MiniPlayerBackgroundStyle.BLUR ||
            miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT ||
<<<<<<< HEAD
            miniPlayerBackground == MiniPlayerBackgroundStyle.GLOW)
=======
            miniPlayerBackground == MiniPlayerBackgroundStyle.ANIMATED_MESH)
>>>>>>> origin/main

    val primaryColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.outline
    val onSurfaceColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.onSurface
    val errorColor = if (forceLightColors) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.error

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp)
                .let { baseModifier ->
                    if (swipeThumbnail) {
                        baseModifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val tryingToSwipeRight = adjustedDragAmount > 0
                                    val tryingToSwipeLeft = adjustedDragAmount < 0
                                    val allowLeft = tryingToSwipeLeft && canSkipNext
                                    val allowRight = tryingToSwipeRight && canSkipPrevious

                                    val canReturnToCenter =
                                        (tryingToSwipeRight && !canSkipPrevious && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNext && offsetXAnimatable.value > 0)

                                    if (allowLeft || allowRight || canReturnToCenter) {
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val minDistanceThreshold = 50f
                                    val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                    val shouldChangeSong =
                                        (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                            (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                    if (shouldChangeSong) {
                                        if (currentOffset > 0 && canSkipPrevious) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (currentOffset <= 0 && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                    .height(64.dp)
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .clip(RoundedCornerShape(32.dp))
                    .background(color = backgroundColor)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    ),
        ) {
            when (miniPlayerBackground) {
                MiniPlayerBackgroundStyle.BLUR -> {
<<<<<<< HEAD
                    mediaMetadata?.thumbnailUrl?.let { url ->
                        // CPU blur: no API-level guard, and no stretched
                        // low-resolution thumbnail showing through.
                        BlurredArtworkBackdrop(
                            url = url,
                            blurStrength = 0.8f,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.58f)),
                        )
=======
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        mediaMetadata?.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(60.dp),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                            )
                        }
>>>>>>> origin/main
                    }
                }
                MiniPlayerBackgroundStyle.GRADIENT -> {
                    val colors = if (gradientColors.isNotEmpty()) gradientColors
                    else listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainer,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(colors)
                            )
                            .background(Color.Black.copy(alpha = 0.15f)),
                    )
                }
<<<<<<< HEAD
                MiniPlayerBackgroundStyle.GLOW -> {
                    val colors = if (gradientColors.isNotEmpty()) gradientColors
                    else listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.secondary,
                    )
                    GlowAnimatedBackground(colors = colors)
                    // Scrim so the title/controls stay readable over the glow.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f)),
                    )
=======
                MiniPlayerBackgroundStyle.ANIMATED_MESH -> {
                    // AnimatedMeshBackground is expected to be present in your project
>>>>>>> origin/main
                }
                else -> {}
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                NewMiniPlayerPlayButton(
                    progressState = progressState,
                    playbackState = playbackState,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    mediaMetadata = mediaMetadata,
                    primaryColor = primaryColor,
                    outlineColor = outlineColor,
                    listenTogetherManager = listenTogetherManager,
                )

                Spacer(modifier = Modifier.width(16.dp))

                NewMiniPlayerSongInfo(
                    mediaMetadata = mediaMetadata,
                    onSurfaceColor = onSurfaceColor,
                    errorColor = errorColor,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(12.dp))

                if (isCasting) {
                    Icon(
                        painter = painterResource(R.drawable.cast_connected),
                        contentDescription = "Casting",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                mediaMetadata?.artists?.firstOrNull()?.id?.let { artistId ->
                    SubscribeButton(
                        artistId = artistId,
                        metadata = mediaMetadata!!,
                        primaryColor = primaryColor,
                        outlineColor = outlineColor,
                        onSurfaceColor = onSurfaceColor,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                mediaMetadata?.let { metadata ->
                    AddToPlaylistButton(
                        onClick = {
                            menuState.show {
                                AddToPlaylistDialog(
                                    isVisible = true,
                                    onGetSong = { listOf(metadata.id) },
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                        outlineColor = outlineColor,
                        onSurfaceColor = onSurfaceColor,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                mediaMetadata?.let { FavoriteButton(
                    songId = it.id,
                    errorColor = errorColor,
                    outlineColor = outlineColor,
                    onSurfaceColor = onSurfaceColor,
                )
                }
            }
        }
    }
}

@Composable
private fun NewMiniPlayerPlayButton(
    progressState: ProgressState,
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
    listenTogetherManager: ListenTogetherManager?,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val trackColor = outlineColor.copy(alpha = 0.2f)
    val strokeWidth = 3.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(48.dp)
                .drawWithContent {
                    drawContent()
                    val progress = progressState.progress
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val startAngle = -90f
                    val sweepAngle = 360f * progress
                    val diameter = size.minDimension
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        if (isListenTogetherGuest) {
                            playerConnection.toggleMute()
                            return@clickable
                        }
                        if (isCasting) {
                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                        } else if (playbackState == Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
        ) {
            mediaMetadata?.let { metadata ->
                val thumbnailUrl =
                    remember(metadata.thumbnailUrl) {
                        metadata.thumbnailUrl?.resize(120, 120)
                    }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }

            if (isListenTogetherGuest && isMuted ||
                (!isListenTogetherGuest && (!effectiveIsPlaying || playbackState == Player.STATE_ENDED))
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                )
                Icon(
                    painter =
                        painterResource(
                            if (isListenTogetherGuest) {
                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            } else if (playbackState == Player.STATE_ENDED) {
                                R.drawable.replay
                            } else {
                                R.drawable.play
                            },
                        ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
<<<<<<< HEAD
        val artworkNotesAnimation by rememberEnumPreference(
            MiniPlayerPlayingAnimationKey,
            defaultValue = MiniPlayerPlayingAnimation.BARS,
        )
        if (artworkNotesAnimation == MiniPlayerPlayingAnimation.NOTES) {
            ArtworkNotesOverlay(
                isPlaying = effectiveIsPlaying,
                color = Color.White,
                modifier = Modifier.matchParentSize(),
            )
        }
=======
>>>>>>> origin/main
    }
}

@Composable
private fun NewMiniPlayerSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }
<<<<<<< HEAD
    val isPlaying by LocalPlayerConnection.current?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
=======
>>>>>>> origin/main

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        mediaMetadata?.let { metadata ->
<<<<<<< HEAD
            // NOTES now rises off the artwork thumbnail itself; only the bars
            // marker lives beside the title, so the NOTES pick leaves no gap.
            val playingAnimation by rememberEnumPreference(
                MiniPlayerPlayingAnimationKey,
                defaultValue = MiniPlayerPlayingAnimation.BARS,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playingAnimation == MiniPlayerPlayingAnimation.BARS) {
                    NowPlayingAnimationIndicator(
                        animation = playingAnimation,
                        isPlaying = isPlaying,
                        color = onSurfaceColor,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Text(
                    text = metadata.title,
                    color = onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier =
                        Modifier
                            .weight(1f)
                            .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                )
            }
=======
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
>>>>>>> origin/main
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (metadata.explicit) MIcon.Explicit()
                 if (metadata.artists.any { it.name.isNotBlank() }) {
                     Text(
                         text = metadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                         color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                }
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.error_playing),
                    color = errorColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// LEGACY MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun LegacyMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val pureBlack by rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)

    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .then(if (isTabletLandscape) Modifier.width(500.dp) else Modifier.fillMaxWidth())
                .height(MiniPlayerHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    if (pureBlack && isSystemInDarkTheme()) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ).clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ).let { baseModifier ->
                    if (swipeThumbnail) {
                        baseModifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val tryingToSwipeRight = adjustedDragAmount > 0
                                    val tryingToSwipeLeft = adjustedDragAmount < 0
                                    val allowLeft = tryingToSwipeLeft && canSkipNext
                                    val allowRight = tryingToSwipeRight && canSkipPrevious

                                    val canReturnToCenter =
                                        (tryingToSwipeRight && !canSkipPrevious && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNext && offsetXAnimatable.value > 0)

                                    if (allowLeft || allowRight || canReturnToCenter) {
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val minDistanceThreshold = 50f
                                    val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                    val shouldChangeSong =
                                        (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                            (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                    if (shouldChangeSong) {
                                        if (currentOffset > 0 && canSkipPrevious) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (currentOffset <= 0 && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                                },
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .drawWithContent {
                        val progress = progressState.progress
                        drawRect(trackColor)
                        drawRect(primaryColor, size = Size(size.width * progress, size.height))
                    },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .padding(end = 12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    LegacyMiniMediaInfo(
                        mediaMetadata = it,
                        pureBlack = pureBlack,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            LegacyPlayPauseButton(
                playbackState = playbackState,
                isCasting = isCasting,
                castHandler = castHandler,
                playerConnection = playerConnection,
                listenTogetherManager = listenTogetherManager,
            )

            IconButton(
                enabled = canSkipNext && !isListenTogetherGuest,
                onClick = if (isListenTogetherGuest) ({}) else ({ playerConnection.seekToNext() }),
            ) {
                Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null)
            }
        }

        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier =
                    Modifier
                        .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next,
                        ),
                    contentDescription = null,
                    tint =
                        primaryColor.copy(
                            alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f),
                        ),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun LegacyPlayPauseButton(
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    IconButton(
        onClick = {
            if (isListenTogetherGuest) {
                playerConnection.toggleMute()
                return@IconButton
            }
            if (isCasting) {
                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
            } else if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.togglePlayPause()
            }
        },
    ) {
        Icon(
            painter =
                painterResource(
                    when {
                        isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        playbackState == Player.STATE_ENDED -> R.drawable.replay
                        effectiveIsPlaying -> R.drawable.pause
                        else -> R.drawable.play
                    },
                ),
            contentDescription = null,
        )
    }
}

@Composable
private fun LegacyMiniMediaInfo(
    mediaMetadata: MediaMetadata,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
<<<<<<< HEAD
    val thumbnailShadow by rememberPreference(ThumbnailShadowKey, true)

    val legacyIsPlaying by LocalPlayerConnection.current?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val legacyPlayingAnimation by rememberEnumPreference(
        MiniPlayerPlayingAnimationKey,
        defaultValue = MiniPlayerPlayingAnimation.BARS,
    )
=======
>>>>>>> origin/main

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
<<<<<<< HEAD
        // Unclipped anchor: the clipped artwork lives inside, the notes rise
        // as its sibling so they can drift past the artwork's top edge.
=======
>>>>>>> origin/main
        Box(
            modifier =
                Modifier
                    .padding(6.dp)
<<<<<<< HEAD
                    .size(48.dp),
            contentAlignment = Alignment.BottomCenter,
=======
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
>>>>>>> origin/main
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
<<<<<<< HEAD
                        .then(
                            if (thumbnailShadow) {
                                Modifier.artworkDropShadow(
                                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                                    radius = 8.dp,
                                    offsetY = 3.dp,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                val thumbnailUrl =
                    remember(mediaMetadata.thumbnailUrl) {
                        mediaMetadata.thumbnailUrl?.resize(144, 144)
                    }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

                androidx.compose.animation.AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = if (pureBlack) Color.Black else Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
            if (legacyPlayingAnimation == MiniPlayerPlayingAnimation.NOTES) {
                ArtworkNotesOverlay(
                    isPlaying = legacyIsPlaying,
                    color = Color.White,
                    modifier = Modifier.matchParentSize(),
                )
=======
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            val thumbnailUrl =
                remember(mediaMetadata.thumbnailUrl) {
                    mediaMetadata.thumbnailUrl?.resize(144, 144)
                }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            androidx.compose.animation.AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = if (pureBlack) Color.Black else Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
>>>>>>> origin/main
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
<<<<<<< HEAD
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (legacyPlayingAnimation == MiniPlayerPlayingAnimation.BARS) {
                    NowPlayingAnimationIndicator(
                        animation = legacyPlayingAnimation,
                        isPlaying = legacyIsPlaying,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    text = mediaMetadata.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).basicMarquee(),
                )
            }
=======
            Text(
                text = mediaMetadata.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
>>>>>>> origin/main

             if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                 Text(
                     text = mediaMetadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                     color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SubscribeButton(
    artistId: String,
    metadata: MediaMetadata,
    primaryColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val database = LocalDatabase.current
    val libraryArtist by database.artist(artistId).collectAsStateWithLifecycle(initialValue = null)
    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isSubscribed) primaryColor.copy(alpha = 0.5f) else outlineColor.copy(alpha = 0.3f),
                    shape = CircleShape,
                ).background(
                    color = if (isSubscribed) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
                    shape = CircleShape,
                ).clickable {
                    database.transaction {
                        val artist = libraryArtist?.artist
                        if (artist != null) {
                            update(artist.toggleLike())
                        } else {
                            metadata.artists.firstOrNull()?.let { artistInfo ->
                                insert(
                                    ArtistEntity(
                                        id = artistInfo.id ?: "",
                                        name = artistInfo.name,
                                        channelId = null,
                                        thumbnailUrl = null,
                                    ).toggleLike(),
                                )
                            }
                        }
                    }
                },
    ) {
        Icon(
            painter = painterResource(if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe),
            contentDescription = null,
            tint = if (isSubscribed) primaryColor else onSurfaceColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AddToPlaylistButton(
    onClick: () -> Unit,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val contentDescription = stringResource(R.string.add_to_playlist_desc)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = outlineColor.copy(alpha = 0.3f),
                shape = CircleShape,
            )
            .background(
                color = Color.Transparent,
                shape = CircleShape,
            )
            .clickable { onClick() },
    ) {
        Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = contentDescription,
            tint = onSurfaceColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FavoriteButton(
    songId: String,
    errorColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(songId).collectAsStateWithLifecycle(initialValue = null)
    val isEpisode = librarySong?.song?.isEpisode == true
    val isLiked = if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isLiked) errorColor.copy(alpha = 0.5f) else outlineColor.copy(alpha = 0.3f),
                    shape = CircleShape,
                ).background(
                    color = if (isLiked) errorColor.copy(alpha = 0.1f) else Color.Transparent,
                    shape = CircleShape,
                ).clickable { playerConnection.service.toggleLike() },
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = null,
            tint = if (isLiked) errorColor else onSurfaceColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}
<<<<<<< HEAD

/** Blur radius of the mini-player glow — modest, so the drift keeps its frames. */
private val GlowBlurRadius = 32.dp

/**
 * Animated glow background for the mini player: a palette wash across the whole
 * bar with four artwork-palette blobs sweeping over it, blurred into a soft
 * aura.
 *
 * The wash is what makes the glow reach the bar's edges — the blobs alone left
 * the far ends dim on a wide mini player. On top of it each blob runs its own
 * phase, orbit, size and speed, so the motion reads as a slow drift that never
 * pulses in lockstep. The blur is deliberately modest: a huge radius costs
 * frames on a bar that redraws every animation tick, and a dropped frame is
 * what made the glow look like it stuttered.
 */
@Composable
private fun GlowAnimatedBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val glowColors =
        if (colors.isNotEmpty()) {
            colors
        } else {
            listOf(Color(0xFF6C7BFF), Color(0xFFB388FF), Color(0xFF4CC9B0))
        }

    // A 32 dp blur across the whole bar, redrawn every frame, is the heaviest
    // thing the mini player can ask for. With ambient motion off the same wash
    // is painted once, unblurred and still — the glow is still there, it just
    // stops costing frames.
    if (!rememberAmbientMotionEnabled()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(
                brush =
                    Brush.horizontalGradient(
                        glowColors.map { it.copy(alpha = 0.30f) },
                    ),
            )
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "miniPlayerGlow")
    val progress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    // A slow full lap, and Restart rather than Reverse: an orbit
                    // that reverses would visibly swing back on itself instead
                    // of looping seamlessly.
                    animation = tween(18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "miniPlayerGlowProgress",
        )

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .blur(GlowBlurRadius),
    ) {
        val primary = glowColors.getOrElse(0) { glowColors.first() }
        val second = glowColors.getOrElse(1) { primary }
        val third = glowColors.getOrElse(2) { primary }

        // Base wash: edge to edge, so the glow covers the entire mini player
        // instead of only the patch a blob happens to be passing over.
        drawRect(
            brush =
                Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = 0.34f),
                        second.copy(alpha = 0.26f),
                        third.copy(alpha = 0.34f),
                    ),
                ),
        )

        fun drawBlob(
            color: Color,
            phase: Float,
            orbitX: Float,
            orbitY: Float,
            radiusFraction: Float,
            /** Laps per animation cycle — different speeds keep the blobs apart. */
            speed: Float,
        ) {
            // A full 360 degree orbit around the centre of the bar: the blob
            // travels its own slow ellipse instead of sliding across on x.
            val angle = ((phase + progress * speed) % 1f) * 2f * PI
            val x = size.width * (0.5f + orbitX * cos(angle).toFloat())
            val y = size.height * (0.5f + orbitY * sin(angle).toFloat())
            val radius = size.width * radiusFraction
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.85f), Color.Transparent),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                radius = radius,
                center = Offset(x, y),
            )
        }

        // Wider orbits than before so each blob reaches the far ends of the bar,
        // and four of them so there is always one crossing the middle.
        drawBlob(primary, 0f, 0.42f, 0.20f, 0.44f, 1f)
        drawBlob(second, 0.35f, 0.36f, 0.24f, 0.38f, 1.35f)
        drawBlob(third, 0.70f, 0.44f, 0.18f, 0.40f, 0.8f)
        drawBlob(primary, 0.5f, 0.30f, 0.26f, 0.34f, 1.15f)
    }
}
=======
>>>>>>> origin/main
