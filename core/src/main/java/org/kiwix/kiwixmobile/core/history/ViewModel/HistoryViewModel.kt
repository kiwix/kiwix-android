package org.kiwix.kiwixmobile.core.history.ViewModel

import Finish
import ProcessActivityResult
import SearchIntentProcessing
import ShowToast
import StartSpeechInput
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
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
import javax.inject.Inject

class HistoryViewModel @Inject constructor() : ViewModel(){
  val state = MutableLiveData<State>().apply{ value = NoResults("")}
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(viwStateRduce(),actionMapper())
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun actionMapper() = actions.map {
    when (it) {
      ExitedSearch -> effects.offer(Finish)
      is OnItemClick -> saveSearchAndOpenItem(it)
      is OnItemLongClick -> showDeleteDialog(it)
      is Filter -> filter.offer(it.term)
      ClickedSearchInText -> searchPreviousScreenWhenStateIsValid()
      is ConfirmedDelete -> deleteItemAndShowToast(it)
      is CreatedWithIntent -> effects.offer(SearchIntentProcessing(it.intent, actions))
      ReceivedPromptForSpeechInput -> effects.offer(StartSpeechInput(actions))
      StartSpeechInputFailed -> effects.offer(ShowToast(R.string.speech_not_supported))
      is ActivityResultReceived ->
        effects.offer(ProcessActivityResult(it.requestCode, it.resultCode, it.data, actions))
    }
  }.subscribe(
    {},
    Throwable::printStackTrace
  )

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
    effects.offer(DeleteRecentSearch(it.searchListItem, recentSearchDao))
    effects.offer(ShowToast(R.string.delete_specific_search_toast))
  }

  private fun searchPreviousScreenWhenStateIsValid(): Any =
    effects.offer(SearchInPreviousScreen(state.value!!.searchString))

  private fun showDeleteDialog(longClick: OnItemLongClick) {
    effects.offer(ShowDeleteSearchDialog(longClick.searchListItem, actions))
  }

  private fun saveSearchAndOpenItem(it: OnItemClick) {
    effects.offer(
      SaveSearchToRecents(recentSearchDao, it.searchListItem, zimReaderContainer.id)
    )
    effects.offer(
      OpenSearchItem(it.searchListItem)
    )
  }

  private fun viewStateReducer() =
    Flowable.combineLatest(
      recentSearchDao.recentSearches(zimReaderContainer.id),
      searchResultsFromZimReader(),
      filter,
      searchOrigin,
      Function4(this::reduce)
    ).subscribe(state::postValue, Throwable::printStackTrace)

  private fun reduce(
    recentSearchResults: List<SearchListItem>,
    zimSearchResults: List<SearchListItem>,
    searchString: String,
    searchOrigin: SearchOrigin
  ) = when {
    searchString.isNotEmpty() && zimSearchResults.isNotEmpty() ->
      Results(searchString, zimSearchResults, searchOrigin)
    searchString.isEmpty() && recentSearchResults.isNotEmpty() ->
      Results(searchString, recentSearchResults, searchOrigin)
    else -> NoResults(searchString, searchOrigin)
  }

  private fun searchResultsFromZimReader() = filter
    .distinctUntilChanged()
    .switchMap(::searchResults)

  private fun searchResults(it: String) = Flowable.fromCallable {
    searchResultGenerator.generateSearchResults(it)
  }.subscribeOn(Schedulers.io())
}
