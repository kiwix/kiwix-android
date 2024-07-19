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

import android.app.DownloadManager
import android.content.Intent
import android.util.Log
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  private val downloadManager: DownloadManager,
  private val downloadRoomDao: DownloadRoomDao
) :
  DownloadMonitor, DownloadManagerBroadcastReceiver.Callback {
  private val updater = PublishSubject.create<() -> Unit>()
  private val lock = Any()

  override fun downloadInformation(intent: Intent) {
    synchronized(lock) {
      intent.extras?.let {
        val downloadedFileId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadedFileId != -1L) {
          val query = DownloadManager.Query().setFilterById(downloadedFileId)
          val cursor = downloadManager.query(query)
          if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(columnIndex)
            val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            val reason = cursor.getInt(columnReason)

            when (status) {
              DownloadManager.STATUS_FAILED -> {
                var error = Error.NONE
                when (reason) {
                  DownloadManager.ERROR_CANNOT_RESUME -> error = Error.ERROR_CANNOT_RESUME
                  DownloadManager.ERROR_DEVICE_NOT_FOUND -> error = Error.ERROR_DEVICE_NOT_FOUND
                  DownloadManager.ERROR_FILE_ALREADY_EXISTS -> error =
                    Error.ERROR_FILE_ALREADY_EXISTS

                  DownloadManager.ERROR_FILE_ERROR -> error = Error.ERROR_FILE_ERROR
                  DownloadManager.ERROR_HTTP_DATA_ERROR -> error = Error.ERROR_HTTP_DATA_ERROR
                  DownloadManager.ERROR_INSUFFICIENT_SPACE -> error = Error.ERROR_INSUFFICIENT_SPACE

                  DownloadManager.ERROR_TOO_MANY_REDIRECTS -> error = Error.ERROR_TOO_MANY_REDIRECTS

                  DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> error =
                    Error.ERROR_UNHANDLED_HTTP_CODE

                  DownloadManager.ERROR_UNKNOWN -> error = Error.UNKNOWN
                }
                updater.onNext {
                  updateDownloadStatus(
                    downloadedFileId,
                    Status.FAILED,
                    error
                  )
                }
              }

              DownloadManager.STATUS_PAUSED -> {
                updater.onNext {
                  updateDownloadStatus(
                    downloadedFileId,
                    Status.PAUSED,
                    Error.NONE
                  )
                }
              }

              DownloadManager.STATUS_PENDING -> {
                updater.onNext {
                  updateDownloadStatus(
                    downloadedFileId,
                    Status.QUEUED,
                    Error.NONE
                  )
                }
              }

              DownloadManager.STATUS_RUNNING -> {
                updater.onNext {
                  updateDownloadStatus(
                    downloadedFileId,
                    Status.DOWNLOADING,
                    Error.NONE
                  )
                }
              }

              DownloadManager.STATUS_SUCCESSFUL -> {
                updater.onNext {
                  updateDownloadStatus(
                    downloadedFileId,
                    Status.COMPLETED,
                    Error.NONE
                  )
                }
                downloadManager.remove(downloadedFileId)
              }
            }
          } else {
            // the download is cancelled
            updater.onNext {
              updateDownloadStatus(downloadedFileId, Status.CANCELLED, Error.CANCELLED)
              downloadRoomDao.delete(downloadedFileId)
            }
          }
          cursor.close()
          Log.e("HELLO", "downloadInformation: $downloadedFileId")
        }
      }
    }
  }

  private fun updateDownloadStatus(downloadFileId: Long, status: Status, error: Error) {
    downloadRoomDao.getEntityForDownloadId(downloadFileId)?.let { downloadEntity ->
      val downloadModel = DownloadModel(downloadEntity).apply {
        state = status
        this.error = error
      }
      updater.onNext { downloadRoomDao.update(downloadModel) }
    }
  }

  init {
    updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      {
        synchronized(lock) { it.invoke() }
      },
      Throwable::printStackTrace
    )
  }

  override fun init() {
    // empty method to so class does not get reported unused}
  }
}
