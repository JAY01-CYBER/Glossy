/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.jay.glossy.R
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.PureBlackKey
import com.jay.glossy.ui.screens.artist.ArtistAlbumsScreen
import com.jay.glossy.ui.screens.artist.ArtistItemsScreen
import com.jay.glossy.ui.screens.artist.ArtistScreen
import com.jay.glossy.ui.screens.artist.ArtistSongsScreen
import com.jay.glossy.ui.screens.equalizer.EqScreen
import com.jay.glossy.ui.screens.equalizer.wizard.WizardScreen
import com.jay.glossy.ui.screens.library.LibraryScreen
import com.jay.glossy.ui.screens.playlist.AutoPlaylistScreen
import com.jay.glossy.ui.screens.playlist.CachePlaylistScreen
import com.jay.glossy.ui.screens.playlist.LocalPlaylistScreen
import com.jay.glossy.ui.screens.playlist.OnlinePlaylistScreen
import com.jay.glossy.ui.screens.playlist.TopPlaylistScreen
import com.jay.glossy.ui.screens.podcast.OnlinePodcastScreen
import com.jay.glossy.ui.screens.recognition.RecognitionHistoryScreen
import com.jay.glossy.ui.screens.recognition.RecognitionScreen
import com.jay.glossy.ui.screens.search.OnlineSearchResult
import com.jay.glossy.ui.screens.search.SearchScreen
import com.jay.glossy.ui.screens.settings.AboutScreen
import com.jay.glossy.ui.screens.settings.AiSettings
import com.jay.glossy.ui.screens.settings.AndroidAutoSettings
import com.jay.glossy.ui.screens.settings.AppearanceSettings
import com.jay.glossy.ui.screens.settings.BackupAndRestore
import com.jay.glossy.ui.screens.settings.ContentSettings
import com.jay.glossy.ui.screens.settings.DarkMode
import com.jay.glossy.ui.screens.settings.PlayerSettings
import com.jay.glossy.ui.screens.settings.PrivacySettings
import com.jay.glossy.ui.screens.settings.RomanizationSettings
import com.jay.glossy.ui.screens.settings.SettingsScreen
import com.jay.glossy.ui.screens.settings.StorageSettings
import com.jay.glossy.ui.screens.settings.StreamSourcesSettings
import com.jay.glossy.ui.screens.settings.ThemeScreen
import com.jay.glossy.ui.screens.settings.UpdaterScreen
import com.jay.glossy.ui.screens.settings.AppFontSettingsScreen
import com.jay.glossy.ui.screens.settings.integrations.DiscordSettings
import com.jay.glossy.ui.screens.settings.integrations.IntegrationScreen
import com.jay.glossy.ui.screens.settings.integrations.LastFMSettings
import com.jay.glossy.ui.screens.settings.integrations.ListenTogetherSettings
import com.jay.glossy.ui.screens.wrapped.WrappedScreen
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit


@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    // --- SPLASH SCREEN ROUTE (New Premium Boot Screen) ---
    composable("splash") {
        GlossySplashScreen(
            savedGuestName = "Jay", // Hardcoded premium name
            onSplashComplete = {
                navController.navigate(Screens.Home.route) {
                    popUpTo("splash") { inclusive = true }
                }
            }
        )
    }

    // --- WELCOME SCREEN ROUTE ---
    composable("welcome") {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        
        GlossyWelcomeScreen(
            onSetupComplete = { 
                coroutineScope.launch {
                    context.safeDataStoreEdit { prefs ->
                        prefs[booleanPreferencesKey("has_seen_welcome")] = true
                    }
                }
                navController.navigate(Screens.Home.route) {
                    popUpTo("welcome") { inclusive = true }
                }
            },
            onGoogleLoginClick = { 
                navController.navigate("login")
            }
        )
    }

    composable(Screens.Home.route) {
        HomeScreen(snackbarHostState = snackbarHostState)
    }

    composable(Screens.Mix.route) {
        MixScreen(navController = navController)
    }

    composable(Screens.Search.route) { backStackEntry ->
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }
        SearchScreen(
            pureBlack = pureBlack,
            savedStateHandle = backStackEntry.savedStateHandle
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen()
    }

    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable(
        route = "listen_together_from_topbar",
    ) {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable("history") {
        HistoryScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController)
    }

    composable("account") {
        AccountScreen(navController)
    }

    composable("new_release") {
        NewReleaseScreen(navController)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            it.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) { backStackEntry ->
        OnlineSearchResult(
            savedStateHandle = backStackEntry.savedStateHandle
        )

    }

    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController)
    }

    composable(
        route = "artist/{artistId}?isPodcastChannel={isPodcastChannel}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("isPodcastChannel") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        ArtistScreen(navController)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController)
    }

    composable(
        route = "online_podcast/{podcastId}",
        arguments =
            listOf(
                navArgument("podcastId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController)
    }

    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController)
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController, latestVersionName)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, activity, snackbarHostState)
    }
    
    composable("settings/appearance/font") {
        AppFontSettingsScreen(navController)
    }

    composable("settings/appearance/theme") {
        ThemeScreen(navController)
    }

    composable("settings/content") {
        ContentSettings(navController)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController)
    }

    composable("settings/ai") {
        AiSettings(navController)
    }

    composable("settings/player") {
        PlayerSettings(navController)
    }

    composable("settings/stream_sources") {
        StreamSourcesSettings(navController)
    }

    composable("settings/storage") {
        StorageSettings(navController)
    }

    composable("settings/privacy") {
        PrivacySettings(navController)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    composable("settings/integrations") {
        IntegrationScreen(navController)
    }

    composable("settings/integrations/discord") {
        DiscordSettings(navController)
    }

    composable("settings/integrations/lastfm") {
        LastFMSettings(navController)
    }

    composable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController)
    }

    composable("settings/updater") {
        UpdaterScreen(navController)
    }

    composable("settings/about") {
        AboutScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("wrapped") {
        WrappedScreen()
    }

    composable("equalizer") {
        EqScreen()
    }

    composable("eq_wizard") {
        WizardScreen(onNavigateBack = {
            navController.popBackStack()
        })
    }

    composable(
        route = "recognition?autoStart={autoStart}",
        arguments =
            listOf(
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/android_auto") {
        AndroidAutoSettings(navController)
    }
}

// ==============================================================
// SPLASH SCREEN COMPOSABLE
// ==============================================================
@Composable
fun GlossySplashScreen(
    savedGuestName: String,
    onSplashComplete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgImage = if (isDark) R.drawable.welcome_bg_dark else R.drawable.welcome_bg_light
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Image with optional blur
        Image(
            painter = painterResource(id = bgImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (supportsBlur) Modifier.blur(16.dp) else Modifier)
        )

        // Overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) Color.Black.copy(alpha = if (supportsBlur) 0.4f else 0.7f)
                    else Color.White.copy(alpha = if (supportsBlur) 0.4f else 0.7f)
                )
        )

        // Loading Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.8f))

            Icon(
                painter = painterResource(R.drawable.small_icon),
                contentDescription = "Glossy Logo",
                tint = textColor,
                modifier = Modifier.size(84.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Glossy",
                color = textColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome Back,\n$savedGuestName",
                color = textColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            CircularProgressIndicator(
                color = textColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "\"Life buffering ho sakti hai, music nahi.\"",
                color = subTextColor,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Jay & M4TRX",
                color = if (isDark) Color.DarkGray else Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // 1.2 Second Splash Delay
    LaunchedEffect(Unit) {
        delay(1200L)
        onSplashComplete()
    }
}
