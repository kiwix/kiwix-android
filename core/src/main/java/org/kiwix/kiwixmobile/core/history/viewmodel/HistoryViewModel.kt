package org.kiwix.kiwixmobile.core.history.viewmodel

import Finish
import OpenHistoryItem
import ShowToast
import StartSpeechInput
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tonyodev.fetch2core.Func2
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.CreatedWithIntent
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.StartSpeechInputFailed
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val zimReaderContainer: ZimReaderContainer
) : ViewModel() {
  val state = MutableLiveData<State>().apply { value = NoResults("") }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val compositeDisposable = CompositeDisposable()
  private val currentBook = BehaviorProcessor.createDefault("")
  private val showAllSwitchToggle = BehaviorProcessor.createDefault(false)
  private val inSelectionMode = BehaviorProcessor.createDefault(false)
  private val searchResults: BehaviorProcessor<List<HistoryListItem>> =
    BehaviorProcessor.createDefault(listOf())
  private val selectedHistoryItems = ArrayList<HistoryListItem>()

  init {
    compositeDisposable.addAll(viewStateReducer(), actionMapper())
  }

  private fun viewStateReducer() = Flowable.combineLatest(
      currentBook,
      searchResults(),
      filter,
      showAllSwitchToggle,
      Function4(::reduce)
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
      .filter { h -> h.historyTitle.contains(searchString, true) }

  private fun reduce(
    currentBook: String,
    historyBookResults: List<HistoryListItem>,
    searchString: String,
    showAllSwitchOn: Boolean
  ): State = when {
    selectedHistoryItems.isNotEmpty() ->
      SelectionResults(
        searchString,
        historyBookResults,
        selectedHistoryItems,
        showAllSwitchOn,
        currentBook
      )
    historyBookResults.isNotEmpty() ->
      Results(searchString, historyBookResults, showAllSwitchOn, currentBook)
    else ->
      NoResults(searchString)
  }

//  private fun ShowAllSwitchToggled()= filter.distinctUntilChanged().switchMap {  }
//  private fun UserClickedItem() = effects.offer(UserClickedItem())
//
//  private fun history() = filter.distinctUntilChanged().switchMap(::)

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun actionMapper() = actions.map {
      when (it) {
        ExitHistory -> effects.offer(Finish)
        is Filter -> filter.offer(it.searchTerm)
        is CreatedWithIntent -> filter.offer(it.searchTerm)
        is ConfirmedDelete -> deleteItemAndShowToast(it)
        is OnItemLongClick -> selectItemAndOpenSelectionMode(it)
        is OnItemClick -> appendItemToSelectionOrOpenIt(it)
        ReceivedPromptForSpeechInput -> effects.offer(StartSpeechInput(actions))
        StartSpeechInputFailed -> effects.offer(ShowToast(R.string.speech_not_supported))
        else -> {
        }
      }
    }
    .subscribe(
      {},
      Throwable::printStackTrace
    )

  private fun selectItemAndOpenSelectionMode(onItemLongClick: Action.OnItemLongClick) {
  }

  private fun appendItemToSelectionOrOpenIt(onItemClick: Action.OnItemClick) {
    Log.d("HistoryViewModel", "appendItemToSelectionOrOpenIt")
    when (inSelectionMode.value) {
      true -> Log.d("HistoryViewModel", "appendToSelection")
      false -> effects.offer(
        OpenHistoryItem(onItemClick.historyListItem as HistoryItem, zimReaderContainer)
      )
    }
  }

  private fun deleteItemsAndShowToast(it: ConfirmedDelete) {
    effects.offer(ShowToast(R.string.delete_specific_search_toast))
  }

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
  }
}
