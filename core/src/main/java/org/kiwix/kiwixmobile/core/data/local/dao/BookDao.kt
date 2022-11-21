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
package org.kiwix.kiwixmobile.core.data.local.dao

import com.yahoo.squidb.data.SquidCursor
import com.yahoo.squidb.sql.Query
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.entity.BookDatabaseEntity
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.hasPart
import java.io.File
import javax.inject.Inject

/**
 * Dao class for books
 */
@Deprecated("")
class BookDao @Inject constructor(private val kiwixDatabase: KiwixDatabase) {
  private fun setBookDetails(
    book: LibraryNetworkEntity.Book,
    bookCursor: SquidCursor<BookDatabaseEntity>
  ) {
    book.id = bookCursor.get(BookDatabaseEntity.BOOK_ID)!!
    book.title = bookCursor.get(BookDatabaseEntity.TITLE)!!
    book.description = bookCursor.get(BookDatabaseEntity.DESCRIPTION)
    book.language = bookCursor.get(BookDatabaseEntity.LANGUAGE)!!
    book.creator = bookCursor.get(BookDatabaseEntity.BOOK_CREATOR)!!
    book.publisher = bookCursor.get(BookDatabaseEntity.PUBLISHER)!!
    book.date = bookCursor.get(BookDatabaseEntity.DATE)!!
    book.file = bookCursor.get(BookDatabaseEntity.URL)?.let(::File)
    book.articleCount = bookCursor.get(BookDatabaseEntity.ARTICLE_COUNT)
    book.mediaCount = bookCursor.get(BookDatabaseEntity.MEDIA_COUNT)
    book.size = bookCursor.get(BookDatabaseEntity.SIZE)!!
    book.favicon = bookCursor.get(BookDatabaseEntity.FAVICON)!!
    book.bookName = bookCursor.get(BookDatabaseEntity.NAME)
  }

  val books: ArrayList<LibraryNetworkEntity.Book>
    @Suppress("TooGenericExceptionCaught")
    get() {
      val books = ArrayList<LibraryNetworkEntity.Book>()
      try {
        val bookCursor = kiwixDatabase.query(
          BookDatabaseEntity::class.java,
          Query.select()
        )
        while (bookCursor.moveToNext()) {
          val book = LibraryNetworkEntity.Book()
          setBookDetails(book, bookCursor)
          books.add(book)
        }
      } catch (exception: Exception) {
        exception.printStackTrace()
      }
      return filterBookResults(books)
    }

  fun filterBookResults(
    books: ArrayList<LibraryNetworkEntity.Book>
  ): ArrayList<LibraryNetworkEntity.Book> {
    val filteredBookList = ArrayList<LibraryNetworkEntity.Book>()
    books
      .asSequence()
      .filterNot { it.file?.let(::hasPart) == true }
      .forEach {
        if (it.file?.exists() == true) {
          filteredBookList.add(it)
        } else {
          kiwixDatabase.deleteWhere(
            BookDatabaseEntity::class.java,
            BookDatabaseEntity.URL.eq(it.file?.path)
          )
        }
      }
    return filteredBookList
  }
}
