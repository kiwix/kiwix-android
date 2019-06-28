package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.util.Log
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.database.newdb.dao.NewDownloadDao
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.files.FileSearch
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import java.io.File
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  downloadDao: NewDownloadDao,
  private val fileSearch: FileSearch
) {

  val booksOnFileSystem = scanFiles()
      .withLatestFrom(
          downloadDao.downloads(),
          BiFunction(this::toFilesThatAreNotDownloading)
      )
      .map {
        it.mapNotNull { file -> convertToBookOnDisk(file) }
      }

  private fun toFilesThatAreNotDownloading(
    files: List<File>,
    downloads: List<DownloadModel>
  ) = files.filter { fileHasNoMatchingDownload(downloads, it) }

  private fun fileHasNoMatchingDownload(
    downloads: List<DownloadModel>,
    file: File
  ) = downloads.firstOrNull {
    file.absolutePath.endsWith(it.fileNameFromUrl)
  } == null

  private fun scanFiles() = fileSearch.scan(sharedPreferenceUtil.prefStorage)
      .subscribeOn(Schedulers.io())

  private fun convertToBookOnDisk(file: File): BookOnDisk? {
    configureZimContentProvider()
    if (ZimContentProvider.canIterate && ZimContentProvider.setZimFile(file.absolutePath) != null) {
      try {
        return BookOnDisk(book = bookFromZimContentProvider(), file = file)
      } catch (e: Exception) {
        // TODO 20171215 Consider more elegant approaches.
        // This is to see if we can catch the exception at all!
        Log.e("kiwix-filesearch", "Problem parsing a book entry from the library file. ", e)
      } finally {
        resetZimContentProvider()
      }
    }
    return null
  }

  private fun bookFromZimContentProvider() = Book().apply {
    title = ZimContentProvider.getZimFileTitle()
    id = ZimContentProvider.getId()
    size = ZimContentProvider.getFileSize()
        .toString()
    favicon = ZimContentProvider.getFavicon()
    creator = ZimContentProvider.getCreator()
    publisher = ZimContentProvider.getPublisher()
    date = ZimContentProvider.getDate()
    description = ZimContentProvider.getDescription()
    language = ZimContentProvider.getLanguage()
  }

  private fun resetZimContentProvider() {
    if (ZimContentProvider.originalFileName != "") {
      ZimContentProvider.setZimFile(ZimContentProvider.originalFileName)
    }
    ZimContentProvider.originalFileName = ""
  }

  private fun configureZimContentProvider() {
    if (ZimContentProvider.zimFileName != null) {
      ZimContentProvider.originalFileName = ZimContentProvider.zimFileName
    }
  }
}
