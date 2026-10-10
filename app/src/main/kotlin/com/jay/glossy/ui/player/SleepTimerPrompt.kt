/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The minute picker behind the sleep timer. It lives on its own because two
 * hosts open it — the player and the player overflow menu — and neither should
 * carry a private copy of the same dialog.
 */

package com.jay.glossy.ui.player

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.constants.SleepTimerDefaultKey
import com.jay.glossy.constants.SleepTimerFadeOutKey
import com.jay.glossy.constants.SleepTimerStopAfterCurrentSongKey
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SleepTimerPrompt(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()

    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)

    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember { derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() } }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        icon = { Icon(painter = painterResource(R.drawable.bedtime), contentDescription = null) },
        title = { Text(stringResource(R.string.sleep_timer)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                playerConnection.service.sleepTimer?.start(
                    minute = sleepTimerValue.roundToInt(),
                    stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                    fadeOut = sleepTimerFadeOut,
                )
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pluralStringResource(R.plurals.minute, sleepTimerValue.roundToInt(), sleepTimerValue.roundToInt()),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = sleepTimerValue,
                    onValueChange = { sleepTimerValue = it },
                    valueRange = 5f..120f,
                    steps = (120 - 5) / 5 - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isAtDefault) {
                        Button(
                            onClick = {
                                scope.launch { context.safeDataStoreEdit { settings -> settings[SleepTimerDefaultKey] = sleepTimerValue } }
                                Toast.makeText(context, String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        ) { Text(stringResource(R.string.set_as_default)) }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch { context.safeDataStoreEdit { settings -> settings[SleepTimerDefaultKey] = sleepTimerValue } }
                                Toast.makeText(context, String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()), Toast.LENGTH_SHORT).show()
                            }
                        ) { Text(stringResource(R.string.set_as_default)) }
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            playerConnection.service.sleepTimer?.start(minute = -1)
                        }
                    ) { Text(stringResource(R.string.end_of_song)) }
                }
            }
        },
    )
}
