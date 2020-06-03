package org.kiwix.kiwixmobile.core.history.viewmodel

import DeleteSelectedOrAllHistoryItems
import OpenHistoryItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.extensions.HeaderizableList
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.RequestDeleteAllHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.RequestDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ToggleShowHistoryFromAllBooks
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ToggleShowAllHistorySwitchAndSaveItsStateToPrefs
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.KiwixDialog.DeleteAllHistory
import org.kiwix.kiwixmobile.core.utils.KiwixDialog.DeleteSelectedHistory
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {
  val state = MutableLiveData<State>().apply { value = NoResults(emptyList()) }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val compositeDisposable = CompositeDisposable()
  private val showAllSwitchToggle =
    BehaviorProcessor.createDefault(!sharedPreferenceUtil.showHistoryCurrentBook)
  private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
  private val deselectAllItems = BehaviorProcessor.createDefault(false)

  init {
    compositeDisposable.addAll(
      viewStateReducer(),
      actionMapper(),
      deselectAllItems()
    )
  }

  private fun deselectAllItems() = deselectAllItems.subscribe {
    state.value?.historyItems?.filterIsInstance<HistoryItem>()?.forEach { it.isSelected = false }
  }

  private fun viewStateReducer() =
    Flowable.combineLatest(
      filter,
      showAllSwitchToggle,
      historyDao.history(),
      Function3(::searchResults)
    ).subscribe { state.postValue(updateResultsState(it)) }

  private fun updateResultsState(
    historyListWithDateItems: List<HistoryListItem>
  ): State {
    return when {
      historyListWithDateItems.isEmpty() -> NoResults(historyListWithDateItems)
      historyListWithDateItems.filterIsInstance<HistoryItem>()
        .any { it.isSelected } -> SelectionResults(
          historyListWithDateItems
      )
      else -> Results(historyListWithDateItems)
    }
  }

  private fun searchResults(
    searchString: String,
    showAllToggle: Boolean,
    historyList: List<HistoryListItem>
  ): List<HistoryListItem> = HeaderizableList(historyList
      .filterIsInstance<HistoryItem>()
      .filter { h ->
        h.historyTitle.contains(searchString, true) &&
          (h.zimName == zimReaderContainer.name || showAllToggle)
      }
      .sortedByDescending { dateFormatter.parse(it.dateString) } as List<HistoryListItem>)
      .foldOverAddingHeaders(
        { historyItem -> DateItem((historyItem as HistoryItem).dateString) },
        { current, next -> (current as HistoryItem).dateString != (next as HistoryItem).dateString }
      )

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun toggleSelectionOfItem(historyItem: HistoryItem): State =
    when (state.value) {
      is Results -> SelectionResults(historyItemsWithToggledSelectedItem(historyItem))
      is SelectionResults -> {
        if (historyItemsWithToggledSelectedItem(historyItem)
            ?.filterIsInstance<HistoryItem>()
            ?.any { it.isSelected } == true) {
          SelectionResults(historyItemsWithToggledSelectedItem(historyItem))
        } else {
          Results(historyItemsWithToggledSelectedItem(historyItem))
        }
      }
      is NoResults -> NoResults(emptyList())
      null -> NoResults(emptyList())
    }

  private fun historyItemsWithToggledSelectedItem(historyItem: HistoryItem):
    List<HistoryListItem>? {
    return state.value
      ?.historyItems
      ?.map {
        if (it.id == historyItem.id && it is HistoryItem)
          it.copy(isSelected = !it.isSelected) else it
      }
  }

  private fun actionMapper() = actions.map {
    when (it) {
      ExitHistory -> effects.offer(Finish)
      is Filter -> filter.offer(it.searchTerm)
      is ToggleShowHistoryFromAllBooks ->
        toggleShowAllHistorySwitchAndSaveItsStateToPrefs(it.isChecked)
      is OnItemLongClick -> state.postValue(toggleSelectionOfItem(it.historyItem))
      is OnItemClick -> appendItemToSelectionOrOpenIt(it)
      is RequestDeleteAllHistoryItems ->
        effects.offer(ShowDeleteHistoryDialog(actions, DeleteAllHistory))
      is RequestDeleteSelectedHistoryItems ->
        effects.offer(ShowDeleteHistoryDialog(actions, DeleteSelectedHistory))
      ExitActionModeMenu -> state.postValue(Results(
        state.value
          ?.historyItems
          ?.map { if (it is HistoryItem) it.copy(isSelected = false) else it })
      )
      DeleteHistoryItems -> effects.offer(DeleteSelectedOrAllHistoryItems(state, historyDao))
    }
  }.subscribe({}, Throwable::printStackTrace)

  private fun toggleShowAllHistorySwitchAndSaveItsStateToPrefs(isChecked: Boolean) {
    effects.offer(
      ToggleShowAllHistorySwitchAndSaveItsStateToPrefs(
        showAllSwitchToggle,
        sharedPreferenceUtil,
        isChecked
      )
    )
  }

  private fun appendItemToSelectionOrOpenIt(onItemClick: OnItemClick) {
    val historyItem = onItemClick.historyListItem as HistoryItem
    if (state.value?.containsSelectedItems() == true) {
      state.postValue(toggleSelectionOfItem(historyItem))
    } else {
      effects.offer(OpenHistoryItem(historyItem, zimReaderContainer))
    }
  }
}
