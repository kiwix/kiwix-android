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

package org.kiwix.kiwixmobile.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.files.FileSearch
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.libkiwix.Book
import java.io.File
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val downloadRoomDao: DownloadRoomDao,
  private val fileSearch: FileSearch,
  private val zimReaderFactory: ZimFileReader.Factory,
  private val libkiwixBookmarks: LibkiwixBookmarks,
  private val libkiwixBookFactory: LibkiwixBookFactory
) {
  fun getBooksOnFileSystem(
    scanningProgressListener: ScanningProgressListener,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ): Flow<List<Book>> = flow {
    val files = scanFiles(scanningProgressListener).first()
    val downloads = downloadRoomDao.downloads().first()
    val result = toFilesThatAreNotDownloading(files, downloads)
      .mapNotNull { convertToLibkiwixBook(it) }
    emit(result)
  }.flowOn(dispatcher)

  private fun scanFiles(scanningProgressListener: ScanningProgressListener): Flow<List<File>> =
    fileSearch.scan(scanningProgressListener)

  private fun toFilesThatAreNotDownloading(files: List<File>, downloads: List<DownloadModel>) =
    files.filter { fileHasNoMatchingDownload(downloads, it) }

  private fun fileHasNoMatchingDownload(downloads: List<DownloadModel>, file: File) =
    downloads.none { file.absolutePath.endsWith(it.fileNameFromUrl) }

  private suspend fun convertToLibkiwixBook(file: File) =
    zimReaderFactory.create(ZimReaderSource(file))
      ?.let { zimFileReader ->
        libkiwixBookFactory.create().apply {
          update(zimFileReader.jniKiwixReader)
        }.also {
          // add the book to libkiwix library to validate the imported bookmarks
          libkiwixBookmarks.addBookToLibrary(archive = zimFileReader.jniKiwixReader)
          zimFileReader.dispose()
        }
      }
}

interface LibkiwixBookFactory {
  fun create(): Book
}
