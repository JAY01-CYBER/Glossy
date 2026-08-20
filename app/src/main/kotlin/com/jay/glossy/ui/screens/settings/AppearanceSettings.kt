/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.settings

import com.jay.glossy.R

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.constants.ChipSortTypeKey
import com.jay.glossy.constants.CropAlbumArtKey
import com.jay.glossy.constants.DefaultOpenTabKey
import com.jay.glossy.constants.DensityScale
import com.jay.glossy.constants.DensityScaleKey
import com.jay.glossy.constants.DynamicThemeKey
import com.jay.glossy.constants.EnableDynamicIconKey
import com.jay.glossy.constants.EnableHighRefreshRateKey
import com.jay.glossy.constants.EnableLandscapeScalingKey
import com.jay.glossy.constants.ExperimentalLyricsKey
import com.jay.glossy.constants.GridItemSize
import com.jay.glossy.constants.GridItemsSizeKey
import com.jay.glossy.constants.HidePlayerThumbnailKey
import com.jay.glossy.constants.HideStatusBarOnFullscreenKey
import com.jay.glossy.constants.LibraryFilter
import com.jay.glossy.constants.ListenTogetherInTopBarKey
import com.jay.glossy.constants.LyricsAnimationStyle
import com.jay.glossy.constants.LyricsAnimationStyleKey
import com.jay.glossy.constants.LyricsClickKey
import com.jay.glossy.constants.LyricsGlowEffectKey
import com.jay.glossy.constants.LyricsLineSpacingKey
import com.jay.glossy.constants.LyricsScrollKey
import com.jay.glossy.constants.LyricsTextPositionKey
import com.jay.glossy.constants.LyricsTextSizeKey
import com.jay.glossy.constants.MiniPlayerBackgroundStyle
import com.jay.glossy.constants.MiniPlayerBackgroundStyleKey
import com.jay.glossy.constants.PlayerBackgroundStyle
import com.jay.glossy.constants.PlayerBackgroundStyleKey
import com.jay.glossy.constants.PlayerButtonsStyle
import com.jay.glossy.constants.PlayerButtonsStyleKey
import com.jay.glossy.constants.PlayerDesignStyle
import com.jay.glossy.constants.PlayerDesignStyleKey
import com.jay.glossy.constants.PureBlackMiniPlayerKey
import com.jay.glossy.constants.RespectAgentPositioningKey
import com.jay.glossy.constants.SelectedThemeColorKey
import com.jay.glossy.constants.ShowCachedPlaylistKey
import com.jay.glossy.constants.ShowDownloadedPlaylistKey
import com.jay.glossy.constants.ShowLikedPlaylistKey
import com.jay.glossy.constants.ShowTopPlaylistKey
import com.jay.glossy.constants.ShowUploadedPlaylistKey
import com.jay.glossy.constants.SliderStyle
import com.jay.glossy.constants.SliderStyleKey
import com.jay.glossy.constants.SlimNavBarKey
import com.jay.glossy.constants.SquigglySliderKey
import com.jay.glossy.constants.SwipeSensitivityKey
import com.jay.glossy.constants.SwipeThumbnailKey
import com.jay.glossy.constants.SwipeToRemoveSongKey
import com.jay.glossy.constants.SwipeToSongKey
import com.jay.glossy.constants.UseNewMiniPlayerDesignKey
import com.jay.glossy.constants.QuickPickShape
import com.jay.glossy.constants.QuickPickShapeKey
import com.jay.glossy.constants.QuickPicksStyle
import com.jay.glossy.constants.QuickPicksStyleKey
import com.jay.glossy.constants.ShowFeaturedCarouselKey
import com.jay.glossy.constants.UseFloatingNavBarKey
import com.jay.glossy.constants.AppFont
import com.jay.glossy.constants.SelectedFontKey
import com.jay.glossy.ui.component.DefaultDialog
import com.jay.glossy.ui.component.EnumDialog
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.Material3SettingsGroup
import com.jay.glossy.ui.component.Material3SettingsItem
import com.jay.glossy.ui.component.PlayerSliderTrack
import com.jay.glossy.ui.component.SquigglySlider
import com.jay.glossy.ui.component.WavySlider
import com.jay.glossy.ui.theme.DefaultThemeColor
import com.jay.glossy.ui.theme.PlayerSliderColors
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.utils.IconUtils
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val (enableDynamicIcon, onEnableDynamicIconPrefChange) = rememberPreference(EnableDynamicIconKey, defaultValue = true)
    val iconContext = LocalContext.current
    val onEnableDynamicIconChange: (Boolean) -> Unit = { newValue ->
        onEnableDynamicIconPrefChange(newValue)
        IconUtils.setIcon(iconContext, newValue)
    }
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(EnableHighRefreshRateKey, defaultValue = true)
    val (enableLandscapeScaling, onEnableLandscapeScalingChange) = rememberPreference(EnableLandscapeScalingKey, defaultValue = false)
    val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    val (selectedFontValue) = rememberPreference(SelectedFontKey, defaultValue = AppFont.SYSTEM.value)
    val currentFont = AppFont.fromValue(selectedFontValue)

    // --- PLAYER DESIGN SELECTION ---
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.MODERN,
    )
    var showPlayerDesignDialog by rememberSaveable { mutableStateOf(false) }

    val (miniPlayerBackground, onMiniPlayerBackgroundChange) = rememberEnumPreference(MiniPlayerBackgroundStyleKey, defaultValue = MiniPlayerBackgroundStyle.DEFAULT)
    val availableMiniPlayerBackgroundStyles = MiniPlayerBackgroundStyle.entries.filter {
        it != MiniPlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }

    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, defaultValue = false)
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(CropAlbumArtKey, defaultValue = false)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(LyricsTextPositionKey, defaultValue = LyricsPosition.CENTER)
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) = rememberPreference(HideStatusBarOnFullscreenKey, defaultValue = false)
    val (respectAgentPositioning, onRespectAgentPositioningChange) = rememberPreference(RespectAgentPositioningKey, defaultValue = true)
    val (experimentalLyrics, onExperimentalLyricsChange) = rememberPreference(ExperimentalLyricsKey, defaultValue = true)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(LyricsAnimationStyleKey, defaultValue = LyricsAnimationStyle.FADE)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)
    val (showFeaturedCarousel, onShowFeaturedCarouselChange) = rememberPreference(ShowFeaturedCarouselKey, defaultValue = true)
    val (quickPicksStyle, onQuickPicksStyleChange) = rememberEnumPreference(QuickPicksStyleKey, defaultValue = QuickPicksStyle.GRID)
    var showQuickPicksStyleDialog by rememberSaveable { mutableStateOf(false) }
    val (quickPickShape, onQuickPickShapeChange) = rememberEnumPreference(QuickPickShapeKey, defaultValue = QuickPickShape.DEFAULT)
    var showQuickPickShapeDialog by rememberSaveable { mutableStateOf(false) }
    var showExperimentalLyricsBetaDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }

    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.DEFAULT)
    val (squigglySlider, onSquigglySliderChange) = rememberPreference(SquigglySliderKey, defaultValue = false)
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(SwipeThumbnailKey, defaultValue = true)
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(SwipeSensitivityKey, defaultValue = 0.73f)
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(GridItemsSizeKey, defaultValue = GridItemSize.SMALL)
    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)
    val (useFloatingNavBar, onUseFloatingNavBarChange) = rememberPreference(UseFloatingNavBarKey, defaultValue = false)

    val context = activity as Context
    val sharedPreferences = remember { context.getSharedPreferences("metrolist_settings", Context.MODE_PRIVATE) }
    val prefDensityScale = remember(sharedPreferences) { sharedPreferences.getFloat("density_scale_factor", 1.0f) }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        sharedPreferences.edit { putFloat("density_scale_factor", newScale) }
        showRestartDialog = true
    }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
    val (swipeToSong, onSwipeToSongChange) = rememberPreference(SwipeToSongKey, defaultValue = false)
    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(SwipeToRemoveSongKey, defaultValue = false)
    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(ShowLikedPlaylistKey, defaultValue = true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(ShowDownloadedPlaylistKey, defaultValue = true)
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(ShowTopPlaylistKey, defaultValue = true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(ShowCachedPlaylistKey, defaultValue = true)
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(ShowUploadedPlaylistKey, defaultValue = true)

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(key = ChipSortTypeKey, defaultValue = LibraryFilter.LIBRARY)

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerButtonsStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsPositionDialog by rememberSaveable { mutableStateOf(false) }

    // --- DIALOG: PLAYER DESIGN ---
    if (showPlayerDesignDialog) {
        EnumDialog(
            onDismiss = { showPlayerDesignDialog = false },
            onSelect = {
                onPlayerDesignStyleChange(it)
                showPlayerDesignDialog = false
            },
            title = "Player Design",
            current = playerDesignStyle,
            values = PlayerDesignStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerDesignStyle.LEGACY -> "Classic Player (Old)"
                    PlayerDesignStyle.MODERN -> "Modern Player (New)"
                    PlayerDesignStyle.WAVY -> "Wavy Pastel Player (3rd Design)"
                }
            },
        )
    }

    if (showQuickPicksStyleDialog) {
        EnumDialog(
            onDismiss = { showQuickPicksStyleDialog = false },
            onSelect = { onQuickPicksStyleChange(it); showQuickPicksStyleDialog = false },
            title = "Quick Picks Layout Style",
            current = quickPicksStyle,
            values = QuickPicksStyle.values().toList(),
            valueText = {
                when (it) {
                    QuickPicksStyle.GRID -> "Grid (4 Rows)"
                    QuickPicksStyle.LIST -> "List (1 Row)"
                    QuickPicksStyle.CAROUSEL -> "Carousel Banner"
                }
            },
        )
    }

    if (showQuickPickShapeDialog) {
        EnumDialog(
            onDismiss = { showQuickPickShapeDialog = false },
            onSelect = { onQuickPickShapeChange(it); showQuickPickShapeDialog = false },
            title = "Quick Picks Shape",
            current = quickPickShape,
            values = QuickPickShape.values().toList(),
            valueText = {
                when (it) {
                    QuickPickShape.DEFAULT -> "Default (Rounded)"
                    QuickPickShape.CIRCLE -> "Circle"
                    QuickPickShape.SQUIRCLE -> "Squircle (Smooth Apple Style)"
                    QuickPickShape.LEAF -> "Leaf"
                    QuickPickShape.INVERTED_LEAF -> "Inverted Leaf"
                    QuickPickShape.TEARDROP -> "Teardrop"
                    QuickPickShape.MESSAGE_BUBBLE -> "Message Bubble"
                    QuickPickShape.TICKET -> "Ticket Style"
                    QuickPickShape.INVERTED_TICKET -> "Inverted Ticket"
                    QuickPickShape.CUT_CORNER -> "Cut Corners"
                    QuickPickShape.OCTAGON -> "Octagon"
                    QuickPickShape.DIAMOND -> "Diamond"
                    QuickPickShape.BOOKMARK -> "Bookmark Style"
                    QuickPickShape.FOLDER -> "Folder Style"
                    QuickPickShape.DYNAMIC -> "Dynamic (All Mixed)"
                }
            },
        )
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = { onLyricsPositionChange(it); showLyricsPositionDialog = false },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = { onLyricsAnimationStyleChange(it); showLyricsAnimationStyleDialog = false },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                }
            },
        )
    }

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = { onPlayerButtonsStyleChange(it); showPlayerButtonsStyleDialog = false },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                }
            },
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = { onPlayerBackgroundChange(it); showPlayerBackgroundDialog = false },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    PlayerBackgroundStyle.ANIMATED_MESH -> "Animated Mesh"
                }
            },
        )
    }

    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = { onMiniPlayerBackgroundChange(it); showMiniPlayerBackgroundDialog = false },
            title = stringResource(R.string.mini_player_background_style),
            current = miniPlayerBackground,
            values = availableMiniPlayerBackgroundStyles,
            valueText = {
                when (it) {
                    MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                    MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                    MiniPlayerBackgroundStyle.ANIMATED_MESH -> "Animated Mesh"
                }
            },
        )
    }

    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = { onDefaultOpenTabChange(it); showDefaultOpenTabDialog = false },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = { onDefaultChipChange(it); showDefaultChipDialog = false },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                    else -> ""
                }
            },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = "Home Screen Layout",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text("Show Featured Spotlight") },
                    description = { Text("Show large animated banner on home screen") },
                    trailingContent = {
                        Switch(
                            checked = showFeaturedCarousel,
                            onCheckedChange = onShowFeaturedCarouselChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(id = if (showFeaturedCarousel) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    },
                    onClick = { onShowFeaturedCarouselChange(!showFeaturedCarousel) },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text("Quick Picks Style") },
                    description = {
                        Text(
                            when (quickPicksStyle) {
                                QuickPicksStyle.GRID -> "Grid (4 Rows)"
                                QuickPicksStyle.LIST -> "List (1 Row)"
                                QuickPicksStyle.CAROUSEL -> "Carousel Banner"
                            },
                        )
                    },
                    onClick = { showQuickPicksStyleDialog = true },
                ),
            ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.theme),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.speed),
                        title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                        description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableHighRefreshRate,
                                onCheckedChange = onEnableHighRefreshRateChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.fullscreen),
                        title = { Text(stringResource(R.string.enable_landscape_scaling)) },
                        description = { Text(stringResource(R.string.enable_landscape_scaling_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableLandscapeScaling,
                                onCheckedChange = onEnableLandscapeScalingChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(id = if (enableLandscapeScaling) R.drawable.check else R.drawable.close),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onEnableLandscapeScalingChange(!enableLandscapeScaling) },
                    ),
                )
                if (!isUsingCustomColor) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                            trailingContent = {
                                Switch(
                                    checked = dynamicTheme,
                                    onCheckedChange = onDynamicThemeChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(id = if (dynamicTheme) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onDynamicThemeChange(!dynamicTheme) },
                        ),
                    )
                }
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text("App Font") },
                        description = { Text(currentFont.displayName) },
                        onClick = { navController.navigate("settings/appearance/font") },
                    ),
                )
            },
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items = listOf(
                // SINGLE DIALOG SELECTION FOR PLAYER DESIGN
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text("Player Design") },
                    description = {
                        Text(
                            when (playerDesignStyle) {
                                PlayerDesignStyle.LEGACY -> "Classic Player (Old)"
                                PlayerDesignStyle.MODERN -> "Modern Player (New)"
                                PlayerDesignStyle.WAVY -> "Wavy Pastel Player (3rd Design)"
                            },
                        )
                    },
                    onClick = { showPlayerDesignDialog = true },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.gradient),
                    title = { Text(stringResource(R.string.player_background_style)) },
                    description = {
                        Text(
                            when (playerBackground) {
                                PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                PlayerBackgroundStyle.ANIMATED_MESH -> "Animated Mesh"
                            },
                        )
                    },
                    onClick = { showPlayerBackgroundDialog = true },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.hide_image),
                    title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                    description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hidePlayerThumbnail,
                            onCheckedChange = onHidePlayerThumbnailChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    },
                    onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.crop),
                    title = { Text(stringResource(R.string.crop_album_art)) },
                    description = { Text(stringResource(R.string.crop_album_art_desc)) },
                    trailingContent = {
                        Switch(
                            checked = cropAlbumArt,
                            onCheckedChange = onCropAlbumArtChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(id = if (cropAlbumArt) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    },
                    onClick = { onCropAlbumArtChange(!cropAlbumArt) },
                ),
            ),
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

enum class DarkMode { ON, OFF, AUTO }
enum class NavigationTab { HOME, SEARCH, LIBRARY }
enum class LyricsPosition { LEFT, CENTER, RIGHT }
enum class PlayerTextAlignment { SIDED, CENTER }
