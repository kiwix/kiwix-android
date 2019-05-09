package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.content.Context
import android.util.Log
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.files.FileSearch
import org.kiwix.kiwixmobile.utils.files.FileSearch.ResultListener
import javax.inject.Inject

class StorageObserver @Inject constructor(
  private val context: Context,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) {

  private val _booksOnFileSystem = PublishProcessor.create<Collection<Book>>()
  val booksOnFileSystem = _booksOnFileSystem.distinctUntilChanged()
      .doOnSubscribe { scanFiles() }

  private fun scanFiles() {
    FileSearch(context, object : ResultListener {
      val foundBooks = mutableSetOf<Book>()

      override fun onBookFound(book: Book) {
        foundBooks.add(book)
        Log.i("Scanner", "File Search: Found Book " + book.title)
      }

      override fun onScanCompleted() {
        _booksOnFileSystem.onNext(foundBooks)

      }
    }).scan(sharedPreferenceUtil.prefStorage)
  }
}
