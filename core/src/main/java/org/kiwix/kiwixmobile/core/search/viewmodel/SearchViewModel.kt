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

package org.kiwix.kiwixmobile.core.search.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.CreatedWithIntent
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ExitedSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.StartSpechInputFailed
import org.kiwix.kiwixmobile.core.search.viewmodel.State.Empty
import org.kiwix.kiwixmobile.core.search.viewmodel.State.Initialising
import org.kiwix.kiwixmobile.core.search.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.DeleteRecentSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.OpenSearchItem
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ProcessActivityResult
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SaveSearchToRecents
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchInPreviousScreen
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchIntentProcessing
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowDeleteSearchDialog
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowToast
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.StartSpeechInput
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class SearchViewModel @Inject constructor(
  val recentSearchDao: NewRecentSearchDao,
  val zimReaderContainer: ZimReaderContainer,
  private val searchResultGenerator: SearchResultGenerator
) : ViewModel() {

  val state = MutableLiveData<State>().apply { value = Initialising }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")

  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(
      viewStateReducer(),
      actionMapper()
    )
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
      ClickedSearchInText -> searchPreviousScreenhenStateIsValid()
      is ConfirmedDelete -> deleteItemAndShowToast(it)
      is CreatedWithIntent -> effects.offer(SearchIntentProcessing(it.intent, actions))
      ReceivedPromptForSpeechInput -> effects.offer(StartSpeechInput(actions))
      StartSpechInputFailed -> effects.offer(ShowToast(string.speech_not_supported))
      is ActivityResultReceived ->
        effects.offer(ProcessActivityResult(it.requestCode, it.resultCode, it.data, actions))
    }
  }.subscribe(
    {},
    Throwable::printStackTrace
  )

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
    effects.offer(DeleteRecentSearch(it.searchListItem, recentSearchDao))
    effects.offer(ShowToast(string.delete_specific_search_toast))
  }

  private fun searchPreviousScreenhenStateIsValid(): Any =
    when (val currentState = state.value) {
      is Results -> effects.offer(SearchInPreviousScreen(currentState.searchString))
      else -> Unit
    }

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
      Function3(this::reduce)
    ).debounce(100, MILLISECONDS)
      .subscribe(state::postValue, Throwable::printStackTrace)

  private fun reduce(
    recentSearchResults: List<SearchListItem>,
    zimSearchResults: List<SearchListItem>,
    searchString: String
  ) = when {
    searchString.isNotEmpty() && zimSearchResults.isNotEmpty() ->
      Results(searchString, zimSearchResults)
    searchString.isEmpty() && recentSearchResults.isNotEmpty() ->
      Results(searchString, recentSearchResults)
    else -> Empty
  }

  private fun searchResultsFromZimReader() = filter
    .distinctUntilChanged()
    .switchMap(::searchResults)

  private fun searchResults(it: String) = Flowable.fromCallable {
    searchResultGenerator.generateSearchResults(it)
  }.subscribeOn(Schedulers.io())
}
