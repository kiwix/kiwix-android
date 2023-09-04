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

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import java.io.File
import javax.inject.Inject

class LibkiwixBookmarks @Inject constructor(
  val library: Library,
  val manager: Manager,
  val sharedPreferenceUtil: SharedPreferenceUtil
) : PageDao {

  private val bookmarksFolderPath: String by lazy {
    sharedPreferenceUtil.getPublicDirectoryPath(sharedPreferenceUtil.defaultStorage()) + "/kiwix/Bookmarks/"
  }

  private val bookmarkFile: File by lazy {
    File("$bookmarksFolderPath/bookmark.txt")
  }

  init {
    // Check if bookmark folder exist if not then create the folder first.
    if (!File(bookmarksFolderPath).isFileExist()) File(bookmarksFolderPath).mkdir()
    // Check if bookmark file exist if not then create the file to save the bookmarks.
    if (!bookmarkFile.isFileExist()) bookmarkFile.createNewFile()
    // set up manager to read the bookmarks from this file
    manager.readBookmarkFile(bookmarkFile.canonicalPath)
  }

  fun bookmarks(): Flowable<List<Page>> =
    Flowable.fromIterable(getBookmarksList())
      .map(::LibkiwixBookmarkItem)
      .toList()
      .toFlowable() as Flowable<List<Page>>

  override fun pages(): Flowable<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) =
    deleteBookmarks(pagesToDelete as List<LibkiwixBookmarkItem>)

  fun getCurrentZimBookmarksUrl(zimFileReader: ZimFileReader?): List<String> {
    return zimFileReader?.let { reader ->
      library
        .getBookmarks(true)
        .filter { it.bookId == reader.id }
        .map { it.url }
    } ?: emptyList()
  }

  fun bookmarkUrlsForCurrentBook(zimFileReader: ZimFileReader): Flowable<List<String>> {
    return Flowable.create({ emitter ->
      // Create a Book object and add it to the library
      val book = Book().apply {
        update(zimFileReader.jniKiwixReader)
      }
      addBookToLibrary(book)
      val urls = getBookmarksList()
        .filter { it.bookId == zimFileReader.id }
        .map { it.url }

      // Emit the list of URLs
      emitter.onNext(urls)
      emitter.onComplete()
    }, BackpressureStrategy.LATEST)
      .subscribeOn(Schedulers.io())
  }

  fun saveBookmark(libkiwixBookmarkItem: LibkiwixBookmarkItem) {
    if (!isBookMarkExist(libkiwixBookmarkItem)) {
      addBookToLibrary(libkiwixBookmarkItem.libKiwixBook)
      val bookmark = Bookmark().apply {
        bookId = libkiwixBookmarkItem.zimId
        title = libkiwixBookmarkItem.title
        url = libkiwixBookmarkItem.url
        bookTitle = libkiwixBookmarkItem.libKiwixBook?.title ?: libkiwixBookmarkItem.zimId
      }
      library.addBookmark(bookmark).also {
        writeBookMarksToFile()
      }
    }
  }

  private fun addBookToLibrary(libKiwixBook: Book?) {
    library.addBook(libKiwixBook)
  }

  fun deleteBookmarks(bookmarks: List<LibkiwixBookmarkItem>) {
    bookmarks.map { deleteBookmark(it.zimId, it.bookmarkUrl) }
  }

  fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    library.removeBookmark(bookId, bookmarkUrl).also {
      writeBookMarksToFile()
    }
  }

  private fun writeBookMarksToFile() {
    library.writeBookmarksToFile(bookmarkFile.canonicalPath)
  }

  private fun getBookmarksList() =
    library.getBookmarks(true)?.toList() ?: emptyList()

  private fun isBookMarkExist(libkiwixBookmarkItem: LibkiwixBookmarkItem): Boolean =
    getBookmarksList()
      .any { it.url == libkiwixBookmarkItem.bookmarkUrl && it.bookId == libkiwixBookmarkItem.zimId }
}
