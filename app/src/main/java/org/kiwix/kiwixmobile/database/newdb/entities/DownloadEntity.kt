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
package org.kiwix.kiwixmobile.database.newdb.entities

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book

@Entity
data class DownloadEntity(
  @Id var id: Long = 0,
  val downloadId: Long,
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
  constructor(downloadModel: DownloadModel) : this(
    0,
    downloadModel.downloadId,
    downloadModel.book.getId(),
    downloadModel.book.getTitle(),
    downloadModel.book.getDescription(),
    downloadModel.book.getLanguage(),
    downloadModel.book.getCreator(),
    downloadModel.book.getPublisher(),
    downloadModel.book.getDate(),
    downloadModel.book.getUrl(),
    downloadModel.book.getArticleCount(),
    downloadModel.book.getMediaCount(),
    downloadModel.book.getSize(),
    downloadModel.book.name,
    downloadModel.book.getFavicon()
  )

  fun toDownloadModel() = DownloadModel(id, downloadId, toBook())

  private fun toBook() = Book().apply {
    id = bookId
    title = this@DownloadEntity.title
    description = this@DownloadEntity.description
    language = this@DownloadEntity.language
    creator = this@DownloadEntity.creator
    publisher = this@DownloadEntity.publisher
    date = this@DownloadEntity.date
    url = this@DownloadEntity.url
    articleCount = this@DownloadEntity.articleCount
    mediaCount = this@DownloadEntity.mediaCount
    size = this@DownloadEntity.size
    bookName = name
    favicon = favIcon
  }
}
