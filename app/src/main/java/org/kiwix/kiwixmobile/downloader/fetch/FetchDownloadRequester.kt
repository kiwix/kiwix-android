/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.downloader.fetch

import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.NetworkType.ALL
import com.tonyodev.fetch2.NetworkType.WIFI_ONLY
import com.tonyodev.fetch2.Request
import org.kiwix.kiwixmobile.downloader.DownloadRequester
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import javax.inject.Inject

class FetchDownloadRequester @Inject constructor(
  private val fetch: Fetch,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : DownloadRequester {

  override fun enqueue(downloadRequest: DownloadRequest): Long {
    val request = downloadRequest.toFetchRequest(sharedPreferenceUtil)
    fetch.enqueue(request)
    return request.id.toLong()
  }

  override fun cancel(downloadItem: DownloadItem) {
    fetch.delete(downloadItem.downloadId.toInt())
  }
}

private fun DownloadRequest.toFetchRequest(sharedPreferenceUtil: SharedPreferenceUtil) =
  Request("$uri", getDestination(sharedPreferenceUtil)).apply {
    networkType = if (sharedPreferenceUtil.prefWifiOnly) WIFI_ONLY else ALL
    autoRetryMaxAttempts = 10
  }
