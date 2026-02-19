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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.zim_manager.Byte
import javax.inject.Inject

const val DOWNLOAD_NOTIFICATION_TITLE = "OPEN_ZIM_FILE"

@Suppress("all")
class FetchDownloadNotificationManager @Inject constructor(
  val context: Context,
  private val downloadRoomDao: DownloadRoomDao
) : DefaultFetchNotificationManager(context) {
  private val notificationBuilderLock = Any()

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

  override fun getSubtitleText(
    context: Context,
    downloadNotification: DownloadNotification
  ): String {
    return when {
      downloadNotification.isCompleted -> context.getString(R.string.complete)
      downloadNotification.isFailed -> context.getString(R.string.download_failed_state)
      downloadNotification.isPaused -> buildSubtitle(
        context.getString(R.string.paused_state),
        downloadNotification.downloaded,
        downloadNotification.total
      )

      downloadNotification.isQueued -> context.getString(R.string.resuming_state)
      downloadNotification.etaInMilliSeconds < 0 -> context.getString(R.string.downloading_state)
      else -> buildSubtitle(
        super.getSubtitleText(context, downloadNotification),
        downloadNotification.downloaded,
        downloadNotification.total
      )
    }
  }

  private fun buildSubtitle(
    mainText: String,
    downloaded: Long,
    total: Long
  ): String {
    val sizeText = getDownloadedSizeText(downloaded, total)
    return "$mainText • $sizeText"
  }

  private fun getDownloadedSizeText(downloadedBytes: Long, totalBytes: Long): String {
    if (downloadedBytes <= 0 || totalBytes <= 0) return ""
    val downloadedText = Byte(downloadedBytes.toString()).humanReadable
    val totalText = Byte(totalBytes.toString()).humanReadable
    return "$downloadedText/$totalText"
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
    val smallIcon =
      if (downloadNotification.isDownloading) {
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
        if (downloadRoomDao.getDownload().firstOrNull() != null) {
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
        } else {
          notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
            .addAction(
              drawable.fetch_notification_cancel,
              context.getString(R.string.cancel),
              getActionPendingIntent(downloadNotification, DownloadNotification.ActionType.DELETE)
            )
        }

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
    // Remove the already shown notification if any, because fetch now pushes a
    // download complete notification.
    removeNotificationIfAlreadyShowingForCompletedDownload(downloadNotification)
  }

  /**
   * We are adding 33 to the groupId (which is the download ID) because the download
   * complete notification is shown by DownloadMonitorService. If the application resumes
   * just before the download completes, Fetch in the application might also push a
   * download complete notification.
   *
   * To avoid duplicate notifications, we clear the previous notification if it is already shown.
   * See #4237 for more information.
   *
   * @see DownloadMonitorService.showDownloadCompletedNotification
   */
  private fun removeNotificationIfAlreadyShowingForCompletedDownload(
    downloadNotification: DownloadNotification
  ) {
    if (downloadNotification.isCompleted) {
      downloadNotificationManager.cancel(downloadNotification.groupId + THIRTY_TREE)
    }
  }

  @SuppressLint("UnspecifiedImmutableFlag")
  private fun notificationCustomisation(
    downloadNotification: DownloadNotification,
    notificationBuilder: NotificationCompat.Builder,
    context: Context
  ) {
    if (downloadNotification.isCompleted) {
      val internal =
        Intents.internal(CoreMainActivity::class.java).apply {
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

  fun showDownloadPauseNotification(
    fetch: Fetch,
    download: Download
  ) {
    val notificationBuilder = getNotificationBuilder(download.id, download.id)
    val pauseNotification = getPauseNotification(fetch, download, notificationBuilder)
    downloadNotificationManager.notify(download.id, pauseNotification)
  }

  @Suppress("InjectDispatcher")
  private fun getPauseNotification(
    fetch: Fetch,
    download: Download,
    notificationBuilder: Builder
  ): Notification {
    synchronized(notificationBuilderLock) {
      val downloadTitle = getDownloadNotificationTitle(download)
      val notificationTitle =
        runBlocking(Dispatchers.IO) {
          downloadRoomDao.getEntityForFileName(downloadTitle)?.title
            ?: downloadTitle
        }
      return notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(notificationTitle)
        .setContentText(
          buildSubtitle(
            context.getString(R.string.paused_state),
            download.downloaded,
            download.total
          )
        )
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
  }

  private fun getActionPendingIntent(
    fetch: Fetch,
    download: Download,
    actionType: DownloadNotification.ActionType
  ): PendingIntent {
    val intent =
      Intent(notificationManagerAction).apply {
        putExtra(EXTRA_NAMESPACE, fetch.namespace)
        putExtra(EXTRA_DOWNLOAD_ID, download.id)
        putExtra(EXTRA_NOTIFICATION_ID, download.id)
        putExtra(EXTRA_GROUP_ACTION, false)
        putExtra(EXTRA_NOTIFICATION_GROUP_ID, download.id)
      }
    val action =
      when (actionType) {
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
