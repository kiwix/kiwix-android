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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.CategoryItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action.Error
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action.Filter
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action.Select
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action.UpdateCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Content
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Saving
import javax.inject.Inject

class CategoryViewModel @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  @OPDSKiwixService private val kiwixService: KiwixService,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  @IoDispatcher val dispatcher: CoroutineDispatcher
) : ViewModel() {
  sealed class Action {
    data class UpdateCategory(val categories: List<Category>) : Action()
    data class Filter(val filter: String) : Action()
    data class Select(val category: CategoryItem) : Action()
    data class Error(val errorMessage: String) : Action()
    object Save : Action()
    object ClearAll : Action()
    object SelectAll : Action()
    object Cancel : Action()
  }

  val state = MutableStateFlow<State>(State.Loading)
  val actions = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)
  val effects = MutableSharedFlow<SideEffect<*>>(extraBufferCapacity = Int.MAX_VALUE)
  private var onDismiss: (() -> Unit)? = null

  private val coroutineJobs = mutableListOf<Job>()

  fun setOnDismissCallback(onDismiss: () -> Unit) {
    this.onDismiss = onDismiss
  }

  init {
    context.registerReceiver(connectivityBroadcastReceiver)
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

  private fun observeCategories() = viewModelScope.launch(dispatcher) {
    state.value = Loading

    val cachedCategoryList = kiwixDataStore.cachedOnlineCategoryList.first()
    val isOnline = connectivityBroadcastReceiver.networkStates.value == NetworkState.CONNECTED
    if (CategorySessionCache.hasFetched && !cachedCategoryList.isNullOrEmpty()) {
      val selectedCategoriesSet = kiwixDataStore.selectedOnlineContentCategory.first()
        .split(",")
        .asSequence()
        .filter { it.isNotEmpty() }
        .toSet()
      val updatedCategories = cachedCategoryList.map { category ->
        category.copy(
          active = if (category.id == 0L) {
            selectedCategoriesSet.isEmpty()
          } else {
            category.category in selectedCategoriesSet
          }
        )
      }
      actions.emit(Action.UpdateCategory(updatedCategories))
      return@launch
    }

    if (isOnline) {
      fetchCategoriesFlow().collect { categories ->
        if (categories.isNotEmpty()) {
          kiwixDataStore.saveOnlineCategoryList(categories)
          CategorySessionCache.hasFetched = true
          actions.emit(Action.UpdateCategory(categories))
        } else {
          emitCachedCategories(cachedCategoryList, true)
        }
      }
      return@launch
    }

    emitCachedCategories(cachedCategoryList, false)
  }

  private suspend fun emitCachedCategories(cachedCategoryList: List<Category>?, isOnline: Boolean) {
    if (!cachedCategoryList.isNullOrEmpty()) {
      actions.emit(Action.UpdateCategory(cachedCategoryList))
    } else {
      val errorMessage = if (isOnline) {
        context.getString(string.no_category_available)
      } else {
        context.getString(R.string.no_network_connection)
      }
      actions.emit(Action.Error(errorMessage))
    }
  }

  @Suppress("MagicNumber")
  private fun fetchCategoriesFlow() = flow {
    val feed = kiwixService.getCategories()
    val selectedCategoriesRaw = kiwixDataStore.selectedOnlineContentCategory.first()
    val selectedCategories = selectedCategoriesRaw
      .split(",")
      .asSequence()
      .filter { it.isNotEmpty() }
      .toSet()
    val categories = feed.entries.orEmpty().mapIndexed { index, entry ->
      Category(
        category = entry.title,
        active = entry.title in selectedCategories,
        id = (index + ONE).toLong()
      )
    }

    val categoryList =
      when {
        categories.isEmpty() -> emptyList()
        else -> buildList {
          add(
            Category(
              category = "",
              active = selectedCategoriesRaw.isEmpty(),
              id = ZERO.toLong()
            )
          )
          addAll(categories)
        }
      }
    emit(categoryList)
  }.retry(5)
    .catch { e ->
      e.printStackTrace()
      emit(emptyList())
    }

  private fun reduce(action: Action, currentState: State): State {
    return when (action) {
      is Action.Error -> State.Error(action.errorMessage)
      is Action.UpdateCategory -> updateCategory(action, currentState)
      is Action.Filter -> filter(action, currentState)
      is Action.Select -> select(action, currentState)
      Action.Save -> saveAction(currentState)
      Action.ClearAll -> clearAll(currentState)
      Action.SelectAll -> selectAll(currentState)
      Action.Cancel -> cancel(currentState)
    }
  }

  private fun updateCategory(action: Action.UpdateCategory, currentState: State): State =
    if (currentState is State.Loading) State.Content(action.categories) else currentState

  private fun filter(action: Action.Filter, currentState: State): State =
    if (currentState is State.Content) filterContent(action.filter, currentState) else currentState

  private fun select(action: Action.Select, currentState: State): State =
    if (currentState is State.Content) updateSelection(action.category, currentState) else currentState

  private fun saveAction(currentState: State): State =
    if (currentState is State.Content) save(currentState) else currentState

  private fun filterContent(
    filter: String,
    currentState: State.Content
  ) = currentState.updateFilter(filter)

  private fun updateSelection(
    categoryItem: CategoryItem,
    currentState: State.Content
  ) = currentState.select(categoryItem)

  private fun save(currentState: State.Content): State {
    val selectedCategories = currentState.items.filter { it.active }
    effects.tryEmit(
      SaveCategoryAndFinish(
        selectedCategories,
        kiwixDataStore,
        viewModelScope,
        requireOnDismissCallBack()
      )
    )
    return Saving(currentState)
  }

  private fun requireOnDismissCallBack() = requireNotNull(onDismiss) {
    "onDismiss callback is not set. " +
      "Set CategoryViewModel.setOnDismissCallback() before using the callback"
  }

  fun onDialogOpened() {
    val current = state.value
    if (current is Saving) {
      state.value = current.items
    }
  }

  private fun clearAll(currentState: State): State =
    if (currentState is State.Content) {
      currentState.copy(items = currentState.items.map { it.copy(active = false) })
    } else {
      currentState
    }

  private fun selectAll(currentState: State): State =
    if (currentState is State.Content) {
      currentState.copy(items = currentState.items.map { it.copy(active = it.id != 0L) })
    } else {
      currentState
    }

  private fun cancel(currentState: State): State {
    if (currentState !is State.Content) return currentState
    effects.tryEmit(object : SideEffect<Unit> {
      override fun invokeWith(activity: AppCompatActivity) {
        activity.onBackPressedDispatcher.onBackPressed()
      }
    })
    return currentState
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
    context.unregisterReceiver(connectivityBroadcastReceiver)
    onDismiss = null
    super.onCleared()
  }
}

object CategorySessionCache {
  var hasFetched: Boolean = false
}
