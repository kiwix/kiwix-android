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
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book

@Entity
data class FetchDownloadEntity(
  @Id var id: Long = 0,
  var downloadId: Long,
  val file: String? = null,
  val etaInMilliSeconds: Long = -1L,
  val bytesDownloaded: Long = -1L,
  val totalSizeOfDownload: Long = -1L,
  @Convert(converter = StatusConverter::class, dbType = Int::class)
  val status: Status = Status.NONE,
  @Convert(converter = ErrorConverter::class, dbType = Int::class)
  val error: Error = Error.NONE,
  val progress: Int = -1,
  val bookId: String,
  val title: String,
  val description: String,
  val language: String,
  val creator: String,
  val publisher: String,
  val date: String,
  val url: String?,
  val articleCount: String?,
  val mediaCount: String?,
  val size: String,
  val name: String?,
  val favIcon: String
) {
  constructor(downloadId: Long, book: Book) : this(
    downloadId = downloadId,
    bookId = book.getId(),
    title = book.getTitle(),
    description = book.getDescription(),
    language = book.getLanguage(),
    creator = book.getCreator(),
    publisher = book.getPublisher(),
    date = book.getDate(),
    url = book.getUrl(),
    articleCount = book.getArticleCount(),
    mediaCount = book.getMediaCount(),
    size = book.getSize(),
    name = book.name,
    favIcon = book.getFavicon()
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

class StatusConverter : EnumConverter<Status>() {
  override fun convertToEntityProperty(databaseValue: Int) = Status.valueOf(databaseValue)
}

class ErrorConverter : EnumConverter<Error>() {
  override fun convertToEntityProperty(databaseValue: Int) = Error.valueOf(databaseValue)
}

abstract class EnumConverter<E : Enum<E>> : PropertyConverter<E, Int> {
  override fun convertToDatabaseValue(entityProperty: E): Int = entityProperty.ordinal
}
