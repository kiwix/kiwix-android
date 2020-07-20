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
import io.reactivex.functions.Function4
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.R
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
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnOpenInNewTabClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ScreenWasStartedFrom
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.StartSpeechInputFailed
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.search.viewmodel.State.NoResults
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val DEBOUNCE_MS = 500L

class SearchViewModel @Inject constructor(
  private val recentSearchDao: NewRecentSearchDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val searchResultGenerator: SearchResultGenerator
) : ViewModel() {

  val state = MutableLiveData<State>().apply { value = NoResults("", FromWebView) }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val searchOrigin = BehaviorProcessor.createDefault(FromWebView)

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
      is OnItemClick -> saveSearchAndOpenItem(it.searchListItem, false)
      is OnOpenInNewTabClick -> saveSearchAndOpenItem(it.searchListItem, true)
      is OnItemLongClick -> showDeleteDialog(it)
      is Filter -> filter.offer(it.term)
      ClickedSearchInText -> searchPreviousScreenWhenStateIsValid()
      is ConfirmedDelete -> deleteItemAndShowToast(it)
      is CreatedWithIntent -> effects.offer(SearchIntentProcessing(it.intent, actions))
      ReceivedPromptForSpeechInput -> effects.offer(StartSpeechInput(actions))
      StartSpeechInputFailed -> effects.offer(ShowToast(R.string.speech_not_supported))
      is ActivityResultReceived ->
        effects.offer(ProcessActivityResult(it.requestCode, it.resultCode, it.data, actions))
      is ScreenWasStartedFrom -> searchOrigin.offer(it.searchOrigin)
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

  private fun saveSearchAndOpenItem(searchListItem: SearchListItem, openInNewTab: Boolean) {
    effects.offer(
      SaveSearchToRecents(recentSearchDao, searchListItem, zimReaderContainer.id)
    )
    effects.offer(
      OpenSearchItem(searchListItem, openInNewTab)
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
    .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
    .switchMap(::searchResults)

  private fun searchResults(it: String) = Flowable.fromCallable {
    searchResultGenerator.generateSearchResults(it)
  }.subscribeOn(Schedulers.io())
}
