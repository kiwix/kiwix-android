/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.core.newdb.dao

import io.objectbox.Box
import io.objectbox.kotlin.query
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.newdb.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.newdb.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.zim_manager.ZimFileReader
import javax.inject.Inject

class NewBookmarksDao @Inject constructor(val box: Box<BookmarkEntity>) {
  fun getBookmarks(
    fromCurrentBook: Boolean,
    zimFileReader: ZimFileReader?
  ) = box.query {
    if (fromCurrentBook) {
      equal(BookmarkEntity_.zimName, zimFileReader?.name ?: "")
    }
    order(BookmarkEntity_.bookmarkTitle)
  }.find()
    .map(::BookmarkItem)

  fun getCurrentZimBookmarksUrl(zimFileReader: ZimFileReader?) = box.query {
    equal(BookmarkEntity_.zimId, zimFileReader?.id ?: "")
      .or()
      .equal(BookmarkEntity_.zimName, zimFileReader?.name ?: "")
    order(BookmarkEntity_.bookmarkTitle)
  }
    .property(BookmarkEntity_.bookmarkUrl)
    .findStrings()
    .toList()
    .distinct()

  fun saveBookmark(bookmarkItem: BookmarkItem) {
    box.put(BookmarkEntity(bookmarkItem))
  }

  fun deleteBookmarks(bookmarks: List<BookmarkItem>) {
    box.remove(bookmarks.map(::BookmarkEntity))
  }

  fun deleteBookmark(bookmarkUrl: String) {
    box.query {
      equal(BookmarkEntity_.bookmarkUrl, bookmarkUrl)
    }.remove()
  }

  fun migrationInsert(
    bookmarks: MutableList<Bookmark>,
    bookDao: NewBookDao
  ) {
    box.put(bookmarks.zip(bookmarks.map(bookDao::getFavIconAndZimFile)).map(::BookmarkEntity))
  }
}
