/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Settings, on the same Material 3 Expressive surface as the screens behind it:
 * a plain top bar, the account card on top, the floating glass props of the key
 * art as a header, then grouped sections — Appearance, Playback, Storage — and
 * everything else the app can configure split into focused groups below them.
 * Nothing is dropped, it is just easier to find.
 *
 * The groups are the shared [Material3SettingsGroup], so spacing, corner radii,
 * icon tiles and switches stay identical between this screen and every settings
 * screen it opens. Three of the four everyday groups are inlined here (rather
 * than behind their own screen) because they are the ones people actually flip
 * while listening. The full appearance screen (player style, mini player,
 * background blur, nav bar, quick picks…) lives behind the "Appearance" row.
 */

package com.jay.glossy.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jay.glossy.BuildConfig
import com.jay.glossy.LocalChangelogState
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.constants.AccountNameKey
import com.jay.glossy.constants.AudioNormalizationKey
import com.jay.glossy.constants.CommunityChannel
import com.jay.glossy.constants.DarkModeKey
import com.jay.glossy.constants.DownloadOverWifiOnlyKey
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.constants.SelectedThemeColorKey
import com.jay.glossy.constants.SkipSilenceKey
import com.jay.glossy.ui.component.GlossyBrandTile
import com.jay.glossy.ui.component.GlossyEyebrow
import com.jay.glossy.ui.component.GlossyInitialAvatar
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.Material3SettingsGroup
import com.jay.glossy.ui.component.Material3SettingsItem
import com.jay.glossy.ui.theme.DefaultThemeColor
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.utils.Updater
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.HomeViewModel
import com.metrolist.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Height of the decorative glass band at the top of the list. */

/** Side inset of the whole settings list, matching the settings sub-screens. */
private val ListInset = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo(
                "com.google.android.projection.gearhead", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---- Account ---------------------------------------------------------
    val (accountNamePref) = rememberPreference(AccountNameKey, defaultValue = "")
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, defaultValue = "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
    val displayName = remember(accountName, accountNamePref) {
        accountName?.takeIf { it.isNotBlank() }
            ?: accountNamePref.takeIf { it.isNotBlank() }
            ?: "Guest"
    }

    // ---- Preferences shown by the groups below ---------------------------
    val (darkMode) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (selectedThemeColorInt) = rememberPreference(
        SelectedThemeColorKey,
        defaultValue = DefaultThemeColor.toArgb()
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = false
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (downloadOverWifiOnly, onDownloadOverWifiOnlyChange) = rememberPreference(
        DownloadOverWifiOnlyKey,
        defaultValue = true
    )

    // ---- Cached music size ----------------------------------------------
    val playerConnection = LocalPlayerConnection.current
    var cachedBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection) {
        while (isActive) {
            cachedBytes = withContext(Dispatchers.IO) {
                runCatching {
                    playerConnection?.service?.playerCache?.cacheSpace ?: 0L
                }.getOrDefault(0L)
            }
            delay(3000)
        }
    }
    val cachedLabel = remember(cachedBytes) {
        Formatter.formatShortFileSize(context, cachedBytes)
    }

    val showChangelog = LocalChangelogState.current

    val themeValue = when (darkMode) {
        DarkMode.ON -> stringResource(R.string.glossy_theme_dark)
        DarkMode.OFF -> stringResource(R.string.glossy_theme_light)
        DarkMode.AUTO -> stringResource(R.string.glossy_theme_system)
    }

    val paletteValue = PaletteColors.firstOrNull {
        it.seedColor != androidx.compose.ui.graphics.Color.Transparent &&
            it.seedColor.toArgb() == selectedThemeColorInt
    }?.let { stringResource(it.nameRes) } ?: stringResource(R.string.glossy_teal)

    val openCommunityLink: (CommunityChannel) -> Unit = { channel ->
        runCatching { uriHandler.openUri(channel.url) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GlossyPalette.Page,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GlossyPalette.Page,
                    titleContentColor = GlossyPalette.TextPrimary,
                    navigationIconContentColor = GlossyPalette.TextSecondary,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            contentPadding = PaddingValues(
                start = ListInset,
                end = ListInset,
                bottom = 40.dp,
            ),
        ) {
            // ---- Account --------------------------------------------------
            item(key = "account_card") {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            leadingContent = {
                                GlossyInitialAvatar(
                                    name = displayName,
                                    size = 48.dp,
                                    imageUrl = accountImageUrl,
                                )
                            },
                            title = {
                                Text(text = displayName, fontWeight = FontWeight.SemiBold)
                            },
                            description = {
                                Text(
                                    stringResource(
                                        if (isLoggedIn) R.string.glossy_signed_in_google
                                        else R.string.glossy_signed_in_guest
                                    )
                                )
                            },
                            showChevron = true,
                            onClick = {
                                navController.navigate(if (isLoggedIn) "account" else "login")
                            },
                        )
                    ),
                )
            }

            // ---- Appearance ----------------------------------------------
            item(key = "group_appearance") {
                Material3SettingsGroup(
                    title = stringResource(R.string.appearance),
                    items = listOf(
                        navRow(
                            icon = R.drawable.contrast,
                            title = stringResource(R.string.appearance),
                            subtitle = stringResource(R.string.glossy_appearance_desc),
                            value = themeValue,
                            onClick = { navController.navigate("settings/appearance") },
                        ),
                        navRow(
                            icon = R.drawable.bedtime,
                            title = stringResource(R.string.theme),
                            value = themeValue,
                            onClick = { navController.navigate("settings/appearance/theme") },
                        ),
                        navRow(
                            icon = R.drawable.gradient,
                            title = stringResource(R.string.color_palette),
                            value = paletteValue,
                            onClick = { navController.navigate("settings/appearance/theme") },
                        ),
                        navRow(
                            icon = R.drawable.translate,
                            title = stringResource(R.string.glossy_font),
                            onClick = { navController.navigate("settings/appearance/font") },
                        ),
                    ),
                )
            }

            // ---- Playback -------------------------------------------------
            item(key = "group_playback") {
                Material3SettingsGroup(
                    title = stringResource(R.string.menu_section_playback),
                    items = listOf(
                        toggleRow(
                            icon = R.drawable.equalizer,
                            title = stringResource(R.string.audio_normalization),
                            checked = audioNormalization,
                            onCheckedChange = onAudioNormalizationChange,
                        ),
                        toggleRow(
                            icon = R.drawable.fast_forward,
                            title = stringResource(R.string.skip_silence),
                            checked = skipSilence,
                            onCheckedChange = onSkipSilenceChange,
                        ),
                        toggleRow(
                            icon = R.drawable.download,
                            title = stringResource(R.string.glossy_download_wifi_only),
                            subtitle = stringResource(R.string.glossy_download_wifi_only_desc),
                            checked = downloadOverWifiOnly,
                            onCheckedChange = onDownloadOverWifiOnlyChange,
                        ),
                    ),
                )
            }

            // ---- Storage --------------------------------------------------
            item(key = "group_storage") {
                Material3SettingsGroup(
                    title = stringResource(R.string.storage),
                    items = listOf(
                        navRow(
                            icon = R.drawable.storage,
                            title = stringResource(R.string.glossy_cached_music),
                            subtitle = stringResource(R.string.glossy_used_amount, cachedLabel),
                            onClick = { navController.navigate("settings/storage") },
                        ),
                    ),
                )
            }

            // ---- Playback & audio ----------------------------------------
            item(key = "group_playback_audio") {
                Material3SettingsGroup(
                    title = stringResource(R.string.glossy_section_playback_audio),
                    items = listOf(
                        navRow(
                            icon = R.drawable.sliders,
                            title = stringResource(R.string.player_and_audio),
                            onClick = { navController.navigate("settings/player") },
                        ),
                        navRow(
                            icon = R.drawable.cloud,
                            title = stringResource(R.string.stream_sources),
                            onClick = { navController.navigate("settings/stream_sources") },
                        ),
                        navRow(
                            icon = R.drawable.lyrics,
                            title = stringResource(R.string.ai_lyrics_translation),
                            onClick = { navController.navigate("settings/ai") },
                        ),
                    ),
                )
            }

            // ---- Content --------------------------------------------------
            item(key = "group_content") {
                Material3SettingsGroup(
                    title = stringResource(R.string.glossy_section_content_group),
                    items = buildList {
                        add(
                            navRow(
                                icon = R.drawable.content_copy,
                                title = stringResource(R.string.content),
                                onClick = { navController.navigate("settings/content") },
                            )
                        )
                        if (hasAndroidAuto) {
                            add(
                                navRow(
                                    icon = R.drawable.ic_android_auto,
                                    title = stringResource(R.string.android_auto),
                                    onClick = { navController.navigate("settings/android_auto") },
                                )
                            )
                        }
                    },
                )
            }

            // ---- Data -----------------------------------------------------
            item(key = "group_data") {
                Material3SettingsGroup(
                    title = stringResource(R.string.glossy_section_data),
                    items = buildList {
                        add(
                            navRow(
                                icon = R.drawable.lock,
                                title = stringResource(R.string.privacy),
                                onClick = { navController.navigate("settings/privacy") },
                            )
                        )
                        add(
                            navRow(
                                icon = R.drawable.backup,
                                title = stringResource(R.string.backup_restore),
                                onClick = { navController.navigate("settings/backup_restore") },
                            )
                        )
                        if (isAndroid12OrLater) {
                            add(
                                navRow(
                                    icon = R.drawable.link,
                                    title = stringResource(R.string.default_links),
                                    onClick = {
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                                "package:${context.packageName}".toUri()
                                            )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                )
                            )
                        }
                    },
                )
            }

            // ---- Integrations ---------------------------------------------
            item(key = "group_integrations") {
                Material3SettingsGroup(
                    title = stringResource(R.string.glossy_section_integrations),
                    items = listOf(
                        navRow(
                            icon = R.drawable.integration,
                            title = stringResource(R.string.integrations),
                            subtitle = "Spotify · Discord · Last.fm · Listen Together",
                            onClick = { navController.navigate("settings/integrations") },
                        ),
                    ),
                )
            }

            // ---- About ----------------------------------------------------
            item(key = "group_about") {
                Material3SettingsGroup(
                    title = stringResource(R.string.glossy_section_about),
                    items = buildList {
                        if (BuildConfig.UPDATER_AVAILABLE) {
                            add(
                                navRow(
                                    icon = R.drawable.update,
                                    title = stringResource(R.string.updater),
                                    onClick = { navController.navigate("settings/updater") },
                                )
                            )
                        }
                        add(
                            navRow(
                                icon = R.drawable.history,
                                title = stringResource(R.string.changelog),
                                onClick = { showChangelog.value = true },
                            )
                        )
                        add(
                            navRow(
                                icon = R.drawable.info,
                                title = stringResource(R.string.about),
                                onClick = { navController.navigate("settings/about") },
                            )
                        )
                    },
                )
            }

            // ---- Update available ----------------------------------------
            if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME) {
                val releaseInfo = Updater.getCachedLatestRelease()
                val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }

                if (downloadUrl != null) {
                    item(key = "group_update") {
                        Material3SettingsGroup(
                            title = stringResource(R.string.about),
                            items = listOf(
                                navRow(
                                    icon = R.drawable.update,
                                    title = stringResource(R.string.new_version_available),
                                    subtitle = latestVersionName,
                                    onClick = { uriHandler.openUri(downloadUrl) },
                                ),
                            ),
                        )
                    }
                }
            }

            // ---- Footer ---------------------------------------------------
            item(key = "footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = GlossyDimens.Section * 2,
                            bottom = GlossyDimens.Section,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GlossyEyebrow(text = stringResource(R.string.glossy_follow_us))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CommunityChannel.entries.forEach { channel ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(GlossyDimens.CornerPill))
                                    .clickable { openCommunityLink(channel) },
                            ) {
                                GlossyBrandTile(
                                    color = channel.brandColor,
                                    icon = channel.iconRes,
                                    size = 42.dp,
                                    iconSize = 20.dp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlossyPalette.TextMuted,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center,
                    )
                    // Which build of that version this is, and which app it is.
                    // versionCode and versionName are the same for every local
                    // build, so without this there was no way to tell from
                    // inside the app whether a freshly built APK had actually
                    // reached the phone — or whether the app being opened was
                    // even the APK that was built.
                    Text(
                        text = "${BuildConfig.APPLICATION_ID} · built ${BuildConfig.BUILD_STAMP}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlossyPalette.TextMuted,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

// ============================================================================
// Rows
// ============================================================================

/**
 * A settings row that leads somewhere: an icon, a title, optional supporting
 * text, and either a right-hand value or a chevron. A row that shows a value
 * drops the chevron, since the value itself is the state being read.
 */
@Composable
private fun navRow(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
): Material3SettingsItem {
    val description: (@Composable () -> Unit)? =
        subtitle?.let { text -> { Text(text = text) } }
    val valueContent: (@Composable () -> Unit)? =
        value?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    return Material3SettingsItem(
        icon = painterResource(icon),
        title = { Text(text = title) },
        description = description,
        trailingContent = valueContent,
        showChevron = value == null,
        onClick = onClick,
    )
}

/**
 * A settings row whose right-hand side is a switch. The whole row toggles, not
 * just the switch, and the switch carries the same check/cross thumb as the
 * rest of the settings screens.
 */
@Composable
private fun toggleRow(
    icon: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
): Material3SettingsItem {
    val description: (@Composable () -> Unit)? =
        subtitle?.let { text -> { Text(text = text) } }
    return Material3SettingsItem(
        icon = painterResource(icon),
        title = { Text(text = title) },
        description = description,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        painter = painterResource(
                            if (checked) R.drawable.check else R.drawable.close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}
