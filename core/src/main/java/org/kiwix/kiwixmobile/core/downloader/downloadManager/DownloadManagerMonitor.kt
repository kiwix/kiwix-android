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
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  private val downloadManager: DownloadManager,
  private val fetchDownloadDao: FetchDownloadDao
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
          val cursor = downloadManager.query(query).also { cursor ->
            Log.e("HELLO", "downloadInformation: ${cursor.moveToFirst()}")
          }
          if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(columnIndex)
            val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            val reason = cursor.getInt(columnReason)

            when (status) {
              DownloadManager.STATUS_FAILED -> {
                var failedReason = ""
                when (reason) {
                  DownloadManager.ERROR_CANNOT_RESUME -> failedReason = "ERROR_CANNOT_RESUME"
                  DownloadManager.ERROR_DEVICE_NOT_FOUND -> failedReason = "ERROR_DEVICE_NOT_FOUND"
                  DownloadManager.ERROR_FILE_ALREADY_EXISTS -> failedReason =
                    "ERROR_FILE_ALREADY_EXISTS"

                  DownloadManager.ERROR_FILE_ERROR -> failedReason = "ERROR_FILE_ERROR"
                  DownloadManager.ERROR_HTTP_DATA_ERROR -> failedReason = "ERROR_HTTP_DATA_ERROR"
                  DownloadManager.ERROR_INSUFFICIENT_SPACE -> failedReason =
                    "ERROR_INSUFFICIENT_SPACE"

                  DownloadManager.ERROR_TOO_MANY_REDIRECTS -> failedReason =
                    "ERROR_TOO_MANY_REDIRECTS"

                  DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> failedReason =
                    "ERROR_UNHANDLED_HTTP_CODE"

                  DownloadManager.ERROR_UNKNOWN -> failedReason = "ERROR_UNKNOWN"
                }
                Log.e("STATUS", "FAILED: $failedReason")
              }

              DownloadManager.STATUS_PAUSED -> {
                var pausedReason = ""

                when (reason) {
                  DownloadManager.PAUSED_QUEUED_FOR_WIFI -> pausedReason = "PAUSED_QUEUED_FOR_WIFI"
                  DownloadManager.PAUSED_UNKNOWN -> pausedReason = "PAUSED_UNKNOWN"
                  DownloadManager.PAUSED_WAITING_FOR_NETWORK -> pausedReason =
                    "PAUSED_WAITING_FOR_NETWORK"

                  DownloadManager.PAUSED_WAITING_TO_RETRY -> pausedReason =
                    "PAUSED_WAITING_TO_RETRY"
                }
                Log.e("STATUS", "STATUS_PAUSED: $pausedReason")
              }

              DownloadManager.STATUS_PENDING -> Log.e(
                "STATUS",
                "STATUS_PENDING"
              )

              DownloadManager.STATUS_RUNNING -> Log.e("STATUS", "STATUS_RUNNING:")

              DownloadManager.STATUS_SUCCESSFUL -> {
                Log.e("STATUS", "STATUS_SUCCESSFUL:")
                // updater.onNext { fetchDownloadDao.update(download) }
                downloadManager.remove(downloadedFileId)
              }
            }
          } else {
            // the download is cancelled
            updater.onNext { fetchDownloadDao.delete(downloadedFileId) }
          }
          Log.e("HELLO", "downloadInformation: $downloadedFileId")
        }
      }
    }
  }

  init {
    updater.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe(
      { it.invoke() },
      Throwable::printStackTrace
    )
  }

  override fun init() {
    // empty method to so class does not get reported unused}
  }
}
