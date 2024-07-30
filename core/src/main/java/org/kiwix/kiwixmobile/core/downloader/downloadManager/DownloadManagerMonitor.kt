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
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val ZERO = 0
const val HUNDERED = 100
const val THOUSAND = 1000
const val DEFAULT_INT_VALUE = -1

class DownloadManagerMonitor @Inject constructor(
  private val downloadManager: DownloadManager,
  val downloadRoomDao: DownloadRoomDao,
  private val context: Context,
  private val downloadNotificationManager: DownloadNotificationManager,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : DownloadMonitor, DownloadManagerBroadcastReceiver.Callback {

  private val updater = PublishSubject.create<() -> Unit>()
  private val lock = Any()
  private val downloadInfoMap = mutableMapOf<Long, DownloadInfo>()
  private var monitoringDisposable: Disposable? = null

  init {
    startMonitoringDownloads()
    setupUpdater()
  }

  override fun downloadCompleteOrCancelled(intent: Intent) {
    synchronized(lock) {
      intent.extras?.let {
        val downloadId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId != -1L) {
          queryDownloadStatus(downloadId)
        }
      }
    }
  }

  /**
   * Starts monitoring ongoing downloads using a periodic observable.
   * This method sets up an observable that runs every 5 seconds to check the status of downloads.
   * It only starts the monitoring process if it's not already running and disposes of the observable
   * when there are no ongoing downloads to avoid unnecessary resource usage.
   */
  @Suppress("MagicNumber")
  fun startMonitoringDownloads() {
    // Check if monitoring is already active. If it is, do nothing.
    if (monitoringDisposable?.isDisposed == false) return
    monitoringDisposable = Observable.interval(ZERO.toLong(), 5, TimeUnit.SECONDS)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          try {
            synchronized(lock) {
              Log.i(
                "DOWNLOAD_MONITOR",
                "Couldn't ${downloadRoomDao.downloads().blockingFirst()}"
              )
              if (downloadRoomDao.downloads().blockingFirst().isNotEmpty()) {
                checkDownloads()
              } else {
                // dispose to avoid unnecessary request to downloadManager
                // when there is no download ongoing.
                monitoringDisposable?.dispose()
              }
            }
          } catch (ignore: Exception) {
            Log.i(
              "DOWNLOAD_MONITOR",
              "Couldn't get the downloads update. Original exception = $ignore"
            )
          }
        },
        Throwable::printStackTrace
      )
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

  @SuppressLint("Range")
  private fun checkDownloads() {
    synchronized(lock) {
      val query = DownloadManager.Query().setFilterByStatus(
        DownloadManager.STATUS_RUNNING or
          DownloadManager.STATUS_PAUSED or
          DownloadManager.STATUS_PENDING
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
  private fun queryDownloadStatus(downloadId: Long) {
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
      cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
    val totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
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
        totalBytes
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
    bytesDownloaded: Int,
    totalBytes: Int
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
    bytesDownloaded: Int,
    totalSizeOfDownload: Int
  ) {
    updateDownloadStatus(
      downloadId = downloadId,
      status = Status.PAUSED,
      error = Error.NONE,
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
    bytesDownloaded: Int,
    totalSizeOfDownload: Int
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

  private fun calculateProgress(bytesDownloaded: Int, totalBytes: Int): Int =
    if (totalBytes > ZERO) {
      (bytesDownloaded / totalBytes.toDouble()).times(HUNDERED).toInt()
    } else {
      ZERO
    }

  private fun calculateETA(downloadedFileId: Long, bytesDownloaded: Int, totalBytes: Int): Long {
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

  @Suppress("LongParameterList")
  private fun updateDownloadStatus(
    downloadId: Long,
    status: Status,
    error: Error,
    progress: Int = DEFAULT_INT_VALUE,
    etaInMilliSeconds: Long = DEFAULT_INT_VALUE.toLong(),
    bytesDownloaded: Int = DEFAULT_INT_VALUE,
    totalSizeOfDownload: Int = DEFAULT_INT_VALUE
  ) {
    synchronized(lock) {
      updater.onNext {
        Log.e("DOWNLOAD_MONITOR", "update status: $status")
        downloadRoomDao.getEntityForDownloadId(downloadId)?.let { downloadEntity ->
          if (shouldUpdateStatus(downloadEntity)) {
            val downloadModel = DownloadModel(downloadEntity).apply {
              state = status
              this.error = error
              if (progress > ZERO) {
                this.progress = progress
              }
              this.etaInMilliSeconds = etaInMilliSeconds
              if (bytesDownloaded != DEFAULT_INT_VALUE) {
                this.bytesDownloaded = bytesDownloaded.toLong()
              }
              if (totalSizeOfDownload != DEFAULT_INT_VALUE) {
                this.totalSizeOfDownload = totalSizeOfDownload.toLong()
              }
            }
            downloadRoomDao.update(downloadModel)
            updateNotification(downloadModel, downloadEntity.title, downloadEntity.description)
            return@let
          }
          cancelNotification(downloadId)
        } ?: run {
          // already downloaded/cancelled so cancel the notification if any running.
          cancelNotification(downloadId)
        }
      }
    }
  }

  private fun cancelNotification(downloadId: Long) {
    downloadNotificationManager.cancelNotification(downloadId.toInt())
  }

  private fun updateNotification(
    downloadModel: DownloadModel,
    title: String,
    description: String?
  ) {
    downloadNotificationManager.updateNotification(
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
        ).toReadableState(context).toString()
      )
    )
  }

  fun pauseDownload(downloadId: Long) {
    synchronized(lock) {
      updater.onNext {
        val downloadEntity = downloadRoomDao.getEntityForDownloadId(downloadId)
        downloadEntity?.let {
          // Save the file path and the downloaded bytes
          val downloadedBytes = getBytesDownloaded(downloadId)
          it.bytesDownloaded = downloadedBytes.toLong()
          downloadRoomDao.update(DownloadModel(it))

          // Cancel the current download
          downloadManager.remove(downloadId)
          updateDownloadStatus(downloadId, Status.PAUSED, Error.NONE)
        }
        // if (pauseResumeDownloadInDownloadManagerContentResolver(downloadId, 1)) {
        //   updateDownloadStatus(downloadId, Status.PAUSED, Error.NONE)
        // }
      }
    }
  }

  fun resumeDownload(downloadId: Long) {
    synchronized(lock) {
      updater.onNext {
        try {
          val downloadEntity = downloadRoomDao.getEntityForDownloadId(downloadId)
          downloadEntity?.let {
            val file = File(it.file)
            val downloadedBytes = it.bytesDownloaded
            val downloadRequest = DownloadRequest(it.url ?: "")

            val newDownloadId =
              downloadManager.enqueue(
                downloadRequest.toDownloadManagerRequest(
                  sharedPreferenceUtil,
                  downloadedBytes
                )
              )
            it.downloadId = newDownloadId
            it.status = Status.QUEUED
            it.error = Error.NONE
            val downloadModel = DownloadModel(it)
            downloadRoomDao.update(downloadModel)
            updateNotification(downloadModel, downloadEntity.title, downloadEntity.description)
          }
        } catch (ignore: Exception) {
          ignore.printStackTrace()
        }
        // if (pauseResumeDownloadInDownloadManagerContentResolver(downloadId, 0)) {
        //   updateDownloadStatus(downloadId, Status.QUEUED, Error.NONE)
        // }
      }
    }
  }

  fun cancelDownload(downloadId: Long) {
    synchronized(lock) {
      downloadManager.remove(downloadId)
      handleCancelledDownload(downloadId)
    }
  }

  @SuppressLint("Range")
  private fun getBytesDownloaded(downloadId: Long): Int {
    downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
      if (cursor.moveToFirst()) {
        return@getBytesDownloaded cursor.getInt(
          cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        )
      }
    }
    return ZERO
  }

  @SuppressLint("Range")
  private fun pauseResumeDownloadInDownloadManagerContentResolver(
    downloadId: Long,
    control: Int
  ): Boolean {
    Log.e("DOWNLOAD_MONITOR", "pauseResumeDownloadInDownloadManagerContentResolver: $control")
    return try {
      // Update the status to paused/resumed in the database
      val contentValues = ContentValues().apply {
        put("control", control)
      }
      val uri =
        ContentUris.withAppendedId(Uri.parse("content://downloads/my_downloads"), downloadId)
      val downloadEntity = downloadRoomDao.getEntityForDownloadId(downloadId)
      context.contentResolver
        .update(
          uri,
          contentValues,
          "title=?",
          arrayOf(downloadEntity?.title)
        )
      true
    } catch (ignore: Exception) {
      Log.e("DOWNLOAD_MONITOR", "Couldn't pause/resume the download. Original exception = $ignore")
      false
    }
  }

  private fun shouldUpdateStatus(downloadRoomEntity: DownloadRoomEntity) =
    downloadRoomEntity.status != Status.COMPLETED

  override fun init() {
    // empty method to so class does not get reported unused
  }
}

data class DownloadInfo(
  var startTime: Long,
  var initialBytesDownloaded: Int
)
