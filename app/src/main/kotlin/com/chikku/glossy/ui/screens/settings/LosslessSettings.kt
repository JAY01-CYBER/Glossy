/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.chikku.glossy.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.R
import com.jay.glossy.constants.LosslessFolderKey
import com.jay.glossy.constants.LosslessFolderNameKey
import com.jay.glossy.constants.PreferLosslessKey
import com.jay.glossy.lossless.LosslessLibrary
import com.jay.glossy.lossless.LosslessOutput
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.Material3SettingsGroup
import com.jay.glossy.ui.component.Material3SettingsItem
import com.jay.glossy.ui.utils.backToMain
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.launch

/**
 * Settings › Playback › Lossless.
 *
 * Three facts the screen has to keep straight: what has been indexed, which
 * files count as lossless, and whether the path those files take out of the
 * phone can carry them. The last one is read from the audio HAL rather than
 * assumed, because most listening happens over Bluetooth, where every sample
 * is re-encoded before it leaves the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LosslessSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (preferLossless, onPreferLosslessChange) = rememberPreference(PreferLosslessKey, false)
    val (folderUri, onFolderChange) = rememberPreference(LosslessFolderKey, "")
    val (folderName, onFolderNameChange) = rememberPreference(LosslessFolderNameKey, "")

    val tracks by LosslessLibrary.tracks.collectAsStateWithLifecycle()
    val scanning by LosslessLibrary.scanning.collectAsStateWithLifecycle()
    val scanned by LosslessLibrary.progress.collectAsStateWithLifecycle()

    var report by remember { mutableStateOf(LosslessOutput.report(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preferLossless) { LosslessLibrary.setEnabled(preferLossless) }
    LaunchedEffect(Unit) {
        LosslessLibrary.restore(context)
        report = LosslessOutput.report(context)
    }

    val scanFolder: (Uri) -> Unit = { uri ->
        scope.launch {
            message = null
            val count = LosslessLibrary.scan(context, uri)
            message =
                when {
                    count < 0 -> "That folder could not be read, so the index is unchanged."
                    count == 0 -> "No FLAC or WAV files found in that folder."
                    else -> "Indexed $count files."
                }
            report = LosslessOutput.report(context)
        }
    }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val name = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: "Selected folder"
                onFolderChange(uri.toString())
                onFolderNameChange(name)
                scanFolder(uri)
            }
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
                "YouTube streams are compressed, so the only lossless audio Glossy can play is a file " +
                    "you already own. Point this at a folder of FLAC or WAV rips and the player will use " +
                    "the file whenever it is the same song as the stream.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Spacer(Modifier.height(18.dp))

        Material3SettingsGroup(
            title = "Your files",
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.graphic_eq),
                        title = { Text("Prefer my lossless copy") },
                        description = { Text("Play the file instead of YouTube's stream when both are the same song.") },
                        trailingContent = {
                            Switch(
                                checked = preferLossless,
                                onCheckedChange = onPreferLosslessChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (preferLossless) R.drawable.check else R.drawable.close,
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onPreferLosslessChange(!preferLossless) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.storage),
                        title = { Text("Lossless folder") },
                        description = { Text(folderName.ifBlank { "No folder chosen" }) },
                        showChevron = true,
                        onClick = { picker.launch(null) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sync),
                        title = { Text(if (scanning) "Reading files…" else "Rescan folder") },
                        description = {
                            Text(
                                when {
                                    scanning -> "$scanned files found so far"
                                    tracks.isEmpty() -> "Nothing indexed yet"
                                    else ->
                                        "${tracks.size} files indexed · " +
                                            tracks.map { it.badge }
                                                .filter { it.isNotBlank() }
                                                .distinct()
                                                .take(3)
                                                .joinToString(", ")
                                },
                            )
                        },
                        enabled = folderUri.isNotBlank() && !scanning,
                        onClick = {
                            folderUri.takeIf { it.isNotBlank() }?.let { scanFolder(Uri.parse(it)) }
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.close),
                        title = { Text("Clear index") },
                        description = { Text("Forget the files without revoking the folder.") },
                        enabled = tracks.isNotEmpty() && !scanning,
                        onClick = {
                            scope.launch {
                                LosslessLibrary.forget(context)
                                message = "Index cleared."
                            }
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

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

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
