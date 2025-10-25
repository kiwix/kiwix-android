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
package org.kiwix.sharedFunctions

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.Status.NONE
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState.Pending
import org.kiwix.kiwixmobile.core.downloader.model.Seconds
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity.FileElement
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity.Pieces
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity.Url
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.libkiwix.Book
import java.io.File

fun bookOnDisk(
  databaseId: Long = 0L,
  book: LibkiwixBook = libkiwixBook(),
  zimReaderSource: ZimReaderSource = ZimReaderSource(File(""))
) = BookOnDisk(databaseId, book, File(""), zimReaderSource)

fun downloadModel(
  databaseId: Long = 1L,
  downloadId: Long = 1L,
  file: String = "",
  etaInMilliSeconds: Long = 0L,
  bytesDownloaded: Long = 1L,
  totalSizeOfDownload: Long = 1L,
  status: Status = NONE,
  error: Error = Error.NONE,
  progress: Int = 1,
  book: LibkiwixBook = libkiwixBook()
) = DownloadModel(
  databaseId, downloadId, file, etaInMilliSeconds, bytesDownloaded, totalSizeOfDownload,
  status, error, progress, book
)

fun downloadItem(
  downloadId: Long = 1L,
  favIcon: String = "favIcon",
  title: String = "title",
  description: String = "description",
  bytesDownloaded: Long = 1L,
  totalSizeBytes: Long = 1L,
  progress: Int = 1,
  eta: Seconds = Seconds(0),
  state: DownloadState = Pending
) = DownloadItem(
  downloadId,
  favIcon,
  title,
  description,
  bytesDownloaded,
  totalSizeBytes,
  progress,
  eta,
  state
)

fun language(
  id: Long = 0,
  isActive: Boolean = false,
  occurencesOfLanguage: Int = 0,
  language: String = "",
  languageLocalized: String = "",
  languageCode: String = "",
  languageCodeISO2: String = ""
) = Language(
  id,
  isActive,
  occurencesOfLanguage,
  language,
  languageLocalized,
  languageCode,
  languageCodeISO2
)

fun category(
  id: Long = 0,
  isActive: Boolean = false,
  category: String = ""
) = Category(id, isActive, category)

fun metaLinkNetworkEntity() =
  MetaLinkNetworkEntity().apply {
    file = fileElement()
  }

fun fileElement(
  urls: List<Url> = listOf(url()),
  name: String = "name",
  hashes: Map<String, String> = mapOf("hash" to "hash"),
  pieces: Pieces = pieces()
) = FileElement().apply {
  this.name = name
  this.urls = urls
  this.hashes = hashes
  this.pieces = pieces
}

fun pieces(
  hashType: String = "hashType",
  pieceHashes: List<String> = listOf("hash")
) = Pieces().apply {
  this.hashType = hashType
  this.pieceHashes = pieceHashes
}

fun url(
  value: String = "${MOCK_BASE_URL}relevantUrl.zim.meta4",
  location: String = "location"
) = Url().apply {
  this.location = location
  this.value = value
}

fun libkiwixBook(
  id: String = "id",
  title: String = "title",
  description: String = "description",
  language: String = "eng",
  creator: String = "creator",
  publisher: String = "publisher",
  date: String = "date",
  url: String = "${MOCK_BASE_URL}url.meta4",
  articleCount: String = "articleCount",
  mediaCount: String = "mediaCount",
  size: String = "1024",
  name: String = "name",
  favIcon: String = "favIcon",
  file: File = File(""),
  nativeBook: Book? = null,
  tags: String? = ""
) = LibkiwixBook().apply {
  this.nativeBook = nativeBook
  this.id = id
  this.title = title
  this.description = description
  this.language = language
  this.creator = creator
  this.publisher = publisher
  this.date = date
  this.url = url
  this.articleCount = articleCount
  this.mediaCount = mediaCount
  this.size = size
  this.file = file
  bookName = name
  favicon = favIcon
  this.tags = tags
}
