package org.kiwix.kiwixmobile.core.history.viewmodel

import OpenHistoryItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.AllHistoryPreferenceChanged
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UpdateHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedConfirmDelete
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {
  val state = MutableLiveData<State>().apply {
    value = Results(emptyList(), sharedPreferenceUtil.showHistoryAllBooks, zimReaderContainer.id)
  }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(
      historyDao.history().subscribe { actions.offer(UpdateHistory(it)) },
      sharedPreferenceUtil.showAllHistoryToggleSwitch.subscribe {
        actions.offer(AllHistoryPreferenceChanged(it))
      },
      viewStateReducer()
    )
  }

  private fun viewStateReducer() =
    actions.map { reduce(it, state.value!!) }.subscribe(state::postValue)

  private fun reduce(action: Action, state: State): State =
    when (action) {
      ExitHistory -> finishHistoryActivity(state)
      ExitActionModeMenu -> deselectAllHistoryItems(state)
      UserClickedConfirmDelete -> offerDeletionOfItems(state)
      UserClickedDeleteButton -> offerShowDeleteDialog(state)
      is OnItemClick -> handleItemClick(state, action)
      is OnItemLongClick -> handleItemLongClick(state, action)
      is UserClickedShowAllToggle -> offerUpdateToShowAllToggle(action, state)
      is Filter -> updateHistoryItemsBasedOnFilter(state, action)
      is UpdateHistory -> updateHistoryList(state, action)
      UserClickedDeleteSelectedHistoryItems -> offerShowDeleteDialog(state)
      is AllHistoryPreferenceChanged -> changeShowHistoryToggle(state, action)
    }

  private fun updateHistoryItemsBasedOnFilter(state: State, action: Filter) =
    when (state) {
      is Results -> state.copy(searchTerm = action.searchTerm)
      is SelectionResults -> state.copy(searchTerm = action.searchTerm)
    }

  private fun changeShowHistoryToggle(
    state: State,
    action: AllHistoryPreferenceChanged
  ): State {
    return when (state) {
      is SelectionResults -> state
      is Results -> state.copy(showAll = action.showAll)
    }
  }

  private fun updateHistoryList(
    state: State,
    action: UpdateHistory
  ): State = when (state) {
    is Results -> state.copy(historyItems = action.history)
    is SelectionResults -> Results(action.history, state.showAll, zimReaderContainer.id)
  }

  private fun offerUpdateToShowAllToggle(
    action: UserClickedShowAllToggle,
    state: State
  ): State {
    effects.offer(
      UpdateAllHistoryPreference(
        sharedPreferenceUtil,
        action.isChecked
      )
    )
    return when (state) {
      is Results -> state.copy(showAll = action.isChecked)
      else -> state
    }
  }

  private fun handleItemLongClick(
    state: State,
    action: OnItemLongClick
  ): State {
    return when (state) {
      is Results -> state.toggleSelectionOfItem(action.historyItem)
      else -> state
    }
  }

  private fun handleItemClick(
    state: State,
    action: OnItemClick
  ): State {
    return when (state) {
      is Results -> {
        effects.offer(OpenHistoryItem(action.historyItem, zimReaderContainer))
        state
      }
      is SelectionResults -> state.toggleSelectionOfItem(action.historyItem)
    }
  }

  private fun offerShowDeleteDialog(state: State): State {
    effects.offer(ShowDeleteHistoryDialog(actions))
    return state
  }

  private fun offerDeletionOfItems(state: State): State {
    return when (state) {
      is Results -> {
        effects.offer(DeleteHistoryItems(state.historyItems, historyDao))
        state
      }
      is SelectionResults -> {
        effects.offer(DeleteHistoryItems(state.selectedItems, historyDao))
        state
      }
    }
  }

  private fun deselectAllHistoryItems(state: State): State {
    return when (state) {
      is SelectionResults -> {
        Results(
          state.historyItems.map { it.copy(isSelected = false) },
          state.showAll,
          state.searchTerm
        )
      }
      else -> state
    }
  }

  private fun finishHistoryActivity(state: State): State {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}
