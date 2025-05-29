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

import android.content.Context
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import javax.inject.Inject

const val ZERO = 0
const val FIVE = 5
const val SIX = 6
const val HUNDERED = 100
const val DEFAULT_INT_VALUE = -1

@Suppress("InjectDispatcher")
class DownloadManagerMonitor @Inject constructor(
  val fetch: Fetch,
  val context: Context,
  val downloadRoomDao: DownloadRoomDao,
  private val fetchDownloadNotificationManager: FetchDownloadNotificationManager
) : DownloadMonitor {
  private val taskFlow = MutableSharedFlow<suspend () -> Unit>(extraBufferCapacity = Int.MAX_VALUE)
  private var updaterJob: Job? = null
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Suppress("TooGenericExceptionCaught")
  private fun setupUpdater() {
    updaterJob = coroutineScope.launch {
      taskFlow.collect { task ->
        try {
          task.invoke()
        } catch (e: Exception) {
          e.printStackTrace()
        }
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
      update(download)
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
      update(download)
    }
  }

  private fun update(download: Download) {
    taskFlow.tryEmit {
      downloadRoomDao.update(download)
      if (download.isPaused()) {
        fetchDownloadNotificationManager.showDownloadPauseNotification(fetch, download)
      }
    }
  }

  private fun delete(download: Download) {
    taskFlow.tryEmit { downloadRoomDao.delete(download) }
  }

  override fun startMonitoringDownload() {
    fetch.addListener(fetchListener, true)
    setupUpdater()
  }

  override fun stopListeningDownloads() {
    fetch.removeListener(fetchListener)
    updaterJob?.cancel()
  }
}
