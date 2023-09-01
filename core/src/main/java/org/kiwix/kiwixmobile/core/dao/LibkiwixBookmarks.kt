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

import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import io.reactivex.Flowable
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
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

  private val bookMarksFile: File by lazy { File(bookmarksFolderPath) }

  init {
    if (!File(bookmarksFolderPath).isFileExist()) File(bookmarksFolderPath).mkdir()
    manager.readBookmarkFile(bookmarksFolderPath)
  }

  fun bookmarks(): Flowable<List<Page>> {
    val bookMarksArray: Array<out Bookmark>? = library.getBookmarks(true)
    return bookMarksArray?.let {
      it.map(::BookmarkItem)
    }
  }

  override fun pages(): Flowable<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) =
    deleteBookmarks(pagesToDelete as List<LibkiwixBookmarkItem>)

  fun getCurrentZimBookmarksUrl(zimFileReader: ZimFileReader?) = box.query {
    equal(
      BookmarkEntity_.zimId, zimFileReader?.id ?: "",
      QueryBuilder.StringOrder.CASE_INSENSITIVE
    )
      .or()
      .equal(
        BookmarkEntity_.zimName, zimFileReader?.name ?: "",
        QueryBuilder.StringOrder.CASE_INSENSITIVE
      )
    order(BookmarkEntity_.bookmarkTitle)
  }.property(BookmarkEntity_.bookmarkUrl)
    .findStrings()
    .toList()
    .distinct()

  fun bookmarkUrlsForCurrentBook(zimFileReader: ZimFileReader): Flowable<List<String>> {
    val book = Book().apply {
      update(zimFileReader.jniKiwixReader)
    }
    library.addBook(book)
    val bookMarksList: Flowable<List<String>> = arrayListOf<String>()
    library.getBookmarks(true)
      .map {
      }
    return bookMarksList
  }

  fun saveBookmark(libkiwixBookmarkItem: LibkiwixBookmarkItem) {
    library.addBook(libkiwixBookmarkItem.libKiwixBook)
    val bookmark = Bookmark().apply {
      bookId = libkiwixBookmarkItem.zimId
      title = libkiwixBookmarkItem.title
      url = libkiwixBookmarkItem.url
      bookTitle = libkiwixBookmarkItem.libKiwixBook?.title ?: libkiwixBookmarkItem.zimId
    }
    library.addBookmark(bookmark).also {
      // if the book name is not found then takes zim id as file name
      val bookMarkFileName = libkiwixBookmarkItem.libKiwixBook?.name ?: libkiwixBookmarkItem.id
      library.writeBookmarksToFile("$bookmarksFolderPath/$bookMarkFileName")
    }
  }

  fun deleteBookmarks(bookmarks: List<LibkiwixBookmarkItem>) {
    bookmarks.map { deleteBookmark(it.zimId, it.bookmarkUrl) }
  }

  fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    library.removeBookmark(bookId, bookmarkUrl)
  }
}
