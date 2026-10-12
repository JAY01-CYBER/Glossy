/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Decisions the host tapped on a Listen Together notification while the app had
 * no usable connection.
 *
 * A notification action reaches [ListenTogetherActionReceiver] as a broadcast,
 * and Android is free to cold-start the process just to deliver it. In that
 * state there is no socket and [ListenTogetherClient.getInstance] is still null,
 * so the tap used to be dropped — which is why "Approve" appeared to do nothing
 * after the app had been killed. A decision that cannot be delivered straight
 * away is written down here instead, and replayed by the client as soon as it is
 * hosting a room again.
 */

package com.jay.glossy.listentogether

import android.content.Context
import com.jay.glossy.constants.ListenTogetherPendingActionsKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/** What the host tapped, and which user or suggestion it was about. */
@Serializable
data class PendingNotificationAction(
    val kind: String,
    val targetId: String,
    val enqueuedAt: Long,
)

object ListenTogetherPendingActions {
    private const val TAG = "ListenTogether"

    const val KIND_APPROVE_JOIN = "approve_join"
    const val KIND_REJECT_JOIN = "reject_join"
    const val KIND_APPROVE_SUGGESTION = "approve_suggestion"
    const val KIND_REJECT_SUGGESTION = "reject_suggestion"

    /**
     * A decision only means something inside the room it was made for, so
     * anything older than this is dropped rather than replayed into whatever
     * room the host happens to be hosting later.
     */
    private const val MAX_AGE_MS = 10 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    /** Writes a decision down, discarding anything that has since gone stale. */
    suspend fun enqueue(
        context: Context,
        action: PendingNotificationAction,
    ) {
        try {
            val kept = (read(context) + action).filterNot { it.isStale() }
            context.safeDataStoreEdit { preferences ->
                preferences[ListenTogetherPendingActionsKey] = json.encodeToString(kept)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to queue notification action")
        }
    }

    /** Takes every queued decision, clearing the store so nothing replays twice. */
    suspend fun takeAll(context: Context): List<PendingNotificationAction> {
        return try {
            val queued = read(context).filterNot { it.isStale() }
            if (queued.isNotEmpty()) {
                context.safeDataStoreEdit { preferences ->
                    preferences.remove(ListenTogetherPendingActionsKey)
                }
            }
            queued
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to read queued notification actions")
            emptyList()
        }
    }

    private fun read(context: Context): List<PendingNotificationAction> {
        val stored = context.dataStore.get(ListenTogetherPendingActionsKey, "")
        if (stored.isEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<PendingNotificationAction>>(stored) }
            .getOrElse {
                Timber.tag(TAG).e(it, "Discarding unreadable queued notification actions")
                emptyList()
            }
    }

    private fun PendingNotificationAction.isStale(): Boolean =
        System.currentTimeMillis() - enqueuedAt > MAX_AGE_MS
}
