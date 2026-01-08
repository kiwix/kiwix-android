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
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.NetworkType.ALL
import com.tonyodev.fetch2.NetworkType.WIFI_ONLY
import com.tonyodev.fetch2.Request
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.utils.AUTO_RETRY_MAX_ATTEMPTS
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class DownloadManagerRequester @Inject constructor(
  private val fetch: Fetch,
  private val kiwixDataStore: KiwixDataStore,
  private val context: Context
) : DownloadRequester {
  override suspend fun enqueue(downloadRequest: DownloadRequest): Long {
    val request = downloadRequest.toFetchRequest(kiwixDataStore)
    fetch.enqueue(request)
    return request.id.toLong()
  }

  override fun cancel(downloadId: Long) {
    fetch.delete(downloadId.toInt())
    startDownloadMonitorService()
  }

  override fun retryDownload(downloadId: Long) {
    fetch.retry(downloadId.toInt())
    startDownloadMonitorService()
  }

  override fun pauseResumeDownload(downloadId: Long, isPause: Boolean) {
    if (isPause) {
      fetch.resume(downloadId.toInt())
    } else {
      fetch.pause(downloadId.toInt())
    }
    startDownloadMonitorService()
  }

  override fun startDownloadMonitorService() {
    (context as CoreApp).getMainActivity().startDownloadMonitorServiceIfOngoingDownloads()
  }

  override fun startApkDownloadService() {
    (context as CoreApp).getMainActivity().startDownloadApkService()
  }
}

private suspend fun DownloadRequest.toFetchRequest(kiwixDataStore: KiwixDataStore) =
  Request("$uri", getDestination(kiwixDataStore)).apply {
    networkType = if (kiwixDataStore.wifiOnly.first()) WIFI_ONLY else ALL
    autoRetryMaxAttempts = AUTO_RETRY_MAX_ATTEMPTS
  }
