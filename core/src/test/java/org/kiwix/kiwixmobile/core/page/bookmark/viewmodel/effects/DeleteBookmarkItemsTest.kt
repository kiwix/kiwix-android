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

package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.processors.PublishProcessor
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.page.bookmark
import org.kiwix.kiwixmobile.core.page.bookmarkState
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowToast

internal class DeleteBookmarkItemsTest {
  private val bookmarksDao: NewBookmarksDao = mockk()
  val activity: AppCompatActivity = mockk()
  private val item1 = bookmark()
  private val item2 = bookmark()
  val effects: PublishProcessor<SideEffect<*>> = mockk(relaxed = true)

  @Test
  fun `delete with selected items only deletes the selected items`() {
    item1.isSelected = true
    DeleteBookmarkItems(effects, bookmarkState(listOf(item1, item2)), bookmarksDao).invokeWith(
      activity
    )
    verify { bookmarksDao.deleteBookmarks(listOf(item1)) }
  }

  @Test
  fun `delete with no selected items deletes all items`() {
    item1.isSelected = false
    DeleteBookmarkItems(effects, bookmarkState(listOf(item1, item2)), bookmarksDao).invokeWith(
      activity
    )
    verify { bookmarksDao.deleteBookmarks(listOf(item1, item2)) }
  }

  @Test
  fun `delete with no selected items shows toast with message all bookmarks cleared`() {
    item1.isSelected = false
    DeleteBookmarkItems(effects, bookmarkState(listOf(item1, item2)), bookmarksDao).invokeWith(
      activity
    )
    verify { effects.offer(ShowToast(R.string.all_bookmarks_cleared)) }
  }

  @Test
  fun `delete with selected items shows toast with message selected bookmarks cleared`() {
    item1.isSelected = true
    DeleteBookmarkItems(effects, bookmarkState(listOf(item1, item2)), bookmarksDao).invokeWith(
      activity
    )
    verify { effects.offer(ShowToast(R.string.selected_bookmarks_cleared)) }
  }
}
