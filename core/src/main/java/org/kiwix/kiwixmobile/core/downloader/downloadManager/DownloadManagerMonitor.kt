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
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_CANCEL
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_PAUSE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_QUERY_DOWNLOAD_STATUS
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DownloadNotificationManager.Companion.ACTION_RESUME
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  val downloadRoomDao: DownloadRoomDao,
  private val context: Context,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) : DownloadMonitor, DownloadManagerBroadcastReceiver.Callback {
  private val lock = Any()

  init {
    context.registerReceiver(connectivityBroadcastReceiver)
    startServiceIfActiveDownloads()
    trackNetworkState()
  }

  @SuppressLint("CheckResult")
  private fun trackNetworkState() {
    connectivityBroadcastReceiver.networkStates
      .distinctUntilChanged()
      .subscribe(
        {
          // Start the service when the network changes so that we can
          // track the progress accurately.
          startServiceIfActiveDownloads()
        },
        Throwable::printStackTrace
      )
  }

  private fun startServiceIfActiveDownloads() {
    CoroutineScope(Dispatchers.IO).launch {
      if (downloadRoomDao.downloads().blockingFirst().isNotEmpty()) {
        startService()
      }
    }
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

  fun startMonitoringDownloads() {
    startService()
  }

  private fun startService() {
    context.startService(Intent(context, DownloadMonitorService::class.java))
  }

  fun pauseDownload(downloadId: Long) {
    context.startService(getDownloadMonitorIntent(ACTION_PAUSE, downloadId.toInt()))
  }

  fun resumeDownload(downloadId: Long) {
    context.startService(getDownloadMonitorIntent(ACTION_RESUME, downloadId.toInt()))
  }

  fun cancelDownload(downloadId: Long) {
    context.startService(getDownloadMonitorIntent(ACTION_CANCEL, downloadId.toInt()))
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
