/*
 * Kiwix Android
 * Copyright (c) 2021 Kiwix <android.kiwix.org>
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

import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader

internal class NewBookmarksDaoTest {
  private val box: Box<BookmarkEntity> = mockk(relaxed = true)
  private val newBookmarksDao = NewBookmarksDao(box)

  @Test
  fun deletePages() {
    val bookmarkItem: BookmarkItem = mockk(relaxed = true)
    val bookmarkItemList: List<BookmarkItem> = listOf(bookmarkItem)
    val pagesToDelete: List<Page> = bookmarkItemList
    newBookmarksDao.deletePages(pagesToDelete)
    verify { newBookmarksDao.deleteBookmarks(bookmarkItemList) }
  }

  @Test
  fun getCurrentZimBookmarksUrl() {
    val bookmarkItem: BookmarkItem = mockk(relaxed = true)
    val zimFileReader: ZimFileReader? = mockk(relaxed = true)
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimId, ""
      )
    } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimName, ""
      )
    } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    every { bookmarkItem.zimId } returns ""
    every { bookmarkItem.zimName } returns ""
    every { bookmarkItem.databaseId } returns 0L
    newBookmarksDao.getCurrentZimBookmarksUrl(zimFileReader)
    val bookmarkEntity: BookmarkEntity = mockk()
    every {
      query.property(BookmarkEntity_.bookmarkUrl).findStrings().toList().distinct()
    } returns listOf("")
    verify { box.query() }
  }

  @Test
  fun bookmarkUrlsForCurrentBook() {
    val bookmarkItem: BookmarkItem = mockk(relaxed = true)
    val zimFileReader: ZimFileReader? = mockk(relaxed = true)
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk()
    every { box.query() } returns queryBuilder
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimId,
        zimFileReader?.id ?: ""
      )
    } returns queryBuilder
    every { queryBuilder.or() } returns queryBuilder
    every {
      queryBuilder.equal(
        BookmarkEntity_.zimName,
        zimFileReader?.name ?: ""
      )
    } returns queryBuilder
    every { queryBuilder.order(BookmarkEntity_.bookmarkTitle) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    every { bookmarkItem.zimId } returns ""
    every { bookmarkItem.zimName } returns ""
    every { bookmarkItem.databaseId } returns 0L
    newBookmarksDao.bookmarkUrlsForCurrentBook(zimFileReader)
    verify { box.query() }
  }

  @Test
  fun saveBookmark() {
    val bookmarkItem: BookmarkItem = mockk(relaxed = true)
    newBookmarksDao.saveBookmark(bookmarkItem)
    verify { box.put(BookmarkEntity(bookmarkItem)) }
  }

  @Test
  fun deleteBookmark() {
    val bookmarkUrl = "bookmarkUrl"
    val queryBuilder: QueryBuilder<BookmarkEntity> = mockk(relaxed = true)
    every { box.query() } returns queryBuilder
    every { queryBuilder.equal(BookmarkEntity_.bookmarkUrl, bookmarkUrl) } returns queryBuilder
    val query: Query<BookmarkEntity> = mockk(relaxed = true)
    every { queryBuilder.build() } returns query
    newBookmarksDao.deleteBookmark(bookmarkUrl)
    verify { query.remove() }
  }
}
