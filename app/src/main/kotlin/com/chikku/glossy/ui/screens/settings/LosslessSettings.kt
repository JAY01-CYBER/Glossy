/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.chikku.glossy.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.R
import com.jay.glossy.constants.AudioQuality
import com.jay.glossy.constants.AudioQualityKey
import com.jay.glossy.constants.DefaultAudioQuality
import com.jay.glossy.constants.LosslessSourceKey
import com.jay.glossy.lossless.LosslessOutput
import com.jay.glossy.lossless.LosslessSource
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.Material3SettingsGroup
import com.jay.glossy.ui.component.Material3SettingsItem
import com.jay.glossy.ui.component.TextFieldDialog
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference

/**
 * Settings › Playback › Lossless.
 *
 * The lane works the way BitChord's does: a source turns a track into a
 * FLAC/ALAC master, and when it has nothing — or the build carries none — the
 * player keeps the best stream YouTube offered. A build can carry its endpoint,
 * so there is no folder to point at and nothing to keep in sync: the lossless
 * tier is a choice in Audio quality, not a library the listener has to build.
 *
 * The screen therefore has three things to say: which tier is selected, where
 * masters come from, and whether the output path can carry them. The last one
 * is read from the audio HAL rather than assumed, because most listening
 * happens over Bluetooth, where every sample is re-encoded before it leaves the
 * phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LosslessSettings(navController: NavController) {
    val context = LocalContext.current

    val (audioQuality, _) = rememberEnumPreference(AudioQualityKey, DefaultAudioQuality)
    val (source, onSourceChange) = rememberPreference(LosslessSourceKey, "")

    var showSourceDialog by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(LosslessOutput.report(context)) }

    LaunchedEffect(source) {
        LosslessSource.configure(source)
        report = LosslessOutput.report(context)
    }

    val builtInHost = LosslessSource.builtInHost()
    val usingBuiltIn = builtInHost != null && source.isBlank()
    // The last thing the source said. A lossy stream under the Lossless tier is
    // otherwise indistinguishable from a slow start, so the screen names it and
    // the badge no longer has to carry the whole explanation.
    val lastCheck by LosslessSource.lastCheck.collectAsState()
    val fallingBack = audioQuality == AudioQuality.LOSSLESS && source.isBlank() && builtInHost == null

    val qualityLabel =
        when (audioQuality) {
            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
            AudioQuality.VERY_HIGH -> stringResource(R.string.audio_quality_very_high)
            AudioQuality.LOSSLESS -> stringResource(R.string.audio_quality_lossless)
        }

    if (showSourceDialog) {
        TextFieldDialog(
            title = { Text("Lossless source") },
            icon = { Icon(painterResource(R.drawable.cloud), null) },
            initialTextFieldValue = TextFieldValue(text = source),
            keyboardType = KeyboardType.Uri,
            // Blank is valid: it means "no override" — the build's endpoint
            // when it carries one, YouTube's best stream otherwise.
            isInputValid = { it.isBlank() || it.startsWith("http://") || it.startsWith("https://") },
            onDone = { value ->
                onSourceChange(value.trim())
                LosslessSource.configure(value)
                showSourceDialog = false
            },
            onDismiss = { showSourceDialog = false },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        Text(
            text = "Lossless",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
        Text(
            text =
                if (usingBuiltIn) {
                    "A FLAC or ALAC master, fetched through the built-in source. When it has " +
                        "nothing for a track, the player falls back to the best audio YouTube serves — " +
                        "so choosing Lossless never leaves a song silent, and there is nothing to set up."
                } else {
                    "A FLAC or ALAC master, fetched through a source module. When the module has " +
                        "nothing for a track, the player falls back to the best audio YouTube serves — " +
                        "so choosing Lossless never leaves a song silent, and there is no folder to keep " +
                        "in sync."
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (fallingBack || lastCheck != null) {
            Spacer(Modifier.height(18.dp))

            Material3SettingsGroup(
                title = "Status",
                items =
                    buildList {
                        if (fallingBack) {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.info),
                                    title = { Text(stringResource(R.string.lossless_fallback_title)) },
                                    description = { Text(stringResource(R.string.lossless_fallback_desc)) },
                                    showChevron = true,
                                    onClick = { showSourceDialog = true },
                                ),
                            )
                        }
                        lastCheck?.let { check ->
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.graphic_eq),
                                    title = { Text(stringResource(R.string.lossless_last_check_title)) },
                                    description = { Text("${check.track} \u2192 ${check.outcome}") },
                                ),
                            )
                        }
                    },
            )
        }

        Spacer(Modifier.height(18.dp))

        Material3SettingsGroup(
            title = "Audio quality",
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.graphic_eq),
                        title = { Text(stringResource(R.string.audio_quality)) },
                        description = {
                            Text(
                                when {
                                    audioQuality != AudioQuality.LOSSLESS ->
                                        "$qualityLabel · turn on Lossless for masters"
                                    source.isNotBlank() -> "$qualityLabel · source first"
                                    usingBuiltIn -> "$qualityLabel · built-in source"
                                    else -> "$qualityLabel · YouTube's best"
                                },
                            )
                        },
                        showChevron = true,
                        onClick = { navController.navigate("settings/player") },
                    ),
                ),
        )

        Spacer(Modifier.height(27.dp))

        Material3SettingsGroup(
            title = "Source",
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.cloud),
                        title = { Text("Lossless source") },
                        description = {
                            Text(
                                when {
                                    source.isNotBlank() -> {
                                        val host = LosslessSource.hostOf(source) ?: source
                                        "$host · sends videoId, title, artist and duration; expects a FLAC/ALAC URL."
                                    }
                                    builtInHost != null ->
                                        "Built in · $builtInHost · nothing to configure; a URL here overrides it."
                                    else ->
                                        "Not configured — Lossless uses YouTube's best stream (lossy)."
                                },
                            )
                        },
                        showChevron = true,
                        onClick = { showSourceDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.close),
                        title = { Text("Clear source") },
                        description = {
                            Text(
                                if (builtInHost != null) {
                                    "Use the built-in source."
                                } else {
                                    "Fall back to YouTube's best audio."
                                },
                            )
                        },
                        enabled = source.isNotBlank(),
                        onClick = {
                            onSourceChange("")
                            LosslessSource.configure(null)
                        },
                    ),
                ),
        )

        Spacer(Modifier.height(27.dp))

        Material3SettingsGroup(
            title = "Output path",
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.headset_applemusic),
                        title = { Text(report.route) },
                        description = { Text(report.note) },
                    ),
                ),
        )

        Text(
            text =
                "The module receives GET ?videoId=&title=&artist=&duration= (or {videoId}, " +
                    "{title}, {artist}, {duration} placeholders in the URL) and answers " +
                    "{\"url\": …, \"codec\": \"FLAC\", \"bitDepth\": 24, \"sampleRate\": 96000}. " +
                    "Only the URL is required.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text("Lossless") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
