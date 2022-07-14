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

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.tonyodev.fetch2.ACTION_TYPE_CANCEL
import com.tonyodev.fetch2.ACTION_TYPE_DELETE
import com.tonyodev.fetch2.ACTION_TYPE_INVALID
import com.tonyodev.fetch2.ACTION_TYPE_PAUSE
import com.tonyodev.fetch2.ACTION_TYPE_RESUME
import com.tonyodev.fetch2.ACTION_TYPE_RETRY
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.DownloadNotification
import com.tonyodev.fetch2.EXTRA_ACTION_TYPE
import com.tonyodev.fetch2.EXTRA_DOWNLOAD_ID
import com.tonyodev.fetch2.EXTRA_GROUP_ACTION
import com.tonyodev.fetch2.EXTRA_NAMESPACE
import com.tonyodev.fetch2.EXTRA_NOTIFICATION_GROUP_ID
import com.tonyodev.fetch2.EXTRA_NOTIFICATION_ID
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.main.CoreMainActivity

const val DOWNLOAD_NOTIFICATION_TITLE = "OPEN_ZIM_FILE"

class FetchDownloadNotificationManager(private val context: Context) :
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
    // super method but with pause button removed
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
      downloadNotification.isDownloading ->
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
          .addAction(
            R.drawable.fetch_notification_cancel,
            context.getString(R.string.fetch_notification_download_cancel),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.CANCEL)
          )
      downloadNotification.isPaused ->
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
          .addAction(
            R.drawable.fetch_notification_resume,
            context.getString(R.string.fetch_notification_download_resume),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.RESUME)
          )
          .addAction(
            R.drawable.fetch_notification_cancel,
            context.getString(R.string.fetch_notification_download_cancel),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.CANCEL)
          )
      downloadNotification.isQueued ->
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
      else -> notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
    }
    notificationCustomisation(downloadNotification, notificationBuilder, context)
  }

  override fun getActionPendingIntent(
    downloadNotification: DownloadNotification,
    actionType: DownloadNotification.ActionType
  ): PendingIntent {
    val intent = Intent(notificationManagerAction)
    intent.putExtra(EXTRA_NAMESPACE, downloadNotification.namespace)
    intent.putExtra(EXTRA_DOWNLOAD_ID, downloadNotification.notificationId)
    intent.putExtra(EXTRA_NOTIFICATION_ID, downloadNotification.notificationId)
    intent.putExtra(EXTRA_GROUP_ACTION, false)
    intent.putExtra(EXTRA_NOTIFICATION_GROUP_ID, downloadNotification.groupId)
    val action = when (actionType) {
      DownloadNotification.ActionType.CANCEL -> ACTION_TYPE_CANCEL
      DownloadNotification.ActionType.DELETE -> ACTION_TYPE_DELETE
      DownloadNotification.ActionType.RESUME -> ACTION_TYPE_RESUME
      DownloadNotification.ActionType.PAUSE -> ACTION_TYPE_PAUSE
      DownloadNotification.ActionType.RETRY -> ACTION_TYPE_RETRY
      else -> ACTION_TYPE_INVALID
    }
    intent.putExtra(EXTRA_ACTION_TYPE, action)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    return@getActionPendingIntent PendingIntent.getBroadcast(
      context,
      downloadNotification.notificationId + action,
      intent,
      flags
    )
  }

  @SuppressLint("UnspecifiedImmutableFlag")
  private fun notificationCustomisation(
    downloadNotification: DownloadNotification,
    notificationBuilder: NotificationCompat.Builder,
    context: Context
  ) {
    if (downloadNotification.isCompleted) {
      val internal = Intents.internal(CoreMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(DOWNLOAD_NOTIFICATION_TITLE, downloadNotification.title)
      }
      val pendingIntent = if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
        getActivity(context, 0, internal, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
      } else {
        getActivity(context, 0, internal, FLAG_UPDATE_CURRENT)
      }
      notificationBuilder.setContentIntent(pendingIntent)
      notificationBuilder.setAutoCancel(true)
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
