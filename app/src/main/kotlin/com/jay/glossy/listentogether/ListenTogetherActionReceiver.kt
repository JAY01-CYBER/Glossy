package com.jay.glossy.listentogether

<<<<<<< HEAD
=======
import com.jay.glossy.R

>>>>>>> origin/main
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

<<<<<<< HEAD
/**
 * Host-side Approve/Reject actions on the Listen Together notifications.
 *
 * The tap is never dropped: if a live client can send the decision it is sent
 * (and the notification dismissed), and otherwise the decision is parked by
 * [ListenTogetherPendingActions] for the client to replay once it is hosting a
 * room again. The notification is only dismissed once the decision is either
 * sent or safely written down, so a failure leaves the host able to retry.
 */
class ListenTogetherActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val decision = intent.toPendingDecision()
        if (decision == null) {
            // Not an action we know, or its target extra is missing: leave the
            // notification up rather than swallowing the tap silently.
            return
        }

        val notifId = intent.getIntExtra(ListenTogetherClient.EXTRA_NOTIFICATION_ID, 0)
        val client = ListenTogetherClient.getInstance()

        if (client != null && client.dispatchNotificationDecision(decision)) {
            NotificationManagerCompat.from(context).cancel(notifId)
            return
        }

        // No live connection — most likely the process was cold-started just to
        // deliver this broadcast. Park the decision and let the client replay it.
        val pendingResult = goAsync()
        scope.launch {
            try {
                ListenTogetherPendingActions.enqueue(context, decision)
                NotificationManagerCompat.from(context).cancel(notifId)
            } finally {
                pendingResult.finish()
=======
class ListenTogetherActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val client = ListenTogetherClient.getInstance() ?: return
        val notifId = intent.getIntExtra(ListenTogetherClient.EXTRA_NOTIFICATION_ID, 0)

        // Cancel the notification immediately
        NotificationManagerCompat.from(context).cancel(notifId)

        when (intent.action) {
            ListenTogetherClient.ACTION_APPROVE_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                scope.launch {
                    client.approveJoin(userId)
                }
            }
            ListenTogetherClient.ACTION_REJECT_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                scope.launch {
                    client.rejectJoin(userId, null)
                }
            }
            ListenTogetherClient.ACTION_APPROVE_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                scope.launch {
                    client.approveSuggestion(suggestionId)
                }
            }
            ListenTogetherClient.ACTION_REJECT_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                scope.launch {
                    client.rejectSuggestion(suggestionId, null)
                }
>>>>>>> origin/main
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
<<<<<<< HEAD

        private fun Intent.toPendingDecision(): PendingNotificationAction? {
            val now = System.currentTimeMillis()
            return when (action) {
                ListenTogetherClient.ACTION_APPROVE_JOIN ->
                    getStringExtra(ListenTogetherClient.EXTRA_USER_ID)?.let {
                        PendingNotificationAction(ListenTogetherPendingActions.KIND_APPROVE_JOIN, it, now)
                    }

                ListenTogetherClient.ACTION_REJECT_JOIN ->
                    getStringExtra(ListenTogetherClient.EXTRA_USER_ID)?.let {
                        PendingNotificationAction(ListenTogetherPendingActions.KIND_REJECT_JOIN, it, now)
                    }

                ListenTogetherClient.ACTION_APPROVE_SUGGESTION ->
                    getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID)?.let {
                        PendingNotificationAction(ListenTogetherPendingActions.KIND_APPROVE_SUGGESTION, it, now)
                    }

                ListenTogetherClient.ACTION_REJECT_SUGGESTION ->
                    getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID)?.let {
                        PendingNotificationAction(ListenTogetherPendingActions.KIND_REJECT_SUGGESTION, it, now)
                    }

                else -> null
            }
        }
=======
>>>>>>> origin/main
    }
}
