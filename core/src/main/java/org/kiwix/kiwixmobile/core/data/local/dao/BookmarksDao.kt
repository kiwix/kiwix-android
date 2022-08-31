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

import com.yahoo.squidb.sql.Query
import com.yahoo.squidb.sql.Update
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import javax.inject.Inject

/**
 * Dao class for bookmarks.
 */
@Deprecated("")
class BookmarksDao @Inject constructor(private val kiwixDatabase: KiwixDatabase) {
  val bookmarks: List<Bookmark>
    @Suppress("TooGenericExceptionCaught")
    get() {
      val bookmarks = ArrayList<Bookmark>()
      val query = Query.select()
      try {
        val squidCursor = kiwixDatabase
          .query(Bookmark::class.java, query.orderBy(Bookmark.BOOKMARK_TITLE.asc()))
        while (squidCursor.moveToNext()) {
          val bookmark = Bookmark()
          bookmark.zimId = squidCursor.get(Bookmark.ZIM_ID)!!
          bookmark.zimName = squidCursor.get(Bookmark.ZIM_NAME)!!
          bookmark.bookmarkTitle = squidCursor.get(Bookmark.BOOKMARK_TITLE)!!
          bookmark.bookmarkUrl = squidCursor.get(Bookmark.BOOKMARK_URL)!!
          bookmarks.add(bookmark)
        }
      } catch (exception: Exception) {
        exception.printStackTrace()
      }
      return bookmarks
    }

  @Suppress("TooGenericExceptionCaught")
  fun processBookmark(operation: StringOperation) {
    try {
      val bookmarkCursor = kiwixDatabase.query(
        Bookmark::class.java,
        Query.select(Bookmark.ROWID, Bookmark.BOOKMARK_URL)
      )
      while (bookmarkCursor.moveToNext()) {
        var url = bookmarkCursor.get(Bookmark.BOOKMARK_URL)
        url = operation.apply(url)
        if (url != null) {
          kiwixDatabase.update(
            Update.table(Bookmark.TABLE)
              .where(Bookmark.ROWID.eq(bookmarkCursor.get(Bookmark.ROWID)))
              .set(Bookmark.BOOKMARK_URL, url)
          )
        }
      }
    } catch (exception: Exception) {
      exception.printStackTrace()
    }
  }

  interface StringOperation {
    fun apply(string: String?): String?
  }
}
