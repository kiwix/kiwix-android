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

import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.database.DownloadDao
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.network.KiwixService
import javax.inject.Inject

class DownloaderImpl @Inject constructor(
  private val downloadRequester: DownloadRequester,
  private val downloadDao: DownloadDao,
  private val kiwixService: KiwixService
) : Downloader {

  override fun download(book: LibraryNetworkEntity.Book) {
    kiwixService.getMetaLinks(book.url)
        .subscribeOn(Schedulers.io())
        .take(1)
        .subscribe(
            {
              val downloadId = downloadRequester.enqueue(
                  DownloadRequest(it, book)
              )
              downloadDao.insert(
                  DownloadModel(downloadId, book)
              )
            },
            Throwable::printStackTrace
        )
  }

  override fun queryStatus(downloadModels: List<DownloadModel>) =
    downloadRequester.query(downloadModels)
        .sortedBy { it.downloadId }

  override fun cancelDownload(downloadItem: DownloadItem) {
    downloadRequester.cancel(downloadItem)
    downloadDao.delete(downloadItem.downloadId)
  }
}
