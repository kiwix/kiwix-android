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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import javax.inject.Inject

class DownloadManagerMonitor @Inject constructor(
  val downloadRoomDao: DownloadRoomDao,
  private val context: Context
) : DownloadMonitor, DownloadManagerBroadcastReceiver.Callback, DownloadMonitorServiceCallback {
  private val lock = Any()
  private var downloadMonitorService: DownloadMonitorService? = null
  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
      downloadMonitorService =
        (binder as? DownloadMonitorService.DownloadMonitorBinder)?.downloadMonitorService?.get()
      downloadMonitorService?.registerCallback(this@DownloadManagerMonitor)
      CoroutineScope(Dispatchers.IO).launch {
        if (downloadRoomDao.downloads().blockingFirst().isNotEmpty()) {
          startService()
        }
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      downloadMonitorService = null
    }
  }

  init {
    bindService()
  }

  private fun bindService() {
    val serviceIntent = Intent(context, DownloadMonitorService::class.java)
    context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  override fun downloadCompleteOrCancelled(intent: Intent) {
    synchronized(lock) {
      intent.extras?.let {
        val downloadId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId != -1L) {
          downloadMonitorService?.queryDownloadStatus(downloadId)
        }
      }
    }
  }

  fun startMonitoringDownloads() {
    bindService()
    startService()
    downloadMonitorService?.startMonitoringDownloads()
  }

  private fun startService() {
    ContextCompat.startForegroundService(
      context,
      Intent(context, DownloadMonitorService::class.java)
    )
  }

  fun pauseDownload(downloadId: Long) {
    downloadMonitorService?.pauseDownload(downloadId)
  }

  fun resumeDownload(downloadId: Long) {
    downloadMonitorService?.resumeDownload(downloadId)
  }

  fun cancelDownload(downloadId: Long) {
    downloadMonitorService?.cancelDownload(downloadId)
  }

  override fun init() {
    // empty method to so class does not get reported unused
  }

  override fun onServiceDestroyed() {
    downloadMonitorService?.registerCallback(null)
    context.unbindService(serviceConnection)
    downloadMonitorService = null
  }
}
