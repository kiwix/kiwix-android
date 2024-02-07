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

package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel

import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class BookmarkViewModel @Inject constructor(
  libkiwixBookmarks: LibkiwixBookmarks,
  zimReaderContainer: ZimReaderContainer,
  sharedPrefs: SharedPreferenceUtil
) : PageViewModel<LibkiwixBookmarkItem, BookmarkState>(
  libkiwixBookmarks,
  sharedPrefs,
  zimReaderContainer
) {

  override fun initialState(): BookmarkState =
    BookmarkState(emptyList(), sharedPreferenceUtil.showBookmarksAllBooks, zimReaderContainer.id)

  override fun updatePagesBasedOnFilter(
    state: BookmarkState,
    action: Action.Filter
  ): BookmarkState =
    state.copy(searchTerm = action.searchTerm)

  override fun updatePages(
    state: BookmarkState,
    action: Action.UpdatePages
  ): BookmarkState =
    state.copy(pageItems = action.pages.filterIsInstance<LibkiwixBookmarkItem>())

  override fun offerUpdateToShowAllToggle(
    action: Action.UserClickedShowAllToggle,
    state: BookmarkState
  ): BookmarkState {
    effects.offer(UpdateAllBookmarksPreference(sharedPreferenceUtil, action.isChecked))
    return state.copy(showAll = action.isChecked)
  }

  override fun deselectAllPages(state: BookmarkState): BookmarkState =
    state.copy(pageItems = state.pageItems.map { it.copy(isSelected = false) })

  override fun createDeletePageDialogEffect(state: BookmarkState) =
    ShowDeleteBookmarksDialog(effects, state, pageDao)

  override fun copyWithNewItems(
    state: BookmarkState,
    newItems: List<LibkiwixBookmarkItem>
  ): BookmarkState =
    state.copy(pageItems = newItems)
}
