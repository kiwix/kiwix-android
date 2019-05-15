package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.content.Context
import android.util.Log
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.database.DownloadDao
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.files.FileSearch
import org.kiwix.kiwixmobile.utils.files.FileSearch.ResultListener
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val context: Context,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val downloadDao: DownloadDao
) {

  private val _booksOnFileSystem = PublishProcessor.create<Collection<BookOnDisk>>()
  val booksOnFileSystem = _booksOnFileSystem.distinctUntilChanged()
      .doOnSubscribe { scanFiles(downloadDao.downloads) }

  private fun scanFiles(downloads: MutableList<DownloadModel>) {
    FileSearch(context, downloads, object : ResultListener {
      val foundBooks = mutableSetOf<BookOnDisk>()

      override fun onBookFound(book: BookOnDisk) {
        foundBooks.add(book)
        Log.i("Scanner", "File Search: Found Book " + book.book.title)
      }

      override fun onScanCompleted() {
        _booksOnFileSystem.onNext(foundBooks)

      }
    }).scan(sharedPreferenceUtil.prefStorage)
  }
}
