/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.playback

import com.jay.glossy.R

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
<<<<<<< HEAD
import android.net.ConnectivityManager
import android.widget.Toast
=======
>>>>>>> origin/main
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
<<<<<<< HEAD
import com.jay.glossy.constants.DownloadOverWifiOnlyKey
import com.jay.glossy.utils.dataStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
=======
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
>>>>>>> origin/main


@AndroidEntryPoint
class ExoDownloadService : DownloadService(
    NOTIFICATION_ID,
    1000L,
    CHANNEL_ID,
    R.string.downloading,
    0
) {
    @Inject
    lateinit var downloadUtil: DownloadUtil

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == REMOVE_ALL_PENDING_DOWNLOADS) {
            downloadManager.currentDownloads.forEach { download ->
                downloadManager.removeDownload(download.request.id)
            }
        }
<<<<<<< HEAD

        // "Download over Wi-Fi only": hold new downloads back while the device
        // is on mobile data, and explain why nothing started.
        if (intent?.action == ACTION_ADD_DOWNLOAD && blockDownloadOnMeteredNetwork()) {
            Toast.makeText(this, R.string.glossy_download_wifi_blocked, Toast.LENGTH_LONG).show()
            return START_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun blockDownloadOnMeteredNetwork(): Boolean {
        val wifiOnly = runCatching {
            runBlocking { dataStore.data.first()[DownloadOverWifiOnlyKey] ?: true }
        }.getOrDefault(true)
        if (!wifiOnly) return false

        val manager = getSystemService(ConnectivityManager::class.java) ?: return false
        return manager.isActiveNetworkMetered
    }

=======
        return super.onStartCommand(intent, flags, startId)
    }

>>>>>>> origin/main
    override fun getDownloadManager() = downloadUtil.downloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val progressNotification =
            downloadUtil.downloadNotificationHelper.buildProgressNotification(
                this,
                R.drawable.download,
                null,
                if (downloads.size == 1) {
                    Util.fromUtf8Bytes(downloads[0].request.data)
                } else {
                    resources.getQuantityString(R.plurals.n_song, downloads.size, downloads.size)
                },
                downloads,
                notMetRequirements
            )

        val cancelAction =
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.close),
                getString(android.R.string.cancel),
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, ExoDownloadService::class.java).setAction(
                        REMOVE_ALL_PENDING_DOWNLOADS
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ).build()

        return Notification.Builder.recoverBuilder(this, progressNotification)
            .setActions(cancelAction)
            .build()
    }


    /**
     * This helper will outlive the lifespan of a single instance of [ExoDownloadService]
     */
    class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private var nextNotificationId: Int,
    ) : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_FAILED) {
                val notification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.error,
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "download"
        const val NOTIFICATION_ID = 1
        const val JOB_ID = 1
        const val REMOVE_ALL_PENDING_DOWNLOADS = "REMOVE_ALL_PENDING_DOWNLOADS"
    }
}
