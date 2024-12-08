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
import android.app.DownloadManager
import android.app.Service
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_CANCEL
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_PAUSE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_QUERY_DOWNLOAD_STATUS
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_RESUME
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val ZERO = 0
const val HUNDERED = 100
const val THOUSAND = 1000
const val DEFAULT_INT_VALUE = -1

/*
  These below values of android.provider.Downloads.Impl class,
  there is no direct way to access them so we defining the values
  from https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/provider/Downloads.java
 */
const val CONTROL_PAUSE = 1
const val CONTROL_RUN = 0
const val STATUS_RUNNING = 192
const val STATUS_PAUSED_BY_APP = 193
const val COLUMN_CONTROL = "control"
val downloadBaseUri: Uri = Uri.parse("content://downloads/my_downloads")

class DownloadMonitorService : Service() {

  @Inject
  lateinit var downloadManager: DownloadManager

  @Inject
  lateinit var downloadRoomDao: DownloadRoomDao

  @Inject
  lateinit var downloadNotificationManager: DownloadNotificationManager
  private val lock = Any()
  private var monitoringDisposable: Disposable? = null
  private val downloadInfoMap = mutableMapOf<Long, DownloadInfo>()
  private val updater = PublishSubject.create<() -> Unit>()
  private var foreGroundServiceInformation: Pair<Boolean, Int> = true to DEFAULT_INT_VALUE

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    CoreApp.coreComponent
      .coreServiceComponent()
      .service(this)
      .build()
      .inject(this)
    super.onCreate()
    setupUpdater()
    startMonitoringDownloads()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val downloadId =
      intent?.getIntExtra(DownloadNotificationManager.EXTRA_DOWNLOAD_ID, DEFAULT_INT_VALUE)
        ?: DEFAULT_INT_VALUE
    val notificationAction = intent?.getStringExtra(DownloadNotificationManager.NOTIFICATION_ACTION)
    if (downloadId != DEFAULT_INT_VALUE) {
      when (notificationAction) {
        ACTION_PAUSE -> pauseDownload(downloadId.toLong())
        ACTION_RESUME -> resumeDownload(downloadId.toLong())
        ACTION_CANCEL -> cancelDownload(downloadId.toLong())
        ACTION_QUERY_DOWNLOAD_STATUS -> queryDownloadStatus(downloadId.toLong())
      }
    }
    return START_NOT_STICKY
  }

  @Suppress("CheckResult")
  private fun setupUpdater() {
    updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      {
        synchronized(lock) { it.invoke() }
      },
      Throwable::printStackTrace
    )
  }

  /**
   * Starts monitoring ongoing downloads using a periodic observable.
   * This method sets up an observable that runs every 5 seconds to check the status of downloads.
   * It only starts the monitoring process if it's not already running and disposes of the observable
   * when there are no ongoing downloads to avoid unnecessary resource usage.
   */
  @Suppress("MagicNumber")
  private fun startMonitoringDownloads() {
    // Check if monitoring is already active. If it is, do nothing.
    if (monitoringDisposable?.isDisposed == false) return
    monitoringDisposable = Observable.interval(ZERO.toLong(), 5, TimeUnit.SECONDS)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          try {
            synchronized(lock) {
              if (downloadRoomDao.downloads().blockingFirst().isNotEmpty()) {
                checkDownloads()
              } else {
                stopForegroundServiceForDownloads()
              }
            }
          } catch (ignore: Exception) {
            Log.e(
              "DOWNLOAD_MONITOR",
              "Couldn't get the downloads update. Original exception = $ignore"
            )
          }
        },
        Throwable::printStackTrace
      )
  }

  @SuppressLint("Range")
  private fun checkDownloads() {
    synchronized(lock) {
      val query = DownloadManager.Query().setFilterByStatus(
        DownloadManager.STATUS_RUNNING or
          DownloadManager.STATUS_PAUSED or
          DownloadManager.STATUS_PENDING or
          DownloadManager.STATUS_SUCCESSFUL
      )
      downloadManager.query(query).use { cursor ->
        if (cursor.moveToFirst()) {
          do {
            val downloadId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
            queryDownloadStatus(downloadId)
          } while (cursor.moveToNext())
        }
      }
    }
  }

  @SuppressLint("Range")
  fun queryDownloadStatus(downloadId: Long) {
    synchronized(lock) {
      downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
        if (cursor.moveToFirst()) {
          handleDownloadStatus(cursor, downloadId)
        } else {
          handleCancelledDownload(downloadId)
        }
      }
    }
  }

  @SuppressLint("Range")
  private fun handleDownloadStatus(cursor: Cursor, downloadId: Long) {
    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
    val bytesDownloaded =
      cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
    val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
    val progress = calculateProgress(bytesDownloaded, totalBytes)

    val etaInMilliSeconds = calculateETA(downloadId, bytesDownloaded, totalBytes)

    when (status) {
      DownloadManager.STATUS_FAILED -> handleFailedDownload(
        downloadId,
        reason,
        progress,
        etaInMilliSeconds,
        bytesDownloaded,
        totalBytes
      )

      DownloadManager.STATUS_PAUSED -> handlePausedDownload(
        downloadId,
        progress,
        bytesDownloaded,
        totalBytes,
        reason
      )

      DownloadManager.STATUS_PENDING -> handlePendingDownload(downloadId)
      DownloadManager.STATUS_RUNNING -> handleRunningDownload(
        downloadId,
        progress,
        etaInMilliSeconds,
        bytesDownloaded,
        totalBytes
      )

      DownloadManager.STATUS_SUCCESSFUL -> handleSuccessfulDownload(
        downloadId,
        progress,
        etaInMilliSeconds
      )
    }
  }

  private fun handleCancelledDownload(downloadId: Long) {
    updater.onNext {
      updateDownloadStatus(downloadId, Status.CANCELLED, Error.CANCELLED)
      downloadRoomDao.delete(downloadId)
      downloadInfoMap.remove(downloadId)
    }
  }

  @Suppress("LongParameterList")
  private fun handleFailedDownload(
    downloadId: Long,
    reason: Int,
    progress: Int,
    etaInMilliSeconds: Long,
    bytesDownloaded: Long,
    totalBytes: Long
  ) {
    val error = mapDownloadError(reason)
    updateDownloadStatus(
      downloadId,
      Status.FAILED,
      error,
      progress,
      etaInMilliSeconds,
      bytesDownloaded,
      totalBytes
    )
  }

  private fun handlePausedDownload(
    downloadId: Long,
    progress: Int,
    bytesDownloaded: Long,
    totalSizeOfDownload: Long,
    reason: Int
  ) {
    val pauseReason = mapDownloadPauseReason(reason)
    updateDownloadStatus(
      downloadId = downloadId,
      status = Status.PAUSED,
      error = pauseReason,
      progress = progress,
      bytesDownloaded = bytesDownloaded,
      totalSizeOfDownload = totalSizeOfDownload
    )
  }

  private fun handlePendingDownload(downloadId: Long) {
    updateDownloadStatus(
      downloadId,
      Status.QUEUED,
      Error.NONE
    )
  }

  private fun handleRunningDownload(
    downloadId: Long,
    progress: Int,
    etaInMilliSeconds: Long,
    bytesDownloaded: Long,
    totalSizeOfDownload: Long
  ) {
    updateDownloadStatus(
      downloadId,
      Status.DOWNLOADING,
      Error.NONE,
      progress,
      etaInMilliSeconds,
      bytesDownloaded,
      totalSizeOfDownload
    )
  }

  private fun handleSuccessfulDownload(
    downloadId: Long,
    progress: Int,
    etaInMilliSeconds: Long
  ) {
    updateDownloadStatus(
      downloadId,
      Status.COMPLETED,
      Error.NONE,
      progress,
      etaInMilliSeconds
    )
    downloadInfoMap.remove(downloadId)
  }

  private fun calculateProgress(bytesDownloaded: Long, totalBytes: Long): Int =
    if (totalBytes > ZERO) {
      (bytesDownloaded / totalBytes.toDouble()).times(HUNDERED).toInt()
    } else {
      ZERO
    }

  private fun calculateETA(downloadedFileId: Long, bytesDownloaded: Long, totalBytes: Long): Long {
    val currentTime = System.currentTimeMillis()
    val downloadInfo = downloadInfoMap.getOrPut(downloadedFileId) {
      DownloadInfo(startTime = currentTime, initialBytesDownloaded = bytesDownloaded)
    }

    val elapsedTime = currentTime - downloadInfo.startTime
    val downloadSpeed = if (elapsedTime > ZERO) {
      (bytesDownloaded - downloadInfo.initialBytesDownloaded) / (elapsedTime / THOUSAND.toFloat())
    } else {
      ZERO.toFloat()
    }

    return if (downloadSpeed > ZERO) {
      ((totalBytes - bytesDownloaded) / downloadSpeed).toLong() * THOUSAND
    } else {
      ZERO.toLong()
    }
  }

  private fun mapDownloadError(reason: Int): Error {
    return when (reason) {
      DownloadManager.ERROR_CANNOT_RESUME -> Error.ERROR_CANNOT_RESUME
      DownloadManager.ERROR_DEVICE_NOT_FOUND -> Error.ERROR_DEVICE_NOT_FOUND
      DownloadManager.ERROR_FILE_ALREADY_EXISTS -> Error.ERROR_FILE_ALREADY_EXISTS
      DownloadManager.ERROR_FILE_ERROR -> Error.ERROR_FILE_ERROR
      DownloadManager.ERROR_HTTP_DATA_ERROR -> Error.ERROR_HTTP_DATA_ERROR
      DownloadManager.ERROR_INSUFFICIENT_SPACE -> Error.ERROR_INSUFFICIENT_SPACE
      DownloadManager.ERROR_TOO_MANY_REDIRECTS -> Error.ERROR_TOO_MANY_REDIRECTS
      DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> Error.ERROR_UNHANDLED_HTTP_CODE
      DownloadManager.ERROR_UNKNOWN -> Error.UNKNOWN
      else -> Error.UNKNOWN
    }
  }

  private fun mapDownloadPauseReason(reason: Int): Error {
    return when (reason) {
      DownloadManager.PAUSED_QUEUED_FOR_WIFI -> Error.QUEUED_FOR_WIFI
      DownloadManager.PAUSED_WAITING_TO_RETRY -> Error.WAITING_TO_RETRY
      DownloadManager.PAUSED_WAITING_FOR_NETWORK -> Error.WAITING_FOR_NETWORK
      DownloadManager.PAUSED_UNKNOWN -> Error.PAUSED_UNKNOWN
      else -> Error.PAUSED_UNKNOWN
    }
  }

  @Suppress("LongParameterList")
  private fun updateDownloadStatus(
    downloadId: Long,
    status: Status,
    error: Error,
    progress: Int = DEFAULT_INT_VALUE,
    etaInMilliSeconds: Long = DEFAULT_INT_VALUE.toLong(),
    bytesDownloaded: Long = DEFAULT_INT_VALUE.toLong(),
    totalSizeOfDownload: Long = DEFAULT_INT_VALUE.toLong()
  ) {
    synchronized(lock) {
      updater.onNext {
        downloadRoomDao.getEntityForDownloadId(downloadId)?.let { downloadEntity ->
          if (shouldUpdateDownloadStatus(downloadEntity)) {
            val downloadModel = DownloadModel(downloadEntity).apply {
              if (shouldUpdateDownloadStatus(status, error, downloadEntity)) {
                state = status
              }
              this.error = error
              if (progress > ZERO) {
                this.progress = progress
              }
              this.etaInMilliSeconds = etaInMilliSeconds
              if (bytesDownloaded != DEFAULT_INT_VALUE.toLong()) {
                this.bytesDownloaded = bytesDownloaded
              }
              if (totalSizeOfDownload != DEFAULT_INT_VALUE.toLong()) {
                this.totalSizeOfDownload = totalSizeOfDownload
              }
            }
            downloadRoomDao.update(downloadModel)
            updateNotification(downloadModel, downloadEntity.title, downloadEntity.description)
            return@let
          }
          cancelNotificationAndAssignNewNotificationToForegroundService(downloadId)
        } ?: run {
          // already downloaded/cancelled so cancel the notification if any running, and
          // assign new notification to foreground service.
          cancelNotificationAndAssignNewNotificationToForegroundService(downloadId)
        }
      }
    }
  }

  /**
   * Determines whether the download status should be updated based on the current status and error.
   *
   * This method checks the current download status and error, and decides whether to update the status
   * of the download entity. Specifically, it handles the case where a download is paused but has been
   * queued for resumption. In such cases, it ensures that the download manager is instructed to resume
   * the download, and prevents the status from being prematurely updated to "Paused".
   *
   * @param status The current status of the download.
   * @param error The current error state of the download.
   * @param downloadRoomEntity The download entity containing the current status and download ID.
   * @return `true` if the status should be updated, `false` otherwise.
   */
  private fun shouldUpdateDownloadStatus(
    status: Status,
    error: Error,
    downloadRoomEntity: DownloadRoomEntity
  ): Boolean {
    synchronized(lock) {
      return@shouldUpdateDownloadStatus if (
        status == Status.PAUSED &&
        downloadRoomEntity.status == Status.QUEUED
      ) {
        // Check if the user has resumed the download.
        // Do not update the download status immediately since the download manager
        // takes some time to actually resume the download. During this time,
        // it will still return the paused state.
        // By not updating the status right away, we ensure that the user
        // sees the "Pending" state, indicating that the download is in the process
        // of resuming.
        when (error) {
          // When the pause reason is unknown or waiting to retry, and the user
          // resumes the download, attempt to resume the download if it was not resumed
          // due to some reason.
          Error.PAUSED_UNKNOWN,
          Error.WAITING_TO_RETRY -> {
            resumeDownload(downloadRoomEntity.downloadId)
            false
          }

          // Return true to update the status of the download if there is any other status,
          // e.g., WAITING_FOR_WIFI, WAITING_FOR_NETWORK, or any other pause reason
          // to inform the user.
          else -> true
        }
      } else {
        true
      }
    }
  }

  private fun cancelNotificationAndAssignNewNotificationToForegroundService(downloadId: Long) {
    downloadNotificationManager.cancelNotification(downloadId.toInt())
    updateForegroundNotificationOrStopService()
  }

  private fun updateForegroundNotificationOrStopService() {
    val activeDownloads = getActiveDownloads()
    if (activeDownloads.isNotEmpty()) {
      // Promote the first active download to foreground
      val downloadRoomEntity = activeDownloads.first()
      foreGroundServiceInformation =
        foreGroundServiceInformation.first to downloadRoomEntity.downloadId.toInt()
      val downloadNotificationModel =
        getDownloadNotificationModel(
          DownloadModel(downloadRoomEntity),
          downloadRoomEntity.title,
          downloadRoomEntity.description
        )
      val notification = downloadNotificationManager.createNotification(downloadNotificationModel)
      startForeground(foreGroundServiceInformation.second, notification)
    } else {
      // Stop the service if no active downloads remain
      stopForegroundServiceForDownloads()
    }
  }

  private fun getActiveDownloads(): List<DownloadRoomEntity> =
    downloadRoomDao.downloadRoomEntity().blockingFirst().filter {
      it.status != Status.PAUSED && it.status != Status.CANCELLED
    }

  private fun updateNotification(
    downloadModel: DownloadModel,
    title: String,
    description: String?
  ) {
    val downloadNotificationModel = getDownloadNotificationModel(downloadModel, title, description)
    val notification = downloadNotificationManager.createNotification(downloadNotificationModel)
    if (foreGroundServiceInformation.first) {
      startForeground(downloadModel.downloadId.toInt(), notification)
      foreGroundServiceInformation = false to downloadModel.downloadId.toInt()
    } else {
      downloadNotificationManager.updateNotification(
        downloadNotificationModel,
        object : AssignNewForegroundServiceNotification {
          override fun assignNewForegroundServiceNotification(downloadId: Long) {
            cancelNotificationAndAssignNewNotificationToForegroundService(downloadId)
          }
        }
      )
    }
  }

  private fun getDownloadNotificationModel(
    downloadModel: DownloadModel,
    title: String,
    description: String?
  ): DownloadNotificationModel =
    DownloadNotificationModel(
      downloadId = downloadModel.downloadId.toInt(),
      status = downloadModel.state,
      progress = downloadModel.progress,
      etaInMilliSeconds = downloadModel.etaInMilliSeconds,
      title = title,
      description = description,
      filePath = downloadModel.file,
      error = DownloadState.from(
        downloadModel.state,
        downloadModel.error,
        downloadModel.book.url
      ).toReadableState(this).toString()
    )

  private fun pauseDownload(downloadId: Long) {
    synchronized(lock) {
      updater.onNext {
        if (pauseResumeDownloadInDownloadManagerContentResolver(
            downloadId,
            CONTROL_PAUSE,
            STATUS_PAUSED_BY_APP
          )
        ) {
          updateDownloadStatus(downloadId, Status.PAUSED, Error.NONE)
        }
      }
    }
  }

  private fun resumeDownload(downloadId: Long) {
    synchronized(lock) {
      updater.onNext {
        if (pauseResumeDownloadInDownloadManagerContentResolver(
            downloadId,
            CONTROL_RUN,
            STATUS_RUNNING
          )
        ) {
          updateDownloadStatus(downloadId, Status.QUEUED, Error.NONE)
        }
      }
    }
  }

  private fun cancelDownload(downloadId: Long) {
    synchronized(lock) {
      downloadManager.remove(downloadId)
      handleCancelledDownload(downloadId)
    }
  }

  @SuppressLint("Range")
  private fun pauseResumeDownloadInDownloadManagerContentResolver(
    downloadId: Long,
    control: Int,
    status: Int
  ): Boolean {
    return try {
      // Update the status to paused/resumed in the database
      val contentValues = ContentValues().apply {
        put(COLUMN_CONTROL, control)
        put(DownloadManager.COLUMN_STATUS, status)
      }
      val uri = ContentUris.withAppendedId(downloadBaseUri, downloadId)
      contentResolver
        .update(uri, contentValues, null, null)
      true
    } catch (ignore: Exception) {
      Log.e("DOWNLOAD_MONITOR", "Couldn't pause/resume the download. Original exception = $ignore")
      false
    }
  }

  private fun shouldUpdateDownloadStatus(downloadRoomEntity: DownloadRoomEntity) =
    downloadRoomEntity.status != Status.COMPLETED

  override fun onDestroy() {
    monitoringDisposable?.dispose()
    super.onDestroy()
  }

  private fun stopForegroundServiceForDownloads() {
    foreGroundServiceInformation = true to DEFAULT_INT_VALUE
    monitoringDisposable?.dispose()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }
}

data class DownloadInfo(
  var startTime: Long,
  var initialBytesDownloaded: Long
)

interface AssignNewForegroundServiceNotification {
  fun assignNewForegroundServiceNotification(downloadId: Long)
}
