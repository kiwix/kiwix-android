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
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.PageDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Exit
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteSelectedPages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.OpenPage
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

abstract class PageViewModel<T : Page>(
  protected val pageDao: PageDao
) : ViewModel() {

  abstract val zimReaderContainer: ZimReaderContainer
  abstract val sharedPreferenceUtil: SharedPreferenceUtil
  abstract val state: MutableLiveData<PageState<T>>

  val compositeDisposable = CompositeDisposable()
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()

  private fun viewStateReducer(): Disposable =
    actions.map { reduce(it, state.value!!) }
      .subscribe(state::postValue, Throwable::printStackTrace)

  protected fun addDisposablesToCompositeDisposable() {
    compositeDisposable.addAll(
      viewStateReducer(),
      pageDao.pages().subscribeOn(Schedulers.io())
        .subscribe({ actions.offer(UpdatePages(it)) }, Throwable::printStackTrace)
    )
  }

  private fun reduce(action: Action, state: PageState<T>): PageState<T> = when (action) {
    Exit -> finishActivity(state)
    ExitActionModeMenu -> deselectAllPages(state)
    UserClickedDeleteButton, UserClickedDeleteSelectedPages -> offerShowDeleteDialog(state)
    is UserClickedShowAllToggle -> offerUpdateToShowAllToggle(action, state)
    is OnItemClick -> handleItemClick(state, action)
    is OnItemLongClick -> handleItemLongClick(state, action)
    is Filter -> updatePagesBasedOnFilter(state, action)
    is UpdatePages -> updatePages(state, action)
  }

  abstract fun updatePagesBasedOnFilter(state: PageState<T>, action: Filter): PageState<T>

  abstract fun updatePages(state: PageState<T>, action: UpdatePages): PageState<T>

  abstract fun offerUpdateToShowAllToggle(
    action: UserClickedShowAllToggle,
    state: PageState<T>
  ): PageState<T>

  private fun offerShowDeleteDialog(state: PageState<T>): PageState<T> {
    effects.offer(createDeletePageDialogEffect(state))
    return state
  }

  private fun handleItemLongClick(state: PageState<T>, action: OnItemLongClick): PageState<T> =
    state.toggleSelectionOfItem(action.page)

  private fun handleItemClick(state: PageState<T>, action: Action.OnItemClick): PageState<T> {
    if (state.isInSelectionState) {
      return state.toggleSelectionOfItem(action.page)
    }
    effects.offer(OpenPage(action.page, zimReaderContainer))
    return state
  }

  abstract fun deselectAllPages(state: PageState<T>): PageState<T>

  private fun finishActivity(state: PageState<T>): PageState<T> {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  abstract fun createDeletePageDialogEffect(state: PageState<T>): SideEffect<*>
}
