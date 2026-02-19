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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook

@Entity
data class DownloadRoomEntity(
  @PrimaryKey(autoGenerate = true)
  var id: Long = 0,
  var downloadId: Long,
  val file: String? = null,
  val etaInMilliSeconds: Long = -1L,
  val bytesDownloaded: Long = -1L,
  val totalSizeOfDownload: Long = -1L,
  @TypeConverters(StatusConverter::class)
  val status: Status = Status.NONE,
  @TypeConverters(ErrorConverter::class)
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
  @TypeConverters(PauseReasonConverter::class)
  val pauseReason: PauseReason = PauseReason.NONE,
  val tags: String? = null
) {
  constructor(downloadId: Long, book: LibkiwixBook) : this(
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

  fun toBook() =
    LibkiwixBook().apply {
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

  fun updateWith(download: Download) =
    copy(
      file = download.file,
      etaInMilliSeconds = download.etaInMilliSeconds,
      bytesDownloaded = download.downloaded,
      totalSizeOfDownload = download.total,
      status = download.status,
      error = download.error,
      progress = download.progress
    )
}

class StatusConverter {
  @TypeConverter
  fun convertToEntityProperty(databaseValue: Int): Status = Status.valueOf(databaseValue)

  @TypeConverter
  fun convertToDatabaseValue(status: Status): Int = status.ordinal
}

class ErrorConverter {
  @TypeConverter
  fun convertToEntityProperty(databaseValue: Int) = com.tonyodev.fetch2.Error.valueOf(databaseValue)

  @TypeConverter
  fun convertToDatabaseValue(error: Error): Int = error.ordinal
}

class PauseReasonConverter {
  @TypeConverter
  fun toEnum(value: Int): PauseReason = PauseReason.entries[value]

  @TypeConverter
  fun toInt(reason: PauseReason): Int = reason.ordinal
}

enum class PauseReason {
  NONE,
  USER,
  SERVICE
}
