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

package org.kiwix.kiwixmobile.core.zim_manager.fileselect_view

import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.newdb.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.utils.files.FileSearch
import org.kiwix.kiwixmobile.core.zim_manager.ZimFileReader
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val downloadDao: FetchDownloadDao,
  private val fileSearch: FileSearch,
  private val zimReaderFactory: ZimFileReader.Factory
) {

  val booksOnFileSystem: Flowable<List<BookOnDisk>>
    get() = scanFiles()
      .withLatestFrom(downloadDao.downloads(), BiFunction(::toFilesThatAreNotDownloading))
      .map { it.mapNotNull(::convertToBookOnDisk) }

  private fun scanFiles() = fileSearch.scan().subscribeOn(Schedulers.io())

  private fun toFilesThatAreNotDownloading(files: List<File>, downloads: List<DownloadModel>) =
    files.filter { fileHasNoMatchingDownload(downloads, it) }

  private fun fileHasNoMatchingDownload(downloads: List<DownloadModel>, file: File) =
    downloads.firstOrNull { file.absolutePath.endsWith(it.fileNameFromUrl) } == null

  private fun convertToBookOnDisk(file: File) =
    zimReaderFactory.create(file)?.let { BookOnDisk(file, it) }
}
