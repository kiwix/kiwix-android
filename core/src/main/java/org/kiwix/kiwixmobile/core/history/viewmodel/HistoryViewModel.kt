package org.kiwix.kiwixmobile.core.history.viewmodel

import OpenHistoryItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function5
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.CreatedWithIntent
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ToggleShowHistoryFromAllBooks
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val zimReaderContainer: ZimReaderContainer
) : ViewModel() {
  val state = MutableLiveData<State>().apply { value = NoResults("", listOf()) }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val compositeDisposable = CompositeDisposable()
  private val currentBook = BehaviorProcessor.createDefault("")
  private val showAllSwitchToggle = BehaviorProcessor.createDefault(false)
  private val unselectAllItems = BehaviorProcessor.createDefault(false)
  private var isInSelectionMode = false

  init {
    compositeDisposable.addAll(viewStateReducer(), actionMapper(), updateSelectionMode())
  }

  private fun updateSelectionMode() =
    unselectAllItems.subscribe({ isInSelectionMode = it }, Throwable::printStackTrace)

  private fun viewStateReducer() = Flowable.combineLatest(
    currentBook,
    searchResults(),
    unselectAllItems,
    filter,
    showAllSwitchToggle,
    Function5(::updateResultsState)
  )
    .subscribe(state::postValue, Throwable::printStackTrace)

  private fun searchResults(): Flowable<List<HistoryListItem>> =
    Flowable.combineLatest(
      filter,
      showAllSwitchToggle,
      historyDao.history(),
      Function3(::searchResults)
    )

  private fun searchResults(
    searchString: String,
    showAllToggle: Boolean,
    historyList: List<HistoryListItem>
  ): List<HistoryListItem> =
    historyList.filterIsInstance<HistoryItem>()
      .filter { h ->
        h.historyTitle.contains(searchString, true) &&
          (h.zimName == zimReaderContainer.name || showAllToggle)
      }

  private fun updateResultsState(
    currentBook: String,
    historyItemSearchResults: List<HistoryListItem>,
    unselectAllItems: Boolean,
    searchString: String,
    showAllSwitchOn: Boolean
  ): State {
    if (unselectAllItems) {
      historyItemSearchResults.filterIsInstance<HistoryItem>().forEach { it.isSelected = false }
    }
    val selectedItems = historyItemSearchResults.filterIsInstance<HistoryItem>().filter { it.isSelected }
    if (selectedItems.isNotEmpty()) {
      return SelectionResults(
        searchString,
        historyItemSearchResults,
        selectedItems,
        showAllSwitchOn,
        currentBook
      )
    }
    return Results(searchString, historyItemSearchResults, showAllSwitchOn, currentBook)
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun actionMapper() = actions.map {
    when (it) {
      ExitHistory -> effects.offer(Finish)
      is Filter -> filter.offer(it.searchTerm)
      is ToggleShowHistoryFromAllBooks -> showAllSwitchToggle.offer(it.isChecked)
      is CreatedWithIntent -> filter.offer(it.searchTerm)
      is OnItemLongClick -> selectItemAndOpenSelectionMode(it.historyItem)
      is OnItemClick -> appendItemToSelectionOrOpenIt(it)
      is DeleteHistoryItems -> historyDao.deleteHistory(it.itemsToDelete)
      ExitActionModeMenu -> unselectAllItems.offer(true)
    }
  }.subscribe({}, Throwable::printStackTrace)

  private fun selectItemAndOpenSelectionMode(historyItem: HistoryItem) {
    historyItem.isSelected = true
    unselectAllItems.offer(false)
  }

  private fun isInSelctionMode(): Boolean {
    return state
      .value
      ?.historyItems
      ?.filterIsInstance<HistoryItem>()
      ?.any { it.isSelected } == true
  }

  private fun deselectAllHistoryItems() {
  }

  private fun appendItemToSelectionOrOpenIt(onItemClick: OnItemClick) {
    val historyItem = onItemClick.historyListItem as HistoryItem
    when {
      historyItem.isSelected -> {
        historyItem.isSelected = false
        unselectAllItems.offer(false)
      }
      isInSelctionMode() -> {
        historyItem.isSelected = true
        unselectAllItems.offer(false)
      }
      else -> {
        effects.offer(OpenHistoryItem(historyItem, zimReaderContainer))
      }
    }
  }
}
