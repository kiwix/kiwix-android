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
import android.content.Intent
import android.database.Cursor
import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  private val downloadManager: DownloadManager,
  private val downloadRoomDao: DownloadRoomDao
) : DownloadMonitor, DownloadManagerBroadcastReceiver.Callback {

  private val updater = PublishSubject.create<() -> Unit>()
  private val lock = Any()
  private val downloadInfoMap = mutableMapOf<Long, DownloadInfo>()

  init {
    startMonitoringDownloads()
    setupUpdater()
  }

  override fun downloadInformation(intent: Intent) {
    synchronized(lock) {
      intent.extras?.let {
        val downloadedFileId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadedFileId != -1L) {
          queryDownloadStatus(downloadedFileId)
        }
      }
    }
  }

  private fun startMonitoringDownloads() {
    // we have to disable this when no downloads is ongoing
    // and should re-enable when download started.
    Observable.interval(0, 5, TimeUnit.SECONDS)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          try {
            if (downloadRoomDao.downloads().blockingFirst().isNotEmpty()) {
              checkDownloads()
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
            val downloadedFileId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
            queryDownloadStatus(downloadedFileId)
          } while (cursor.moveToNext())
        }
      }
    }
  }

  @SuppressLint("Range")
  private fun queryDownloadStatus(downloadedFileId: Long) {
    synchronized(lock) {
      downloadManager.query(DownloadManager.Query().setFilterById(downloadedFileId)).use { cursor ->
        if (cursor.moveToFirst()) {
          handleDownloadStatus(cursor, downloadedFileId)
        } else {
          handleCancelledDownload(downloadedFileId)
        }
      }
    }
  }

  @SuppressLint("Range")
  private fun handleDownloadStatus(cursor: Cursor, downloadedFileId: Long) {
    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
    val bytesDownloaded =
      cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
    val totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
    val progress = calculateProgress(bytesDownloaded, totalBytes)

    Log.e(
      "HELLO",
      "downloadInformation: $downloadedFileId\n totalBytes= $totalBytes \n bytesDownloaded = $bytesDownloaded \n progress = $progress"
    )

    val etaInMilliSeconds = calculateETA(downloadedFileId, bytesDownloaded, totalBytes)

    when (status) {
      DownloadManager.STATUS_FAILED -> handleFailedDownload(
        downloadedFileId,
        reason,
        progress,
        etaInMilliSeconds
      )

      DownloadManager.STATUS_PAUSED -> handlePausedDownload(
        downloadedFileId,
        progress,
        etaInMilliSeconds
      )

      DownloadManager.STATUS_PENDING -> handlePendingDownload(downloadedFileId, etaInMilliSeconds)
      DownloadManager.STATUS_RUNNING -> handleRunningDownload(
        downloadedFileId,
        progress,
        etaInMilliSeconds
      )

      DownloadManager.STATUS_SUCCESSFUL -> handleSuccessfulDownload(
        downloadedFileId,
        progress,
        etaInMilliSeconds
      )
    }
  }

  private fun handleCancelledDownload(downloadedFileId: Long) {
    updater.onNext {
      updateDownloadStatus(downloadedFileId, Status.CANCELLED, Error.CANCELLED)
      downloadRoomDao.delete(downloadedFileId)
    }
  }

  private fun handleFailedDownload(
    downloadedFileId: Long,
    reason: Int,
    progress: Int,
    etaInMilliSeconds: Long
  ) {
    val error = mapDownloadError(reason)
    updateDownloadStatus(downloadedFileId, Status.FAILED, error, progress, etaInMilliSeconds)
  }

  private fun handlePausedDownload(downloadedFileId: Long, progress: Int, etaInMilliSeconds: Long) {
    updateDownloadStatus(downloadedFileId, Status.PAUSED, Error.NONE, progress, etaInMilliSeconds)
  }

  private fun handlePendingDownload(downloadedFileId: Long, etaInMilliSeconds: Long) {
    updateDownloadStatus(
      downloadedFileId,
      Status.QUEUED,
      Error.NONE,
      etaInMilliSeconds = etaInMilliSeconds
    )
  }

  private fun handleRunningDownload(
    downloadedFileId: Long,
    progress: Int,
    etaInMilliSeconds: Long
  ) {
    updateDownloadStatus(
      downloadedFileId,
      Status.DOWNLOADING,
      Error.NONE,
      progress,
      etaInMilliSeconds
    )
  }

  private fun handleSuccessfulDownload(
    downloadedFileId: Long,
    progress: Int,
    etaInMilliSeconds: Long
  ) {
    updateDownloadStatus(
      downloadedFileId,
      Status.COMPLETED,
      Error.NONE,
      progress,
      etaInMilliSeconds
    )
    downloadManager.remove(downloadedFileId)
  }

  private fun calculateProgress(bytesDownloaded: Int, totalBytes: Int): Int =
    if (totalBytes > 0) ((bytesDownloaded / totalBytes.toDouble()) * 100).toInt() else 0

  private fun calculateETA(downloadedFileId: Long, bytesDownloaded: Int, totalBytes: Int): Long {
    val currentTime = System.currentTimeMillis()
    val downloadInfo = downloadInfoMap.getOrPut(downloadedFileId) {
      DownloadInfo(startTime = currentTime, initialBytesDownloaded = bytesDownloaded)
    }

    val elapsedTime = currentTime - downloadInfo.startTime
    val downloadSpeed =
      if (elapsedTime > 0) (bytesDownloaded - downloadInfo.initialBytesDownloaded) / (elapsedTime / 1000.0) else 0.0
    return if (downloadSpeed > 0) ((totalBytes - bytesDownloaded) / downloadSpeed).toLong() * 1000 else 0L
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

  private fun updateDownloadStatus(
    downloadFileId: Long,
    status: Status,
    error: Error,
    progress: Int = -1,
    etaInMilliSeconds: Long = -1L
  ) {
    synchronized(lock) {
      downloadRoomDao.getEntityForDownloadId(downloadFileId)?.let { downloadEntity ->
        val downloadModel = DownloadModel(downloadEntity).apply {
          state = status
          this.error = error
          if (progress > 0) {
            this.progress = progress
          }
          if (etaInMilliSeconds != -1L) {
            this.etaInMilliSeconds = etaInMilliSeconds
          }
        }
        updater.onNext { downloadRoomDao.update(downloadModel) }
      }
    }
  }

  override fun init() {
    // empty method to so class does not get reported unused
  }
}

data class DownloadInfo(
  var startTime: Long,
  var initialBytesDownloaded: Int
)
