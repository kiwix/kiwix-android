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
import io.reactivex.Flowable
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
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

  private val bookmarksPath: String by lazy {
    sharedPreferenceUtil.getPublicDirectoryPath(sharedPreferenceUtil.defaultStorage()) + "/kiwix/Bookmarks.txt"
  }

  private val bookMarksFile: File by lazy { File(bookmarksPath) }

  init {
    if (!File(bookmarksPath).isFileExist()) File(bookmarksPath).createNewFile()
  }

  fun bookmarks(): Flowable<List<Page>> {
    manager.readBookmarkFile(bookmarksPath)
    val bookMarksArray: Array<out Bookmark>? = library.getBookmarks(true)
   return bookMarksArray?.let {
        it.map(::BookmarkItem)
    } ?: emptyList<BookmarkItem>()
  }
    box.asFlowable(
    box.query {
      order(BookmarkEntity_.bookmarkTitle)
    }
  ).map { it.map(org.kiwix.kiwixmobile.core.page.bookmark.adapter::BookmarkItem) }

  override fun pages(): Flowable<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) {
  }

  fun saveBookmark(bookmark: Bookmark) {

  }
}
