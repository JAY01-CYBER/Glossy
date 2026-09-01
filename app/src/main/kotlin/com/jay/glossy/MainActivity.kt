/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy

import com.jay.glossy.R

import android.Manifest
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.jay.glossy.constants.AppBarHeight
import com.jay.glossy.constants.AppLanguageKey
import com.jay.glossy.constants.CheckForUpdatesKey
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.DefaultOpenTabKey
import com.jay.glossy.constants.DisableScreenshotKey
import com.jay.glossy.constants.DynamicThemeKey
import com.jay.glossy.constants.EnableHighRefreshRateKey
import com.jay.glossy.constants.EnableLandscapeScalingKey
import com.jay.glossy.constants.ExperimentalLyricsKey
import com.jay.glossy.constants.LastSeenVersionKey
import com.jay.glossy.constants.ListenTogetherInTopBarKey
import com.jay.glossy.constants.ListenTogetherUsernameKey
import com.jay.glossy.constants.LyricsProviderOrderKey
import com.jay.glossy.constants.MiniPlayerBottomSpacing
import com.jay.glossy.constants.MiniPlayerHeight
import com.jay.glossy.constants.NavigationBarAnimationSpec
import com.jay.glossy.constants.NavigationBarHeight
import com.jay.glossy.constants.PauseListenHistoryKey
import com.jay.glossy.constants.PauseSearchHistoryKey
import com.jay.glossy.constants.PreferredLyricsProvider
import com.jay.glossy.constants.PreferredLyricsProviderKey
import com.jay.glossy.constants.PureBlackKey
import com.jay.glossy.constants.SYSTEM_DEFAULT
import com.jay.glossy.constants.SelectedThemeColorKey
import com.jay.glossy.constants.SimpMusicMigrationDoneKey
import com.jay.glossy.constants.SlimNavBarHeight
import com.jay.glossy.constants.SlimNavBarKey
import com.jay.glossy.constants.StopMusicOnTaskClearKey
import com.jay.glossy.constants.UpdateNotificationsEnabledKey
import com.jay.glossy.constants.UseFloatingNavBarKey
import com.jay.glossy.constants.UseNewMiniPlayerDesignKey
import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.db.entities.SearchHistory
import com.jay.glossy.extensions.toEnum
import com.jay.glossy.lyrics.LyricsProviderRegistry
import com.metrolist.models.toMediaMetadata
import com.jay.glossy.playback.DownloadUtil
import com.jay.glossy.playback.MusicService
import com.jay.glossy.playback.MusicService.MusicBinder
import com.jay.glossy.playback.PlayerConnection
import com.jay.glossy.playback.queues.YouTubeQueue
import com.jay.glossy.ui.component.AccountSettingsDialog
import com.jay.glossy.ui.component.AppNavigationBar
import com.jay.glossy.ui.component.AppNavigationRail
import com.jay.glossy.ui.component.BottomSheetMenu
import com.jay.glossy.ui.component.BottomSheetPage
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.rememberBottomSheetState
import com.jay.glossy.ui.component.shimmer.ShimmerTheme
import com.jay.glossy.ui.menu.YouTubeSongMenu
import com.jay.glossy.ui.player.BottomSheetPlayer
import com.jay.glossy.ui.screens.Screens
import com.jay.glossy.ui.screens.navigationBuilder
import com.jay.glossy.ui.screens.settings.ChangelogScreen
import com.jay.glossy.ui.screens.settings.DarkMode
import com.jay.glossy.ui.screens.settings.NavigationTab
import com.jay.glossy.ui.theme.ColorSaver
import com.jay.glossy.ui.theme.DefaultThemeColor
import com.jay.glossy.ui.theme.MetrolistTheme
import com.jay.glossy.ui.theme.extractThemeColor
import com.jay.glossy.ui.utils.appBarScrollBehavior
import com.jay.glossy.ui.utils.resetHeightOffset
import com.jay.glossy.utils.ReleaseInfo
import com.jay.glossy.utils.SearchRoutes
import com.jay.glossy.utils.SyncUtils
import com.jay.glossy.utils.Updater
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.safeDataStoreEdit
import com.jay.glossy.utils.get
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.reportException
import com.jay.glossy.utils.setAppLocale
import com.jay.glossy.viewmodels.HomeViewModel
import com.jay.glossy.widget.PlaylistWidgetReceiver
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.ui.platform.LocalContext

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.jay.glossy.action.SEARCH"
        private const val ACTION_LIBRARY = "com.jay.glossy.action.LIBRARY"
        const val ACTION_RECOGNITION = "com.jay.glossy.action.RECOGNITION"
        const val ACTION_OPEN_WIDGET_TARGET = "com.jay.glossy.action.OPEN_WIDGET_TARGET"
        const val EXTRA_AUTO_START_RECOGNITION = "auto_start_recognition"
        const val EXTRA_WIDGET_TARGET_TYPE = "widget_target_type"
        const val EXTRA_WIDGET_TARGET_ID = "widget_target_id"
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var listenTogetherManager: com.jay.glossy.listentogether.ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    // Keep PlayerConnection as regular property - NOT mutableStateOf to prevent UI recomposition
    // when it becomes null during onStop. Only update the snapshot for Compose when needed.
    private var playerConnection: PlayerConnection? = null

    // This is the snapshot we pass to Compose - changes here trigger recomposition
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)

    private var isServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playerConnectionSnapshot = playerConnection
                    listenTogetherManager.setPlayerConnection(playerConnection)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Disconnect Listen Together manager
                listenTogetherManager.setPlayerConnection(null)
                playerConnection?.dispose()
                // DO NOT null out playerConnection here - keep it for when service reconnects
                // DO NOT update playerConnectionSnapshot - this is the key to preventing recomposition
            }
        }

    private fun safeUnbindService(source: String) {
        if (!isServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.tag("MainActivity").w(e, "Service was not bound when attempting to unbind in $source")
        } finally {
            isServiceBound = false
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            // DO NOT null out playerConnection here - keep it for reconnection
            // DO NOT update playerConnectionSnapshot - this prevents UI recomposition
        }
    }

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        // Start the playback service explicitly once so it can outlive binding.
        // Re-issuing startForegroundService() while an existing service instance is already
        // running can trigger "did not then call startForeground" on some Android 9 devices
        // when the framework expects a fresh foreground promotion for that start request.
        if (!MusicService.isRunning) {
            val serviceIntent = Intent(this, MusicService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Timber.w(e, "Cannot start foreground service from background")
            } catch (e: IllegalStateException) {
                Timber.w(e, "Failed to start foreground service")
            }
        }

        // Bind to service - if already bound, this is a no-op but ensures we stay connected
        if (!isServiceBound) {
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
            isServiceBound = true
        }
    }

    override fun onStop() {
        // Keep the service binding, PlayerConnection and Listen Together wiring alive while
        // the Activity is backgrounded. The MusicService is a foreground service and keeps
        // running, so the host must keep reporting playback state to the LT server; detaching
        // the player listener here used to break LT for any host that wasn't staring at the
        // app the whole session. Full teardown happens in onDestroy() via safeUnbindService().
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            listenTogetherManager.disconnect()
        }
        super.onDestroy()
        // Use effective playing state so Cast (local player paused, remote playing) is included.
        val stopServiceOnClear =
            dataStore.get(StopMusicOnTaskClearKey, false) &&
                playerConnection?.isEffectivelyPlaying?.value == true &&
                isFinishing

        // Full cleanup - only on actual destroy
        playerConnection?.dispose()
        playerConnection = null
        playerConnectionSnapshot = null

        // Unbind before stopService: a started+bound service does not stop until all clients unbind.
        safeUnbindService("onDestroy()")

        if (stopServiceOnClear) {
            stopService(Intent(this, MusicService::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleWidgetTargetIntent(intent, navController)
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Listen Together manager
        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale =
                dataStore[AppLanguageKey]
                    ?.takeUnless { it == SYSTEM_DEFAULT }
                    ?.let { Locale.forLanguageTag(it) }
                    ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // Defer migration and version tracking to avoid blocking first frame
        lifecycleScope.launch(Dispatchers.IO) {
            val preferences = dataStore.data.first()
            val currentVersion = BuildConfig.VERSION_NAME

            // SimpMusic Removal Migration
            if (preferences[SimpMusicMigrationDoneKey] != true) {
                safeDataStoreEdit { settings ->
                    val currentOrder = settings[LyricsProviderOrderKey] ?: ""
                    if (currentOrder.contains("SimpMusic")) {
                        val orderList =
                            currentOrder
                                .split(";")
                                .map { it.trim() }
                                .filter { it.isNotBlank() && it != "SimpMusic" }
                                .toMutableList()
                        if (orderList.isEmpty()) {
                            settings[LyricsProviderOrderKey] = ""
                        } else {
                            settings[LyricsProviderOrderKey] = orderList.joinToString(";")
                        }
                    }
                    if (settings[PreferredLyricsProviderKey] == "SIMPMUSIC") {
                        settings[PreferredLyricsProviderKey] = PreferredLyricsProvider.LRCLIB.name
                    }
                    settings[SimpMusicMigrationDoneKey] = true
                    settings[LastSeenVersionKey] = currentVersion
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            safeDataStoreEdit { settings ->
                settings[LastSeenVersionKey] = BuildConfig.VERSION_NAME
            }
        }

        setContent {
            MetrolistApp(
                latestVersionName = latestVersionName,
                onLatestVersionNameChange = { latestVersionName = it },
                playerConnection = playerConnectionSnapshot,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MetrolistApp(
        latestVersionName: String,
        onLatestVersionNameChange: (String) -> Unit,
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)
        var kmpRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
        var kmpUpgradeDismissed by rememberSaveable { mutableStateOf(false) }

        if (BuildConfig.UPDATER_AVAILABLE) {
            LaunchedEffect(checkForUpdates) {
                if (checkForUpdates) {
                    withContext(Dispatchers.IO) {
                        val updatesEnabled = dataStore.get(CheckForUpdatesKey, true)
                        val notifEnabled = dataStore.get(UpdateNotificationsEnabledKey, true)
                        if (!updatesEnabled) return@withContext

                        Updater.checkForUpdate().onSuccess { (releaseInfo, hasUpdate) ->
                            if (releaseInfo != null) {
                                onLatestVersionNameChange(releaseInfo.versionName)
                                if (hasUpdate && notifEnabled) {
                                    val downloadUrl = Updater.getDownloadUrlForCurrentVariant(releaseInfo)
                                    if (downloadUrl != null) {
                                        val intent = Intent(Intent.ACTION_VIEW, downloadUrl.toUri())

                                        val flags =
                                            PendingIntent.FLAG_UPDATE_CURRENT or
                                                (PendingIntent.FLAG_IMMUTABLE)
                                        val pending = PendingIntent.getActivity(this@MainActivity, 1001, intent, flags)

                                        val notif =
                                            NotificationCompat
                                                .Builder(this@MainActivity, "updates")
                                                .setSmallIcon(R.drawable.update)
                                                .setContentTitle(getString(R.string.update_available_title))
                                                .setContentText(releaseInfo.versionName)
                                                .setContentIntent(pending)
                                                .setAutoCancel(true)
                                                .build()

                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) ==
                                            PackageManager.PERMISSION_GRANTED
                                        ) {
                                            NotificationManagerCompat.from(this@MainActivity).notify(1001, notif)
                                        }
                                    }
                                }
                            }
                        }

                        Updater.getLatestKmpRelease().onSuccess { releaseInfo ->
                            kmpRelease = releaseInfo
                        }
                    }
                } else {
                    onLatestVersionNameChange(BuildConfig.VERSION_NAME)
                    kmpRelease = null
                }
            }
        }

        val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

        LaunchedEffect(enableHighRefreshRate) {
            val window = this@MainActivity.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val layoutParams = window.attributes
                if (enableHighRefreshRate) {
                    layoutParams.preferredDisplayModeId = 0
                } else {
                    val modes = window.windowManager.defaultDisplay.supportedModes
                    val mode60 =
                        modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                            ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }

                    if (mode60 != null) {
                        layoutParams.preferredDisplayModeId = mode60.modeId
                    }
                }
                window.attributes = layoutParams
            } else {
                val params = window.attributes
                if (enableHighRefreshRate) {
                    params.preferredRefreshRate = 0f
                } else {
                    params.preferredRefreshRate = 60f
                }
                window.attributes = params
            }
        }

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }

        LaunchedEffect(useDarkTheme) {
            setSystemBarAppearance(useDarkTheme)
        }

        val enableLandscapeScaling by rememberPreference(EnableLandscapeScalingKey, defaultValue = false)
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor = Color(selectedThemeColorInt)

        val showChangelog = rememberSaveable { mutableStateOf(false) }

        var themeColor by rememberSaveable(stateSaver = ColorSaver) {
            mutableStateOf(selectedThemeColor)
        }

        val themeColorCache = remember { mutableMapOf<String, Color>() }

        LaunchedEffect(selectedThemeColor) {
            if (!enableDynamicTheme) {
                themeColor = selectedThemeColor
            }
        }

        LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
            val playerConnection = playerConnection
            if (!enableDynamicTheme || playerConnection == null) {
                themeColor = selectedThemeColor
                return@LaunchedEffect
            }

            playerConnection.service.currentMediaMetadata
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { song ->
                    if (song?.thumbnailUrl != null) {
                        val cached = themeColorCache[song.thumbnailUrl]
                        if (cached != null) {
                            withFrameNanos { }
                            themeColor = cached
                            return@collectLatest
                        }
                        withContext(Dispatchers.IO) {
                            try {
                                val result =
                                    imageLoader.execute(
                                        ImageRequest
                                            .Builder(this@MainActivity)
                                            .data(song.thumbnailUrl)
                                            .allowHardware(false)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(false)
                                            .build(),
                                    )
                                val extractedColor = result.image?.toBitmap()?.extractThemeColor() ?: selectedThemeColor
                                themeColorCache[song.thumbnailUrl] = extractedColor
                                withFrameNanos { }
                                themeColor = extractedColor
                            } catch (e: Exception) {
                                withFrameNanos { }
                                themeColor = selectedThemeColor
                            }
                        }
                    } else {
                        themeColor = selectedThemeColor
                    }
                }
        }

        MetrolistTheme(
            darkTheme = useDarkTheme,
            pureBlack = pureBlack,
            themeColor = themeColor,
        ) {
            val currentDensity = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val containerSize = windowInfo.containerDpSize
            val smallestDimensionDp = minOf(containerSize.width, containerSize.height)

            val densityScale = remember(smallestDimensionDp, enableLandscapeScaling) {
                if (enableLandscapeScaling) {
                    when {
                        smallestDimensionDp >= 840.dp -> 1.15f
                        smallestDimensionDp >= 720.dp -> 1.1f
                        smallestDimensionDp >= 600.dp -> 1.05f
                        else -> 1.0f
                    }
                } else {
                    1.0f
                }
            }
            val scaledDensity: Density = remember(currentDensity, densityScale) {
                Density(
                    density = currentDensity.density * densityScale,
                    fontScale = currentDensity.fontScale,
                )
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface),
            ) {
                val density = LocalDensity.current
                val configuration = LocalWindowInfo.current
                val cutoutInsets = WindowInsets.displayCutout
                val windowsInsets = WindowInsets.systemBars
                val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                val navController = rememberNavController()

                val context = LocalContext.current
                val hasSeenWelcome = remember {
                    context.dataStore.get(booleanPreferencesKey("has_seen_welcome"), false)
                }

                LaunchedEffect(Unit) {
                    val lastSeenVersion = dataStore.data.first()[LastSeenVersionKey] ?: ""
                    val currentVersion = BuildConfig.VERSION_NAME
                    if (lastSeenVersion != currentVersion) {
                        showChangelog.value = true
                    }
                }

                val homeViewModel: HomeViewModel = hiltViewModel()
                val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
                val navigationItems =
                    remember(listenTogetherInTopBar) {
                        if (listenTogetherInTopBar) {
                            Screens.MainScreens.filter { it != Screens.ListenTogether }
                        } else {
                            Screens.MainScreens
                        }
                    }
                val routeIndexMap = remember(navigationItems) {
                    navigationItems.mapIndexed { i, s -> s.route to i }.toMap()
                }
                val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                val (useFloatingNavBar) = rememberPreference(UseFloatingNavBarKey, defaultValue = false)
                val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                val (defaultOpenTabInt) = rememberPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME.name)
                val defaultOpenTab = remember(defaultOpenTabInt) {
                    try {
                        NavigationTab.valueOf(defaultOpenTabInt)
                    } catch (_: IllegalArgumentException) {
                        NavigationTab.HOME
                    }
                }
                val tabOpenedFromShortcut =
                    remember {
                        when (intent?.action) {
                            ACTION_SEARCH -> NavigationTab.LIBRARY
                            ACTION_LIBRARY -> NavigationTab.SEARCH
                            else -> null
                        }
                    }

                val topLevelScreens =
                    remember {
                        listOf(
                            Screens.Home.route,
                            Screens.Library.route,
                            Screens.Mix.route,
                            Screens.ListenTogether.route,
                            "settings",
                        )
                    }

                val (query, onQueryChange) =
                    rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }

                val onSearch: (String) -> Unit =
                    remember {
                        { searchQuery ->
                            if (searchQuery.isNotEmpty()) {
                                navController.navigate(SearchRoutes.resultRoute(searchQuery))

                                if (dataStore[PauseSearchHistoryKey] != true) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        runCatching {
                                            database.insert(SearchHistory(query = searchQuery))
                                        }.onFailure { throwable ->
                                            Timber
                                                .tag("MainActivity")
                                                .w(throwable, "Failed to save search history for query: %s", searchQuery)
                                        }
                                    }
                                }
                            }
                        }
                    }

                val currentRoute by remember {
                    derivedStateOf { navBackStackEntry?.destination?.route }
                }

                val inSearchScreen by remember {
                    derivedStateOf { currentRoute?.startsWith("search/") == true }
                }
                val navigationItemRoutes =
                    remember(navigationItems) {
                        navigationItems.map { it.route }.toSet()
                    }

                val shouldShowNavigationBar =
                    remember(currentRoute, navigationItemRoutes) {
                        currentRoute == null ||
                            navigationItemRoutes.contains(currentRoute) ||
                            currentRoute!!.startsWith("search/")
                    }

                val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height
                val isTablet = configuration.containerDpSize.width >= 600.dp

                val showRail = (isLandscape || isTablet) && !inSearchScreen

                val navPadding =
                    if (shouldShowNavigationBar && !showRail) {
                        if (slimNav) SlimNavBarHeight else NavigationBarHeight
                    } else {
                        0.dp
                    }

                val navigationBarHeight by animateDpAsState(
                    targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                    animationSpec = NavigationBarAnimationSpec,
                    label = "navBarHeight",
                )

                val playerBottomSheetState =
                    rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound =
                            bottomInset +
                                (if (!showRail && shouldShowNavigationBar) {
                                    if (useFloatingNavBar) 76.dp else navPadding
                                } else 0.dp) +
                                (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                val playerReadyState =
                    playerConnection?.service?.isPlayerReady?.collectAsStateWithLifecycle()
                        ?: remember { mutableStateOf(false) }
                val playerReady by playerReadyState
                val activePlayerConnection = if (playerReady) playerConnection else null

                val playerAwareWindowInsets =
                    remember(
                        bottomInset,
                        shouldShowNavigationBar,
                        playerBottomSheetState.isDismissed,
                        showRail,
                        useFloatingNavBar
                    ) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar && !showRail) {
                            bottom += if (useFloatingNavBar) 76.dp else NavigationBarHeight
                        }
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }
                appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

                val topAppBarScrollBehavior =
                    appBarScrollBehavior(
                        canScroll = {
                            !inSearchScreen &&
                                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        },
                    )

                // Navigation tracking
                LaunchedEffect(navBackStackEntry) {
                    if (inSearchScreen) {
                        val searchQuery =
                            SearchRoutes.decodeQuery(
                                navBackStackEntry?.arguments?.getString("query").orEmpty(),
                            )
                        onQueryChange(
                            TextFieldValue(
                                searchQuery,
                                TextRange(searchQuery.length),
                            ),
                        )
                    } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        onQueryChange(TextFieldValue())
                    }

                    // Reset scroll behavior for main navigation items
                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        if (navigationItems.fastAny { it.route == previousTab }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    topAppBarScrollBehavior.state.resetHeightOffset()

                    // Collapse player when navigating to equalizer
                    if (navBackStackEntry?.destination?.route == "equalizer" &&
                        playerBottomSheetState.isExpanded
                    ) {
                        playerBottomSheetState.collapseSoft()
                    }

                    // Track previous tab for animations
                    navController.currentBackStackEntry?.destination?.route?.let {
                        setPreviousTab(it)
                    }
                }

                LaunchedEffect(activePlayerConnection) {
                    val player = runCatching { activePlayerConnection?.player }.getOrNull()
                    if (player?.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                        return@LaunchedEffect
                    }

                    if (playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }

                DisposableEffect(activePlayerConnection, playerBottomSheetState) {
                    val player = runCatching { activePlayerConnection?.player }.getOrNull()
                        ?: return@DisposableEffect onDispose { }
                    val listener =
                        object : Player.Listener {
                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int,
                            ) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                    mediaItem != null &&
                                    playerBottomSheetState.isDismissed
                                ) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                    player.addListener(listener)
                    onDispose {
                        player.removeListener(listener)
                    }
                }

                var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry, listenTogetherInTopBar) {
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isListenTogetherScreen =
                        currentRoute == Screens.ListenTogether.route ||
                            currentRoute == "listen_together_from_topbar"
                    shouldShowTopBar = currentRoute in topLevelScreens &&
                        currentRoute != "settings" &&
                        !(isListenTogetherScreen && listenTogetherInTopBar)
                }

                val coroutineScope = rememberCoroutineScope()
                var sharedSong: SongItem? by remember {
                    mutableStateOf(null)
                }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    if (pendingIntent != null) {
                        handleWidgetTargetIntent(pendingIntent!!, navController)
                        handleRecognitionIntent(pendingIntent!!, navController)
                        handleDeepLinkIntent(pendingIntent!!, navController)
                        pendingIntent = null
                    } else {
                        handleWidgetTargetIntent(intent, navController)
                        handleRecognitionIntent(intent, navController)
                        handleDeepLinkIntent(intent, navController)
                    }
                }

                DisposableEffect(Unit) {
                    val listener =
                        Consumer<Intent> { intent ->
                            handleWidgetTargetIntent(intent, navController)
                            handleRecognitionIntent(intent, navController)
                            handleDeepLinkIntent(intent, navController)
                        }

                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                var showAccountDialog by remember { mutableStateOf(false) }

                val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
                val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
                val showHistoryButton =
                    remember(pauseListenHistory, eventCount) {
                        !(pauseListenHistory && eventCount == 0)
                    }

                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

                CompositionLocalProvider(
                    LocalDatabase provides database,
                    LocalNavController provides navController,
                    LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil provides downloadUtil,
                    LocalShimmerTheme provides ShimmerTheme,
                    LocalSyncUtils provides syncUtils,
                    LocalListenTogetherManager provides listenTogetherManager,
                    LocalChangelogState provides showChangelog,
                ) {
                    if (showChangelog.value) {
                        ChangelogScreen(onDismiss = { showChangelog.value = false })
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            AnimatedVisibility(
                                visible = shouldShowTopBar,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val gradientColors = if (pureBlack) {
                                        listOf(
                                            Color.Black,
                                            Color.Black.copy(alpha = 0.98f),
                                            Color.Black.copy(alpha = 0.90f),
                                            Color.Black.copy(alpha = 0.70f),
                                            Color.Black.copy(alpha = 0.40f),
                                            Color.Black.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    } else {
                                        listOf(
                                            baseBg,
                                            baseBg.copy(alpha = 0.98f),
                                            baseBg.copy(alpha = 0.90f),
                                            baseBg.copy(alpha = 0.70f),
                                            baseBg.copy(alpha = 0.40f),
                                            baseBg.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(AppBarHeight + with(LocalDensity.current) { WindowInsets.systemBars.getTop(LocalDensity.current).toDp() } + 48.dp)
                                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors = gradientColors))
                                    )

                                    TopAppBar(
                                        title = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // 1. Aapka Small Icon - Gap nikal diya gaya hai
                                                Icon(
                                                    painter = painterResource(R.drawable.small_icon), 
                                                    contentDescription = null,
                                                    modifier = Modifier.size(52.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                                // 2. TEXT WITH CUSTOM FONT
                                                Text(
                                                    text = "Glossy",
                                                    style = MaterialTheme.typography.headlineMedium.copy(
                                                        fontFamily = FontFamily(Font(R.font.roundex)),
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        actions = {
                                            // 3. SOLID PILL WITH PREMIUM FADE EFFECT
                                            Surface(
                                                shape = CircleShape, 
                                                // 100% Solid background (No transparency issue) jisse peeche ke gaane na dikhein
                                                color = MaterialTheme.colorScheme.surfaceVariant, 
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                // Yahan Premium Fade Effect add kiya hai (Box ke andar Gradient)
                                                Box(
                                                    modifier = Modifier.background(
                                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) // Halka sa premium fade/glow
                                                            )
                                                        )
                                                    )
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        // EKDUM Tight Spacing
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp) 
                                                    ) {
                                                        if (showHistoryButton) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.history),
                                                                contentDescription = stringResource(R.string.history),
                                                                modifier = Modifier
                                                                    .clip(CircleShape)
                                                                    .clickable { navController.navigate("history") }
                                                                    .padding(6.dp)
                                                                    .size(22.dp),
                                                                tint = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        Icon(
                                                            painter = painterResource(R.drawable.stats),
                                                            contentDescription = stringResource(R.string.stats),
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .clickable { navController.navigate("stats") }
                                                                .padding(6.dp)
                                                                .size(22.dp),
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (listenTogetherInTopBar) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.group_outlined),
                                                                contentDescription = stringResource(R.string.together),
                                                                modifier = Modifier
                                                                    .clip(CircleShape)
                                                                    .clickable { navController.navigate("listen_together_from_topbar") }
                                                                    .padding(6.dp)
                                                                    .size(22.dp),
                                                                tint = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        
                                                        // Profile icon
                                                        BadgedBox(
                                                            badge = {
                                                                if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                                    Badge()
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .padding(start = 4.dp, end = 2.dp)
                                                                .clip(CircleShape)
                                                                .clickable { showAccountDialog = true }
                                                        ) {
                                                            if (accountImageUrl != null) {
                                                                AsyncImage(
                                                                    model = accountImageUrl,
                                                                    contentDescription = stringResource(R.string.account),
                                                                    modifier = Modifier
                                                                        .size(28.dp)
                                                                        .clip(CircleShape),
                                                                )
                                                            } else {
                                                                Surface(
                                                                    shape = CircleShape,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(28.dp)
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.account),
                                                                        contentDescription = stringResource(R.string.account),
                                                                        modifier = Modifier.padding(4.dp),
                                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        scrollBehavior = topAppBarScrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color.Transparent, // Set transparent to let our box background shine through
                                            scrolledContainerColor = Color.Transparent, 
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        modifier = Modifier.windowInsetsPadding(
                                            if (showRail) {
                                                WindowInsets(left = NavigationBarHeight)
                                                    .add(cutoutInsets.only(WindowInsetsSides.Start))
                                            } else {
                                                cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                            }
                                        )
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            val currentBackStackEntry = navController.currentBackStackEntry

                            val onNavItemClick: (Screens, Boolean) -> Unit =
                                remember(
                                    navController,
                                    coroutineScope,
                                    topAppBarScrollBehavior,
                                    playerBottomSheetState,
                                    currentBackStackEntry,
                                ) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }
                                        if (isSelected) {
                                            val targetEntry =
                                                try {
                                                    val route = navController.currentBackStackEntry?.destination?.route
                                                    if (route == SearchRoutes.ROUTE || route == "search_input") {
                                                        navController.getBackStackEntry("search_input")
                                                    } else {
                                                        navController.currentBackStackEntry
                                                    }
                                                } catch (e: Exception) {
                                                    null
                                                }

                                            if (screen == Screens.Search) {
                                                val current = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                                targetEntry?.savedStateHandle?.set("scrollToTopCount", current + 1)
                                            } else {
                                                targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                            }

                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }

                            val onSearchLongClick: () -> Unit =
                                remember(navController) {
                                    {
                                        navController.navigate("recognition") {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                            val navBarTotalHeight = bottomInset + NavigationBarHeight + if (useFloatingNavBar) 12.dp else 0.dp
                            val fadeBrush = remember(baseBg) {
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, baseBg.copy(alpha = 0.6f), baseBg),
                                    startY = 0f
                                )
                            }
                            val solidBrush = remember(baseBg) {
                                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(baseBg, baseBg))
                            }

                            if (!showRail && currentRoute != "wrapped") {
                                Box {
                                    if (activePlayerConnection != null) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                        )
                                    }

                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .height(if (useFloatingNavBar) bottomInsetDp + 90.dp else bottomInsetDp)
                                                .graphicsLayer {
                                                    val progress = playerBottomSheetState.progress
                                                    alpha =
                                                        if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) {
                                                            0f
                                                        } else {
                                                            1f
                                                        }
                                                }
                                                .background(if (useFloatingNavBar) fadeBrush else solidBrush)
                                    )

                                    AppNavigationBar(
                                        navigationItems = navigationItems,
                                        currentRoute = currentRoute,
                                        onItemClick = onNavItemClick,
                                        pureBlack = pureBlack,
                                        slimNav = slimNav,
                                        onSearchLongClick = onSearchLongClick,
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(horizontal = if (useFloatingNavBar) 16.dp else 0.dp)
                                                .padding(bottom = if (useFloatingNavBar) bottomInsetDp + 4.dp else 0.dp)
                                                .then(
                                                    if (useFloatingNavBar) Modifier
                                                    else Modifier.height(bottomInset + navPadding).clip(RectangleShape)
                                                )
                                                .graphicsLayer {
                                                    val navBarHeightPx = navigationBarHeight.toPx()
                                                    val totalHeightPx = navBarTotalHeight.toPx()

                                                    translationY =
                                                        if (navBarHeightPx == 0f) {
                                                            totalHeightPx
                                                        } else {
                                                            val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                            val slideOffset = totalHeightPx * progress
                                                            val hideOffset =
                                                                totalHeightPx * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                                            slideOffset + hideOffset
                                                        }
                                                },
                                    )
                                }
                            } else {
                                if (currentRoute != "wrapped") {
                                    if (activePlayerConnection != null) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                        )
                                    }
                                }

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInsetDp)
                                            .graphicsLayer {
                                                val progress = playerBottomSheetState.progress
                                                alpha =
                                                    if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar) || useFloatingNavBar) 0f else 1f
                                            }.background(baseBg),
                                )
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            val onRailItemClick: (Screens, Boolean) -> Unit =
                                remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }

                                        if (isSelected) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }

                            val onRailSearchLongClick: () -> Unit =
                                remember(navController) {
                                    {
                                        navController.navigate("recognition") {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                            if (showRail && currentRoute != "wrapped") {
                                AppNavigationRail(
                                    navigationItems = navigationItems,
                                    currentRoute = currentRoute,
                                    onItemClick = onRailItemClick,
                                    pureBlack = pureBlack,
                                    onSearchLongClick = onRailSearchLongClick,
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                NavHost(
                                    navController = navController,
                                    startDestination =
                                        if (!hasSeenWelcome) "welcome" else {
                                            when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                                NavigationTab.HOME -> Screens.Home
                                                NavigationTab.LIBRARY -> Screens.Library
                                                else -> Screens.Home
                                            }.route
                                        },
                                    enterTransition = {
                                        val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                        val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

                                        if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        } else {
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                        }
                                    },
                                    exitTransition = {
                                        val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                        val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

                                        if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        } else {
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                        }
                                    },
                                    popEnterTransition = {
                                        val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                        val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

                                        if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        } else {
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                        }
                                    },
                                    popExitTransition = {
                                        val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                        val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

                                        if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        } else {
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                        }
                                    },
                                    modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                                ) {
                                    navigationBuilder(
                                        navController = navController,
                                        scrollBehavior = topAppBarScrollBehavior,
                                        latestVersionName = latestVersionName,
                                        activity = this@MainActivity,
                                        snackbarHostState = snackbarHostState,
                                    )
                                }
                            }
                        }
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    BottomSheetPage(
                        state = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    if (showAccountDialog) {
                        AccountSettingsDialog(
                            onDismiss = {
                                showAccountDialog = false
                                homeViewModel.refresh()
                            },
                            latestVersionName = latestVersionName,
                        )
                    }

                    sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    modifier = Modifier.padding(24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = AlertDialogDefaults.containerColor,
                                    tonalElevation = AlertDialogDefaults.TonalElevation,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        YouTubeSongMenu(
                                            song = song,
                                            onDismiss = { sharedSong = null },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!showChangelog.value && !kmpUpgradeDismissed) {
                        kmpRelease?.let { release ->
                            val downloadUrl = release.assets.first { it.name == Updater.KMP_APK_NAME }.downloadUrl
                            AlertDialog(
                                onDismissRequest = { kmpUpgradeDismissed = true },
                                title = {
                                    Text(stringResource(R.string.kmp_upgrade_title, release.versionName))
                                },
                                text = {
                                    Column(
                                        modifier =
                                            Modifier
                                                .heightIn(max = 480.dp)
                                                .verticalScroll(rememberScrollState()),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.kmp_upgrade_warning),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = stringResource(R.string.changelog),
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                        )
                                        Text(
                                            text = release.description.ifBlank { stringResource(R.string.changelog_empty) },
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            kmpUpgradeDismissed = true
                                            startActivity(Intent(Intent.ACTION_VIEW, downloadUrl.toUri()))
                                        },
                                    ) {
                                        Text(stringResource(R.string.kmp_upgrade_action))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { kmpUpgradeDismissed = true }) {
                                        Text(stringResource(R.string.kmp_upgrade_later))
                                    }
                                },
                            )
                        }
                    }
                }
            }
            }
        }
    }

    /**
     * Handles the ACTION_RECOGNITION intent sent from the Music Recognizer Widget.
     * Always navigates to the recognition screen to show the result.
     */
    private fun handleRecognitionIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_RECOGNITION) return
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START_RECOGNITION, false)
        intent.action = null
        intent.removeExtra(EXTRA_AUTO_START_RECOGNITION)
        navController.navigate(if (autoStart) "recognition?autoStart=true" else "recognition") {
            launchSingleTop = true
        }
    }

    private sealed class WidgetTargetRoute(val route: String) {
        data class LocalPlaylist(val id: String) : WidgetTargetRoute("local_playlist/$id")
        data class OnlinePlaylist(val id: String) : WidgetTargetRoute("online_playlist/$id")
        data object LikedSongs : WidgetTargetRoute("auto_playlist/liked")
        data object DownloadedSongs : WidgetTargetRoute("auto_playlist/downloaded")
        data class TopSongs(val limit: String) : WidgetTargetRoute("top_playlist/$limit")
    }

    private fun handleWidgetTargetIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_OPEN_WIDGET_TARGET) return

        val targetType = intent.getStringExtra(EXTRA_WIDGET_TARGET_TYPE)
        val targetId = intent.getStringExtra(EXTRA_WIDGET_TARGET_ID)
        intent.action = null
        intent.removeExtra(EXTRA_WIDGET_TARGET_TYPE)
        intent.removeExtra(EXTRA_WIDGET_TARGET_ID)

        val normalizedTargetId = targetId?.takeIf { it.isNotBlank() }

        val targetRoute = when (targetType) {
            PlaylistWidgetReceiver.TARGET_TYPE_LOCAL ->
                normalizedTargetId?.let { WidgetTargetRoute.LocalPlaylist(it) }

            PlaylistWidgetReceiver.TARGET_TYPE_ONLINE ->
                normalizedTargetId?.let { WidgetTargetRoute.OnlinePlaylist(it) }

            PlaylistWidgetReceiver.TARGET_TYPE_LIKED ->
                WidgetTargetRoute.LikedSongs

            PlaylistWidgetReceiver.TARGET_TYPE_DOWNLOADED ->
                WidgetTargetRoute.DownloadedSongs

            PlaylistWidgetReceiver.TARGET_TYPE_TOP ->
                WidgetTargetRoute.TopSongs(normalizedTargetId ?: "50")

            else -> null
        } ?: return

        navController.navigate(targetRoute.route)
    }

    private fun handleDeepLinkIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val listenCode =
            uri.getQueryParameter("code")
                ?: uri.getQueryParameter("room")
                ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> {
                uri.getQueryParameter("list")?.let { playlistId ->
                    if (playlistId.startsWith("OLAK5uy_")) {
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube
                                .albumSongs(playlistId)
                                .onSuccess { songs ->
                                    songs.firstOrNull()?.album?.id?.let { browseId ->
                                        withContext(Dispatchers.Main) {
                                            navController.navigate("album/$browseId")
                                        }
                                    }
                                }.onFailure { reportException(it) }
                        }
                    } else {
                        navController.navigate("online_playlist/$playlistId")
                    }
                }
            }

            "browse" -> {
                uri.lastPathSegment?.let { browseId ->
                    navController.navigate("album/$browseId")
                }
            }

            "channel", "c" -> {
                uri.lastPathSegment?.let { artistId ->
                    navController.navigate("artist/$artistId")
                }
            }

            "search" -> {
                uri.getQueryParameter("q")?.let {
                    navController.navigate(SearchRoutes.resultRoute(it))
                }
            }

            else -> {
                val videoId =
                    when {
                        path == "watch" -> uri.getQueryParameter("v")
                        uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                        else -> null
                    }

                val playlistId = uri.getQueryParameter("list")

                if (videoId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(listOf(videoId), playlistId)
                            .onSuccess { queue ->
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                            queue.firstOrNull()?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else if (playlistId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(null, playlistId)
                            .onSuccess { queue ->
                                val firstItem = queue.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                            firstItem?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalNavController = staticCompositionLocalOf<NavController> { error("No NavController provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<com.jay.glossy.listentogether.ListenTogetherManager?> { null }
val LocalChangelogState = staticCompositionLocalOf<MutableState<Boolean>> { error("No LocalChangelogState provided") }
val LocalIsPlayerExpanded = compositionLocalOf { false }
