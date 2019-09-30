package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.database.newdb.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.utils.files.FileSearch
import org.kiwix.kiwixmobile.zim_manager.ZimFileReader
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
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
