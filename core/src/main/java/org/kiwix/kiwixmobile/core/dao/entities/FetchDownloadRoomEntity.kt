/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity

@Entity
data class FetchDownloadRoomEntity(
  @PrimaryKey var id: Long = 0,
  var downloadId: Long,
  val file: String? = null,
  val etaInMilliSeconds: Long = -1L,
  val bytesDownloaded: Long = -1L,
  val totalSizeOfDownload: Long = -1L,
  val status: Status = Status.NONE,
  val error: Error = Error.NONE,
  val progress: Int = -1,
  val bookId: String,
  val title: String,
  val description: String?,
  val language: String,
  val creator: String,
  val publisher: String,
  val date: String,
  val url: String?,
  val articleCount: String?,
  val mediaCount: String?,
  val size: String,
  val name: String?,
  val favIcon: String,
  val tags: String? = null
) {
  constructor(downloadId: Long, book: LibraryNetworkEntity.Book) : this(
    downloadId = downloadId,
    bookId = book.id,
    title = book.title,
    description = book.description,
    language = book.language,
    creator = book.creator,
    publisher = book.publisher,
    date = book.date,
    url = book.url,
    articleCount = book.articleCount,
    mediaCount = book.mediaCount,
    size = book.size,
    name = book.bookName,
    favIcon = book.favicon,
    tags = book.tags
  )

  fun toBook() = LibraryNetworkEntity.Book().apply {
    id = bookId
    title = this@FetchDownloadRoomEntity.title
    description = this@FetchDownloadRoomEntity.description
    language = this@FetchDownloadRoomEntity.language
    creator = this@FetchDownloadRoomEntity.creator
    publisher = this@FetchDownloadRoomEntity.publisher
    date = this@FetchDownloadRoomEntity.date
    url = this@FetchDownloadRoomEntity.url
    articleCount = this@FetchDownloadRoomEntity.articleCount
    mediaCount = this@FetchDownloadRoomEntity.mediaCount
    size = this@FetchDownloadRoomEntity.size
    bookName = name
    favicon = favIcon
    tags = this@FetchDownloadRoomEntity.tags
  }

  fun updateWith(download: Download) = copy(
    file = download.file,
    etaInMilliSeconds = download.etaInMilliSeconds,
    bytesDownloaded = download.downloaded,
    totalSizeOfDownload = download.total,
    status = download.status,
    error = download.error,
    progress = download.progress
  )
}
