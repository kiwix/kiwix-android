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

package org.kiwix.kiwixmobile.core.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.objectbox.annotation.Convert
import io.objectbox.converter.PropertyConverter
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Error
import org.kiwix.kiwixmobile.core.downloader.downloadManager.Status
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book

@Entity
data class DownloadRoomEntity(
  @PrimaryKey(autoGenerate = true)
  var id: Long = 0,
  var downloadId: Long,
  val file: String? = null,
  val etaInMilliSeconds: Long = -1L,
  var bytesDownloaded: Long = -1L,
  val totalSizeOfDownload: Long = -1L,
  @Convert(converter = StatusConverter::class, dbType = Int::class)
  var status: Status = Status.NONE,
  @Convert(converter = ErrorConverter::class, dbType = Int::class)
  var error: Error = Error.NONE,
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
  val tags: String? = null,
  var pausedByUser: Boolean = false
) {
  constructor(downloadUrl: String, downloadId: Long, book: Book, file: String?) : this(
    file = file,
    downloadId = downloadId,
    bookId = book.id,
    title = book.title,
    description = book.description,
    language = book.language,
    creator = book.creator,
    publisher = book.publisher,
    date = book.date,
    url = downloadUrl,
    articleCount = book.articleCount,
    mediaCount = book.mediaCount,
    size = book.size,
    name = book.bookName,
    favIcon = book.favicon,
    tags = book.tags
  )

  fun toBook() = Book().apply {
    id = bookId
    title = this@DownloadRoomEntity.title
    description = this@DownloadRoomEntity.description
    language = this@DownloadRoomEntity.language
    creator = this@DownloadRoomEntity.creator
    publisher = this@DownloadRoomEntity.publisher
    date = this@DownloadRoomEntity.date
    url = this@DownloadRoomEntity.url
    articleCount = this@DownloadRoomEntity.articleCount
    mediaCount = this@DownloadRoomEntity.mediaCount
    size = this@DownloadRoomEntity.size
    bookName = name
    favicon = favIcon
    tags = this@DownloadRoomEntity.tags
  }

  fun updateWith(download: DownloadModel) = copy(
    file = download.file,
    etaInMilliSeconds = download.etaInMilliSeconds,
    bytesDownloaded = download.bytesDownloaded,
    totalSizeOfDownload = download.totalSizeOfDownload,
    status = download.state,
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
