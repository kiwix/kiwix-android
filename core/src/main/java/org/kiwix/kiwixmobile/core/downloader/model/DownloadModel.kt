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
package org.kiwix.kiwixmobile.core.downloader.model

import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Error
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.StorageUtils

data class DownloadModel(
  val databaseId: Long,
  var downloadId: Long,
  val file: String?,
  var etaInMilliSeconds: Long,
  var bytesDownloaded: Long,
  var totalSizeOfDownload: Long,
  var state: Status,
  var error: Error,
  var progress: Int,
  val book: Book
) {
  val bytesRemaining: Long by lazy { totalSizeOfDownload - bytesDownloaded }
  val fileNameFromUrl: String by lazy { StorageUtils.getFileNameFromUrl(book.url) }

  constructor(downloadEntity: DownloadRoomEntity) : this(
    downloadEntity.id,
    downloadEntity.downloadId,
    downloadEntity.file,
    downloadEntity.etaInMilliSeconds,
    downloadEntity.bytesDownloaded,
    downloadEntity.totalSizeOfDownload,
    downloadEntity.status,
    downloadEntity.error,
    downloadEntity.progress,
    downloadEntity.toBook()
  )
}
