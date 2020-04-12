package org.kiwix.kiwixmobile.core.history.ViewModel

import DeleteRecentSearch
import Finish
import OpenSearchItem
import ProcessActivityResult
import SearchInPreviousScreen
import SearchIntentProcessing
import ShowToast
import StartSpeechInput
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.CreatedWithIntent
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.ExitedSearch
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.Filter
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.history.ViewModel.Action.StartSpeechInputFailed
import org.kiwix.kiwixmobile.core.history.ViewModel.State.NoResults
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchResultGenerator
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val searchResultGenerator: SearchResultGenerator
) : ViewModel(){
  val state = MutableLiveData<State>().apply{ value = NoResults("")}
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val compositeDisposable = CompositeDisposable()

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun actionMapper() = actions.map {
    when (it) {
      ExitedSearch -> effects.offer(Finish)
      is Filter -> filter.offer(it.term)
      ClickedSearchInText -> searchPreviousScreenWhenStateIsValid()
      is ConfirmedDelete -> deleteItemAndShowToast(it)
      is CreatedWithIntent -> effects.offer(SearchIntentProcessing(it.intent, actions))
      ReceivedPromptForSpeechInput -> effects.offer(StartSpeechInput(actions))
      StartSpeechInputFailed -> effects.offer(ShowToast(R.string.speech_not_supported))
      is ActivityResultReceived ->
        effects.offer(ProcessActivityResult(it.requestCode, it.resultCode, it.data, actions))
      else ->  {}
    }
  }.subscribe(
    {},
    Throwable::printStackTrace
  )

  private fun deleteItemsAndShowToast(it: ConfirmedDelete) {
    effects.offer(DeleteRecentSearch(it.historyListItems, historyDao))
    effects.offer(ShowToast(R.string.delete_specific_search_toast))

  }

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
  }

  private fun searchPreviousScreenWhenStateIsValid(): Any =
    effects.offer(SearchInPreviousScreen(state.value!!.searchString))

  private fun searchResults(it: String) = Flowable.fromCallable {
    searchResultGenerator.generateSearchResults(it)
  }.subscribeOn(Schedulers.io())
}
