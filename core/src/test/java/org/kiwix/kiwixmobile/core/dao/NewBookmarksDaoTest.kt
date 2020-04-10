/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import org.junit.Test
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.sharedFunctions.bookmarkItem

internal class NewBookmarksDaoTest {

  private val box: Box<BookmarkEntity> = mockk(relaxed = true)
  private val newBookmarksDao = NewBookmarksDao(box)

  // @Test
  // fun `get bookmarks url of current zim file 3 - distinct`() {
  //   val zimFileReader: ???
  //   val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
  //   every { box.query() } returns queryBuilder
  //   every {
  //     queryBuilder.equal(
  //       BookmarkEntity_.zimId,
  //       zimFileReader?.id ?: ""
  //     )
  //   } returns queryBuilder
  //   every {
  //     queryBuilder.equal(
  //       BookmarkEntity_.zimId,
  //       zimFileReader?.name ?: ""
  //     )
  //   } returns queryBuilder
  //   val query: Query<BookmarkEntity> = mockk(relaxed = true)
  //   every { queryBuilder.build() } returns query
  //   // newBookmarksDao.bookmarkUrlsForCurrentBook(zimFileReader).test()
  //   //   .assertValue { it.size == HashSet(it).size }
  // }

  @Test
  fun `saveBookmark saves the specific bookmark Item`() {
    val bookmarkItem =
      bookmarkItem(0L, "zimId", "zimName", "zimFilePath", "bookmarkUrl", "bookmarkTitle", "favicon")
    newBookmarksDao.saveBookmark(bookmarkItem)
    verify { box.put(BookmarkEntity(bookmarkItem)) }
  }

  @Test
  fun `deleteBookmark removes query results for the url`() {
    val bookmarkUrl = "bookmarkUrl"
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.bookmarkUrl, bookmarkUrl) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.deleteBookmark(bookmarkUrl)
    verify { query.remove() }
  }

  @Test
  fun `deleteBookmarks removes specified bookmark item list`() {
    val bookmarksList: List<BookmarkItem> = mockk(relaxed = true)
    newBookmarksDao.deleteBookmarks(bookmarksList)
    verify { box.remove(bookmarksList.map(::BookmarkEntity)) }
  }
}
