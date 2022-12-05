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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchRoomDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.CreatedWithArguments
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ExitedSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnOpenInNewTabClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ScreenWasStartedFrom
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.StartSpeechInputFailed
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.DeleteRecentSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.OpenSearchItem
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.PopFragmentBackstack
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ProcessActivityResult
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SaveSearchToRecents
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchArgumentProcessing
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchInPreviousScreen
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowDeleteSearchDialog
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowToast
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.StartSpeechInput
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
  private val recentSearchDao: NewRecentSearchRoomDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val searchResultGenerator: SearchResultGenerator
) : ViewModel() {

  private val initialState: SearchState =
    SearchState("", SearchResultsWithTerm("", emptyList()), emptyList(), FromWebView)
  val state: MutableStateFlow<SearchState> = MutableStateFlow(initialState)
  private val _effects = Channel<SideEffect<*>>()
  val effects = _effects.receiveAsFlow()
  val actions = Channel<Action>(Channel.UNLIMITED)
  private val filter = ConflatedBroadcastChannel("")
  private val searchOrigin = ConflatedBroadcastChannel(FromWebView)

  init {
    viewModelScope.launch { reducer() }
    viewModelScope.launch { actionMapper() }
  }

  private suspend fun reducer() {
    combine(
      filter.asFlow(),
      searchResults(),
      recentSearchDao.recentSearches(zimReaderContainer.id),
      searchOrigin.asFlow()
    ) { searchTerm, searchResultsWithTerm, recentResults, searchOrigin ->
      SearchState(
        searchTerm, searchResultsWithTerm,
        recentResults as List<SearchListItem.RecentSearchListItem>, searchOrigin
      )
    }
      .collect { state.value = it }
  }

  private fun searchResults() = filter.asFlow()
    .mapLatest {
      val zimFileReader = zimReaderContainer.copyReader()
      try {
        SearchResultsWithTerm(it, searchResultGenerator.generateSearchResults(it, zimFileReader))
      } finally {
        zimFileReader?.dispose()
      }
    }

  private suspend fun actionMapper() = actions.consumeEach {
    when (it) {
      ExitedSearch -> _effects.trySend(PopFragmentBackstack).isSuccess
      is OnItemClick -> saveSearchAndOpenItem(it.searchListItem, false)
      is OnOpenInNewTabClick -> saveSearchAndOpenItem(it.searchListItem, true)
      is OnItemLongClick -> showDeleteDialog(it)
      is Filter -> filter.trySendBlocking(it.term)
      ClickedSearchInText -> searchPreviousScreenWhenStateIsValid()
      is ConfirmedDelete -> deleteItemAndShowToast(it)
      is CreatedWithArguments -> _effects.trySend(
        SearchArgumentProcessing(
          it.arguments,
          actions
        )
      ).isSuccess
      ReceivedPromptForSpeechInput -> _effects.trySend(StartSpeechInput(actions)).isSuccess
      StartSpeechInputFailed -> _effects.trySend(ShowToast(R.string.speech_not_supported)).isSuccess
      is ActivityResultReceived ->
        _effects.trySend(
          ProcessActivityResult(
            it.requestCode,
            it.resultCode,
            it.data,
            actions
          )
        ).isSuccess
      is ScreenWasStartedFrom -> searchOrigin.trySendBlocking(it.searchOrigin)
    }
  }

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
    _effects.trySend(DeleteRecentSearch(it.searchListItem, recentSearchDao)).isSuccess
    _effects.trySend(ShowToast(R.string.delete_specific_search_toast)).isSuccess
  }

  private fun searchPreviousScreenWhenStateIsValid(): Any =
    _effects.trySend(SearchInPreviousScreen(state.value.searchTerm)).isSuccess

  private fun showDeleteDialog(longClick: OnItemLongClick) {
    _effects.trySend(ShowDeleteSearchDialog(longClick.searchListItem, actions)).isSuccess
  }

  private fun saveSearchAndOpenItem(searchListItem: SearchListItem, openInNewTab: Boolean) {
    _effects.trySend(
      SaveSearchToRecents(
        recentSearchDao,
        searchListItem,
        zimReaderContainer.id
      )
    ).isSuccess
    _effects.trySendBlocking(OpenSearchItem(searchListItem, openInNewTab))
  }
}

data class SearchResultsWithTerm(val searchTerm: String, val results: List<SearchListItem>)
