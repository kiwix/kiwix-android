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
import android.app.DownloadManager.Request
import android.app.DownloadManager.Request.VISIBILITY_HIDDEN
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.Log
import javax.inject.Inject

class DownloadManagerRequester @Inject constructor(
  private val downloadManager: DownloadManager,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val downloadManagerMonitor: DownloadManagerMonitor
) : DownloadRequester {
  override fun enqueue(downloadRequest: DownloadRequest): Long =
    downloadManager.enqueue(downloadRequest.toDownloadManagerRequest(sharedPreferenceUtil))

  override fun onDownloadAdded() {
    // Start monitoring downloads after enqueuing a new download request.
    downloadManagerMonitor.startMonitoringDownloads()
  }

  override fun cancel(downloadId: Long) {
    downloadManagerMonitor.cancelDownload(downloadId)
  }

  override fun retryDownload(downloadId: Long) {
    // Retry the download by enqueuing it again with the same request
    CoroutineScope(Dispatchers.IO).launch {
      try {
        downloadManagerMonitor
          .downloadRoomDao
          .getEntityForDownloadId(downloadId)?.let { downloadRoomEntity ->
            downloadRoomEntity.url?.let {
              val downloadRequest = DownloadRequest(urlString = it)
              val newDownloadEntity =
                downloadRoomEntity.copy(downloadId = enqueue(downloadRequest), id = 0)
              // cancel the previous download and its data from database and fileSystem.
              cancel(downloadId)
              // save the new downloads into the database so that it will show
              // this new downloads on the download screen.
              downloadManagerMonitor.downloadRoomDao.saveDownload(newDownloadEntity)
            }
          }.also {
            // Start monitoring downloads after retrying.
            onDownloadAdded()
          }
      } catch (ignore: Exception) {
        Log.e(
          "DOWNLOAD_MANAGER",
          "Could not retry the download. Original exception = $ignore"
        )
      }
    }
  }

  override fun pauseResumeDownload(downloadId: Long, isPause: Boolean) {
    if (isPause) {
      downloadManagerMonitor.resumeDownload(downloadId)
    } else {
      downloadManagerMonitor.pauseDownload(downloadId)
    }
  }
}

fun DownloadRequest.toDownloadManagerRequest(
  sharedPreferenceUtil: SharedPreferenceUtil,
  bytesDownloaded: Long = ZERO.toLong()
) =
  DownloadManager.Request(uri).apply {
    setDestinationUri(Uri.fromFile(getDestinationFile(sharedPreferenceUtil)))
    setAllowedNetworkTypes(
      if (sharedPreferenceUtil.prefWifiOnly)
        Request.NETWORK_WIFI
      else
        Request.NETWORK_MOBILE
    )
    setAllowedOverMetered(true)
    setNotificationVisibility(VISIBILITY_HIDDEN) // hide the default notification.
    if (bytesDownloaded > 0) {
      addRequestHeader("Range", "bytes=$bytesDownloaded-")
    }
  }
