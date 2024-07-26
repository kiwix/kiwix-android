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
import org.kiwix.kiwixmobile.core.downloader.fetch.DOWNLOAD_NOTIFICATION_TITLE
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.DEFAULT_NOTIFICATION_TIMEOUT_AFTER
import org.kiwix.kiwixmobile.core.utils.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import javax.inject.Inject

class DownloadNotificationManager @Inject constructor(
  private val context: Context,
  private val notificationManager: NotificationManager
) {
  private val downloadNotificationsMap = mutableMapOf<Int, DownloadModel>()
  private val downloadNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
        notificationManager.createNotificationChannel(createChannel(CHANNEL_ID, context))
      }
    }
  }

  fun updateNotification(
    downloadNotificationModel: DownloadNotificationModel
  ) {
    createNotificationChannel()
    val notificationBuilder = getNotificationBuilder(downloadNotificationModel.downloadId)
    val smallIcon = if (downloadNotificationModel.progress != 100) {
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
      notificationBuilder.setProgress(0, 0, false)
    } else {
      notificationBuilder.setProgress(100, downloadNotificationModel.progress, false)
    }
    when {
      downloadNotificationModel.isDownloading ->
        notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER)
          .addAction(
            R.drawable.fetch_notification_pause,
            context.getString(R.string.tts_pause),
            getActionPendingIntent(ACTION_PAUSE, downloadNotificationModel.downloadId)
          ).addAction(
            R.drawable.fetch_notification_cancel,
            context.getString(R.string.cancel),
            getActionPendingIntent(ACTION_CANCEL, downloadNotificationModel.downloadId)
          )

      downloadNotificationModel.isPaused ->
        notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER)
          .addAction(
            R.drawable.fetch_notification_resume,
            context.getString(R.string.tts_resume),
            getActionPendingIntent(ACTION_RESUME, downloadNotificationModel.downloadId)
          ).addAction(
            R.drawable.fetch_notification_cancel,
            context.getString(R.string.cancel),
            getActionPendingIntent(ACTION_CANCEL, downloadNotificationModel.downloadId)
          )

      downloadNotificationModel.isQueued ->
        notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER)

      else -> notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
    }
    notificationCustomisation(downloadNotificationModel, notificationBuilder, context)
    notificationManager.notify(downloadNotificationModel.downloadId, notificationBuilder.build())
  }

  @SuppressLint("UnspecifiedImmutableFlag")
  private fun notificationCustomisation(
    downloadNotificationModel: DownloadNotificationModel,
    notificationBuilder: NotificationCompat.Builder,
    context: Context
  ) {
    if (downloadNotificationModel.isCompleted) {
      val internal = Intents.internal(CoreMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(DOWNLOAD_NOTIFICATION_TITLE, downloadNotificationModel.title)
      }
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          0,
          internal,
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
      notificationBuilder.setContentIntent(pendingIntent)
      notificationBuilder.setAutoCancel(true)
    }
  }

  @SuppressLint("RestrictedApi")
  private fun getNotificationBuilder(notificationId: Int): NotificationCompat.Builder {
    synchronized(downloadNotificationsMap) {
      val notificationBuilder = downloadNotificationsBuilderMap[notificationId]
        ?: NotificationCompat.Builder(context, CHANNEL_ID)
      downloadNotificationsBuilderMap[notificationId] = notificationBuilder
      notificationBuilder
        .setGroup("$notificationId")
        .setStyle(null)
        .setProgress(0, 0, false)
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
    val intent =
      Intent(context, DownloadNotificationActionsBroadcastReceiver::class.java).apply {
        this.action = action
        putExtra(EXTRA_DOWNLOAD_ID, downloadId)
      }
    return PendingIntent.getBroadcast(
      context,
      0,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel(channelId: String, context: Context) =
    NotificationChannel(
      channelId,
      context.getString(R.string.app_name),
      NotificationManager.IMPORTANCE_DEFAULT
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
      downloadNotificationModel.etaInMilliSeconds <= 0 -> context.getString(R.string.running_state)
      else -> Seconds(
        downloadNotificationModel.etaInMilliSeconds / 1000L
      ).toHumanReadableTime()
    }
  }

  companion object {
    const val CHANNEL_ID = "kiwix_notification_channel_id"
    const val ACTION_PAUSE = "action_pause"
    const val ACTION_RESUME = "action_resume"
    const val ACTION_CANCEL = "action_cancel"
    const val EXTRA_DOWNLOAD_ID = "extra_download_id"
  }
}
