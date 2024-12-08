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
import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_CANCEL
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_PAUSE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_QUERY_DOWNLOAD_STATUS
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_RESUME
import org.kiwix.kiwixmobile.core.extensions.isServiceRunning
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  val downloadRoomDao: DownloadRoomDao,
  private val context: Context
) : DownloadMonitor, DownloadManagerBroadcastReceiver.Callback {
  private val lock = Any()
  private var monitoringDisposable: Disposable? = null

  init {
    startMonitoringDownloads()
  }

  @Suppress("MagicNumber")
  fun startMonitoringDownloads() {
    if (monitoringDisposable?.isDisposed == false) return
    monitoringDisposable = Observable.interval(ZERO.toLong(), 5, TimeUnit.SECONDS)
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        {
          try {
            synchronized(lock) {
              // Observe downloads when the application is in the foreground.
              // This is especially useful when downloads are resumed but the
              // Download Manager takes some time to update the download status.
              // In such cases, the foreground service may stop prematurely due to
              // a lack of active downloads during this update delay.
              if (downloadRoomDao.downloads().blockingFirst().isNotEmpty()) {
                // Check if there are active downloads and the service is not running.
                // If so, start the DownloadMonitorService to properly track download progress.
                if (shouldStartService()) {
                  startService()
                } else {
                  // Do nothing; it is for fixing the error when "if" is used as an expression.
                }
              } else {
                monitoringDisposable?.dispose()
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

  /**
   * Determines if the DownloadMonitorService should be started.
   * Checks if there are active downloads and if the service is not already running.
   */
  private fun shouldStartService(): Boolean =
    getActiveDownloads().isNotEmpty() &&
      !context.isServiceRunning(DownloadMonitorService::class.java)

  private fun getActiveDownloads(): List<DownloadRoomEntity> =
    downloadRoomDao.downloadRoomEntity().blockingFirst().filter {
      it.status != Status.PAUSED && it.status != Status.CANCELLED
    }

  override fun downloadCompleteOrCancelled(intent: Intent) {
    synchronized(lock) {
      intent.extras?.let {
        val downloadId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId != -1L) {
          context.startService(
            getDownloadMonitorIntent(
              ACTION_QUERY_DOWNLOAD_STATUS,
              downloadId.toInt()
            )
          )
        }
      }
    }
  }

  private fun startService() {
    context.startService(Intent(context, DownloadMonitorService::class.java))
  }

  fun pauseDownload(downloadId: Long) {
    context.startService(getDownloadMonitorIntent(ACTION_PAUSE, downloadId.toInt()))
    startMonitoringDownloads()
  }

  fun resumeDownload(downloadId: Long) {
    context.startService(getDownloadMonitorIntent(ACTION_RESUME, downloadId.toInt()))
    startMonitoringDownloads()
  }

  fun cancelDownload(downloadId: Long) {
    context.startService(getDownloadMonitorIntent(ACTION_CANCEL, downloadId.toInt()))
    startMonitoringDownloads()
  }

  private fun getDownloadMonitorIntent(action: String, downloadId: Int): Intent =
    Intent(context, DownloadMonitorService::class.java).apply {
      putExtra(DownloadNotificationManager.NOTIFICATION_ACTION, action)
      putExtra(DownloadNotificationManager.EXTRA_DOWNLOAD_ID, downloadId)
    }

  override fun init() {
    // empty method to so class does not get reported unused
  }
}
