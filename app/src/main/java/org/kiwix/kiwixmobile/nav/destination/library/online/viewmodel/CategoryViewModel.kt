/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityObserver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.offlineMessage
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveCategories
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.CategoryItem
import javax.inject.Inject

@HiltViewModel
open class CategoryViewModel @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  private val observeCategories: ObserveCategories,
  private val connectivityObserver: ConnectivityObserver
) : ViewModel() {
  sealed class Action {
    data class UpdateCategory(val categories: List<Category>) : Action()
    data class Select(val category: CategoryItem) : Action()
    data class Error(val errorMessage: String) : Action()
  }

  val state = MutableStateFlow<State>(State.Loading)
  val actions = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)
  val effects = MutableSharedFlow<SideEffect<*>>(extraBufferCapacity = Int.MAX_VALUE)

  private val coroutineJobs = mutableListOf<Job>()

  init {
    connectivityObserver.register()
    coroutineJobs.apply {
      add(observeActions())
      add(observeCategories())
    }
  }

  private fun observeActions() =
    actions
      .map { action -> reduce(action, state.value) }
      .distinctUntilChanged()
      .onEach { newState -> state.value = newState }
      .launchIn(viewModelScope)

  private fun offlineMessage(actionResId: Int): String =
    connectivityObserver.offlineMessage(context, actionResId)

  private fun observeCategories() = viewModelScope.launch {
    state.value = State.Loading
    when (
      val result = observeCategories(
        errorNoCategory = context.getString(string.no_category_available),
        errorNoNetwork = offlineMessage(R.string.action_load_categories),
        isOnline = connectivityObserver.networkStates.value == NetworkState.CONNECTED
      )
    ) {
      is ObserveCategories.Result.Success ->
        actions.emit(Action.UpdateCategory(result.categories))

      is ObserveCategories.Result.Error ->
        actions.emit(Action.Error(result.message))
    }
  }

  private fun reduce(action: Action, currentState: State): State =
    when (action) {
      is Action.Error -> State.Error(action.errorMessage)
      is Action.UpdateCategory -> updateCategory(action, currentState)
      is Action.Select -> select(action, currentState)
    }

  private fun updateCategory(action: Action.UpdateCategory, currentState: State): State =
    if (currentState == State.Loading) State.Content(action.categories) else currentState

  private fun select(action: Action.Select, currentState: State): State =
    if (currentState is State.Content) {
      val newState = updateSelection(action.category, currentState)
      saveSelection(newState)
      newState
    } else {
      currentState
    }

  private fun updateSelection(
    categoryItem: CategoryItem,
    currentState: State.Content
  ) = currentState.select(categoryItem)

  private fun saveSelection(currentState: State.Content) {
    val selectedCategories = currentState.items.filter { it.active }
    effects.tryEmit(
      SaveCategory(
        selectedCategories,
        kiwixDataStore,
        viewModelScope
      )
    )
  }

  @VisibleForTesting
  fun onClearedExposed() {
    onCleared()
  }

  override fun onCleared() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    connectivityObserver.unregister()
    super.onCleared()
  }
}
