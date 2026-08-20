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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.jay.glossy.constants.PlayerDesignStyle
import com.jay.glossy.constants.PlayerDesignStyleKey
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.constants.QueuePeekHeight
import com.jay.glossy.constants.SleepTimerDefaultKey
import com.jay.glossy.constants.SleepTimerFadeOutKey
import com.jay.glossy.constants.SleepTimerStopAfterCurrentSongKey
import com.jay.glossy.constants.SliderStyle
import com.jay.glossy.constants.SliderStyleKey
import com.jay.glossy.constants.SquigglySliderKey
import com.jay.glossy.constants.ThumbnailCornerRadius
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
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // --- PLAYER DESIGN SELECTION STATE ---
    val playerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.MODERN,
    )

    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    var showInlineLyrics by rememberSaveable { mutableStateOf(false) }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val playerButtonsStyle by rememberEnumPreference(PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)

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
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH -> insetsController.isAppearanceLightStatusBars = false
                PlayerBackgroundStyle.DEFAULT -> insetsController.isAppearanceLightStatusBars = !useDarkTheme
            }
            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
            if (keepScreenOn && state.isExpanded) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    BackHandler(enabled = state.isExpanded) { state.collapseSoft() }

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

    val castHandler = remember(playerConnection) { try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null } }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) { delay(100); try { focusRequester.requestFocus() } catch (e: Exception) {} }
    }

    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf((mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L)) ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L))
    }

    var position by positionState
    var duration by durationState

    val effectivePosition by remember { derivedStateOf { if (isCasting) castPosition else position } }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) playerConnection.service.addToQueueAutomix(automix[0], 0)

    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_MESH) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) { gradientColors = cachedColors; return@LaunchedEffect }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context).data(currentMetadata.thumbnailUrl).size(100, 100).allowHardware(false).memoryCacheKey("gradient_${currentMetadata.id}").build()
                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    val bitmap = result?.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate() }
                        val extractedColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                        gradientColorsCache[currentMetadata.id] = extractedColors
                        withContext(Dispatchers.Main) { gradientColors = extractedColors }
                    }
                }
            }
        } else gradientColors = emptyList()
    }

    val TextBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            else -> Color.White
        }, label = "TextBackgroundColor",
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

    val sleepTimerEnabled = remember(playerConnection.service.sleepTimer?.triggerTime, playerConnection.service.sleepTimer?.pauseWhenSongEnd) { playerConnection.service.sleepTimer?.isActive ?: false }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) playerConnection.player.duration - playerConnection.player.currentPosition else (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val scope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember { derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() } }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)

    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = { Icon(painterResource(R.drawable.bedtime), null) },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer?.start(minute = sleepTimerValue.roundToInt(), stopAfterCurrentSong = sleepTimerStopAfterCurrentSong, fadeOut = sleepTimerFadeOut)
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showSleepTimerDialog = false }) { Text(stringResource(android.R.string.cancel)) } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pluralStringResource(R.plurals.minute, sleepTimerValue.roundToInt(), sleepTimerValue.roundToInt()), style = MaterialTheme.typography.bodyLarge)
                    Slider(value = sleepTimerValue, onValueChange = { sleepTimerValue = it }, valueRange = 5f..120f, steps = (120 - 5) / 5 - 1)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedIconButton(onClick = { showSleepTimerDialog = false; playerConnection.service.sleepTimer?.start(minute = -1) }) { Text(stringResource(R.string.end_of_song)) }
                    }
                }
            },
        )
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

    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val queueSheetState = com.jay.glossy.ui.component.rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = if (playerDesignStyle == PlayerDesignStyle.WAVY) 0.dp else (dismissedBound + 1.dp),
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
            Box(modifier = Modifier.fillMaxSize().background(bottomSheetBackgroundColor)) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(targetState = mediaMetadata?.thumbnailUrl, transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) }, label = "blurBackground") { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(model = ImageRequest.Builder(context).data(thumbnailUrl).size(100, 100).allowHardware(false).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(if (useDarkTheme) 150.dp else 100.dp))
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(targetState = gradientColors, transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) }, label = "gradientBackground") { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops = if (colors.size >= 3) arrayOf(0.0f to colors[0], 0.5f to colors[1], 1.0f to colors[2]) else arrayOf(0.0f to colors[0], 0.6f to colors[0].copy(alpha = 0.7f), 1.0f to Color.Black)
                                Box(Modifier.fillMaxSize().alpha(backgroundAlpha).background(Brush.verticalGradient(colorStops = gradientColorStops)).background(Color.Black.copy(alpha = 0.2f)))
                            }
                        }
                    }
                    PlayerBackgroundStyle.ANIMATED_MESH -> {
                        AnimatedContent(targetState = gradientColors, transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) }, label = "meshBackground") { colors ->
                            if (colors.isNotEmpty()) AnimatedMeshBackground(colors = colors, modifier = Modifier.fillMaxSize().alpha(backgroundAlpha).background(Color.Black.copy(alpha = 0.2f)))
                        }
                    }
                    else -> {}
                }
            }
        },
        onDismiss = if (!isListenTogetherGuest) { { playerConnection.service.clearAutomix(); playerConnection.player.stop(); playerConnection.player.clearMediaItems() } } else null,
        collapsedContent = { MiniPlayer(positionState = positionState, durationState = durationState, onClick = { state.expandSoft() }) },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(targetValue = if (isPlaying) 24.dp else 36.dp, animationSpec = tween(durationMillis = 90, easing = LinearEasing), label = "playPauseRoundness")

            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = mediaMetadata.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextBackgroundColor, modifier = Modifier.basicMarquee())
                    if (mediaMetadata.artists.isNotEmpty()) {
                        Text(text = mediaMetadata.artists.joinToString { it.name }, style = MaterialTheme.typography.titleMedium, color = TextBackgroundColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (playerDesignStyle == PlayerDesignStyle.MODERN) {
                    val isFavorite = if (currentSong?.song?.isEpisode == true) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                    FilledIconButton(onClick = playerConnection::toggleLike, shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)) {
                        Icon(painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), null, Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when (sliderStyle) {
                SliderStyle.WAVY -> SquigglySlider(
                    value = (sliderPosition ?: effectivePosition).toFloat(),
                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                    onValueChange = { sliderPosition = it.toLong() },
                    onValueChangeFinished = { sliderPosition?.let { playerConnection.player.seekTo(it); position = it }; sliderPosition = null },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                    isPlaying = effectiveIsPlaying,
                )
                else -> Slider(
                    value = (sliderPosition ?: effectivePosition).toFloat(),
                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                    onValueChange = { sliderPosition = it.toLong() },
                    onValueChangeFinished = { sliderPosition?.let { playerConnection.player.seekTo(it); position = it }; sliderPosition = null },
                    colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)) {
                Text(text = makeTimeString(sliderPosition ?: effectivePosition), style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor)
                Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor)
            }

            Spacer(Modifier.height(24.dp))

            if (playerDesignStyle == PlayerDesignStyle.MODERN) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                    FilledIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(50), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(0.45f)) {
                        Icon(painterResource(R.drawable.skip_previous), null, Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = playerConnection::togglePlayPause, shape = RoundedCornerShape(50), colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.height(68.dp).weight(1.3f)) {
                        Icon(painterResource(if (effectiveIsPlaying) R.drawable.pause else R.drawable.play), null, Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(50), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(0.45f)) {
                        Icon(painterResource(R.drawable.skip_next), null, Modifier.size(32.dp))
                    }
                }
            } else {
                // LEGACY CLASSIC 5 BUTTONS
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat },
                            color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center), onClick = playerConnection.player::toggleRepeatMode,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(icon = R.drawable.skip_previous, enabled = canSkipPrevious && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center), onClick = playerConnection::seekToPrevious)
                    }
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(playPauseRoundness)).background(textButtonColor).clickable { playerConnection.togglePlayPause() },
                    ) {
                        Image(painter = painterResource(if (effectiveIsPlaying) R.drawable.pause else R.drawable.play), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(36.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(icon = R.drawable.skip_next, enabled = canSkipNext && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center), onClick = playerConnection::seekToNext)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val isFavorite = if (currentSong?.song?.isEpisode == true) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                        ResizableIconButton(icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border, color = if (isFavorite) MaterialTheme.colorScheme.error else TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center), onClick = playerConnection::toggleLike)
                    }
                }
            }
        }

        if (playerDesignStyle == PlayerDesignStyle.WAVY && mediaMetadata != null) {
            // --- 3RD WAVY PLAYER (DESIGN) ---
            WavyPlayerDesign(
                mediaMetadata = mediaMetadata!!,
                state = state,
                effectivePosition = effectivePosition,
                duration = duration,
                sliderPosition = sliderPosition,
                onSliderPositionChange = { newPos -> if (!isListenTogetherGuest) sliderPosition = newPos },
                onSliderPositionChangeFinished = {
                    if (!isListenTogetherGuest) {
                        sliderPosition?.let { pos -> playerConnection.player.seekTo(pos); position = pos }
                        sliderPosition = null
                    }
                },
                effectiveIsPlaying = effectiveIsPlaying,
                isListenTogetherGuest = isListenTogetherGuest,
                isMuted = isMuted,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                repeatMode = repeatMode,
                isFavorite = if (currentSong?.song?.isEpisode == true) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                sideButtonContainerColor = sideButtonContainerColor,
                sideButtonContentColor = sideButtonContentColor,
                TextBackgroundColor = TextBackgroundColor,
                playerBackground = playerBackground,
                useDarkTheme = useDarkTheme,
                onToggleLyrics = { showInlineLyrics = !showInlineLyrics },
                onShowQueue = { scope.launch { queueSheetState.expandSoft() } },
                onShowSleepTimer = { showSleepTimerDialog = true },
                onShowMenu = {
                    menuState.show {
                        PlayerMenu(mediaMetadata = mediaMetadata!!, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata!!.id.let { id -> bottomSheetPageState.show { ShowMediaInfo(id) } } }, onDismiss = menuState::dismiss)
                    }
                },
                onSkipPrevious = { playerConnection.seekToPrevious() },
                onSkipNext = { playerConnection.seekToNext() },
                onPlayPause = { playerConnection.togglePlayPause() },
                onToggleLike = { playerConnection.toggleLike() },
                onToggleRepeatMode = { playerConnection.player.toggleRepeatMode() },
            )
        } else {
            // --- STANDARD LEGACY & MODERN PORTRAIT / LANDSCAPE ---
            val bottomPadding by animateDpAsState(targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound, label = "bottomPadding")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)).padding(bottom = bottomPadding).animateContentSize()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                    val currentSliderPosition by rememberUpdatedState(sliderPosition)
                    val sliderPositionProvider = remember { { currentSliderPosition } }
                    val isExpandedProvider = remember(state) { { state.isExpanded } }
                    AnimatedContent(targetState = showInlineLyrics, label = "Lyrics", transitionSpec = { fadeIn() togetherWith fadeOut() }) { showLyrics ->
                        if (showLyrics) {
                            InlineLyricsView(mediaMetadata = mediaMetadata, showLyrics = showLyrics, positionProvider = { effectivePosition })
                        } else {
                            Thumbnail(sliderPositionProvider = sliderPositionProvider, modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection), isPlayerExpanded = isExpandedProvider, isListenTogetherGuest = isListenTogetherGuest)
                        }
                    }
                }
                mediaMetadata?.let { controlsContent(it) }
                Spacer(Modifier.height(30.dp))
            }
        }

        // QUEUE BOTTOM SHEET OVERLAY
        AnimatedVisibility(visible = !isFullScreen && playerDesignStyle != PlayerDesignStyle.WAVY, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Queue(state = queueSheetState, playerBottomSheetState = state, background = if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer, onBackgroundColor = onBackgroundColor, TextBackgroundColor = TextBackgroundColor, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, pureBlack = pureBlack, showInlineLyrics = showInlineLyrics, playerBackground = playerBackground, onToggleLyrics = { showInlineLyrics = !showInlineLyrics })
        }
    }
}

// -----------------------------------------------------------------------------
// WAVY PLAYER DESIGN COMPOSABLE (80647.jpg Clean Layout)
// -----------------------------------------------------------------------------

@Composable
fun WavyPlayerDesign(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
    effectivePosition: Long,
    duration: Long,
    sliderPosition: Long?,
    onSliderPositionChange: (Long) -> Unit,
    onSliderPositionChangeFinished: () -> Unit,
    effectiveIsPlaying: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    sideButtonContainerColor: Color,
    sideButtonContentColor: Color,
    TextBackgroundColor: Color,
    playerBackground: PlayerBackgroundStyle,
    useDarkTheme: Boolean,
    onToggleLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowMenu: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleRepeatMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = { state.collapseSoft() }) {
                Icon(painterResource(R.drawable.arrow_back), "Back", tint = TextBackgroundColor)
            }
            Text(text = "Now Playing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextBackgroundColor)
            androidx.compose.material3.IconButton(onClick = onShowSleepTimer) {
                Icon(painterResource(R.drawable.bedtime), "Sleep Timer", tint = TextBackgroundColor)
            }
        }

        // ALBUM ART (FLEXIBLE HEIGHT)
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SONG INFO
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mediaMetadata.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                if (mediaMetadata.artists.isNotEmpty()) {
                    Text(text = mediaMetadata.artists.joinToString { it.name }, style = MaterialTheme.typography.bodyMedium, color = TextBackgroundColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            androidx.compose.material3.IconButton(onClick = onShowMenu) {
                Icon(painterResource(R.drawable.more_horiz), "Menu", tint = TextBackgroundColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // WAVY PROGRESS BAR
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = makeTimeString(sliderPosition ?: effectivePosition), style = MaterialTheme.typography.labelSmall, color = TextBackgroundColor.copy(alpha = 0.7f))
            SquigglySlider(
                value = (sliderPosition ?: effectivePosition).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onSliderPositionChange(it.toLong()) },
                onValueChangeFinished = onSliderPositionChangeFinished,
                colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                isPlaying = effectiveIsPlaying,
            )
            Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelSmall, color = TextBackgroundColor.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PLAYBACK CONTROLS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onSkipPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(20.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.size(64.dp)) {
                Icon(painterResource(R.drawable.skip_previous), null, Modifier.size(28.dp))
            }
            FilledIconButton(onClick = onPlayPause, shape = RoundedCornerShape(28.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(80.dp)) {
                Icon(painterResource(if (effectiveIsPlaying) R.drawable.pause else R.drawable.play), null, Modifier.size(36.dp))
            }
            FilledIconButton(onClick = onSkipNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(20.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.size(64.dp)) {
                Icon(painterResource(R.drawable.skip_next), null, Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PILLS ROW 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            androidx.compose.material3.Surface(shape = CircleShape, color = sideButtonContainerColor.copy(alpha = 0.4f), modifier = Modifier.clickable { onToggleLike() }) {
                Icon(painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), "Like", tint = if (isFavorite) MaterialTheme.colorScheme.error else TextBackgroundColor, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).size(20.dp))
            }
            androidx.compose.material3.Surface(shape = CircleShape, color = sideButtonContainerColor.copy(alpha = 0.4f), modifier = Modifier.clickable {}) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.offline), null, tint = TextBackgroundColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download", color = TextBackgroundColor, style = MaterialTheme.typography.labelMedium)
                }
            }
            androidx.compose.material3.Surface(shape = CircleShape, color = sideButtonContainerColor.copy(alpha = 0.4f), modifier = Modifier.clickable { onToggleRepeatMode() }) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), "Repeat", tint = if (repeatMode != Player.REPEAT_MODE_OFF) textButtonColor else TextBackgroundColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Repeat", color = TextBackgroundColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // PILLS ROW 2
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            androidx.compose.material3.Surface(shape = CircleShape, color = sideButtonContainerColor.copy(alpha = 0.5f), modifier = Modifier.clickable { onToggleLyrics() }) {
                Row(modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.lyrics), null, tint = TextBackgroundColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lyrics", color = TextBackgroundColor, style = MaterialTheme.typography.labelMedium)
                }
            }
            androidx.compose.material3.Surface(shape = CircleShape, color = sideButtonContainerColor.copy(alpha = 0.5f), modifier = Modifier.clickable { onShowQueue() }) {
                Row(modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.queue_music), null, tint = TextBackgroundColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Queue", color = TextBackgroundColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// INLINE LYRICS & MESH BACKGROUND
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(mediaMetadata: MediaMetadata?, showLyrics: Boolean, positionProvider: () -> Long) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }

    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        when {
            lyrics == null -> ContainedLoadingIndicator()
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            else -> ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, textAlign = TextAlign.Center)) { Lyrics(sliderPositionProvider = positionProvider, modifier = Modifier.padding(horizontal = 24.dp), showLyrics = showLyrics) }
        }
    }
}

@Composable
fun AnimatedMeshBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val offset1 = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "offset1")
    val offset2 = infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "offset2")

    val safeColors = if (colors.size >= 2) colors else listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)

    Canvas(modifier = modifier.fillMaxSize().blur(60.dp)) {
        val w = size.width
        val h = size.height
        drawRect(color = safeColors[0].copy(alpha = 0.3f))
        drawCircle(brush = Brush.radialGradient(colors = listOf(safeColors[0], Color.Transparent), center = Offset(w * offset1.value, h * offset2.value), radius = w * 0.9f), radius = w * 0.9f, center = Offset(w * offset1.value, h * offset2.value))
    }
}
