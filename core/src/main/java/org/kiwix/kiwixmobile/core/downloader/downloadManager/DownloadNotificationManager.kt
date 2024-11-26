/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.DEFAULT_NOTIFICATION_TIMEOUT_AFTER
import org.kiwix.kiwixmobile.core.utils.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import org.kiwix.kiwixmobile.core.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import java.util.Locale
import javax.inject.Inject

const val DOWNLOAD_NOTIFICATION_TITLE = "OPEN_ZIM_FILE"

class DownloadNotificationManager @Inject constructor(
  private val context: Context,
  private val notificationManager: NotificationManager
) {
  private val downloadNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (notificationManager.getNotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID) == null) {
        notificationManager.createNotificationChannel(createChannel(context))
      }
    }
  }

  fun updateNotification(
    downloadNotificationModel: DownloadNotificationModel,
    assignNewForegroundServiceNotification: AssignNewForegroundServiceNotification
  ) {
    synchronized(downloadNotificationsBuilderMap) {
      if (shouldUpdateNotification(downloadNotificationModel)) {
        notificationManager.notify(
          downloadNotificationModel.downloadId,
          createNotification(downloadNotificationModel)
        )
      } else {
        // the download is cancelled/paused so remove the notification.
        assignNewForegroundServiceNotification.assignNewForegroundServiceNotification(
          downloadNotificationModel.downloadId.toLong()
        )
      }
    }
  }

  fun createNotification(downloadNotificationModel: DownloadNotificationModel): Notification {
    synchronized(downloadNotificationsBuilderMap) {
      createNotificationChannel()
      val notificationBuilder = getNotificationBuilder(downloadNotificationModel.downloadId)
      val smallIcon = if (downloadNotificationModel.progress != HUNDERED) {
        android.R.drawable.stat_sys_download
      } else {
        android.R.drawable.stat_sys_download_done
      }

      notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setSmallIcon(smallIcon)
        .setContentTitle(downloadNotificationModel.title)
        .setContentText(getSubtitleText(context, downloadNotificationModel))
        .setOngoing(downloadNotificationModel.isOnGoingNotification)
        .setGroupSummary(false)
      if (downloadNotificationModel.isFailed || downloadNotificationModel.isCompleted) {
        notificationBuilder.setProgress(ZERO, ZERO, false)
      } else {
        notificationBuilder.setProgress(HUNDERED, downloadNotificationModel.progress, false)
      }
      when {
        downloadNotificationModel.isDownloading ->
          notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER)
            .addAction(
              R.drawable.ic_baseline_stop,
              context.getString(R.string.cancel),
              getActionPendingIntent(ACTION_CANCEL, downloadNotificationModel.downloadId)
            ).addAction(
              R.drawable.ic_baseline_pause,
              getPauseOrResumeTitle(true),
              getActionPendingIntent(ACTION_PAUSE, downloadNotificationModel.downloadId)
            )

        downloadNotificationModel.isPaused ->
          notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER)
            .addAction(
              R.drawable.ic_baseline_stop,
              context.getString(R.string.cancel),
              getActionPendingIntent(ACTION_CANCEL, downloadNotificationModel.downloadId)
            ).addAction(
              R.drawable.ic_baseline_play,
              getPauseOrResumeTitle(false),
              getActionPendingIntent(ACTION_RESUME, downloadNotificationModel.downloadId)
            )

        downloadNotificationModel.isQueued ->
          notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER)

        else -> notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
      }
      notificationCustomisation(downloadNotificationModel, notificationBuilder, context)
      return@createNotification notificationBuilder.build()
    }
  }

  private fun getPauseOrResumeTitle(isPause: Boolean): String {
    val pauseOrResumeTitle = if (isPause) {
      context.getString(R.string.tts_pause)
    } else {
      context.getString(R.string.tts_resume)
    }
    return pauseOrResumeTitle.replaceFirstChar {
      if (it.isLowerCase()) {
        it.titlecase(Locale.ROOT)
      } else {
        "$it"
      }
    }
  }

  private fun shouldUpdateNotification(
    downloadNotificationModel: DownloadNotificationModel
  ): Boolean = !downloadNotificationModel.isCancelled && !downloadNotificationModel.isPaused

  @SuppressLint("UnspecifiedImmutableFlag")
  private fun notificationCustomisation(
    downloadNotificationModel: DownloadNotificationModel,
    notificationBuilder: NotificationCompat.Builder,
    context: Context
  ) {
    if (downloadNotificationModel.isCompleted) {
      val internal = Intents.internal(CoreMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(DOWNLOAD_NOTIFICATION_TITLE, downloadNotificationModel.filePath)
      }
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          ZERO,
          internal,
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
      notificationBuilder.setContentIntent(pendingIntent)
      notificationBuilder.setAutoCancel(true)
    }
  }

  @SuppressLint("RestrictedApi")
  private fun getNotificationBuilder(notificationId: Int): NotificationCompat.Builder {
    synchronized(downloadNotificationsBuilderMap) {
      val notificationBuilder = downloadNotificationsBuilderMap[notificationId]
        ?: NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
      downloadNotificationsBuilderMap[notificationId] = notificationBuilder
      notificationBuilder
        .setGroup("$notificationId")
        .setStyle(null)
        .setProgress(ZERO, ZERO, false)
        .setContentTitle(null)
        .setContentText(null)
        .setContentIntent(null)
        .setGroupSummary(false)
        .setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
        .setOngoing(false)
        .setOnlyAlertOnce(true)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .mActions.clear()
      return@getNotificationBuilder notificationBuilder
    }
  }

  private fun getActionPendingIntent(action: String, downloadId: Int): PendingIntent {
    val pendingIntent =
      Intent(context, DownloadMonitorService::class.java).apply {
        putExtra(NOTIFICATION_ACTION, action)
        putExtra(EXTRA_DOWNLOAD_ID, downloadId)
      }
    val requestCode = downloadId + action.hashCode()
    return PendingIntent.getService(
      context,
      requestCode,
      pendingIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel(context: Context) =
    NotificationChannel(
      DOWNLOAD_NOTIFICATION_CHANNEL_ID,
      context.getString(R.string.download_notification_channel_name),
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      setSound(null, null)
      enableVibration(false)
    }

  private fun getSubtitleText(
    context: Context,
    downloadNotificationModel: DownloadNotificationModel
  ): String {
    return when {
      downloadNotificationModel.isCompleted -> context.getString(R.string.complete)
      downloadNotificationModel.isFailed -> context.getString(
        R.string.failed_state,
        downloadNotificationModel.error
      )

      downloadNotificationModel.isPaused -> context.getString(R.string.paused_state)
      downloadNotificationModel.isQueued -> context.getString(R.string.pending_state)
      downloadNotificationModel.etaInMilliSeconds <= ZERO ->
        context.getString(R.string.running_state)

      else -> Seconds(
        downloadNotificationModel.etaInMilliSeconds / THOUSAND.toLong()
      ).toHumanReadableTime()
    }
  }

  fun cancelNotification(notificationId: Int) {
    synchronized(downloadNotificationsBuilderMap) {
      notificationManager.cancel(notificationId)
      downloadNotificationsBuilderMap.remove(notificationId)
    }
  }

  companion object {
    const val NOTIFICATION_ACTION = "notification_action"
    const val ACTION_PAUSE = "action_pause"
    const val ACTION_RESUME = "action_resume"
    const val ACTION_CANCEL = "action_cancel"
    const val ACTION_QUERY_DOWNLOAD_STATUS = "action_query_download_status"
    const val EXTRA_DOWNLOAD_ID = "extra_download_id"
  }
}
