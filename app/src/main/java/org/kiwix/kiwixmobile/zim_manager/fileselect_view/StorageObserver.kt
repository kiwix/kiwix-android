package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.content.Context
import android.util.Log
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.database.newdb.dao.NewDownloadDao
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.files.FileSearch
import org.kiwix.kiwixmobile.utils.files.FileSearch.ResultListener
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val context: Context,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val downloadDao: NewDownloadDao
) {

  private val _booksOnFileSystem = PublishProcessor.create<List<BookOnDisk>>()
  val booksOnFileSystem = _booksOnFileSystem.distinctUntilChanged()
      .doOnSubscribe {
        downloadDao.downloads()
            .subscribeOn(Schedulers.io())
            .take(1)
            .subscribe(this::scanFiles, Throwable::printStackTrace)
      }

  private fun scanFiles(downloads: List<DownloadModel>) {
    FileSearch(context, downloads, object : ResultListener {
      val foundBooks = mutableSetOf<BookOnDisk>()

      override fun onBookFound(book: BookOnDisk) {
        foundBooks.add(book)
        Log.i("Scanner", "File Search: Found Book " + book.book.title)
      }

      override fun onScanCompleted() {
        _booksOnFileSystem.onNext(foundBooks.toList())

      }
    }).scan(sharedPreferenceUtil.prefStorage)
  }
}
