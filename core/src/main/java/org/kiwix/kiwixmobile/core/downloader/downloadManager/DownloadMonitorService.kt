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

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.R.drawable
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import com.tonyodev.fetch2core.DownloadBlock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.Intents
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.PauseReason
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.ACTIVE_DOWNLOAD_GROUP_KEY
import org.kiwix.kiwixmobile.core.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

const val THIRTY_TREE = 33
const val DOWNLOAD_SERVICE_NOTIFICATION_ID = 1
const val DOWNLOAD_TIMEOUT_RESUME_INTENT = "downloadTimeoutResumeIntent"
const val BACKGROUND_DOWNLOAD_LIMIT_REACH_ACTION = "backgroundDownloadLimitReachAction"
const val DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID = 2
const val DOWNLOAD_NOTIFICATION_GROUP_SUMMARY_ID = 3
const val DOWNLOAD_TIMEOUT_NOTIFICATION_YES_REQUEST_CODE = 2001
const val DOWNLOAD_TIMEOUT_NOTIFICATION_NO_REQUEST_CODE = 2002

private val NETWORK_RELATED_ERRORS = setOf(
  Error.NO_NETWORK_CONNECTION,
  Error.CONNECTION_TIMED_OUT,
  Error.UNKNOWN_HOST
)

class DownloadMonitorService : Service() {
  private val taskFlow = MutableSharedFlow<suspend () -> Unit>(extraBufferCapacity = Int.MAX_VALUE)

  @Inject
  @IoDispatcher
  lateinit var ioDispatcher: CoroutineDispatcher

  private var updaterJob: Job? = null
  private var scope: CoroutineScope? = null
  private val notificationManager: NotificationManager by lazy {
    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  }


  @Inject
  lateinit var fetch: Fetch

  @Inject
  lateinit var fetchDownloadNotificationManager: FetchDownloadNotificationManager

  @Inject
  lateinit var downloadRoomDao: DownloadRoomDao

  @Inject
  lateinit var connectivityManager: ConnectivityManager

  @Inject
  lateinit var kiwixDataStore: KiwixDataStore
  
  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      resumeQueuedDownloadsOnNetworkAvailable()
    }

    override fun onLost(network: Network) {
      scope?.launch {
        fetch.getDownloadsWithStatus(Status.DOWNLOADING) { activeDownloads ->
          activeDownloads.forEach { download ->
            fetchDownloadNotificationManager.showDownloadPauseNotification(
              fetch,
              download,
              isOffline = true
            )
          }
        }
      }
    }
  }

  /**
   * Resumes all downloads that are currently in the QUEUED state
   * when network connectivity becomes available.
   *
   * It ensures that any downloads paused due to lack of connectivity
   * are resumed automatically once the network is restored.
   */
  private fun resumeQueuedDownloadsOnNetworkAvailable() {
    scope?.launch {
      fetch.getDownloadsWithStatus(listOf(Status.QUEUED, Status.FAILED)) { downloadsToResume ->
        downloadsToResume.forEach { download ->
          when (download.status) {
            Status.FAILED if download.error in NETWORK_RELATED_ERRORS ->
              fetch.retry(download.id)

            Status.QUEUED -> fetch.resume(download.id)
            else -> {}
          }
        }
      }
    }
  }

  override fun onCreate() {
    CoreApp.coreComponent
      .coreServiceComponent()
      .service(this)
      .build()
      .inject(this)
    super.onCreate()
    scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    fetch.addListener(fetchListener, true)
    setupUpdater()
    startForegroundService()
    registerNetworkCallback()
    isDownloadMonitorServiceRunning = true
  }

  private fun registerNetworkCallback() {
    runCatching {
      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        .build()
      connectivityManager.registerNetworkCallback(request, networkCallback)
    }.onFailure { it.printStackTrace() }
  }

  private fun unregisterNetworkCallback() {
    runCatching {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    }.onFailure { it.printStackTrace() }
  }

  private fun setupUpdater() {
    updaterJob = scope?.launch {
      taskFlow.collect { task ->
        runCatching {
          task.invoke()
        }.onFailure { it.printStackTrace() }
      }
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == STOP_DOWNLOAD_SERVICE) {
      stopForegroundServiceForDownloads()
    }
    return START_STICKY
  }

  /**
   * Called when the foreground service is about to reach its timeout limit.
   *
   * Starting from Android 15, foreground services can run for only 6 hours per day
   * while running in the background, unless the user explicitly opens the app
   * again, which resets this timer.
   *
   * To prevent the system from killing the service and throwing
   * `ForegroundServiceDidNotStopInTimeException`, we proactively stop the
   * download service here. When the user returns to the app, the download
   * process will resume automatically.
   *
   * More details: https://developer.android.com/develop/background-work/services/fgs/timeout
   */
  override fun onTimeout(startId: Int, fgsType: Int) {
    // We have to use runBlocking otherwise it will call `super.onTimeout` immediately
    // and our service will not properly stop.
    runBlocking {
      showDownloadBackgroundLimitReachNotification()
    }
    super.onTimeout(startId, fgsType)
  }

  /**
   * Shows a notification when the download background limit is reached.
   *
   * The notification contains two buttons: "Yes" and "No".
   * - Tapping "Yes" launches the app, which resets the 6-hour background limit.
   * - Tapping "No" simply dismisses the notification. The user can still open
   *   the app later to resume the download.
   *
   * This method also dismisses any ongoing or paused download notifications,
   * because once this limit is reached, the user can no longer resume downloads
   * from notifications. Keeping those notifications visible can be confusing.
   */
  private suspend fun showDownloadBackgroundLimitReachNotification() {
    downloadRoomDao.getOngoingDownloads().forEach { downloadModel ->
      // Remove all ongoing notification along with paused notifications.
      // Also, pause the ongoing downloads.
      runCatching {
        if (!downloadModel.isPaused) {
          fetch.pause(downloadModel.downloadId.toInt())
          updatePauseReasonInDatabase(downloadModel.downloadId)
        }
        notificationManager.cancel(downloadModel.downloadId.toInt())
      }.onFailure { it.printStackTrace() }
    }
    notificationManager.notify(
      DOWNLOAD_TIMEOUT_LIMIT_REACH_NOTIFICATION_ID,
      buildTimeoutNotification()
    )
    stopForegroundServiceForDownloads()
  }

  /**
   * Updates the pause reason of a download in the database.
   *
   * This marks the download as paused due to the service (i.e., Android's background
   * timeout limit). By storing this information, we can later identify and automatically
   * resume only those downloads when the app returns to the foreground.
   */
  private fun updatePauseReasonInDatabase(downloadId: Long) {
    taskFlow.tryEmit {
      // Update the download entity in database so that we can resume all downloads
      // once app become visible in foreground
      downloadRoomDao.getEntityForDownloadId(downloadId)?.let { downloadRoomEntity ->
        downloadRoomDao.updateDownloadItem(
          downloadRoomEntity.copy(pauseReason = PauseReason.SERVICE, status = Status.PAUSED)
        )
      }
    }
  }

  private suspend fun buildTimeoutNotification(): Notification {
    val yesIntent = Intents.internal(CoreMainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      // on clicking on yes button it will open the "Download" screen.
      // For custom apps, it will simply open the app, and the rest custom reader screen
      // automatically handles it.
      putExtra(DOWNLOAD_TIMEOUT_RESUME_INTENT, true)
    }
    val yesPendingIntent = PendingIntent.getActivity(
      this,
      DOWNLOAD_TIMEOUT_NOTIFICATION_YES_REQUEST_CODE,
      yesIntent,
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
    )

    val noIntent = Intent(this, DownloadTimeoutDismissReceiver::class.java).apply {
      action = BACKGROUND_DOWNLOAD_LIMIT_REACH_ACTION
    }
    val noPendingIntent = PendingIntent.getBroadcast(
      this,
      DOWNLOAD_TIMEOUT_NOTIFICATION_NO_REQUEST_CODE,
      noIntent,
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
      .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setContentTitle(kiwixDataStore.appName.first())
      .setContentText(getString(R.string.download_timeout_resume_message))
      .setAutoCancel(true)
      .setOngoing(false)
      .setOnlyAlertOnce(true)
      .addAction(
        drawable.fetch_notification_resume,
        getString(R.string.yes),
        yesPendingIntent
      )
      .addAction(
        drawable.fetch_notification_cancel,
        getString(R.string.no),
        noPendingIntent
      )
      .build()
  }

  private fun startForegroundService() {
    runCatching {
      CoroutineScope(ioDispatcher).launch {
        fetchDownloadNotificationManager.createNotificationChannels(this@DownloadMonitorService, notificationManager)
        startForeground(DOWNLOAD_SERVICE_NOTIFICATION_ID, buildForegroundNotification())
        startPausedDownloadsDueToAndroidServiceLimitation()
      }
    }.onFailure { it.printStackTrace() }
  }

  private suspend fun buildForegroundNotification(): Notification =
    NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
      .setContentTitle(kiwixDataStore.appName.first())
      .setContentText(getString(string.download_notification_channel_description))
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setGroup(ACTIVE_DOWNLOAD_GROUP_KEY)
      .setOnlyAlertOnce(true)
      .setWhen(System.currentTimeMillis())
      .build()


  /**
   * Resumes all downloads that were previously paused by the service due to Android's
   * background timeout limitation.
   *
   * This method:
   * 1. Fetches all downloads marked with `PauseReason.SERVICE`.
   * 2. Resumes each download using Fetch.
   * 3. Resets their `pauseReason` to `NONE` and updates the status to `QUEUED`
   *    so they can continue downloading normally.
   */
  private suspend fun startPausedDownloadsDueToAndroidServiceLimitation() {
    downloadRoomDao.getDownloadsPausedByService()
      .forEach {
        fetch.resume(it.downloadId.toInt())
        // Reset the PauseReason.
        downloadRoomDao.getEntityForDownloadId(it.downloadId)?.let { downloadRoomEntity ->
          downloadRoomDao.updateDownloadItem(
            downloadRoomEntity.copy(pauseReason = PauseReason.NONE, status = Status.QUEUED)
          )
        }
      }
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
      if (error in NETWORK_RELATED_ERRORS) {
        taskFlow.tryEmit {
          fetchDownloadNotificationManager.showDownloadPauseNotification(
            fetch,
            download,
            isOffline = true
          )
        }
      }
      update(download)
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
      taskFlow.tryEmit {
        fetchDownloadNotificationManager.showDownloadPauseNotification(
          fetch,
          download,
          isOffline = true
        )
      }
      update(download)
    }

    private fun update(
      download: Download,
      updateForeGroundService: Boolean = false
    ) {
      taskFlow.tryEmit {
        downloadRoomDao.update(download)
        if (download.status == Status.COMPLETED) {
          downloadRoomDao.getEntityForDownloadId(download.id.toLong())?.let {
            showDownloadCompletedNotification(download)
            kiwixDataStore.setRateAppDownloadCompleted()
            // to move these downloads in LibkiwixBookOnDisk.
            @Suppress("IgnoredReturnValue")
            downloadRoomDao.downloads().first()
          }
        }

        // Show a pause notification only when the user explicitly paused the download.
        // Network-loss pausing is handled via onWaitingNetwork, onError (network errors only),
        // and networkCallback.onLost — those paths call showDownloadPauseNotification(isOffline=true).
        when {
          download.status == Status.FAILED || download.status == Status.QUEUED ->
            fetchDownloadNotificationManager.showDownloadPauseNotification(
              fetch,
              download,
              isOffline = true
            )

          download.isPaused() ->
            fetchDownloadNotificationManager.showDownloadPauseNotification(
              fetch,
              download,
              isOffline = false
            )
        }
        if (updateForeGroundService) {
          stopForegroundServiceIfNoActiveDownloads(fetch)
        }
      }
    }

    private fun delete(download: Download) {
      taskFlow.tryEmit {
        downloadRoomDao.delete(download)
        stopForegroundServiceIfNoActiveDownloads(fetch)
      }
    }
  }

  private fun stopForegroundServiceIfNoActiveDownloads(fetch: Fetch) {
    taskFlow.tryEmit {
      fetch.getDownloadsWithStatus(
        listOf(Status.NONE, Status.ADDED, Status.QUEUED, Status.DOWNLOADING, Status.FAILED)
      ) { activeDownloads ->
        if (activeDownloads.isEmpty()) {
          stopForegroundServiceForDownloads()
        }
      }
    }
  }

  private fun showDownloadCompletedNotification(download: Download) {
    val downloadTitle = fetchDownloadNotificationManager.getDownloadNotificationTitle(download)
    val notificationTitle =
      downloadRoomDao.getEntityForFileName(downloadTitle)?.title
        ?: download.file
    val openActionPendingIntent = fetchDownloadNotificationManager.getOpenActionPendingIntent(
      this,
      downloadTitle,
      download.id + THIRTY_TREE
    )
    val notificationBuilder = NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
    notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle(notificationTitle)
      .setContentText(getString(string.complete))
      .setOngoing(false)
      .setGroup(download.id.toString())
      .setGroupSummary(false)
      .setProgress(ZERO, ZERO, false)
      .setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
      .setAutoCancel(true)
      .setContentIntent(openActionPendingIntent)
      .addAction(
        android.R.drawable.ic_menu_send,
        getString(R.string.open),
        openActionPendingIntent
      )
    // Assigning a new ID to the notification because the same ID is used for the foreground
    // notification. If we use the same ID, changing the foreground notification for another
    // ongoing download cancels the previous notification for that id, preventing the download
    // complete notification from being displayed.
    val downloadCompleteNotificationId = download.id + THIRTY_TREE
    // Cancel the complete download notification if already shown due to the application's
    // lifecycle fetch. See #4237 for more details.
    notificationManager.cancel(download.id - THIRTY_TREE)
    // Cancel the fetch related any notification if present.
    notificationManager.cancel(download.id)
    notificationManager.notify(downloadCompleteNotificationId, notificationBuilder.build())
  }



  @OptIn(ExperimentalCoroutinesApi::class)
  private fun stopForegroundServiceForDownloads() {
    updaterJob?.cancel()
    scope?.cancel()
    scope = null
    unregisterNetworkCallback()
    fetch.removeListener(fetchListener)
    notificationManager.cancel(DOWNLOAD_NOTIFICATION_GROUP_SUMMARY_ID)
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    isDownloadMonitorServiceRunning = false
  }

  companion object {
    const val STOP_DOWNLOAD_SERVICE = "stop_download_service"

    @JvmField var isDownloadMonitorServiceRunning = false
  }
}
