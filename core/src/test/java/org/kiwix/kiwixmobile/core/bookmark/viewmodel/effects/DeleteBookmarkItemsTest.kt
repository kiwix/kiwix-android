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

package org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.createSimpleBookmarkItem
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao

internal class DeleteBookmarkItemsTest {
  @Test
  fun `delete without selection deletes all items`() {
    val bookmark1: BookmarkItem = createSimpleBookmarkItem()
    val bookmark2: BookmarkItem = createSimpleBookmarkItem()
    val bookmarksDao: NewBookmarksDao = mockk()
    val state: State = mockk()
    every { state.bookmarks } returns listOf(bookmark1, bookmark2)
    every { state.isInSelectionState } returns false
    val activity: AppCompatActivity = mockk()
    DeleteBookmarkItems(state, bookmarksDao).invokeWith(activity)
    verify { bookmarksDao.deleteBookmarks(listOf(bookmark1, bookmark2)) }
  }

  @Test
  fun `delete with selection deletes selected items`() {
    val bookmark1: BookmarkItem = createSimpleBookmarkItem(isSelected = true)
    val bookmark2: BookmarkItem = createSimpleBookmarkItem()
    val bookmarksDao: NewBookmarksDao = mockk()
    val state: State = mockk()
    every { state.bookmarks } returns listOf(bookmark1, bookmark2)
    every { state.isInSelectionState } returns true
    val activity: AppCompatActivity = mockk()
    DeleteBookmarkItems(state, bookmarksDao).invokeWith(activity)
    verify { bookmarksDao.deleteBookmarks(listOf(bookmark1)) }
  }
}
