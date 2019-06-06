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

import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.downloader.model.DownloadState
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Pending
import org.kiwix.kiwixmobile.downloader.model.DownloadStatus
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDiskItem
import java.io.File

fun bookOnDiskItem(bookOnDisk: BookOnDisk = bookOnDisk()) = BookOnDiskItem(bookOnDisk)

fun bookOnDisk(
  book: Book = book(),
  databaseId: Long = 0L,
  file: File = File("")
) = BookOnDisk(databaseId, book, file)

fun book(id: String = "0") = Book().apply { this.id = id }

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
