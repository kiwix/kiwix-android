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
import android.net.Uri
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class DownloadManagerRequester @Inject constructor(
  private val downloadManager: DownloadManager,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val fetchDownloadDao: FetchDownloadDao
) : DownloadRequester {
  override fun enqueue(downloadRequest: DownloadRequest): Long =
    downloadManager.enqueue(downloadRequest.toDownloadManagerRequest(sharedPreferenceUtil))

  override fun cancel(downloadId: Long) {
    downloadManager.remove(downloadId).also {
      fetchDownloadDao.delete(downloadId)
    }
  }

  override fun retryDownload(downloadId: Long) {
  }

  override fun pauseResumeDownload(downloadId: Long, isPause: Boolean) {
  }
}

private fun DownloadRequest.toDownloadManagerRequest(sharedPreferenceUtil: SharedPreferenceUtil) =
  DownloadManager.Request(uri).apply {
    setDestinationUri(Uri.fromFile(getDestinationFile(sharedPreferenceUtil)))
    setAllowedNetworkTypes(
      if (sharedPreferenceUtil.prefWifiOnly)
        Request.NETWORK_WIFI
      else
        Request.NETWORK_MOBILE
    )
    setTitle(notificationTitle)
    setAllowedOverMetered(true)
    setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
  }
