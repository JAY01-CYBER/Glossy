/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import com.jay.glossy.R

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.CropAlbumArtKey
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.HidePlayerThumbnailKey
import com.jay.glossy.constants.HideStatusBarOnFullscreenKey
import com.jay.glossy.constants.KeepScreenOn
import com.jay.glossy.constants.ListItemHeight
import com.jay.glossy.constants.PlayerBackgroundStyle
import com.jay.glossy.constants.PlayerBackgroundStyleKey
import com.jay.glossy.constants.PlayerButtonsStyle
import com.jay.glossy.constants.PlayerButtonsStyleKey
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.constants.PlayerStyle
import com.jay.glossy.constants.PlayerStyleKey
import com.jay.glossy.constants.QueueEditLockKey
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
import com.jay.glossy.extensions.move
import com.jay.glossy.extensions.togglePlayPause
import com.jay.glossy.extensions.toggleRepeatMode
import com.jay.glossy.jayaudioutils.AudioDeviceBottomSheet
import com.jay.glossy.jayaudioutils.isBluetoothHeadphoneConnected
import com.jay.glossy.jayaudioutils.isWiredHeadphoneConnected
import com.jay.glossy.listentogether.RoomRole
import com.jay.glossy.ui.component.ActionPromptDialog
import com.jay.glossy.ui.component.BottomSheet
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.Lyrics
import com.jay.glossy.ui.component.MediaMetadataListItem
import com.jay.glossy.ui.component.PlayerSliderTrack
import com.jay.glossy.ui.component.ResizableIconButton
import com.jay.glossy.ui.component.SquigglySlider
import com.jay.glossy.ui.component.WavySlider
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.menu.QueueMenu
import com.jay.glossy.ui.menu.SelectionMediaMetadataMenu
import com.jay.glossy.ui.screens.settings.DarkMode
import com.jay.glossy.ui.theme.PlayerColorExtractor
import com.jay.glossy.ui.theme.PlayerSliderColors
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.ui.utils.ShowOffsetDialog
import com.jay.glossy.utils.joinToArtistString
import com.jay.glossy.utils.makeTimeString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit
import com.metrolist.models.MediaMetadata
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }

    val isHeadsetConnected by produceState(initialValue = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                value = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)
            }
        }

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)
                }
            }
        } else null

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }
        
        context.registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }

        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    val (playerStyle) = rememberEnumPreference(
        com.jay.glossy.constants.PlayerStyleKey, 
        defaultValue = com.jay.glossy.constants.PlayerStyle.MODERN
    )

    // Listen Together state
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)

    val currentFormat by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }

    // Cast state
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
            listSaver<MutableList<String>, String>(
                save = { it.toList() },
                restore = { it.toMutableStateList() },
            ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer?.triggerTime,
        playerConnection.service.sleepTimer?.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer?.isActive ?: false
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    val (useNewPlayerDesignSetting, setUseNewPlayerDesign) = rememberPreference(UseNewPlayerDesignKey, defaultValue = true)

    LaunchedEffect(playerStyle) {
        val shouldBeModern = playerStyle != PlayerStyle.CLASSIC && playerStyle.name != "VIVI_NEW" && playerStyle.name != "APPLE_MUSIC"
        if (useNewPlayerDesignSetting != shouldBeModern) {
            setUseNewPlayerDesign(shouldBeModern)
        }
    }

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

    BackHandler(enabled = state.isExpanded) { state.collapseSoft() }

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
        val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        useDarkTheme && pureBlack
    }

    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)
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
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L)
        )
    }

    var position by positionState
    var duration by durationState

    val effectivePosition by remember { derivedStateOf { if (isCasting) castPosition else position } }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground, playerStyle) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_MESH || playerStyle.name == "APPLE_MUSIC") {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

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
        label = "TextBackgroundColor"
    )

    val icBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH -> Color.Black
        },
        label = "icBackgroundColor"
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

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

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

    val playbackState by playerConnection.playbackState.collectAsState()
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
            if (playerStyle.name != "APPLE_MUSIC") {
                Box(modifier = Modifier.fillMaxSize().background(bottomSheetBackgroundColor)) {
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
                                    AnimatedMeshBackground(colors = colors, modifier = Modifier.fillMaxSize().alpha(backgroundAlpha).background(Color.Black.copy(alpha = 0.2f)))
                                }
                            }
                        }
                        else -> { }
                    }
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
            MiniPlayer(positionState = positionState, durationState = durationState, onClick = { state.expandSoft() })
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            if (playerStyle.name == "VIVI_NEW") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = mediaMetadata.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextBackgroundColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            Text(
                                text = mediaMetadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextBackgroundColor.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        AnimatedVisibility(visible = showInlineLyrics) {
                            val fsInteractionSource = remember { MutableInteractionSource() }
                            val isFsPressed by fsInteractionSource.collectIsPressedAsState()
                            val fsScale by animateFloatAsState(if (isFsPressed) 0.7f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "fsScale")
                            Box(
                                modifier = Modifier.size(40.dp).graphicsLayer(scaleX = fsScale, scaleY = fsScale).clip(CircleShape).clickable(interactionSource = fsInteractionSource, indication = androidx.compose.foundation.LocalIndication.current) { isFullScreen = !isFullScreen },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.fullscreen), contentDescription = "Fullscreen", tint = TextBackgroundColor, modifier = Modifier.size(24.dp))
                            }
                        }
                        AnimatedVisibility(visible = !showInlineLyrics) {
                            val isEpisode = currentSong?.song?.isEpisode == true
                            val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                            val likeInteractionSource = remember { MutableInteractionSource() }
                            val isLikePressed by likeInteractionSource.collectIsPressedAsState()
                            val likeScale by animateFloatAsState(if (isLikePressed) 0.7f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "likeScale")
                            Box(
                                modifier = Modifier.size(40.dp).graphicsLayer(scaleX = likeScale, scaleY = likeScale).clip(CircleShape).clickable(interactionSource = likeInteractionSource, indication = androidx.compose.foundation.LocalIndication.current) { playerConnection.toggleLike() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), contentDescription = "Like", tint = if (isFavorite) MaterialTheme.colorScheme.error else TextBackgroundColor, modifier = Modifier.size(24.dp))
                            }
                        }
                        val moreInteractionSource = remember { MutableInteractionSource() }
                        val isMorePressed by moreInteractionSource.collectIsPressedAsState()
                        val moreScale by animateFloatAsState(if (isMorePressed) 0.7f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "moreScale")
                        Box(
                            modifier = Modifier.size(40.dp).graphicsLayer(scaleX = moreScale, scaleY = moreScale).clip(CircleShape).clickable(interactionSource = moreInteractionSource, indication = androidx.compose.foundation.LocalIndication.current) {
                                if (showInlineLyrics) {
                                    val currentLyrics = playerConnection.currentLyrics.value
                                    menuState.show {
                                        com.jay.glossy.ui.menu.LyricsMenu(
                                            lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss,
                                            onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }
                                        )
                                    }
                                } else {
                                    menuState.show {
                                        PlayerMenu(
                                            mediaMetadata = mediaMetadata, playerBottomSheetState = state,
                                            onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(if (showInlineLyrics) R.drawable.more_horiz else R.drawable.more_vert), contentDescription = "Options", tint = TextBackgroundColor, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                AnimatedVisibility(visible = !showInlineLyrics, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        val trackInteractionSource = remember { MutableInteractionSource() }
                        val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                        val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                        val isTrackActive = isTrackDragged || isTrackPressed
                        val trackHeight by animateDpAsState(targetValue = if (isTrackActive) 12.dp else 6.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "trackScale")
                        Slider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { value -> if (!isListenTogetherGuest) sliderPosition = value.toLong() },
                            onValueChangeFinished = {
                                if (!isListenTogetherGuest) {
                                    sliderPosition?.let { pos ->
                                        if (isCasting) { castHandler?.seekTo(pos); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(pos) }
                                        position = pos
                                        sliderPosition = null
                                    }
                                }
                            },
                            enabled = !isListenTogetherGuest,
                            interactionSource = trackInteractionSource,
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), trackHeight = trackHeight) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = makeTimeString(sliderPosition ?: effectivePosition), style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, fontWeight = FontWeight.Bold)
                            Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val onPlayPauseLogic: () -> Unit = {
                            if (isListenTogetherGuest) playerConnection.toggleMute()
                            else if (isCasting) { if (castIsPlaying) castHandler?.pause() else castHandler?.play() }
                            else if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true }
                            else playerConnection.togglePlayPause()
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            val prevInteractionSource = remember { MutableInteractionSource() }
                            val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
                            val prevScale by animateFloatAsState(if (isPrevPressed) 0.7f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "prevScale")
                            androidx.compose.material3.IconButton(
                                onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, interactionSource = prevInteractionSource,
                                modifier = Modifier.size(64.dp).graphicsLayer(scaleX = prevScale, scaleY = prevScale)
                            ) { Icon(painterResource(R.drawable.apple_skip_previous), contentDescription = "Previous", modifier = Modifier.size(48.dp), tint = TextBackgroundColor) }
                            val playInteractionSource = remember { MutableInteractionSource() }
                            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                            val playScale by animateFloatAsState(if (isPlayPressed) 0.75f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "playScale")
                            androidx.compose.material3.IconButton(
                                onClick = onPlayPauseLogic, interactionSource = playInteractionSource,
                                modifier = Modifier.size(88.dp).focusRequester(focusRequester).graphicsLayer(scaleX = playScale, scaleY = playScale)
                            ) {
                                Icon(
                                    painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else if (playbackState == STATE_ENDED) R.drawable.replay else if (effectiveIsPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic),
                                    contentDescription = "Play/Pause", tint = TextBackgroundColor, modifier = Modifier.size(80.dp)
                                )
                            }
                            val nextInteractionSource = remember { MutableInteractionSource() }
                            val isNextPressed by nextInteractionSource.collectIsPressedAsState()
                            val nextScale by animateFloatAsState(if (isNextPressed) 0.7f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "nextScale")
                            androidx.compose.material3.IconButton(
                                onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, interactionSource = nextInteractionSource,
                                modifier = Modifier.size(64.dp).graphicsLayer(scaleX = nextScale, scaleY = nextScale)
                            ) { Icon(painterResource(R.drawable.apple_skip_next), contentDescription = "Next", modifier = Modifier.size(48.dp), tint = TextBackgroundColor) }
                        }
                        Spacer(Modifier.height(24.dp))
                        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                        val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
                        val systemVolume by produceState(initialValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume) {
                            val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { if (intent.action == "android.media.VOLUME_CHANGED_ACTION") { value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume } } }
                            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
                            context.registerReceiver(receiver, filter)
                            awaitDispose { context.unregisterReceiver(receiver) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                            val volumeInteractionSource = remember { MutableInteractionSource() }
                            val isVolumeDragged by volumeInteractionSource.collectIsDraggedAsState()
                            val isVolumePressed by volumeInteractionSource.collectIsPressedAsState()
                            val isVolumeActive = isVolumeDragged || isVolumePressed
                            var dragVolume by remember { mutableFloatStateOf(systemVolume) }
                            LaunchedEffect(systemVolume) { if (!isVolumeActive) dragVolume = systemVolume }
                            val animatedSystemVolume by animateFloatAsState(targetValue = systemVolume, animationSpec = tween(150, easing = LinearOutSlowInEasing), label = "animatedSystemVolume")
                            val volume = if (isVolumeActive) dragVolume else animatedSystemVolume
                            val volumeTrackHeight by animateDpAsState(targetValue = if (isVolumeActive) 16.dp else 10.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "volumeTrackHeight")
                            val volumeIconScale by animateFloatAsState(targetValue = if (isVolumeActive) 1.15f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "volumeIconScale")
                            Icon(painter = painterResource(R.drawable.volume_mute), contentDescription = null, tint = TextBackgroundColor, modifier = Modifier.size(20.dp).graphicsLayer(scaleX = volumeIconScale, scaleY = volumeIconScale))
                            Spacer(Modifier.width(16.dp))
                            Slider(
                                value = volume,
                                onValueChange = { newVolume -> dragVolume = newVolume; scope.launch(Dispatchers.Default) { val newStep = (newVolume * maxSystemVolume).roundToInt(); audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newStep, 0) } },
                                modifier = Modifier.weight(1f).height(24.dp), interactionSource = volumeInteractionSource, thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = androidx.compose.material3.SliderDefaults.colors(activeTrackColor = TextBackgroundColor.copy(alpha = 0.7f), inactiveTrackColor = TextBackgroundColor.copy(alpha = 0.15f)), trackHeight = volumeTrackHeight) }
                            )
                            Spacer(Modifier.width(16.dp))
                            Icon(painter = painterResource(R.drawable.volume_up), contentDescription = null, tint = TextBackgroundColor, modifier = Modifier.size(24.dp).graphicsLayer(scaleX = volumeIconScale, scaleY = volumeIconScale))
                        }
                    }
                }
            } else {
                val playPauseRoundness by animateDpAsState(targetValue = if (isPlaying) 24.dp else 36.dp, animationSpec = tween(durationMillis = 90, easing = LinearEasing), label = "playPauseRoundness")
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                    AnimatedContent(targetState = showInlineLyrics, label = "ThumbnailAnimation") { showLyrics ->
                        if (showLyrics) {
                            Row {
                                if (hidePlayerThumbnail) {
                                    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(ThumbnailCornerRadius)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(painter = painterResource(R.drawable.small_icon), contentDescription = null, modifier = Modifier.size(32.dp)) }
                                } else {
                                    AsyncImage(model = mediaMetadata.thumbnailUrl, contentDescription = null, contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(ThumbnailCornerRadius)))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        } else { Spacer(modifier = Modifier.width(0.dp)) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedContent(targetState = mediaMetadata.title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { title ->
                            Text(
                                text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextBackgroundColor,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp).combinedClickable(enabled = true, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { val albumId = mediaMetadata.album?.id ?: currentSong?.album?.id ?: currentSong?.song?.albumId; if (albumId != null) { navController.navigate("album/$albumId"); state.collapseSoft() } }, onLongClick = { val clip = ClipData.newPlainText(copiedTitleStr, title); clipboardManager.setPrimaryClip(clip); Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show() })
                            )
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (mediaMetadata.explicit) com.jay.glossy.ui.component.Icon.Explicit()
                            if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                                val annotatedString = buildAnnotatedString { mediaMetadata.artists.forEachIndexed { index, artist -> val tag = "artist_${artist.id.orEmpty()}"; pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty()); withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) { append(artist.name) }; pop(); if (index != mediaMetadata.artists.lastIndex) append(", ") } }
                                Box(modifier = Modifier.fillMaxWidth().basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp).padding(end = 12.dp)) {
                                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                    var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                    Text(
                                        text = annotatedString, style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor), maxLines = 1, overflow = TextOverflow.Ellipsis, onTextLayout = { layoutResult = it },
                                        modifier = Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { val event = awaitPointerEvent(); val tapPosition = event.changes.firstOrNull()?.position; if (tapPosition != null) { clickOffset = tapPosition } } } }.combinedClickable(enabled = true, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { val tapPosition = clickOffset; val layout = layoutResult; if (tapPosition != null && layout != null) { val offset = layout.getOffsetForPosition(tapPosition); annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { ann -> val artistId = ann.item; if (artistId.isNotBlank()) { navController.navigate("artist/$artistId"); state.collapseSoft() } } } }, onLongClick = { val clip = ClipData.newPlainText(copiedArtistStr, annotatedString); clipboardManager.setPrimaryClip(clip); Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show() })
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
                                        FilledIconButton(onClick = { isFullScreen = !isFullScreen }, shape = shareShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)) { Icon(painter = painterResource(R.drawable.fullscreen), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                    } else {
                                        FilledIconButton(onClick = { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }, shape = shareShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)) { Icon(painter = painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                    }
                                }
                                AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                                    if (showLyrics) {
                                        val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                        FilledIconButton(
                                            onClick = { menuState.show { com.jay.glossy.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) } },
                                            shape = favShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)
                                        ) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                    } else {
                                        val isEpisode = currentSong?.song?.isEpisode == true
                                        val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                        FilledIconButton(onClick = playerConnection::toggleLike, shape = favShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)) { Icon(painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, modifier = Modifier.size(24.dp)) }
                                    }
                                }
                            }
                        }
                        "CLASSIC" -> {
                            AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                                if (showLyrics) { Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { isFullScreen = !isFullScreen }) { Icon(painter = painterResource(R.drawable.fullscreen), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) }
                                } else { Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { val intent = Intent().apply { action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}") }; context.startActivity(Intent.createChooser(intent, null)) }) { Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) } }
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                                if (showLyrics) {
                                    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { com.jay.glossy.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) } }) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) }
                                } else { PlayerMoreMenuButton(mediaMetadata = mediaMetadata, state = state, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor) }
                            }
                        }
                        "WAVY" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AnimatedVisibility(visible = showInlineLyrics, enter = fadeIn(), exit = fadeOut()) { Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { isFullScreen = !isFullScreen }) { Icon(painterResource(R.drawable.fullscreen), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) } }
                                AnimatedContent(targetState = showInlineLyrics, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "MoreButton") { showLyrics ->
                                    if (showLyrics) {
                                        val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { com.jay.glossy.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) } }) { Icon(painterResource(R.drawable.more_horiz), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) }
                                    } else {
                                        androidx.compose.material3.IconButton(onClick = { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.more_horiz), contentDescription = null, tint = TextBackgroundColor, modifier = Modifier.size(24.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { if (!isListenTogetherGuest) { sliderPosition = it.toLong() } },
                            onValueChangeFinished = { if (!isListenTogetherGuest) { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null } },
                            enabled = !isListenTogetherGuest, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        )
                    }
                    SliderStyle.WAVY -> {
                        if (squigglySlider) {
                            SquigglySlider(
                                value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                onValueChange = { sliderPosition = it.toLong() },
                                onValueChangeFinished = { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null },
                                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding), colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), isPlaying = effectiveIsPlaying,
                            )
                        } else {
                            WavySlider(
                                value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                onValueChange = { sliderPosition = it.toLong() },
                                onValueChangeFinished = { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null },
                                colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), modifier = Modifier.padding(horizontal = PlayerHorizontalPadding), isPlaying = effectiveIsPlaying,
                            )
                        }
                    }
                    SliderStyle.SLIM -> {
                        Slider(
                            value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { if (!isListenTogetherGuest) { sliderPosition = it.toLong() } },
                            onValueChangeFinished = { if (!isListenTogetherGuest) { sliderPosition?.let { if (isCasting) { castHandler?.seekTo(it); lastManualSeekTime = System.currentTimeMillis() } else { playerConnection.player.seekTo(it) }; position = it }; sliderPosition = null } },
                            enabled = !isListenTogetherGuest, thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme)) }, modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)) {
                    Text(text = makeTimeString(sliderPosition ?: effectivePosition), style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                    Column {
                        val onPlayPauseLogic: () -> Unit = {
                            if (isListenTogetherGuest) { playerConnection.toggleMute() } else if (isCasting) { if (castIsPlaying) castHandler?.pause() else castHandler?.play() } else if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else { playerConnection.togglePlayPause() }
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

                                    FilledIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(50), interactionSource = backInteractionSource, colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(backButtonWeight)) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp)) }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(onClick = onPlayPauseLogic, shape = RoundedCornerShape(50), interactionSource = playPauseInteractionSource, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.height(68.dp).weight(playPauseWeight).focusRequester(focusRequester)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else { if (effectiveIsPlaying) R.drawable.pause else R.drawable.play }), contentDescription = if (isListenTogetherGuest) { if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute) } else { if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play) }, modifier = Modifier.size(32.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text = if (isListenTogetherGuest) { if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute) } else { if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play) }, style = MaterialTheme.typography.titleMedium) } }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(50), interactionSource = nextInteractionSource, colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(nextButtonWeight)) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp)) }
                                }
                            }
                            "CLASSIC" -> {
                                val playPauseRoundness by animateDpAsState(targetValue = if (isPlaying) 24.dp else 36.dp, animationSpec = tween(durationMillis = 90, easing = LinearEasing), label = "playPauseRoundness")
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                                    Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = when (repeatMode) { Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat; Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> throw IllegalStateException() }, color = TextBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest || repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f), enabled = !isListenTogetherGuest, onClick = { playerConnection.player.toggleRepeatMode() }) }
                                    Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = R.drawable.skip_previous, enabled = canSkipPrevious && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), onClick = playerConnection::seekToPrevious) }
                                    Spacer(Modifier.width(8.dp))
                                    Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(playPauseRoundness)).background(textButtonColor).clickable { onPlayPauseLogic() }.focusRequester(focusRequester)) { Image(painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else if (playbackState == STATE_ENDED) { R.drawable.replay } else if (effectiveIsPlaying) { R.drawable.pause } else { R.drawable.play }), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(36.dp)) }
                                    Spacer(Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = R.drawable.skip_next, enabled = canSkipNext && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), onClick = playerConnection::seekToNext) }
                                    Box(modifier = Modifier.weight(1f)) { val isEpisode = currentSong?.song?.isEpisode == true; val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true; ResizableIconButton(icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border, color = if (isFavorite) MaterialTheme.colorScheme.error else TextBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center), onClick = playerConnection::toggleLike) }
                                }
                            }
                            "WAVY" -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        FilledIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(24.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(72.dp).width(80.dp)) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp)) }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        FilledIconButton(onClick = onPlayPauseLogic, shape = RoundedCornerShape(24.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.height(72.dp).width(112.dp).focusRequester(focusRequester)) { Icon(painter = painterResource(if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } else { if (effectiveIsPlaying) R.drawable.pause else R.drawable.play }), contentDescription = null, modifier = Modifier.size(40.dp)) }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        FilledIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(24.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(72.dp).width(80.dp)) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp)) }
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        val isEpisode = currentSong?.song?.isEpisode == true
                                        val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                        Surface(shape = RoundedCornerShape(50), color = if (isFavorite) textButtonColor else sideButtonContainerColor, contentColor = if (isFavorite) MaterialTheme.colorScheme.error else sideButtonContentColor, modifier = Modifier.height(52.dp).weight(0.8f), onClick = { playerConnection.toggleLike() }) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, modifier = Modifier.size(22.dp)) } }
                                        Surface(shape = RoundedCornerShape(50), color = if (sleepTimerEnabled) textButtonColor else sideButtonContainerColor, contentColor = if (sleepTimerEnabled) iconButtonColor else sideButtonContentColor, modifier = Modifier.height(52.dp).weight(1.5f), onClick = { if (sleepTimerEnabled) { playerConnection.service.sleepTimer?.clear() } else { showSleepTimerDialog = true } }) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.bedtime), contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(text = if (sleepTimerEnabled) makeTimeString(sleepTimerTimeLeft) else stringResource(R.string.sleep_timer), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
                                        Surface(shape = RoundedCornerShape(50), color = if (repeatMode != Player.REPEAT_MODE_OFF) textButtonColor else sideButtonContainerColor, contentColor = if (repeatMode != Player.REPEAT_MODE_OFF) iconButtonColor else sideButtonContentColor, modifier = Modifier.height(52.dp).weight(1.5f), onClick = { playerConnection.player.toggleRepeatMode() }) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(when (repeatMode) { Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }), contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Repeat", style = MaterialTheme.typography.labelMedium, maxLines = 1) } }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Surface(shape = RoundedCornerShape(50), color = if (showInlineLyrics) textButtonColor else sideButtonContainerColor, contentColor = if (showInlineLyrics) iconButtonColor else sideButtonContentColor, modifier = Modifier.height(40.dp).weight(1f), onClick = { showInlineLyrics = !showInlineLyrics }) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.lyrics), contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Lyrics", style = MaterialTheme.typography.labelMedium, maxLines = 1) } }
                                        Surface(shape = RoundedCornerShape(50), color = sideButtonContainerColor, contentColor = sideButtonContentColor, modifier = Modifier.height(40.dp).weight(1f), onClick = { coroutineScope.launch { queueSheetState.expandSoft() } }) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.queue_music), contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Queue", style = MaterialTheme.typography.labelMedium, maxLines = 1) } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 🍎 APPLE MUSIC CONDITIONAL INJECTION 🍎
        // ==========================================
        if (playerStyle.name == "APPLE_MUSIC") {
            mediaMetadata?.let { metadata ->
                AppleMusicPlayerLayout(
                    mediaMetadata = metadata,
                    position = effectivePosition,
                    duration = duration,
                    gradientColors = gradientColors,
                    onClose = { state.collapseSoft() }
                )
            }
        } else {
            // ORIGINAL LAYOUT RENDERING FOR NON-APPLE MUSIC
            when (LocalConfiguration.current.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    val density = LocalDensity.current
                    val verticalPadding = max(WindowInsets.systemBars.getTop(density), WindowInsets.systemBars.getBottom(density))
                    val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                    val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)
                    Row(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets)).padding(bottom = 24.dp).fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).nestedScroll(state.preUpPostDownNestedScrollConnection)) {
                            val currentSliderPosition by rememberUpdatedState(sliderPosition)
                            val sliderPositionProvider = remember { { currentSliderPosition } }
                            val isExpandedProvider = remember(state) { { state.isExpanded } }
                            AnimatedContent(targetState = showInlineLyrics, label = "Lyrics", transitionSpec = { fadeIn() togetherWith fadeOut() }) { showLyrics ->
                                if (showLyrics) { InlineLyricsView(mediaMetadata = mediaMetadata, showLyrics = showLyrics, positionProvider = { effectivePosition }) } else { Thumbnail(sliderPositionProvider = sliderPositionProvider, modifier = Modifier.animateContentSize(), isPlayerExpanded = isExpandedProvider, isLandscape = true, isListenTogetherGuest = isListenTogetherGuest) }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(if (showInlineLyrics) 0.65f else 1f, false).animateContentSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))) {
                            Spacer(Modifier.weight(1f))
                            mediaMetadata?.let { controlsContent(it) }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                else -> {
                    val bottomPadding by animateDpAsState(targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound, label = "bottomPadding")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)).padding(bottom = bottomPadding).animateContentSize()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                            val currentSliderPosition by rememberUpdatedState(sliderPosition)
                            val sliderPositionProvider = remember { { currentSliderPosition } }
                            val isExpandedProvider = remember(state) { { state.isExpanded } }
                            AnimatedContent(targetState = showInlineLyrics, label = "Lyrics", transitionSpec = { fadeIn() togetherWith fadeOut() }) { showLyrics ->
                                if (showLyrics) { InlineLyricsView(mediaMetadata = mediaMetadata, showLyrics = showLyrics, positionProvider = { effectivePosition }) } else { Thumbnail(sliderPositionProvider = sliderPositionProvider, modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection), isPlayerExpanded = isExpandedProvider, isListenTogetherGuest = isListenTogetherGuest) }
                            }
                        }
                        mediaMetadata?.let { controlsContent(it) }
                        Spacer(Modifier.height(if (playerStyle.name == "WAVY") 8.dp else if (playerStyle.name == "VIVI_NEW") 0.dp else 30.dp))
                    }
                }
            }
            
            AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                Queue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    background = if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                    onBackgroundColor = onBackgroundColor,
                    TextBackgroundColor = TextBackgroundColor,
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    pureBlack = pureBlack,
                    showInlineLyrics = showInlineLyrics,
                    playerBackground = playerBackground,
                    onToggleLyrics = { showInlineLyrics = !showInlineLyrics },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(mediaMetadata: MediaMetadata?, showLyrics: Boolean, positionProvider: () -> Long) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember { mutableStateOf(ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, _ -> appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata = remember(queueWindows, currentWindowIndex) { if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) { queueWindows[currentWindowIndex + 1].mediaItem.metadata } else { null } }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.jay.glossy.di.LyricsHelperEntryPoint::class.java)
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query { upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider)) }
                } catch (e: Exception) {}
            }
        }
    }

    LaunchedEffect(nextMetadata?.id, showLyrics, appInForeground, mediaMetadata?.id, currentLyrics) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent = currentLyrics?.let { lyrics -> mediaMetadata == null || lyrics.id == mediaMetadata.id } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.jay.glossy.di.LyricsHelperEntryPoint::class.java)
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query { upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider)) }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        when {
            lyrics == null -> { ContainedLoadingIndicator() }
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> { Text(text = stringResource(R.string.lyrics_not_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center) }
            else -> {
                ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, textAlign = TextAlign.Center)) {
                    Lyrics(sliderPositionProvider = positionProvider, modifier = Modifier.padding(horizontal = 24.dp), showLyrics = showLyrics)
                }
            }
        }
    }
}

@Composable
fun MoreActionsButton(mediaMetadata: MediaMetadata, navController: NavController, state: BottomSheetState, textButtonColor: Color, iconButtonColor: Color) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }
    ) { Image(painter = painterResource(R.drawable.more_horiz), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor)) }
}

@Composable
private fun PlayerMoreMenuButton(mediaMetadata: MediaMetadata, state: BottomSheetState, textButtonColor: Color, iconButtonColor: Color) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }
    ) { Image(painter = painterResource(R.drawable.more_horiz), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor)) }
}

@Composable
fun AnimatedMeshBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val offset1 = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "offset1")
    val offset2 = infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "offset2")
    val offset3 = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "offset3")

    val safeColors = if (colors.size >= 3) colors else listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.tertiaryContainer)

    Canvas(modifier = modifier.fillMaxSize().blur(60.dp)) {
        val w = size.width; val h = size.height
        val o1 = offset1.value; val o2 = offset2.value; val o3 = offset3.value
        drawRect(color = safeColors[0].copy(alpha = 0.3f))
        drawCircle(brush = Brush.radialGradient(colors = listOf(safeColors[0], Color.Transparent), center = Offset(w * o1, h * o2), radius = w * 0.9f), radius = w * 0.9f, center = Offset(w * o1, h * o2))
        drawCircle(brush = Brush.radialGradient(colors = listOf(safeColors[1], Color.Transparent), center = Offset(w * o2, h * o3), radius = w * 0.9f), radius = w * 0.9f, center = Offset(w * o2, h * o3))
        if (safeColors.size > 2) { drawCircle(brush = Brush.radialGradient(colors = listOf(safeColors[2], Color.Transparent), center = Offset(w * o3, h * o1), radius = w * 0.9f), radius = w * 0.9f, center = Offset(w * o3, h * o1)) }
    }
}

// ----------------------------------------------------------------------
// APPLE MUSIC STYLE WRAPPERS & UTILS
// ----------------------------------------------------------------------

fun appleMusicGradientColorAt(seedColor: Color, fraction: Float): Color {
    val top = androidx.compose.ui.graphics.lerp(seedColor, Color.Black, 0.05f)
    val mid = androidx.compose.ui.graphics.lerp(seedColor, Color.Black, 0.32f)
    val bottom = androidx.compose.ui.graphics.lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        androidx.compose.ui.graphics.lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        androidx.compose.ui.graphics.lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}

fun Modifier.appleMusicVerticalFadeEdges(topFade: androidx.compose.ui.unit.Dp, bottomFade: androidx.compose.ui.unit.Dp): Modifier =
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
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }

@Composable
fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicThinSlider(
    value: Float,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val trackHeight by animateDpAsState(
        targetValue = if (pressed || dragged) 14.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "sliderInflate"
    )
    CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp) {
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
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.26f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(activeColor)
                    )
                }
            },
            thumb = { Spacer(Modifier.size(0.dp)) }
        )
    }
}

enum class AppleMusicView { MAIN, LYRICS, QUEUE }

@Composable
fun AppleMusicPlayerLayout(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    gradientColors: List<Color>,
    onClose: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(null)
    
    var viewState by remember { mutableStateOf(AppleMusicView.MAIN) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val currentPosition = sliderPosition ?: position

    val seedColor = gradientColors.firstOrNull() ?: Color(0xFF121212)
    val backdropBrush = Brush.verticalGradient(
        0f to appleMusicGradientColorAt(seedColor, 0f),
        0.48f to appleMusicGradientColorAt(seedColor, 0.48f),
        1f to appleMusicGradientColorAt(seedColor, 1f)
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // EXACT SimpMusic Frosted Background (Unbounded Blur so edges don't cut off)
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        )
        // Correct SimpMusic Tint Overlay
        Box(modifier = Modifier.fillMaxSize().alpha(0.62f).background(backdropBrush))

        Crossfade(targetState = viewState, label = "AppleMusicTabs", modifier = Modifier.fillMaxSize()) { view ->
            when (view) {
                AppleMusicView.MAIN -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Grabber Handle
                        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() }, contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(36.dp, 5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.35f)))
                        }
                        
                        // Fading Artwork
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.85f)
                                    .appleMusicVerticalFadeEdges(0.dp, 300.dp)
                            )
                        }

                        // Title & Actions Row (Matching SimpMusic exactly)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaMetadata.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mediaMetadata.artists.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            val isFavorite = currentSong?.song?.liked == true
                            Icon(
                                painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                                contentDescription = "Like",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                                modifier = Modifier.appleMusicPressInflate().size(32.dp).clickable { playerConnection.toggleLike() }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            val menuState = com.jay.glossy.ui.component.LocalMenuState.current
                            val bottomSheetState = com.jay.glossy.ui.component.LocalBottomSheetPageState.current as? com.jay.glossy.ui.component.BottomSheetState
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.appleMusicPressInflate().size(24.dp).clickable {
                                    if(bottomSheetState != null) {
                                        menuState.show {
                                            com.jay.glossy.ui.menu.PlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                playerBottomSheetState = bottomSheetState,
                                                onShowDetailsDialog = {},
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Bottom Controls
                        AppleMusicBottomClusterGlossy(
                            position = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            onSeek = { sliderPosition = it },
                            onSeekFinished = {
                                sliderPosition?.let { playerConnection.player.seekTo(it) }
                                sliderPosition = null
                            },
                            onPlayPause = { playerConnection.togglePlayPause() },
                            onNext = { playerConnection.seekToNext() },
                            onPrev = { playerConnection.seekToPrevious() },
                            viewState = viewState,
                            onTabSelect = { viewState = it }
                        )
                    }
                }
                AppleMusicView.LYRICS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp))
                        AppleMusicCompactHeader(mediaMetadata = mediaMetadata, onClose = { viewState = AppleMusicView.MAIN })
                        
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Lyrics(
                                sliderPositionProvider = { currentPosition },
                                modifier = Modifier.fillMaxSize(),
                                showLyrics = true
                            )
                        }
                        
                        AppleMusicBottomClusterGlossy(
                            position = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            onSeek = { sliderPosition = it },
                            onSeekFinished = {
                                sliderPosition?.let { playerConnection.player.seekTo(it) }
                                sliderPosition = null
                            },
                            onPlayPause = { playerConnection.togglePlayPause() },
                            onNext = { playerConnection.seekToNext() },
                            onPrev = { playerConnection.seekToPrevious() },
                            viewState = viewState,
                            onTabSelect = { viewState = it },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                AppleMusicView.QUEUE -> {
                    AppleMusicQueueViewGlossy(
                        mediaMetadata = mediaMetadata,
                        position = currentPosition,
                        duration = duration,
                        isPlaying = isPlaying,
                        viewState = viewState,
                        onTabSelect = { viewState = it },
                        onClose = { viewState = AppleMusicView.MAIN },
                        onSeek = { sliderPosition = it },
                        onSeekFinished = {
                            sliderPosition?.let { playerConnection.player.seekTo(it) }
                            sliderPosition = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppleMusicBottomClusterGlossy(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    viewState: AppleMusicView,
    onTabSelect: (AppleMusicView) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        // Slider
        Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
            val safeDuration = if (duration > 0) duration.toFloat() else 1f
            AppleMusicThinSlider(
                value = (position.toFloat() / safeDuration).coerceIn(0f, 1f),
                activeColor = Color.White.copy(alpha = 0.92f),
                onValueChange = { onSeek((it * safeDuration).toLong()) },
                onValueChangeFinished = onSeekFinished,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Times Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(makeTimeString(position), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
            Text("-${makeTimeString(kotlin.math.max(0L, duration - position))}", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Row (Exact SimpMusic Spacing & Circular Icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.appleMusicPressInflate().size(46.dp).clickable { onPrev() }
            )
            Box(
                modifier = Modifier
                    .appleMusicPressInflate()
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(66.dp)
                )
            }
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.appleMusicPressInflate().size(46.dp).clickable { onNext() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Volume Row
        AppleMusicVolumeRowGlossy()

        Spacer(modifier = Modifier.height(24.dp))

        // Dock Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppleMusicDockButton(
                icon = R.drawable.lyrics,
                isActive = viewState == AppleMusicView.LYRICS,
                onClick = { onTabSelect(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS) }
            )
            AppleMusicDockButton(
                icon = R.drawable.queue_music,
                isActive = viewState == AppleMusicView.QUEUE,
                onClick = { onTabSelect(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE) }
            )
        }
    }
}

@Composable
fun AppleMusicVolumeRowGlossy() {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    
    var dragVolume by remember { mutableStateOf<Float?>(null) }
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

    val volume = dragVolume ?: systemVolume

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(painterResource(R.drawable.volume_mute), contentDescription = null, tint = Color.White.copy(alpha=0.72f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(18.dp), contentAlignment = Alignment.Center) {
            AppleMusicThinSlider(
                value = volume.coerceIn(0f, 1f),
                activeColor = Color.White.copy(alpha=0.92f),
                onValueChange = { 
                    dragVolume = it
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxSystemVolume).roundToInt(), 0)
                },
                onValueChangeFinished = { dragVolume = null },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(painterResource(R.drawable.volume_up), contentDescription = null, tint = Color.White.copy(alpha=0.72f), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun AppleMusicDockButton(icon: Int, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .appleMusicPressInflate()
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun AppleMusicCompactHeader(mediaMetadata: MediaMetadata, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clickable { onClose() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mediaMetadata.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicQueueViewGlossy(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    viewState: AppleMusicView,
    onTabSelect: (AppleMusicView) -> Unit,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val currentPlayingUid = if (currentWindowIndex in queueWindows.indices) queueWindows[currentWindowIndex].uid else null
    
    val mutableQueueWindows = remember { androidx.compose.runtime.mutableStateListOf<androidx.media3.common.Timeline.Window>() }
    LaunchedEffect(queueWindows) { mutableQueueWindows.apply { clear(); addAll(queueWindows) } }
    
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val headerItems = 1
    val reorderableState = sh.calvin.reorderable.rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = WindowInsets.systemBars.add(WindowInsets(top = ListItemHeight, bottom = ListItemHeight)).asPaddingValues(),
    ) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) from.index to to.index else currentDragInfo.first to to.index
        val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
        val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
        mutableQueueWindows.move(safeFrom, safeTo)
    }
    
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)
                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(safeFrom, safeTo)
                } else {
                    playerConnection.player.setShuffleOrder(DefaultShuffleOrder(queueWindows.map { it.firstPeriodIndex }.toMutableList().move(safeFrom, safeTo).toIntArray(), System.currentTimeMillis()))
                }
                dragInfo = null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp))
        AppleMusicCompactHeader(mediaMetadata = mediaMetadata, onClose = onClose)
        
        Box(modifier = Modifier.weight(1f).appleMusicVerticalFadeEdges(24.dp, 48.dp)) {
            LazyColumn(
                state = lazyListState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "queue_top_spacer") {
                    Spacer(modifier = Modifier.height(0.dp))
                }

                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    sh.calvin.reorderable.ReorderableItem(
                        state = reorderableState,
                        key = window.uid.hashCode(),
                    ) {
                        val isActive = window.uid == currentPlayingUid

                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.animateItem()) {
                            MediaMetadataListItem(
                                mediaMetadata = window.mediaItem.metadata!!,
                                isSelected = false,
                                isActive = isActive,
                                isPlaying = isPlaying && isActive,
                                trailingContent = {
                                    IconButton(onClick = { }, modifier = Modifier.draggableHandle()) { Icon(painterResource(R.drawable.drag_handle), null, tint = Color.White) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (index == currentWindowIndex) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                playerConnection.player.playWhenReady = true
                                            }
                                        }
                                    ),
                            )
                        }
                    }
                }
            }
        }
        
        AppleMusicBottomClusterGlossy(
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            onPlayPause = { playerConnection.togglePlayPause() },
            onNext = { playerConnection.seekToNext() },
            onPrev = { playerConnection.seekToPrevious() },
            viewState = viewState,
            onTabSelect = onTabSelect
        )
    }
}
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    background: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    pureBlack: Boolean,
    showInlineLyrics: Boolean,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    onToggleLyrics: () -> Unit = {},
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }

    val isHeadsetConnected by produceState(initialValue = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                value = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)
            }
        }

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = isBluetoothHeadphoneConnected(context) || isWiredHeadphoneConnected(context)
                }
            }
        } else null

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }
        
        context.registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }

        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    val (playerStyle) = rememberEnumPreference(
        com.jay.glossy.constants.PlayerStyleKey, 
        defaultValue = com.jay.glossy.constants.PlayerStyle.MODERN
    )

    // Listen Together state
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)

    val currentFormat by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }

    // Cast state
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
            listSaver<MutableList<String>, String>(
                save = { it.toList() },
                restore = { it.toMutableStateList() },
            ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer?.triggerTime,
        playerConnection.service.sleepTimer?.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer?.isActive ?: false
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(Modifier.fillMaxSize().background(Color.Unspecified))
        },
        collapsedContent = {
            if (playerStyle.name == "VIVI_NEW") {
                // EXACT VIVI NEW Bottom Bar Layout
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    val iconTint = TextBackgroundColor
                    val circleBorder = TextBackgroundColor.copy(alpha = 0.35f)
                    val circleBg = Color.Transparent
                    
                    // Queue Button (Left)
                    val queueInteractionSource = remember { MutableInteractionSource() }
                    val isQueuePressed by queueInteractionSource.collectIsPressedAsState()
                    val queueScale by animateFloatAsState(if (isQueuePressed) 0.7f else 1f, spring(0.6f, 500f), label = "queueScale")
                    
                    androidx.compose.material3.IconButton(
                        onClick = { state.expandSoft() },
                        interactionSource = queueInteractionSource,
                        modifier = Modifier.size(48.dp).graphicsLayer(scaleX = queueScale, scaleY = queueScale)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music), 
                            contentDescription = "Queue", 
                            tint = iconTint, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Middle Group (Dynamic: Play/Pause & Next when Lyrics Open, Devices & Sleep Timer when closed)
                    AnimatedContent(
                        targetState = showInlineLyrics,
                        label = "ViviMiddleControls"
                    ) { isLyricsOpen ->
                        if (isLyricsOpen) {
                            // WHEN LYRICS ARE OPEN: Show Play/Pause and Next
                            val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
                            val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
                            val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

                            val onPlayPauseLogic: () -> Unit = {
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

                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                // Play/Pause Button
                                val playInteractionSource = remember { MutableInteractionSource() }
                                val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                                val playScale by animateFloatAsState(if (isPlayPressed) 0.7f else 1f, spring(0.6f, 500f), label = "playScale")

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer(scaleX = playScale, scaleY = playScale)
                                        .clip(CircleShape)
                                        .border(1.dp, circleBorder, CircleShape)
                                        .background(circleBg)
                                        .clickable(
                                            interactionSource = playInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            onPlayPauseLogic()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isListenTogetherGuest) {
                                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                            } else if (isPlaying) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            }
                                        ),
                                        contentDescription = "Play/Pause",
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Next Button
                                val nextInteractionSource = remember { MutableInteractionSource() }
                                val isNextPressed by nextInteractionSource.collectIsPressedAsState()
                                val nextScale by animateFloatAsState(if (isNextPressed) 0.7f else 1f, spring(0.6f, 500f), label = "nextScale")

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer(scaleX = nextScale, scaleY = nextScale)
                                        .clip(CircleShape)
                                        .border(1.dp, circleBorder, CircleShape)
                                        .background(circleBg)
                                        .clickable(
                                            enabled = canSkipNext && !isListenTogetherGuest,
                                            interactionSource = nextInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            playerConnection.seekToNext()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = "Next",
                                        tint = if (canSkipNext && !isListenTogetherGuest) iconTint else iconTint.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            // WHEN LYRICS ARE CLOSED: Show Original Devices and Sleep Timer
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                // Devices Button (Circle Box)
                                val devicesInteractionSource = remember { MutableInteractionSource() }
                                val isDevicesPressed by devicesInteractionSource.collectIsPressedAsState()
                                val devicesScale by animateFloatAsState(if (isDevicesPressed) 0.7f else 1f, spring(0.6f, 500f), label = "devicesScale")
                                
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer(scaleX = devicesScale, scaleY = devicesScale)
                                        .clip(CircleShape)
                                        .border(1.dp, circleBorder, CircleShape)
                                        .background(circleBg)
                                        .clickable(
                                            interactionSource = devicesInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            showAudioDeviceBottomSheet = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isHeadsetConnected) R.drawable.headset_applemusic else R.drawable.speaker_apple
                                        ), 
                                        contentDescription = "Audio Devices", 
                                        tint = iconTint, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                // Sleep Timer Button (Circle to Pill Box)
                                val sleepInteractionSource = remember { MutableInteractionSource() }
                                val isSleepPressed by sleepInteractionSource.collectIsPressedAsState()
                                val sleepScale by animateFloatAsState(if (isSleepPressed) 0.7f else 1f, spring(0.6f, 500f), label = "sleepScale")
                                
                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .widthIn(min = 44.dp)
                                        .animateContentSize()
                                        .graphicsLayer(scaleX = sleepScale, scaleY = sleepScale)
                                        .clip(CircleShape)
                                        .border(1.dp, if (sleepTimerEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else circleBorder, CircleShape)
                                        .background(if (sleepTimerEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else circleBg)
                                        .clickable(
                                            interactionSource = sleepInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            if (sleepTimerEnabled) playerConnection.service.sleepTimer?.clear()
                                            else showSleepTimerDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = if (sleepTimerEnabled) 12.dp else 0.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.bedtime), 
                                            contentDescription = "Sleep Timer", 
                                            tint = if (sleepTimerEnabled) MaterialTheme.colorScheme.primary else iconTint, 
                                            modifier = Modifier.size(20.dp)
                                        )
                                        // Yahan aayega live countdown timer pill shape mein
                                        if (sleepTimerEnabled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = makeTimeString(sleepTimerTimeLeft),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Lyrics Button (Right)
                    val lyricsInteractionSource = remember { MutableInteractionSource() }
                    val isLyricsPressed by lyricsInteractionSource.collectIsPressedAsState()
                    val lyricsScale by animateFloatAsState(if (isLyricsPressed) 0.7f else 1f, spring(0.6f, 500f), label = "lyricsScale")
                    
                    androidx.compose.material3.IconButton(
                        onClick = onToggleLyrics,
                        interactionSource = lyricsInteractionSource,
                        modifier = Modifier.size(48.dp).graphicsLayer(scaleX = lyricsScale, scaleY = lyricsScale)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lyrics), 
                            contentDescription = "Lyrics", 
                            tint = if(showInlineLyrics) MaterialTheme.colorScheme.primary else iconTint, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else if (useNewPlayerDesign) {
                // New design
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 12.dp)
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                            ),
                        ),
                ) {
                    val buttonSize = 42.dp
                    val iconSize = 24.dp
                    val queueShape =
                        RoundedCornerShape(
                            topStart = 50.dp, bottomStart = 50.dp,
                            topEnd = 3.dp, bottomEnd = 3.dp,
                        )
                    val middleShape = RoundedCornerShape(3.dp)
                    val repeatShape =
                        RoundedCornerShape(
                            topStart = 3.dp, bottomStart = 3.dp,
                            topEnd = 50.dp, bottomEnd = 50.dp,
                        )

                    PlayerQueueButton(
                        icon = R.drawable.queue_music,
                        onClick = { state.expandSoft() },
                        isActive = false,
                        shape = queueShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    PlayerQueueButton(
                        icon = R.drawable.bedtime,
                        onClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer?.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        isActive = sleepTimerEnabled,
                        enabled = !isListenTogetherGuest,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        text = if (sleepTimerEnabled) makeTimeString(sleepTimerTimeLeft) else null,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                    PlayerQueueButton(
                        icon = R.drawable.shuffle,
                        onClick = {
                            playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                        },
                        isActive = shuffleModeEnabled,
                        enabled = !isListenTogetherGuest,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    PlayerQueueButton(
                        icon = R.drawable.lyrics,
                        onClick = { onToggleLyrics() },
                        isActive = showInlineLyrics,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    PlayerQueueButton(
                        icon = when (repeatMode) {
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        },
                        onClick = {
                            playerConnection.player.toggleRepeatMode()
                        },
                        isActive = repeatMode != Player.REPEAT_MODE_OFF,
                        enabled = !isListenTogetherGuest,
                        shape = repeatShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(textButtonColor)
                            .clickable {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = mediaMetadata,
                                        playerBottomSheetState = playerBottomSheetState,
                                        onShowDetailsDialog = {
                                            mediaMetadata?.id?.let {
                                                bottomSheetPageState.show {
                                                    ShowMediaInfo(it)
                                                }
                                            }
                                        },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more_vert),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = iconButtonColor,
                        )
                    }
                }
            } else {
                // Old design
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 12.dp)
                        .windowInsetsPadding(
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        ),
                ) {
                    TextButton(
                        onClick = { state.expandSoft() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.queue),
                                color = TextBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }

                    TextButton(
                        enabled = !isListenTogetherGuest,
                        onClick = {
                            if (!isListenTogetherGuest) {
                                if (sleepTimerEnabled) {
                                    playerConnection.service.sleepTimer?.clear()
                                } else {
                                    showSleepTimerDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.bedtime),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AnimatedContent(
                                label = "sleepTimer",
                                targetState = sleepTimerEnabled,
                            ) { enabled ->
                                if (enabled) {
                                    Text(
                                        text = makeTimeString(sleepTimerTimeLeft),
                                        color = TextBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.sleep_timer),
                                        color = TextBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { onToggleLyrics() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.lyrics),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.lyrics),
                                color = TextBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }
                }
            }
            
            if (showAudioDeviceBottomSheet) {
                AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
            }

            if (showSleepTimerDialog) {
                ActionPromptDialog(
                    titleBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    },
                    onDismiss = { showSleepTimerDialog = false },
                    onConfirm = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer?.start(
                            minute = sleepTimerValue.roundToInt(),
                            stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                            fadeOut = sleepTimerFadeOut,
                        )
                    },
                    onCancel = {
                        showSleepTimerDialog = false
                    },
                    onReset = {
                        sleepTimerValue = sleepTimerDefault
                    },
                    content = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text =
                                    pluralStringResource(
                                        R.plurals.minute,
                                        sleepTimerValue.roundToInt(),
                                        sleepTimerValue.roundToInt(),
                                    ),
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Spacer(Modifier.height(16.dp))

                            Slider(
                                value = sleepTimerValue,
                                onValueChange = { sleepTimerValue = it },
                                valueRange = 5f..120f,
                                steps = (120 - 5) / 5 - 1,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(8.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (isAtDefault) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                context.safeDataStoreEdit { settings ->
                                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                                }
                                            }
                                            Toast.makeText(
                                                context,
                                                String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    ) {
                                        Text(stringResource(R.string.set_as_default))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                context.safeDataStoreEdit { settings ->
                                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                                }
                                            }
                                            Toast.makeText(
                                                context,
                                                String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                    ) {
                                        Text(stringResource(R.string.set_as_default))
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showSleepTimerDialog = false
                                        playerConnection.service.sleepTimer?.start(minute = -1)
                                    },
                                ) {
                                    Text(stringResource(R.string.end_of_song))
                                }
                            }
                        }
                    },
                )
            }
        },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
        val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
        val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength = remember(queueWindows) {
            queueWindows.sumOf { it.mediaItem.metadata!!.duration }
        }

        val coroutineScope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val headerItems = 1

        val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
            if (currentWindowIndex in queueWindows.indices) {
                queueWindows[currentWindowIndex].uid
            } else null
        }

        val reorderableState = sh.calvin.reorderable.rememberReorderableLazyListState(
            lazyListState = lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars
                .add(WindowInsets(top = ListItemHeight, bottom = ListItemHeight))
                .asPaddingValues(),
        ) { from, to ->
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                from.index to to.index
            } else {
                currentDragInfo.first to to.index
            }

            val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
            val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)

            mutableQueueWindows.move(safeFrom, safeTo)
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    } else {
                        playerConnection.player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows.map { it.firstPeriodIndex }
                                    .toMutableList()
                                    .move(safeFrom, safeTo)
                                    .toIntArray(),
                                System.currentTimeMillis(),
                            ),
                        )
                    }
                    dragInfo = null
                }
            }
        }

        LaunchedEffect(queueWindows) {
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(mutableQueueWindows, currentWindowIndex) {
            if (currentWindowIndex != -1) {
                lazyListState.scrollToItem(currentWindowIndex)
            }
        }

        // ====== VIVI DESIGN PORT ======
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Like") } },
                        state = rememberTooltipState(),
                    ) {
                        FilledTonalIconButton(
                            onClick = playerConnection::toggleLike,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                                ),
                                contentDescription = "Like",
                                tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current
                            )
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(if (locked) "Unlock Queue" else "Lock Queue") } },
                        state = rememberTooltipState(),
                    ) {
                        FilledTonalIconButton(
                            onClick = { locked = !locked },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                contentDescription = "Lock",
                            )
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("More Options") } },
                        state = rememberTooltipState(),
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = mediaMetadata,
                                        playerBottomSheetState = playerBottomSheetState,
                                        onShowDetailsDialog = {
                                            mediaMetadata?.id?.let {
                                                bottomSheetPageState.show {
                                                    ShowMediaInfo(it)
                                                }
                                            }
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = "More Options",
                            )
                        }
                    }
                }

                val shuffleModeEnabledInside by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()

                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ToggleButton(
                        checked = shuffleModeEnabledInside,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(if (shuffleModeEnabledInside) currentWindowIndex else 0)
                            }.invokeOnCompletion {
                                playerConnection.player.shuffleModeEnabled = checked
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = "Shuffle",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ToggleButton(
                        checked = repeatMode != Player.REPEAT_MODE_OFF,
                        onCheckedChange = { playerConnection.player.toggleRepeatMode() },
                        enabled = !isListenTogetherGuest,
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            try {
                                playerConnection.startRadioSeamlessly()
                                Toast.makeText(context, "Starting Radio", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Radio not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = "Radio",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Continue playing",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Next in queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, queueWindows.size, queueWindows.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = makeTimeString(queueLength * 1000L),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                    visible = inSelectMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    val selectedSongs = remember(selection.toList(), mutableQueueWindows) {
                        mutableQueueWindows.filter { it.mediaItem.mediaId in selection }
                            .mapNotNull { it.mediaItem.metadata }
                    }
                    val selectedItems = remember(selection.toList(), mutableQueueWindows) {
                        mutableQueueWindows.filter { it.mediaItem.mediaId in selection }
                    }
                    val count = selection.size
                    Row(
                        modifier = Modifier.height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onExitSelectionMode) {
                            Icon(painter = painterResource(R.drawable.close), contentDescription = null)
                        }
                        Text(
                            text = pluralStringResource(R.plurals.n_selected, count, count),
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = count == mutableQueueWindows.size && count > 0,
                            onCheckedChange = {
                                if (count == mutableQueueWindows.size) {
                                    selection.clear()
                                } else {
                                    selection.clear()
                                    mutableQueueWindows.forEach { selection.add(it.mediaItem.mediaId) }
                                }
                            }
                        )
                        IconButton(
                            enabled = count > 0,
                            onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = selectedSongs,
                                        onDismiss = menuState::dismiss,
                                        clearAction = onExitSelectionMode,
                                        currentItems = selectedItems,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = LocalContentColor.current,
                            )
                        }
                    }
                }
            }

            // List Container
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        .add(WindowInsets(top = 8.dp, bottom = ListItemHeight + 8.dp))
                        .asPaddingValues(),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                ) {
                    item(key = "queue_top_spacer") {
                        Spacer(
                            modifier = Modifier
                                .animateContentSize()
                                .height(if (inSelectMode) 48.dp else 0.dp),
                        )
                    }

                    itemsIndexed(
                        items = mutableQueueWindows,
                        key = { _, item -> item.uid.hashCode() },
                    ) { index, window ->
                        sh.calvin.reorderable.ReorderableItem(
                            state = reorderableState,
                            key = window.uid.hashCode(),
                        ) {
                            val currentItem by rememberUpdatedState(window)
                            val isActive = window.uid == currentPlayingUid
                            val dismissBoxState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance }
                            )

                            var processedDismiss by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss && !isListenTogetherGuest && (
                                            dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart
                                        )
                                ) {
                                    processedDismiss = true
                                    playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                    dismissJob?.cancel()
                                    dismissJob = coroutineScope.launch {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.removed_song_from_playlist,
                                                currentItem.mediaItem.metadata?.title,
                                            ),
                                            actionLabel = context.getString(R.string.undo),
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            playerConnection.player.addMediaItem(currentItem.mediaItem)
                                            playerConnection.player.moveMediaItem(
                                                mutableQueueWindows.size,
                                                currentItem.firstPeriodIndex,
                                            )
                                        }
                                    }
                                }
                                if (dv == SwipeToDismissBoxValue.Settled) {
                                    processedDismiss = false
                                }
                            }

                            val onCheckedChange: (Boolean) -> Unit = {
                                if (it) selection.add(window.mediaItem.mediaId)
                                else selection.remove(window.mediaItem.mediaId)
                            }

                            val content: @Composable () -> Unit = {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.animateItem(),
                                ) {
                                    MediaMetadataListItem(
                                        mediaMetadata = window.mediaItem.metadata!!,
                                        isSelected = false,
                                        isActive = isActive,
                                        isPlaying = isPlaying && isActive,
                                        trailingContent = {
                                            if (inSelectMode) {
                                                Checkbox(
                                                    checked = window.mediaItem.mediaId in selection,
                                                    onCheckedChange = onCheckedChange
                                                )
                                            } else {
                                                if (!isListenTogetherGuest) {
                                                    IconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                QueueMenu(
                                                                    mediaMetadata = window.mediaItem.metadata!!,
                                                                    playerBottomSheetState = playerBottomSheetState,
                                                                    onShowDetailsDialog = {
                                                                        window.mediaItem.mediaId.let {
                                                                            bottomSheetPageState.show {
                                                                                ShowMediaInfo(it)
                                                                            }
                                                                        }
                                                                    },
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.more_vert),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                }
                                                if (!locked && !isListenTogetherGuest) {
                                                    IconButton(
                                                        onClick = { },
                                                        modifier = Modifier.draggableHandle()
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.drag_handle),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(background)
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelectMode) {
                                                        onCheckedChange(window.mediaItem.mediaId !in selection)
                                                    } else if (!isListenTogetherGuest) {
                                                        if (index == currentWindowIndex) {
                                                            if (isCasting) {
                                                                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                                            } else {
                                                                playerConnection.togglePlayPause()
                                                            }
                                                        } else {
                                                            if (isCasting) {
                                                                val mediaId = window.mediaItem.mediaId
                                                                val navigated = castHandler?.navigateToMediaIfInQueue(mediaId) ?: false
                                                                if (!navigated) {
                                                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                }
                                                            } else {
                                                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                playerConnection.player.playWhenReady = true
                                                            }
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inSelectMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        inSelectMode = true
                                                        onCheckedChange(true)
                                                    }
                                                },
                                            ),
                                    )
                                }
                            }

                            if (locked) {
                                content()
                            } else {
                                SwipeToDismissBox(
                                    state = dismissBoxState,
                                    backgroundContent = {},
                                ) {
                                    content()
                                }
                            }
                        }
                    }

                    if (automix.isNotEmpty()) {
                        item(key = "automix_divider") {
                            Text(
                                text = stringResource(R.string.similar_content),
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }

                        itemsIndexed(
                            items = automix,
                            key = { _, it -> it.mediaId },
                        ) { index, item ->
                            Row(
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                MediaMetadataListItem(
                                    mediaMetadata = item.metadata!!,
                                    trailingContent = {
                                        if (!isListenTogetherGuest) {
                                            IconButton(
                                                onClick = {
                                                    playerConnection.service.playNextAutomix(item, index)
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.playlist_play),
                                                    contentDescription = null,
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    playerConnection.service.addToQueueAutomix(item, index)
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.queue_music),
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                menuState.show {
                                                    QueueMenu(
                                                        mediaMetadata = item.metadata!!,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        onShowDetailsDialog = {
                                                            item.mediaId.let {
                                                                bottomSheetPageState.show {
                                                                    ShowMediaInfo(it)
                                                                }
                                                            }
                                                        },
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(
                            bottom = ListItemHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                        )
                        .align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun PlayerQueueButton(
    icon: Int,
    onClick: () -> Unit,
    isActive: Boolean,
    enabled: Boolean = true,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    text: String? = null,
    textButtonColor: Color,
    iconButtonColor: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    textBackgroundColor: Color,
    playerBackground: PlayerBackgroundStyle
) {
    val buttonModifier = Modifier
        .clip(shape)
        .clickable(enabled = enabled, onClick = onClick)

    val alphaFactor = if (enabled) 1f else 0.35f

    val appliedModifier = if (isActive) {
        modifier.then(buttonModifier.background(textButtonColor)).alpha(alphaFactor)
    } else {
        modifier.then(
            buttonModifier.border(
                width = 1.dp,
                color = textButtonColor.copy(alpha = 0.3f),
                shape = shape
            )
        ).alpha(alphaFactor)
    }

    Box(
        modifier = appliedModifier,
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                color = iconButtonColor.copy(alpha = if (enabled) 1f else 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
        } else {
            val baseTint = if (isActive) {
                iconButtonColor
            } else {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_MESH ->
                        Color.White
                    PlayerBackgroundStyle.DEFAULT ->
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            }
            val finalTint = if (enabled) baseTint else baseTint.copy(alpha = 0.5f)
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = finalTint
            )
        }
    }
}
