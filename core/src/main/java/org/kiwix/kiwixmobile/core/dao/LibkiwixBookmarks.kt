/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import android.os.Build
import android.util.Base64
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ILLUSTRATION_SIZE
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import org.kiwix.libzim.Archive
import java.io.File
import javax.inject.Inject

class LibkiwixBookmarks @Inject constructor(
  val library: Library,
  manager: Manager,
  val sharedPreferenceUtil: SharedPreferenceUtil
) : PageDao {

  private val bookmarkListBehaviour: BehaviorSubject<List<LibkiwixBookmarkItem>>? by lazy {
    BehaviorSubject.createDefault(getBookmarksList())
  }

  private val bookmarksFolderPath: String by lazy {
    if (Build.DEVICE.contains("generic")) {
      // Workaround for emulators: Emulators have limited memory and
      // restrictions on creating folders, so we will use the default
      // path for saving the bookmark file.
      sharedPreferenceUtil.context.filesDir.path
    } else {
      sharedPreferenceUtil.getPublicDirectoryPath(
        sharedPreferenceUtil.defaultStorage()
      ) + "/kiwix/Bookmarks/"
    }
  }

  private val bookmarkFile: File by lazy {
    File("$bookmarksFolderPath/bookmark.txt")
  }

  private val libraryFile: File by lazy {
    File("$bookmarksFolderPath/library.txt")
  }

  init {
    // Check if bookmark folder exist if not then create the folder first.
    if (!File(bookmarksFolderPath).isFileExist()) File(bookmarksFolderPath).mkdir()
    // Check if library file exist if not then create the file to save the library with book information.
    if (!libraryFile.isFileExist()) libraryFile.createNewFile()
    // set up manager to read the library from this file
    manager.readFile(libraryFile.canonicalPath)
    // Check if bookmark file exist if not then create the file to save the bookmarks.
    if (!bookmarkFile.isFileExist()) bookmarkFile.createNewFile()
    // set up manager to read the bookmarks from this file
    manager.readBookmarkFile(bookmarkFile.canonicalPath)
  }

  fun bookmarks(): Flowable<List<Page>> =
    flowableBookmarkList()
      .map { it }

  override fun pages(): Flowable<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) =
    deleteBookmarks(pagesToDelete as List<LibkiwixBookmarkItem>)

  fun getCurrentZimBookmarksUrl(zimFileReader: ZimFileReader?): List<String> {
    return zimFileReader?.let { reader ->
      getBookmarksList()
        .filter { it.zimId == reader.id }
        .map(LibkiwixBookmarkItem::bookmarkUrl)
    } ?: emptyList()
  }

  fun bookmarkUrlsForCurrentBook(zimFileReader: ZimFileReader): Flowable<List<String>> =
    flowableBookmarkList()
      .map { bookmarksList ->
        bookmarksList.filter { it.zimId == zimFileReader.id }
          .map(LibkiwixBookmarkItem::bookmarkUrl)
      }
      .subscribeOn(Schedulers.io()).also {
        val book = Book().apply {
          update(zimFileReader.jniKiwixReader)
        }
        addBookToLibraryIfNotExist(book)
      }

  fun saveBookmark(libkiwixBookmarkItem: LibkiwixBookmarkItem) {
    if (!isBookMarkExist(libkiwixBookmarkItem)) {
      addBookToLibraryIfNotExist(libkiwixBookmarkItem.libKiwixBook)
      val bookmark = Bookmark().apply {
        bookId = libkiwixBookmarkItem.zimId
        title = libkiwixBookmarkItem.title
        url = libkiwixBookmarkItem.url
        bookTitle = libkiwixBookmarkItem.libKiwixBook?.title ?: libkiwixBookmarkItem.zimId
      }
      library.addBookmark(bookmark).also {
        writeBookMarksAndSaveLibraryToFile()
        updateFlowableBookmarkList()
      }
    }
  }

  private fun addBookToLibraryIfNotExist(libKiwixBook: Book?) {
    libKiwixBook?.let { book ->
      if (!library.booksIds.any { it == book.id }) {
        library.addBook(libKiwixBook).also {
          if (BuildConfig.DEBUG) {
            Log.d(
              TAG,
              "Added Book to Library:\n" +
                "ZIM File Path: ${book.path}\n" +
                "Book ID: ${book.id}\n" +
                "Book Title: ${book.title}"
            )
          }
        }
      }
    }
  }

  fun deleteBookmarks(bookmarks: List<LibkiwixBookmarkItem>) {
    bookmarks.map { deleteBookmark(it.zimId, it.bookmarkUrl) }
  }

  fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    library.removeBookmark(bookId, bookmarkUrl).also {
      writeBookMarksAndSaveLibraryToFile()
      updateFlowableBookmarkList()
    }
  }

  /**
   * Asynchronously writes the library and bookmarks data to their respective files in a background thread
   * to prevent potential data loss and ensures that the library holds the updated ZIM file paths and favicons.
   */
  private fun writeBookMarksAndSaveLibraryToFile() {
    CoroutineScope(Dispatchers.IO).launch {
      // Save the library, which contains ZIM file paths and favicons, to a file.
      library.writeToFile(libraryFile.canonicalPath)

      // Save the bookmarks data to a separate file.
      library.writeBookmarksToFile(bookmarkFile.canonicalPath)
    }
  }

  private fun getBookmarksList(): List<LibkiwixBookmarkItem> {
    // Retrieve the list of bookmarks from the library, or return an empty list if it's null.
    val bookmarkList = library.getBookmarks(false)?.toList() ?: return emptyList()

    // Create a list to store LibkiwixBookmarkItem objects.
    return bookmarkList.mapNotNull { bookmark ->
      // Check if the library contains the book associated with the bookmark.
      val book = if (library.booksIds.contains(bookmark.bookId)) {
        library.getBookById(bookmark.bookId)
      } else {
        if (BuildConfig.DEBUG) {
          Log.d(
            TAG,
            "Library does not contain the book for this bookmark:\n" +
              "Book Title: ${bookmark.bookTitle}\n" +
              "Bookmark URL: ${bookmark.url}"
          )
        }
        null
      }

      // Create an Archive object for the book's path, if it exists.
      val archive: Archive? = book?.run {
        try {
          Archive(this.path)
        } catch (exception: Exception) {
          // to handle if zim file not found
          // TODO should we delete bookmark if zim file not found?
          // deleteBookmark(book.id, bookmark.url)
          if (BuildConfig.DEBUG) {
            Log.e(
              TAG,
              "Failed to create an archive for path: ${book.path}\n" +
                "Exception: $exception"
            )
          }
          null
        }
      }

      // Check if the Archive has an illustration of the specified size and encode it to Base64.
      val favicon = archive?.takeIf { it.hasIllustration(ILLUSTRATION_SIZE) }?.let {
        Base64.encodeToString(it.getIllustrationItem(ILLUSTRATION_SIZE).data.data, Base64.DEFAULT)
      }
      // Create a LibkiwixBookmarkItem object with bookmark, favicon, and book path.
      val libkiwixBookmarkItem = LibkiwixBookmarkItem(
        bookmark,
        favicon,
        book?.path
      )

      // Dispose of the Archive object to release resources.
      archive?.dispose()

      // Return the LibkiwixBookmarkItem, filtering out null results.
      return@mapNotNull libkiwixBookmarkItem
    }
  }

  private fun isBookMarkExist(libkiwixBookmarkItem: LibkiwixBookmarkItem): Boolean =
    getBookmarksList()
      .any {
        it.url == libkiwixBookmarkItem.bookmarkUrl &&
          it.zimFilePath == libkiwixBookmarkItem.zimFilePath
      }

  private fun flowableBookmarkList(
    backpressureStrategy: BackpressureStrategy = LATEST
  ): Flowable<List<LibkiwixBookmarkItem>> {
    return Flowable.create({ emitter ->
      val disposable = bookmarkListBehaviour?.subscribe(
        { list ->
          if (!emitter.isCancelled) {
            emitter.onNext(list.toList())
          }
        },
        emitter::onError,
        emitter::onComplete
      )

      emitter.setDisposable(disposable)
    }, backpressureStrategy)
  }

  private fun updateFlowableBookmarkList() {
    bookmarkListBehaviour?.onNext(getBookmarksList())
  }

  companion object {
    const val TAG = "LibkiwixBookmark"
  }
}
