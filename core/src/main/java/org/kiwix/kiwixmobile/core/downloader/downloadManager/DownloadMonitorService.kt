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
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.R
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import com.tonyodev.fetch2core.DownloadBlock
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import javax.inject.Inject

class DownloadMonitorService : Service() {
  private val updater = PublishSubject.create<() -> Unit>()
  private var updaterDisposable: Disposable? = null
  private var monitoringDisposable: Disposable? = null
  private val notificationManager: NotificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }
  private val downloadNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()

  @Inject
  lateinit var fetch: Fetch

  @Inject
  lateinit var fetchDownloadNotificationManager: FetchDownloadNotificationManager

  @Inject
  lateinit var downloadRoomDao: DownloadRoomDao

  override fun onCreate() {
    CoreApp.coreComponent
      .coreServiceComponent()
      .service(this)
      .build()
      .inject(this)
    super.onCreate()
    setupUpdater()
    fetch.addListener(fetchListener, true)
    setForegroundNotification()
  }

  private fun setupUpdater() {
    updaterDisposable = updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      { it.invoke() },
      Throwable::printStackTrace
    )
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == STOP_DOWNLOAD_SERVICE) {
      stopForegroundServiceForDownloads()
    }
    return START_NOT_STICKY
  }

  /**
   * Sets the foreground notification for the service.
   * This notification is used to display the current download progress,
   * and it is updated dynamically based on the state of the downloads.
   *
   * The method checks for any active downloads and, if found, updates the notification
   * with the latest download progress. If there are no active downloads,
   * the service is stopped and removed from the foreground. Additionally, if the user cancels a
   * download, the corresponding notification is immediately removed to reflect the cancellation.
   *
   * @param downloadId Optional parameter representing the ID of the download whose notification
   *                   should be canceled if the user cancels the download.
   */
  private fun setForegroundNotification(downloadId: Int? = null) {
    updater.onNext {
      // Cancel the ongoing download notification if the user cancels the download.
      downloadId?.let(::cancelNotificationForId)
      fetch.getDownloads { downloadList ->
        downloadList.firstOrNull {
          it.status == Status.NONE ||
            it.status == Status.ADDED ||
            it.status == Status.QUEUED ||
            it.status == Status.DOWNLOADING ||
            it.isPaused()
        }?.let(::setForegroundNotificationForDownload) ?: kotlin.run {
          stopForegroundServiceForDownloads()
          // Cancel the last ongoing notification after detaching it from
          // the foreground service if no active downloads are found.
          downloadId?.let(::cancelNotificationForId)
        }
      }
    }
  }

  private fun setForegroundNotificationForDownload(it: Download) {
    val notificationBuilder =
      fetchDownloadNotificationManager.getNotificationBuilder(it.id, it.id)
    var foreGroundServiceNotification = notificationBuilder.build()
    if (it.isPaused()) {
      // Clear any pending actions on this notification builder.
      notificationBuilder.clearActions()
      // If a download is paused that means there is no notification for it, so we have to
      // show our custom cancel notification.
      foreGroundServiceNotification =
        fetchDownloadNotificationManager.getCancelNotification(fetch, it, notificationBuilder)
    }
    startForeground(it.id, foreGroundServiceNotification)
  }

  private fun cancelNotificationForId(downloadId: Int) {
    notificationManager.cancel(downloadId)
  }

  private val fetchListener = object : FetchListener {
    override fun onAdded(download: Download) {
      // Do nothing
    }

    override fun onCancelled(download: Download) {
      delete(download)
    }

    override fun onCompleted(download: Download) {
      update(download, true)
    }

    override fun onDeleted(download: Download) {
      delete(download)
    }

    override fun onDownloadBlockUpdated(
      download: Download,
      downloadBlock: DownloadBlock,
      totalBlocks: Int
    ) {
      update(download)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
      update(download, true)
    }

    override fun onPaused(download: Download) {
      update(download)
    }

    override fun onProgress(
      download: Download,
      etaInMilliSeconds: Long,
      downloadedBytesPerSecond: Long
    ) {
      update(download)
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
      update(download)
    }

    override fun onRemoved(download: Download) {
      delete(download)
    }

    override fun onResumed(download: Download) {
      update(download)
    }

    override fun onStarted(
      download: Download,
      downloadBlocks: List<DownloadBlock>,
      totalBlocks: Int
    ) {
      update(download)
    }

    override fun onWaitingNetwork(download: Download) {
      update(download)
    }

    private fun update(
      download: Download,
      shouldSetForegroundNotification: Boolean = false
    ) {
      updater.onNext {
        downloadRoomDao.update(download)
        if (download.status == Status.COMPLETED) {
          downloadRoomDao.getEntityForDownloadId(download.id.toLong())?.let {
            showDownloadCompletedNotification(download)
            // to move these downloads in NewBookDao.
            downloadRoomDao.downloads().blockingFirst()
          }
        }
        // If someone pause the Download then post a notification since fetch removes the
        // notification for ongoing download when pause so we needs to show our custom notification.
        if (download.isPaused()) {
          fetchDownloadNotificationManager.showDownloadPauseNotification(fetch, download).also {
            setForeGroundServiceNotificationIfNoActiveDownloads(fetch, download)
          }
        }
        if (shouldSetForegroundNotification) {
          setForegroundNotification(download.id)
        }
      }
    }

    private fun delete(download: Download) {
      updater.onNext {
        downloadRoomDao.delete(download)
        setForegroundNotification(download.id)
      }
    }
  }

  private fun setForeGroundServiceNotificationIfNoActiveDownloads(
    fetch: Fetch,
    download: Download
  ) {
    updater.onNext {
      // Check if there are any ongoing downloads.
      // If the list is empty, it means no other downloads are running,
      // so we need to promote this download to a foreground service.
      fetch.getDownloadsWithStatus(
        listOf(Status.NONE, Status.ADDED, Status.QUEUED, Status.DOWNLOADING)
      ) { activeDownloads ->
        if (activeDownloads.isEmpty()) {
          setForegroundNotificationForDownload(download)
        }
      }
    }
  }

  @Suppress("MagicNumber")
  private fun showDownloadCompletedNotification(download: Download) {
    downloadNotificationChannel()
    val notificationBuilder = getNotificationBuilder(download.id)
    val notificationTitle =
      downloadRoomDao.getEntityForFileName(getDownloadNotificationTitle(download))?.title
        ?: download.file
    notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle(notificationTitle)
      .setContentText(getString(R.string.fetch_notification_download_complete))
      .setOngoing(false)
      .setGroup(download.id.toString())
      .setGroupSummary(false)
      .setProgress(ZERO, ZERO, false)
      .setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
      .setContentIntent(getPendingIntentForDownloadedNotification(download))
      .setAutoCancel(true)
    // Assigning a new ID to the notification because the same ID is used for the foreground
    // notification. If we use the same ID, changing the foreground notification for another
    // ongoing download cancels the previous notification for that id, preventing the download
    // complete notification from being displayed.
    val downloadCompleteNotificationId = download.id + 33
    notificationManager.notify(downloadCompleteNotificationId, notificationBuilder.build())
  }

  private fun getPendingIntentForDownloadedNotification(download: Download): PendingIntent {
    val internal =
      Intents.internal(CoreMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(DOWNLOAD_NOTIFICATION_TITLE, getDownloadNotificationTitle(download))
      }
    return PendingIntent.getActivity(
      this,
      download.id,
      internal,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  private fun downloadNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (notificationManager.getNotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID) == null) {
        notificationManager.createNotificationChannel(createChannel())
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel() =
    NotificationChannel(
      DOWNLOAD_NOTIFICATION_CHANNEL_ID,
      getString(string.download_notification_channel_name),
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      setSound(null, null)
      enableVibration(false)
    }

  @SuppressLint("RestrictedApi")
  private fun getNotificationBuilder(notificationId: Int): NotificationCompat.Builder {
    synchronized(downloadNotificationsBuilderMap) {
      val notificationBuilder =
        downloadNotificationsBuilderMap[notificationId]
          ?: NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
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

  private fun getDownloadNotificationTitle(download: Download): String =
    fetchDownloadNotificationManager.getDownloadNotificationTitle(download)

  /**
   * Stops the foreground service, disposes of resources, and removes the Fetch listener.
   */
  private fun stopForegroundServiceForDownloads() {
    monitoringDisposable?.dispose()
    updaterDisposable?.dispose()
    fetch.removeListener(fetchListener)
    stopForeground(STOP_FOREGROUND_DETACH)
    stopSelf()
  }

  companion object {
    const val STOP_DOWNLOAD_SERVICE = "stop_download_service"
  }
}
