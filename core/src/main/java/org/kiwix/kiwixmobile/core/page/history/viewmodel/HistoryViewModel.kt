package org.kiwix.kiwixmobile.core.page.history.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Exit
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteSelectedPages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.OpenPage
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : PageViewModel, ViewModel() {
  override val state = MutableLiveData<PageState>().apply {
    value =
      HistoryState(emptyList(), sharedPreferenceUtil.showHistoryAllBooks, zimReaderContainer.id)
  }
  override val effects = PublishProcessor.create<SideEffect<*>>()
  override val actions = PublishProcessor.create<Action>()
  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(
      viewStateReducer(),
      historyDao.history().subscribeOn(Schedulers.io())
        .subscribe({ actions.offer(UpdatePages(it)) }, Throwable::printStackTrace)
    )
  }

  private fun viewStateReducer() =
    actions.map { reduce(it, state.value!! as HistoryState) }
      .subscribe(state::postValue, Throwable::printStackTrace)

  private fun reduce(action: Action, state: HistoryState): HistoryState = when (action) {
    Exit -> finishHistoryActivity(state)
    ExitActionModeMenu -> deselectAllHistoryItems(state)
    UserClickedDeleteButton, UserClickedDeleteSelectedPages -> offerShowDeleteDialog(state)
    is UserClickedShowAllToggle -> offerUpdateToShowAllToggle(action, state)
    is OnItemClick -> handleItemClick(state, action)
    is OnItemLongClick -> handleItemLongClick(state, action)
    is Filter -> updateHistoryItemsBasedOnFilter(state, action)
    is UpdatePages -> updateHistoryList(state, action)
  }

  private fun updateHistoryItemsBasedOnFilter(state: HistoryState, action: Filter) =
    state.copy(searchTerm = action.searchTerm)

  private fun updateHistoryList(state: HistoryState, action: UpdatePages): HistoryState =
    state.copy(pageItems = action.pages.filterIsInstance<HistoryItem>())

  private fun offerUpdateToShowAllToggle(
    action: UserClickedShowAllToggle,
    state: HistoryState
  ): HistoryState {
    effects.offer(UpdateAllHistoryPreference(sharedPreferenceUtil, action.isChecked))
    return state.copy(showAll = action.isChecked)
  }

  private fun handleItemLongClick(state: HistoryState, action: OnItemLongClick): HistoryState =
    state.toggleSelectionOfItem(action.page as HistoryItem)

  private fun handleItemClick(state: HistoryState, action: OnItemClick): HistoryState {
    if (state.isInSelectionState) {
      return state.toggleSelectionOfItem(action.page as HistoryItem)
    }
    effects.offer(OpenPage(action.page, zimReaderContainer))
    return state
  }

  private fun offerShowDeleteDialog(state: HistoryState): HistoryState {
    effects.offer(ShowDeleteHistoryDialog(effects, state, historyDao))
    return state
  }

  private fun deselectAllHistoryItems(state: HistoryState): HistoryState =
    state.copy(pageItems = state.pageItems.map { it.copy(isSelected = false) })

  private fun finishHistoryActivity(state: HistoryState): HistoryState {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}
