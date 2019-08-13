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
package org.kiwix.kiwixmobile

import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Pending
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity.FileElement
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity.Pieces
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity.Url
import org.kiwix.kiwixmobile.zim_manager.Language
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File
import java.util.LinkedList

fun bookOnDisk(
  book: Book = book(),
  databaseId: Long? = 0L,
  file: File = File("")
) = BookOnDisk(databaseId, book, file)

fun downloadStatus(
  downloadId: Long = 0L,
  title: String = "",
  description: String = "",
  downloadState: DownloadState = Pending,
  bytesDownloadedSoFar: Long = 0L,
  totalSizeBytes: Long = 0L,
  lastModified: String = "",
  localUri: String? = null,
  mediaProviderUri: String? = null,
  mediaType: String? = null,
  uri: String? = null,
  book: Book = book()
) = DownloadStatus(
  downloadId, title, description, downloadState, bytesDownloadedSoFar,
  totalSizeBytes, lastModified, localUri, mediaProviderUri, mediaType, uri, book
)

fun downloadModel(
  databaseId: Long? = 1L,
  downloadId: Long = 1L,
  book: Book = book()
) = DownloadModel(databaseId, downloadId, book)

fun language(
  id: Long = 0,
  isActive: Boolean = false,
  occurencesOfLanguage: Int = 0,
  language: String = "",
  languageLocalized: String = "",
  languageCode: String = "",
  languageCodeISO2: String = ""
) = Language(
  id, isActive, occurencesOfLanguage, language, languageLocalized, languageCode,
  languageCodeISO2
)

fun languageItem(language: Language = language()) =
  LanguageItem(language)

fun metaLinkNetworkEntity() = MetaLinkNetworkEntity().apply {
  file = fileElement()
}

fun fileElement(
  urls: List<Url> = listOf(
    url()
  ),
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

fun book(
  id: String = "id",
  title: String = "title",
  description: String = "description",
  language: String = "eng",
  creator: String = "creator",
  publisher: String = "publisher",
  date: String = "date",
  url: String = "${MOCK_BASE_URL}url",
  articleCount: String = "mediaCount",
  mediaCount: String = "mediaCount",
  size: String = "1024",
  name: String = "name",
  favIcon: String = "favIcon"
) =
  Book().apply {
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
    bookName = name
    favicon = favIcon
  }

fun libraryNetworkEntity(books: List<Book> = emptyList()) = LibraryNetworkEntity().apply {
  book = LinkedList(books)
}
