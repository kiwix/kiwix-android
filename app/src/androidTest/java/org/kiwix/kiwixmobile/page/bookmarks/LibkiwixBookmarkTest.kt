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

package org.kiwix.kiwixmobile.page.bookmarks

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.Bookmark
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager

internal class LibkiwixBookmarkTest {
  private val library: Library = mockk(relaxed = true)
  private val manager = Manager(library)
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk(relaxed = true)
  private val libkiwixBookmarks = LibkiwixBookmarks(library, manager, sharedPreferenceUtil)

  @Test
  internal fun saveBookmark() {
    val bookmark: Bookmark = mockk(relaxed = true)
    libkiwixBookmarks.saveBookmark(LibkiwixBookmarkItem(bookmark, null, null))
    verify { library.addBookmark(bookmark) }
  }
}
