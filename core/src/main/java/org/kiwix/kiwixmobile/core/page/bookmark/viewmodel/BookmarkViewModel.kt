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

import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class BookmarkViewModel @Inject constructor(
  override val pageDao: NewBookmarksDao,
  override val zimReaderContainer: ZimReaderContainer,
  override val sharedPreferenceUtil: SharedPreferenceUtil
) : PageViewModel<BookmarkState>() {

  override fun initialState(): BookmarkState =
    BookmarkState(emptyList(), sharedPreferenceUtil.showBookmarksAllBooks, zimReaderContainer.id)

  init {
    compositeDisposable.addAll(
      viewStateReducer(),
      pageDao.pages().subscribeOn(Schedulers.io())
        .subscribe({ actions.offer(Action.UpdatePages(it)) }, Throwable::printStackTrace)
    )
  }

  override fun updatePagesBasedOnFilter(state: PageState, action: Action.Filter): PageState =
    (state as BookmarkState).copy(searchTerm = action.searchTerm)

  override fun updatePages(state: PageState, action: Action.UpdatePages): PageState =
    (state as BookmarkState).copy(pageItems = action.pages.filterIsInstance<BookmarkItem>())

  override fun offerUpdateToShowAllToggle(
    action: Action.UserClickedShowAllToggle,
    state: PageState
  ): PageState {
    effects.offer(UpdateAllBookmarksPreference(sharedPreferenceUtil, action.isChecked))
    return (state as BookmarkState).copy(showAll = action.isChecked)
  }

  override fun offerShowDeleteDialog(state: PageState): PageState {
    effects.offer(ShowDeleteBookmarksDialog(effects, state as BookmarkState, pageDao))
    return state
  }

  override fun deselectAllPages(state: PageState): PageState =
    (state as BookmarkState).copy(pageItems = state.pageItems.map { it.copy(isSelected = false) })
}
