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
package org.kiwix.kiwixmobile.core.dao.entities

import com.tonyodev.fetch2.Download
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book

@Entity
data class FetchDownloadEntity(
  @Id var id: Long = 0,
  var downloadId: Long,
  val file: String? = null,
  val etaInMilliSeconds: Long = -1L,
  val bytesDownloaded: Long = -1L,
  val totalSizeOfDownload: Long = -1L,
  // @Convert(converter = StatusConverter::class, dbType = Int::class)
  // val status: Status = Status.NONE,
  // @Convert(converter = ErrorConverter::class, dbType = Int::class)
  // val error: Error = Error.NONE,
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
  constructor(downloadId: Long, book: Book, file: String?) : this(
    file = file,
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

  fun toBook() = Book().apply {
    id = bookId
    title = this@FetchDownloadEntity.title
    description = this@FetchDownloadEntity.description
    language = this@FetchDownloadEntity.language
    creator = this@FetchDownloadEntity.creator
    publisher = this@FetchDownloadEntity.publisher
    date = this@FetchDownloadEntity.date
    url = this@FetchDownloadEntity.url
    articleCount = this@FetchDownloadEntity.articleCount
    mediaCount = this@FetchDownloadEntity.mediaCount
    size = this@FetchDownloadEntity.size
    bookName = name
    favicon = favIcon
    tags = this@FetchDownloadEntity.tags
  }

  fun updateWith(download: Download) = copy(
    file = download.file,
    etaInMilliSeconds = download.etaInMilliSeconds,
    bytesDownloaded = download.downloaded,
    totalSizeOfDownload = download.total,
    // status = download.status,
    // error = download.error,
    progress = download.progress
  )
}
