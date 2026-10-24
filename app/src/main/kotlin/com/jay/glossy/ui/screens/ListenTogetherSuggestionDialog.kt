/**
 * Glossy Project
 * Licensed under GPL-3.0
 *
 * The host's in-app popup for a guest's song suggestion. A suggestion also
 * arrives as a notification, but the host is usually inside the app while a
 * friend is picking music — this surfaces the pending request wherever they
 * are. Accept plays it for the room immediately; Deny (or swiping it away)
 * refuses it and leaves playback untouched.
 */

package com.jay.glossy.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.R
import com.chikku.glossy.listentogether.ListenTogetherManager

@Composable
fun ListenTogetherSuggestionDialog(
    manager: ListenTogetherManager,
) {
    // The client only fills this list for the host, so an empty list means
    // either nothing is pending or this device is a guest.
    val suggestions by manager.pendingSuggestions.collectAsStateWithLifecycle()
    val suggestion = suggestions.firstOrNull() ?: return

    AlertDialog(
        onDismissRequest = {
            manager.rejectSuggestion(suggestion.suggestionId, "Rejected by host")
        },
        title = { Text(stringResource(R.string.pending_suggestions)) },
        text = {
            Text(
                stringResource(
                    R.string.listen_together_suggestion_received,
                    suggestion.fromUsername,
                    suggestion.trackInfo.title,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { manager.approveSuggestion(suggestion.suggestionId) },
            ) {
                Text(stringResource(R.string.approve))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { manager.rejectSuggestion(suggestion.suggestionId, "Rejected by host") },
            ) {
                Text(stringResource(R.string.reject))
            }
        },
    )
}