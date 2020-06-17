package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects

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

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkState
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowToast

data class DeleteBookmarkItems(
  private val state: BookmarkState,
  private val bookmarksDao: NewBookmarksDao
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    if (state.isInSelectionState) {
      bookmarksDao.deleteBookmarks(state.bookmarks.filter(BookmarkItem::isSelected))
    } else {
      bookmarksDao.deleteBookmarks(state.bookmarks)
      ShowToast(R.string.all_bookmarks_cleared)
    }
  }
}
