/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.download

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState.Failed
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import org.kiwix.kiwixmobile.custom.download.Action.DatabaseEmission
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
import org.kiwix.kiwixmobile.custom.download.effects.DownloadCustom
import org.kiwix.kiwixmobile.custom.download.effects.NavigateToCustomReader
import org.kiwix.kiwixmobile.custom.download.effects.SetPreferredStorageWithMostSpace
import javax.inject.Inject

class CustomDownloadViewModel @Inject constructor(
  downloadRoomDao: DownloadRoomDao,
  setPreferredStorageWithMostSpace: SetPreferredStorageWithMostSpace,
  private val downloadCustom: DownloadCustom,
  private val navigateToCustomReader: NavigateToCustomReader
) : ViewModel() {
  private val _state = MutableStateFlow<State>(DownloadRequired)
  val state: StateFlow<State> = _state.asStateFlow()
  val actions = MutableSharedFlow<Action>(Channel.UNLIMITED)
  private val _effects = MutableSharedFlow<SideEffect<*>>(replay = 0)
  val effects: Flow<SideEffect<*>> = _effects
    .onStart { emit(setPreferredStorageWithMostSpace) }

  init {
    observeActions()
    observeDownloads(downloadRoomDao)
  }

  @VisibleForTesting
  fun getStateForTesting() = _state

  private fun observeActions() {
    viewModelScope.launch {
      actions
        .collect { action ->
          val currentState = _state.value
          val newState = reduce(action, currentState)
          if (newState != currentState) {
            _state.value = newState
          }
        }
    }
  }

  private fun observeDownloads(
    downloadRoomDao: DownloadRoomDao,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) {
    viewModelScope.launch {
      downloadRoomDao.downloads()
        .map { it.map(::DownloadItem) }
        .flowOn(dispatcher)
        .collect { downloads ->
          actions.emit(DatabaseEmission(downloads))
        }
    }
  }

  private suspend fun reduce(action: Action, state: State): State {
    return when (action) {
      is DatabaseEmission -> reduceDatabaseEmission(state, action)
      ClickedRetry,
      ClickedDownload -> state.also { _effects.emit(downloadCustom) }
    }
  }

  private suspend fun reduceDatabaseEmission(state: State, action: DatabaseEmission) =
    when (state) {
      is DownloadFailed,
      DownloadRequired ->
        if (action.downloads.isNotEmpty()) {
          DownloadInProgress(action.downloads)
        } else {
          state
        }

      is DownloadInProgress ->
        if (action.downloads.isNotEmpty()) {
          if (action.downloads[0].downloadState is Failed) {
            DownloadFailed(action.downloads[0].downloadState)
          } else {
            DownloadInProgress(action.downloads)
          }
        } else {
          DownloadComplete.also { _effects.emit(navigateToCustomReader) }
        }

      DownloadComplete -> state
    }
}
