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

package org.kiwix.kiwixmobile.core.downloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import javax.inject.Inject

class DownloaderImpl @Inject constructor(
  private val downloadRequester: DownloadRequester,
  private val downloadRoomDao: DownloadRoomDao,
  private val kiwixService: KiwixService
) : Downloader {
  @Suppress("InjectDispatcher")
  override fun downloadApk(url: String) {
    CoroutineScope(Dispatchers.IO).launch {
      runCatching {
        downloadRequester.enqueue(DownloadRequest(url))
        downloadRequester.startApkDownloadService()
      }.onFailure {
        it.printStackTrace()
      }
    }
  }

  @Suppress("InjectDispatcher")
  override fun download(book: LibkiwixBook) {
    CoroutineScope(Dispatchers.IO).launch {
      runCatching {
        urlProvider(book)?.let {
          downloadRoomDao.addIfDoesNotExist(it, book, downloadRequester)
          downloadRequester.startDownloadMonitorService()
        }
      }.onFailure {
        it.printStackTrace()
      }
    }
  }

  @Suppress("UnsafeCallOnNullableType")
  private suspend fun urlProvider(book: LibkiwixBook): String? =
    if (book.url?.endsWith("meta4") == true) {
      kiwixService.getMetaLinks(book.url!!)?.relevantUrl?.value
    } else {
      book.url
    }

  override fun cancelDownload(downloadId: Long) {
    downloadRequester.cancel(downloadId)
  }

  override fun retryDownload(downloadId: Long) {
    downloadRequester.retryDownload(downloadId)
  }

  override fun pauseResumeDownload(downloadId: Long, isPause: Boolean) {
    downloadRequester.pauseResumeDownload(downloadId, isPause)
  }
}
