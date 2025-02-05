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
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import com.tonyodev.fetch2.ACTION_TYPE_CANCEL
import com.tonyodev.fetch2.ACTION_TYPE_DELETE
import com.tonyodev.fetch2.ACTION_TYPE_INVALID
import com.tonyodev.fetch2.ACTION_TYPE_PAUSE
import com.tonyodev.fetch2.ACTION_TYPE_RESUME
import com.tonyodev.fetch2.ACTION_TYPE_RETRY
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.DownloadNotification
import com.tonyodev.fetch2.DownloadNotification.ActionType.CANCEL
import com.tonyodev.fetch2.DownloadNotification.ActionType.DELETE
import com.tonyodev.fetch2.DownloadNotification.ActionType.PAUSE
import com.tonyodev.fetch2.DownloadNotification.ActionType.RESUME
import com.tonyodev.fetch2.DownloadNotification.ActionType.RETRY
import com.tonyodev.fetch2.EXTRA_ACTION_TYPE
import com.tonyodev.fetch2.EXTRA_DOWNLOAD_ID
import com.tonyodev.fetch2.EXTRA_GROUP_ACTION
import com.tonyodev.fetch2.EXTRA_NAMESPACE
import com.tonyodev.fetch2.EXTRA_NOTIFICATION_GROUP_ID
import com.tonyodev.fetch2.EXTRA_NOTIFICATION_ID
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.R.drawable
import com.tonyodev.fetch2.R.string
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import javax.inject.Inject

const val DOWNLOAD_NOTIFICATION_TITLE = "OPEN_ZIM_FILE"

class FetchDownloadNotificationManager @Inject constructor(
  val context: Context,
  private val downloadRoomDao: DownloadRoomDao
) : DefaultFetchNotificationManager(context) {
  private val downloadNotificationManager: NotificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  override fun getFetchInstanceForNamespace(namespace: String): Fetch = Fetch.getDefaultInstance()

  override fun registerBroadcastReceiver() {
    val context = CoreApp.instance.applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(
        broadcastReceiver,
        IntentFilter(notificationManagerAction),
        Context.RECEIVER_EXPORTED
      )
    } else {
      context.registerReceiver(
        broadcastReceiver,
        IntentFilter(notificationManagerAction)
      )
    }
  }

  override fun createNotificationChannels(
    context: Context,
    notificationManager: NotificationManager
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channelId =
        context.getString(string.fetch_notification_default_channel_id)
      if (notificationManager.getNotificationChannel(channelId) == null) {
        notificationManager.createNotificationChannel(createChannel(channelId, context))
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel(channelId: String, context: Context) =
    NotificationChannel(
      channelId,
      context.getString(string.fetch_notification_default_channel_name),
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      setSound(null, null)
      enableVibration(false)
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
    val notificationTitle =
      downloadRoomDao.getEntityForFileName(downloadNotification.title)?.title
        ?: downloadNotification.title
    notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(smallIcon)
      .setContentTitle(notificationTitle)
      .setContentText(getSubtitleText(context, downloadNotification))
      .setOngoing(downloadNotification.isOnGoingNotification)
      .setGroup(downloadNotification.groupId.toString())
      .setGroupSummary(false)
    if (downloadNotification.isFailed || downloadNotification.isCompleted) {
      notificationBuilder.setProgress(ZERO, ZERO, false)
    } else {
      val progressIndeterminate = downloadNotification.progressIndeterminate
      val maxProgress = if (downloadNotification.progressIndeterminate) ZERO else HUNDERED
      val progress =
        if (downloadNotification.progress < ZERO) ZERO else downloadNotification.progress
      notificationBuilder.setProgress(maxProgress, progress, progressIndeterminate)
    }
    when {
      downloadNotification.isDownloading ->
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
          .addAction(
            drawable.fetch_notification_cancel,
            context.getString(R.string.cancel),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.DELETE)
          ).addAction(
            drawable.fetch_notification_pause,
            context.getString(R.string.notification_pause_button_text),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.PAUSE)
          )

      downloadNotification.isPaused ->
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
          .addAction(
            drawable.fetch_notification_resume,
            context.getString(R.string.notification_resume_button_text),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.RESUME)
          )
          .addAction(
            drawable.fetch_notification_cancel,
            context.getString(R.string.cancel),
            getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.DELETE)
          )

      downloadNotification.isQueued ->
        notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())

      else -> notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
    }
    notificationCustomisation(downloadNotification, notificationBuilder, context)
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
      val pendingIntent =
        getActivity(
          context,
          downloadNotification.notificationId,
          internal,
          FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
      notificationBuilder.setContentIntent(pendingIntent)
      notificationBuilder.setAutoCancel(true)
    }
  }

  fun showDownloadPauseNotification(fetch: Fetch, download: Download) {
    CoroutineScope(Dispatchers.IO).launch {
      val notificationBuilder = getNotificationBuilder(download.id, download.id)
      val cancelNotification = getCancelNotification(fetch, download, notificationBuilder)
      downloadNotificationManager.notify(download.id, cancelNotification)
    }
  }

  fun getCancelNotification(
    fetch: Fetch,
    download: Download,
    notificationBuilder: Builder
  ): Notification {
    val downloadTitle = getDownloadNotificationTitle(download)
    val notificationTitle =
      runBlocking(Dispatchers.IO) {
        downloadRoomDao.getEntityForFileName(downloadTitle)?.title
          ?: downloadTitle
      }
    return notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle(notificationTitle)
      .setContentText(context.getString(string.fetch_notification_download_paused))
      // Set the ongoing true so that could not cancel the pause notification.
      // However, on Android 14 and above user can cancel the notification by swipe right so we
      // can't control that see https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
      .setOngoing(true)
      .setGroup(download.id.toString())
      .setGroupSummary(false)
      .setProgress(HUNDERED, download.progress, false)
      .addAction(
        drawable.fetch_notification_cancel,
        context.getString(R.string.cancel),
        getActionPendingIntent(fetch, download, DownloadNotification.ActionType.DELETE)
      )
      .addAction(
        drawable.fetch_notification_resume,
        context.getString(R.string.notification_resume_button_text),
        getActionPendingIntent(fetch, download, DownloadNotification.ActionType.RESUME)
      )
      .build()
  }

  private fun getActionPendingIntent(
    fetch: Fetch,
    download: Download,
    actionType: DownloadNotification.ActionType
  ): PendingIntent {
    val intent = Intent(notificationManagerAction).apply {
      putExtra(EXTRA_NAMESPACE, fetch.namespace)
      putExtra(EXTRA_DOWNLOAD_ID, download.id)
      putExtra(EXTRA_NOTIFICATION_ID, download.id)
      putExtra(EXTRA_GROUP_ACTION, false)
      putExtra(EXTRA_NOTIFICATION_GROUP_ID, download.id)
    }
    val action = when (actionType) {
      CANCEL -> ACTION_TYPE_CANCEL
      DELETE -> ACTION_TYPE_DELETE
      RESUME -> ACTION_TYPE_RESUME
      PAUSE -> ACTION_TYPE_PAUSE
      RETRY -> ACTION_TYPE_RETRY
      else -> ACTION_TYPE_INVALID
    }
    intent.putExtra(EXTRA_ACTION_TYPE, action)
    return PendingIntent.getBroadcast(
      context,
      download.id + action,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }
}

fun Download.isPaused() = status == Status.PAUSED
