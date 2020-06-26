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

package org.kiwix.kiwixmobile.core.page.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.PageDao
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Exit
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteSelectedPages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

abstract class PageViewModel : ViewModel() {
  abstract val zimReaderContainer: ZimReaderContainer
  abstract val sharedPreferenceUtil: SharedPreferenceUtil
  abstract val pageDao: PageDao
  abstract val state: MutableLiveData<PageState>
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  val compositeDisposable = CompositeDisposable()

  fun viewStateReducer(): Disposable =
    actions.map { reduce(it, state.value!!) }
      .subscribe(state::postValue, Throwable::printStackTrace)

  private fun reduce(action: Action, state: PageState): PageState = when (action) {
    Exit -> finishActivity(state)
    ExitActionModeMenu -> deselectAllPages(state)
    UserClickedDeleteButton, UserClickedDeleteSelectedPages -> offerShowDeleteDialog(state)
    is UserClickedShowAllToggle -> offerUpdateToShowAllToggle(action, state)
    is OnItemClick -> handleItemClick(state, action)
    is OnItemLongClick -> handleItemLongClick(state, action)
    is Filter -> updatePagesBasedOnFilter(state, action)
    is UpdatePages -> updateBookmarks(state, action)
  }

  abstract fun updatePagesBasedOnFilter(state: PageState, action: Filter): PageState

  abstract fun updateBookmarks(state: PageState, action: UpdatePages): PageState

  abstract fun offerUpdateToShowAllToggle(
    action: UserClickedShowAllToggle,
    state: PageState
  ): PageState

  private fun handleItemLongClick(state: PageState, action: OnItemLongClick): PageState =
    state.toggleSelectionOfItem(action.page)

  abstract fun handleItemClick(state: PageState, action: OnItemClick): PageState

  abstract fun offerShowDeleteDialog(state: PageState): PageState

  abstract fun deselectAllPages(state: PageState): PageState

  private fun finishActivity(state: PageState): PageState {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}
