/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.downloader

import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.net.Uri
import android.os.Build
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.extensions.forEachRow
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.StorageUtils
import java.io.File
import javax.inject.Inject

class DownloadManagerRequester @Inject constructor(
  private val downloadManager: DownloadManager,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : DownloadRequester {

  override fun enqueue(downloadRequest: DownloadRequest) =
    downloadManager.enqueue(downloadRequest.toDownloadManagerRequest(sharedPreferenceUtil))

  override fun query(downloadModels: List<DownloadModel>): List<DownloadStatus> {
    val downloadStatuses = mutableListOf<DownloadStatus>()
    if (downloadModels.isNotEmpty()) {
      downloadModels.forEach { model ->
        downloadManager.query(model.toQuery())
          .forEachRow {
            downloadStatuses.add(DownloadStatus(it, model))
          }
      }
    }
    return downloadStatuses
  }

  override fun cancel(downloadItem: DownloadItem) {
    downloadManager.remove(downloadItem.downloadId)
  }

  private fun DownloadRequest.toDownloadManagerRequest(sharedPreferenceUtil: SharedPreferenceUtil) =
    Request(uri).apply {
      setAllowedNetworkTypes(
        if (sharedPreferenceUtil.prefWifiOnly) {
          Request.NETWORK_WIFI
        } else {
          Request.NETWORK_MOBILE or Request.NETWORK_WIFI
        }
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        setAllowedOverMetered(true)
      }
      setAllowedOverRoaming(true)
      setTitle(title)
      setDescription(description)
      setDestinationUri(toDestinationUri(sharedPreferenceUtil))
      setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setVisibleInDownloadsUi(true)
    }

  private fun DownloadRequest.toDestinationUri(sharedPreferenceUtil: SharedPreferenceUtil) =
    Uri.fromFile(
      File(
        "${sharedPreferenceUtil.prefStorage}/Kiwix/${
        StorageUtils.getFileNameFromUrl(urlString)
        }"
      )
    )

  private fun DownloadModel.toQuery() =
    DownloadManager.Query().setFilterById(downloadId)
}
