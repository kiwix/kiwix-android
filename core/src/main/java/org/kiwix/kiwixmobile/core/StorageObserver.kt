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

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.files.FileSearch
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val downloadRoomDao: DownloadRoomDao,
  private val fileSearch: FileSearch,
  private val zimReaderFactory: ZimFileReader.Factory,
  private val libkiwixBookmarks: LibkiwixBookmarks
) {

  fun getBooksOnFileSystem(
    scanningProgressListener: ScanningProgressListener
  ): Flowable<List<BookOnDisk>> {
    return scanFiles(scanningProgressListener)
      .withLatestFrom(downloadRoomDao.downloads(), BiFunction(::toFilesThatAreNotDownloading))
      .flatMapSingle { files ->
        Single.create { emitter ->
          CoroutineScope(Dispatchers.IO).launch {
            try {
              emitter.onSuccess(files.mapNotNull { convertToBookOnDisk(it) })
            } catch (ignore: Exception) {
              emitter.onError(ignore)
            }
          }
        }
      }
  }

  private fun scanFiles(scanningProgressListener: ScanningProgressListener) =
    fileSearch.scan(scanningProgressListener).subscribeOn(Schedulers.io())

  private fun toFilesThatAreNotDownloading(files: List<File>, downloads: List<DownloadModel>) =
    files.filter { fileHasNoMatchingDownload(downloads, it) }

  private fun fileHasNoMatchingDownload(downloads: List<DownloadModel>, file: File) =
    downloads.firstOrNull { file.absolutePath.endsWith(it.fileNameFromUrl) } == null

  private suspend fun convertToBookOnDisk(file: File) =
    zimReaderFactory.create(ZimReaderSource(file))
      ?.let { zimFileReader ->
        BookOnDisk(zimFileReader).also {
          // add the book to libkiwix library to validate the imported bookmarks
          libkiwixBookmarks.addBookToLibrary(archive = zimFileReader.jniKiwixReader)
          zimFileReader.dispose()
        }
      }
}
