/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import com.jay.glossy.R

import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.jay.glossy.LocalNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.CropAlbumArtKey
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.HidePlayerThumbnailKey
import com.jay.glossy.constants.HideStatusBarOnFullscreenKey
import com.jay.glossy.constants.KeepScreenOn
import com.jay.glossy.constants.PlayerBackgroundStyle
import com.jay.glossy.constants.PlayerBackgroundStyleKey
import com.jay.glossy.constants.PlayerButtonsStyle
import com.jay.glossy.constants.PlayerButtonsStyleKey
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.constants.PlayerStyle
import com.jay.glossy.constants.PlayerStyleKey
import com.jay.glossy.constants.QueuePeekHeight
import com.jay.glossy.constants.SleepTimerDefaultKey
import com.jay.glossy.constants.SleepTimerFadeOutKey
import com.jay.glossy.constants.SleepTimerStopAfterCurrentSongKey
import com.jay.glossy.constants.SliderStyle
import com.jay.glossy.constants.SliderStyleKey
import com.jay.glossy.constants.SquigglySliderKey
import com.jay.glossy.constants.ThumbnailCornerRadius
import com.jay.glossy.constants.UseNewPlayerDesignKey
import com.jay.glossy.db.entities.LyricsEntity
import com.jay.glossy.extensions.metadata
import com.jay.glossy.extensions.togglePlayPause
import com.jay.glossy.extensions.toggleRepeatMode
import com.jay.glossy.listentogether.RoomRole
import com.metrolist.models.MediaMetadata
import com.jay.glossy.ui.component.BottomSheet
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.Lyrics
import com.jay.glossy.ui.component.PlayerSliderTrack
import com.jay.glossy.ui.component.ResizableIconButton
import com.jay.glossy.ui.component.SquigglySlider
import com.jay.glossy.ui.component.WavySlider
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.screens.settings.DarkMode
import com.jay.glossy.ui.theme.PlayerColorExtractor
import com.jay.glossy.ui.theme.PlayerSliderColors
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.ui.utils.ShowOffsetDialog
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.jay.glossy.ui.component.Icon as MIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (playerStyle) = rememberEnumPreference(PlayerStyleKey, defaultValue = PlayerStyle.MODERN)
    val (useNewPlayerDesignSetting, setUseNewPlayerDesign) = rememberPreference(UseNewPlayerDesignKey, defaultValue = true)

    LaunchedEffect(playerStyle) {
        val shouldBeModern = playerStyle != PlayerStyle.CLASSIC && playerStyle.name != "VIVI_NEW"
        if (useNewPlayerDesignSetting != shouldBeModern) {
            setUseNewPlayerDesign(shouldBeModern)
        }
    }

    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    var showInlineLyrics by rememberSaveable { mutableStateOf(false) }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT,
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT,
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH -> {
                    insetsController.isAppearanceLightStatusBars = false
                }
                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }

            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    BackHandler(enabled = state.isExpanded) {
        state.collapseSoft()
    }

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
        val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        useDarkTheme && pureBlack
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val castHandler = remember(playerConnection) {
        try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null }
    }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            delay(100)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf(
            (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }

    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) castPosition else position
        }
    }

    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_MESH) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context).data(currentMetadata.thumbnailUrl).size(100, 100).allowHardware(false).memoryCacheKey("gradient_${currentMetadata.id}").build()
                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate()
                            }
                            val extractedColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH -> Color.White
        },
        label = "TextBackgroundColor",
    )

    val icBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH -> Color.Black
        },
        label = "icBackgroundColor",
    )

    val (textButtonColor, iconButtonColor) = when {
        playerBackground == PlayerBackgroundStyle.BLUR || playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_MESH -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(Color.White, Color.Black)
                PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> if (useDarkTheme) Pair(Color.White, Color.Black) else Pair(Color.Black, Color.White)
                PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }

    val (sideButtonContainerColor, sideButtonContentColor) = when {
        playerBackground == PlayerBackgroundStyle.BLUR || playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_MESH -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(Color.White.copy(alpha = 0.2f), Color.White)
                PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
                PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }

    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    position = playerConnection.player.currentPosition
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L)) ?: playerConnection.player.duration
        }
    }

    var previousMediaId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        val currentId = mediaMetadata?.id
        if (currentId != null && currentId != previousMediaId && previousMediaId != null && playbackState == Player.STATE_ENDED && repeatMode == Player.REPEAT_MODE_ONE && !isListenTogetherGuest) {
            playerConnection.player.setRepeatMode(Player.REPEAT_MODE_ALL)
        }
        previousMediaId = currentId
    }

    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val actualPeekHeight = if (playerStyle.name == "WAVY") 0.dp else QueuePeekHeight
    val dismissedBound = actualPeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = com.jay.glossy.ui.component.rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = dismissedBound + 1.dp,
        initialAnchor = 1,
    )

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH -> MaterialTheme.colorScheme.surfaceContainer
        else -> if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier.fillMaxSize().background(bottomSheetBackgroundColor),
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                            label = "blurBackground",
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(thumbnailUrl).size(100, 100).allowHardware(false).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().blur(if (useDarkTheme) 150.dp else 100.dp),
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                            label = "gradientBackground",
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops = if (colors.size >= 3) {
                                    arrayOf(0.0f to colors[0], 0.5f to colors[1], 1.0f to colors[2])
                                } else {
                                    arrayOf(0.0f to colors[0], 0.6f to colors[0].copy(alpha = 0.7f), 1.0f to Color.Black)
                                }
                                Box(Modifier.fillMaxSize().alpha(backgroundAlpha).background(Brush.verticalGradient(colorStops = gradientColorStops)).background(Color.Black.copy(alpha = 0.2f)))
                            }
                        }
                    }
                    PlayerBackgroundStyle.ANIMATED_MESH -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                            label = "meshBackground",
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                AnimatedMeshBackground(
                                    colors = colors,
                                    modifier = Modifier.fillMaxSize().alpha(backgroundAlpha).background(Color.Black.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        onDismiss = if (!isListenTogetherGuest) {
            {
                playerConnection.service.clearAutomix()
                playerConnection.player.stop()
                playerConnection.player.clearMediaItems()
            }
        } else null,
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onClick = { state.expandSoft() },
            )
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
            ) {
                AnimatedContent(targetState = showInlineLyrics, label = "ThumbnailAnimation") { showLyrics ->
                    if (showLyrics) {
                        Row {
                            if (hidePlayerThumbnail) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(ThumbnailCornerRadius)).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(painter = painterResource(R.drawable.small_icon), contentDescription = null, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                AsyncImage(
                                    model = mediaMetadata.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(targetState = mediaMetadata.title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp).combinedClickable(
                                enabled = true, indication = null, interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    val albumId = mediaMetadata.album?.id ?: currentSong?.album?.id ?: currentSong?.song?.albumId
                                    if (albumId != null) { navController.navigate("album/$albumId"); state.collapseSoft() }
                                },
                                onLongClick = {
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText(copiedTitleStr, title))
                                    Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show()
                                },
                            ),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (mediaMetadata.explicit) MIcon.Explicit()
                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            val annotatedString = buildAnnotatedString {
                                mediaMetadata.artists.forEachIndexed { index, artist ->
                                    val tag = "artist_${artist.id.orEmpty()}"
                                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                    withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) { append(artist.name) }
                                    pop()
                                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp).padding(end = 12.dp)) {
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, onTextLayout = { layoutResult = it },
                                    modifier = Modifier.pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val tapPosition = event.changes.firstOrNull()?.position
                                                if (tapPosition != null) clickOffset = tapPosition
                                            }
                                        }
                                    }.combinedClickable(
                                        enabled = true, indication = null, interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val tapPosition = clickOffset; val layout = layoutResult
                                            if (tapPosition != null && layout != null) {
                                                val offset = layout.getOffsetForPosition(tapPosition)
                                                annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { ann ->
                                                    val artistId = ann.item
                                                    if (artistId.isNotBlank()) { navController.navigate("artist/$artistId"); state.collapseSoft() }
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            clipboardManager.setPrimaryClip(ClipData.newPlainText(copiedArtistStr, annotatedString))
                                            Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show()
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                when (playerStyle.name) {
                    "MODERN" -> {
                        val shareShape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 3.dp, bottomEnd = 3.dp)
                        val favShape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 50.dp, bottomEnd = 50.dp)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                                if (showLyrics) {
                                    FilledIconButton(
                                        onClick = { isFullScreen = !isFullScreen }, shape = shareShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor),
                                        modifier = Modifier.size(42.dp),
                                    ) { Icon(painter = painterResource(R.drawable.fullscreen), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                } else {
                                    FilledIconButton(
                                        onClick = {
                                            val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }
                                            context.startActivity(Intent.createChooser(intent, null))
                                        },
                                        shape = shareShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor),
                                        modifier = Modifier.size(42.dp),
                                    ) { Icon(painter = painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                }
                            }

                            AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                                if (showLyrics) {
                                    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                    FilledIconButton(
                                        onClick = {
                                            menuState.show {
                                                com.jay.glossy.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } })
                                            }
                                        },
                                        shape = favShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor),
                                        modifier = Modifier.size(42.dp),
                                    ) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                } else {
                                    val isEpisode = currentSong?.song?.isEpisode == true
                                    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                    FilledIconButton(
                                        onClick = playerConnection::toggleLike, shape = favShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor),
                                        modifier = Modifier.size(42.dp),
                                    ) { Icon(painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                }
                            }
                        }
                    }
                    "CLASSIC" -> {
                        AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                            if (showLyrics) {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { isFullScreen = !isFullScreen }) {
                                    Icon(painter = painterResource(R.drawable.fullscreen), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                }
                            } else {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable {
                                    val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }
                                    context.startActivity(Intent.createChooser(intent, null))
                                }) {
                                    Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                            if (showLyrics) {
                                val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable {
                                    menuState.show { com.jay.glossy.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) }
                                }) {
                                    Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                }
                            } else {
                                PlayerMoreMenuButton(mediaMetadata = mediaMetadata, state = state, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor)
                            }
                        }
                    }
                    "WAVY" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = showInlineLyrics) {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { isFullScreen = !isFullScreen }) {
                                    Icon(painterResource(R.drawable.fullscreen), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                }
                            }
                            AnimatedContent(targetState = showInlineLyrics, label = "MoreButton") { showLyrics ->
                                if (showLyrics) {
                                    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable {
                                        menuState.show { com.jay.glossy.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) }
                                    }) {
                                        Icon(painterResource(R.drawable.more_horiz), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                    }
                                } else {
                                    androidx.compose.material3.IconButton(onClick = {
                                        menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) }
                                    }, modifier = Modifier.size(40.dp)) {
                                        Icon(painterResource(R.drawable.more_horiz), contentDescription = null, tint = TextBackgroundColor, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                    "VIVI_NEW" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(textButtonColor.copy(alpha = 0.2f)).clickable {
                                menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) }
                            }, contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.more_vert), contentDescription = "Options", tint = iconButtonColor, modifier = Modifier.size(24.dp))
                            }
                            val isEpisode = currentSong?.song?.isEpisode == true
                            val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(textButtonColor.copy(alpha = 0.2f)).clickable { playerConnection.toggleLike() }, contentAlignment = Alignment.Center) {
                                Icon(painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), contentDescription = "Like", tint = if (isFavorite) MaterialTheme.colorScheme.error else iconButtonColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = { if (!isListenTogetherGuest) sliderPosition = it.toLong() },
                        onValueChangeFinished = { if (!isListenTogetherGuest) { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null } },
                        enabled = !isListenTogetherGuest,
                        colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }
                SliderStyle.WAVY -> {
                    if (squigglySlider) {
                        SquigglySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null },
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                            isPlaying = effectiveIsPlaying,
                        )
                    } else {
                        WavySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null },
                            colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            isPlaying = effectiveIsPlaying,
                        )
                    }
                }
                SliderStyle.SLIM -> {
                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = { if (!isListenTogetherGuest) sliderPosition = it.toLong() },
                        onValueChangeFinished = { if (!isListenTogetherGuest) { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null } },
                        enabled = !isListenTogetherGuest,
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme)) },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(text = makeTimeString(sliderPosition ?: effectivePosition), style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column {
                    val onPlayPauseLogic: () -> Unit = {
                        if (isListenTogetherGuest) { playerConnection.toggleMute() }
                        else if (isCasting) { if (castIsPlaying) castHandler?.pause() else castHandler?.play() }
                        else if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true }
                        else { playerConnection.togglePlayPause() }
                    }

                    when (playerStyle.name) {
                        "MODERN" -> {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                                val backInteractionSource = remember { MutableInteractionSource() }
                                val nextInteractionSource = remember { MutableInteractionSource() }
                                val playPauseInteractionSource = remember { MutableInteractionSource() }

                                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                                val isBackPressed by backInteractionSource.collectIsPressedAsState()
                                val isNextPressed by nextInteractionSource.collectIsPressedAsState()

                                val playPauseWeight by animateFloatAsState(targetValue = if (isPlayPausePressed) 1.9f else if (isBackPressed || isNextPressed) 1.1f else 1.3f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "playPauseWeight")
                                val backButtonWeight by animateFloatAsState(targetValue = if (isBackPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "backButtonWeight")
                                val nextButtonWeight by animateFloatAsState(targetValue = if (isNextPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "nextButtonWeight")

                                FilledIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(50), interactionSource = backInteractionSource, colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(backButtonWeight)) {
                                    Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledIconButton(onClick = onPlayPauseLogic, shape = RoundedCornerShape(50), interactionSource = playPauseInteractionSource, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.height(68.dp).weight(playPauseWeight).focusRequester(focusRequester)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else { if (effectiveIsPlaying) R.drawable.pause else R.drawable.play }), contentDescription = null, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = if (isListenTogetherGuest) { if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute) } else { if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play) }, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(50), interactionSource = nextInteractionSource, colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(nextButtonWeight)) {
                                    Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        "CLASSIC" -> {
                            val playPauseRoundness by animateDpAsState(targetValue = if (isPlaying) 24.dp else 36.dp, animationSpec = tween(durationMillis = 90, easing = LinearEasing), label = "playPauseRoundness")

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ResizableIconButton(icon = when (repeatMode) { Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat; Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> throw IllegalStateException() }, color = TextBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest || repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f), enabled = !isListenTogetherGuest, onClick = { playerConnection.player.toggleRepeatMode() })
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ResizableIconButton(icon = R.drawable.skip_previous, enabled = canSkipPrevious && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), onClick = playerConnection::seekToPrevious)
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(playPauseRoundness)).background(textButtonColor).clickable { onPlayPauseLogic() }.focusRequester(focusRequester)) {
                                    Image(painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else if (playbackState == STATE_ENDED) { R.drawable.replay } else if (effectiveIsPlaying) { R.drawable.pause } else { R.drawable.play }), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(36.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    ResizableIconButton(icon = R.drawable.skip_next, enabled = canSkipNext && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), onClick = playerConnection::seekToNext)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    val isEpisode = currentSong?.song?.isEpisode == true
                                    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                    ResizableIconButton(icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border, color = if (isFavorite) MaterialTheme.colorScheme.error else TextBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center), onClick = playerConnection::toggleLike)
                                }
                            }
                        }
                        "WAVY" -> {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    FilledIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(24.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(72.dp).width(80.dp)) {
                                        Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    FilledIconButton(onClick = onPlayPauseLogic, shape = RoundedCornerShape(24.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.height(72.dp).width(112.dp).focusRequester(focusRequester)) {
                                        Icon(painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else { if (effectiveIsPlaying) R.drawable.pause else R.drawable.play }), contentDescription = null, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    FilledIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(24.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(72.dp).width(80.dp)) {
                                        Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val isEpisode = currentSong?.song?.isEpisode == true
                                    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                    Surface(shape = RoundedCornerShape(50), color = sideButtonContainerColor, contentColor = if (isFavorite) MaterialTheme.colorScheme.error else sideButtonContentColor, modifier = Modifier.height(52.dp).weight(0.8f), onClick = { playerConnection.toggleLike() }) {
                                        Box(contentAlignment = Alignment.Center) { Icon(painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), null, modifier = Modifier.size(22.dp)) }
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = sideButtonContainerColor, contentColor = sideButtonContentColor, modifier = Modifier.height(52.dp).weight(1.5f), onClick = { /* Add Download logic */ }) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.offline), null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Download", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = sideButtonContainerColor, contentColor = if (repeatMode != Player.REPEAT_MODE_OFF) textButtonColor else sideButtonContentColor, modifier = Modifier.height(52.dp).weight(1.5f), onClick = { playerConnection.player.toggleRepeatMode() }) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Repeat", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Surface(shape = RoundedCornerShape(50), color = sideButtonContainerColor, contentColor = sideButtonContentColor, modifier = Modifier.height(40.dp).weight(1f), onClick = { showInlineLyrics = !showInlineLyrics }) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Lyrics", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = sideButtonContainerColor, contentColor = sideButtonContentColor, modifier = Modifier.height(40.dp).weight(1f), onClick = { scope.launch { queueSheetState.expandSoft() } }) {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Queue", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        "VIVI_NEW" -> {
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
                                val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
                                context.registerReceiver(receiver, filter)
                                awaitDispose {
                                    context.unregisterReceiver(receiver)
                                }
                            }

                            // VIVI NEW EXCLUSIVE PLAYBACK ROW
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                            ) {
                                // Previous
                                androidx.compose.material3.IconButton(
                                    onClick = playerConnection::seekToPrevious,
                                    enabled = canSkipPrevious && !isListenTogetherGuest,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.apple_skip_previous), 
                                        contentDescription = "Previous", 
                                        modifier = Modifier.size(40.dp), 
                                        tint = TextBackgroundColor
                                    )
                                }

                                Spacer(Modifier.width(32.dp))

                                // Play / Pause
                                androidx.compose.material3.IconButton(
                                    onClick = onPlayPauseLogic,
                                    modifier = Modifier.size(84.dp).focusRequester(focusRequester)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isListenTogetherGuest) {
                                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                            } else if (playbackState == STATE_ENDED) {
                                                R.drawable.replay
                                            } else if (effectiveIsPlaying) {
                                                R.drawable.pause_applemusic
                                            } else {
                                                R.drawable.play_applemusic
                                            }
                                        ),
                                        contentDescription = "Play/Pause",
                                        tint = TextBackgroundColor,
                                        modifier = Modifier.size(68.dp)
                                    )
                                }

                                Spacer(Modifier.width(32.dp))

                                // Next
                                androidx.compose.material3.IconButton(
                                    onClick = playerConnection::seekToNext,
                                    enabled = canSkipNext && !isListenTogetherGuest,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.apple_skip_next), 
                                        contentDescription = "Next", 
                                        modifier = Modifier.size(40.dp), 
                                        tint = TextBackgroundColor
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // VIVI NEW EXCLUSIVE VOLUME ROW
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                            ) {
                                val volumeInteractionSource = remember { MutableInteractionSource() }
                                val isVolumeDragged by volumeInteractionSource.collectIsDraggedAsState()
                                val isVolumePressed by volumeInteractionSource.collectIsPressedAsState()
                                val isVolumeActive = isVolumeDragged || isVolumePressed

                                var dragVolume by remember { mutableFloatStateOf(systemVolume) }
                                val scope = rememberCoroutineScope()
                                
                                LaunchedEffect(systemVolume) {
                                    if (!isVolumeActive) dragVolume = systemVolume
                                }

                                val animatedSystemVolume by animateFloatAsState(
                                    targetValue = systemVolume,
                                    animationSpec = tween(150, easing = LinearOutSlowInEasing),
                                    label = "animatedSystemVolume"
                                )
                                
                                val volume = if (isVolumeActive) dragVolume else animatedSystemVolume
                                
                                val volumeTrackHeight by animateDpAsState(
                                    targetValue = if (isVolumeActive) 16.dp else 10.dp,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
                                    label = "volumeTrackHeight"
                                )

                                val volumeIconScale by animateFloatAsState(
                                    targetValue = if (isVolumeActive) 1.15f else 1f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
                                    label = "volumeIconScale"
                                )

                                Icon(
                                    painter = painterResource(R.drawable.volume_mute),
                                    contentDescription = null,
                                    tint = TextBackgroundColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(scaleX = volumeIconScale, scaleY = volumeIconScale)
                                )

                                Spacer(Modifier.width(12.dp))

                                Slider(
                                    value = volume,
                                    onValueChange = { newVolume ->
                                        dragVolume = newVolume
                                        scope.launch(Dispatchers.Default) {
                                            val newStep = (newVolume * maxSystemVolume).roundToInt()
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newStep, 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    interactionSource = volumeInteractionSource,
                                    thumb = {},
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            colors = androidx.compose.material3.SliderDefaults.colors(
                                                activeTrackColor = TextBackgroundColor.copy(alpha = 0.7f),
                                                inactiveTrackColor = TextBackgroundColor.copy(alpha = 0.15f)
                                            ),
                                            trackHeight = volumeTrackHeight
                                        )
                                    }
                                )

                                Spacer(Modifier.width(12.dp))

                                Icon(
                                    painter = painterResource(R.drawable.volume_up),
                                    contentDescription = null,
                                    tint = TextBackgroundColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(scaleX = volumeIconScale, scaleY = volumeIconScale)
                                )
                            }
                        }
                    }
                }
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                val density = LocalDensity.current
                val verticalPadding =
                    max(
                        WindowInsets.systemBars.getTop(density),
                        WindowInsets.systemBars.getBottom(density),
                    )
                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)

                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets),
                            ).padding(bottom = 24.dp)
                            .fillMaxSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .weight(1f)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                    ) {
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition },
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.animateContentSize(),
                                    isPlayerExpanded = isExpandedProvider,
                                    isLandscape = true,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(if (showInlineLyrics) 0.65f else 1f, false)
                                .animateContentSize()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                val bottomPadding by animateDpAsState(
                    targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound,
                    label = "bottomPadding",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = bottomPadding)
                            .animateContentSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition },
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                    isPlayerExpanded = isExpandedProvider,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                )
                            }
                        }
                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(if (playerStyle.name == "WAVY") 8.dp else 30.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                background =
                    if (useBlackBackground) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                onBackgroundColor = onBackgroundColor,
                TextBackgroundColor = TextBackgroundColor,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                pureBlack = pureBlack,
                showInlineLyrics = showInlineLyrics,
                playerBackground = playerBackground,
                onToggleLyrics = {
                    showInlineLyrics = !showInlineLyrics
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember {
        mutableStateOf(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, _ ->
                appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) {
                queueWindows[currentWindowIndex + 1].mediaItem.metadata
            } else {
                null
            }
        }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.jay.glossy.di.LyricsHelperEntryPoint::class.java,
                        )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    LaunchedEffect(
        nextMetadata?.id,
        showLyrics,
        appInForeground,
        mediaMetadata?.id,
        currentLyrics,
    ) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent =
            currentLyrics?.let { lyrics ->
                mediaMetadata == null || lyrics.id == mediaMetadata.id
            } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.jay.glossy.di.LyricsHelperEntryPoint::class.java,
                    )
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query {
                    upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }

            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics,
                    )
                }
                ProvideTextStyle(
                    value =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                ) {
                    lyricsContent()
                }
            }
        }
    }
}

@Composable
fun MoreActionsButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textButtonColor)
                .clickable {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = {
                                mediaMetadata.id.let {
                                    bottomSheetPageState.show {
                                        ShowMediaInfo(it)
                                    }
                                }
                            },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

@Composable
private fun PlayerMoreMenuButton(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textButtonColor)
                .clickable {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = {
                                mediaMetadata.id.let {
                                    bottomSheetPageState.show {
                                        ShowMediaInfo(it)
                                    }
                                }
                            },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

@Composable
fun AnimatedMeshBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    
    val offset1 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset1"
    )
    val offset2 = infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset2"
    )
    val offset3 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset3"
    )

    val safeColors = if (colors.size >= 3) colors else listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )

    Canvas(modifier = modifier.fillMaxSize().blur(60.dp)) {
        val w = size.width
        val h = size.height
        
        val o1 = offset1.value
        val o2 = offset2.value
        val o3 = offset3.value

        drawRect(color = safeColors[0].copy(alpha = 0.3f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(safeColors[0], Color.Transparent),
                center = Offset(w * o1, h * o2),
                radius = w * 0.9f
            ),
            radius = w * 0.9f,
            center = Offset(w * o1, h * o2)
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(safeColors[1], Color.Transparent),
                center = Offset(w * o2, h * o3),
                radius = w * 0.9f
            ),
            radius = w * 0.9f,
            center = Offset(w * o2, h * o3)
        )
        
        if (safeColors.size > 2) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(safeColors[2], Color.Transparent),
                    center = Offset(w * o3, h * o1),
                    radius = w * 0.9f
                ),
                radius = w * 0.9f,
                center = Offset(w * o3, h * o1)
            )
        }
    }
}
