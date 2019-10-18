/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.downloader.fetch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.DownloadNotification
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string

class FetchDownloadNotificationManager(context: Context) :
  DefaultFetchNotificationManager(context) {
  override fun getFetchInstanceForNamespace(namespace: String) = Fetch.getDefaultInstance()

  override fun createNotificationChannels(
    context: Context,
    notificationManager: NotificationManager
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channelId = context.getString(R.string.fetch_notification_default_channel_id)
      if (notificationManager.getNotificationChannel(channelId) == null) {
        notificationManager.createNotificationChannel(createChannel(channelId, context))
      }
    }
  }

  override fun updateNotification(
    notificationBuilder: NotificationCompat.Builder,
    downloadNotification: DownloadNotification,
    context: Context
  ) {
    val smallIcon = if (downloadNotification.isDownloading) {
      android.R.drawable.stat_sys_download
    } else {
      android.R.drawable.stat_sys_download_done
    }
    notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(smallIcon)
      .setContentTitle(downloadNotification.title)
      .setContentText(getSubtitleText(context, downloadNotification))
      .setOngoing(downloadNotification.isOnGoingNotification)
      .setGroup(downloadNotification.groupId.toString())
      .setSound(null)
      .setSound(null, AudioManager.STREAM_NOTIFICATION)
      .setVibrate(null)
      .setGroupSummary(false)
    if (downloadNotification.isFailed || downloadNotification.isCompleted) {
      notificationBuilder.setProgress(0, 0, false)
    } else {
      val progressIndeterminate = downloadNotification.progressIndeterminate
      val maxProgress = if (downloadNotification.progressIndeterminate) 0 else 100
      val progress = if (downloadNotification.progress < 0) 0 else downloadNotification.progress
      notificationBuilder.setProgress(maxProgress, progress, progressIndeterminate)
    }
    when {
      downloadNotification.isDownloading ||
        downloadNotification.isPaused ||
        downloadNotification.isQueued -> {
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
      }
      else -> {
        notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
      }
    }
  }

  @RequiresApi(VERSION_CODES.O)
  private fun createChannel(channelId: String, context: Context) =
    NotificationChannel(
      channelId,
      context.getString(string.fetch_notification_default_channel_name),
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      setSound(null, null)
      enableVibration(false)
    }
}
