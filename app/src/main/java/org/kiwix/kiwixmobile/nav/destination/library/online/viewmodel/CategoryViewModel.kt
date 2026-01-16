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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_LANGUAGE_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.Error
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.Select
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.UpdateCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.CategoryItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Content
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Saving
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

@Suppress("UnusedPrivateProperty")
class CategoryViewModel @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  private var kiwixService: KiwixService,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) : ViewModel() {
  val state = MutableStateFlow<State>(Loading)
  val actions = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)
  val effects = MutableSharedFlow<SideEffect<*>>(extraBufferCapacity = Int.MAX_VALUE)

  private val coroutineJobs = mutableListOf<Job>()

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

  private fun observeCategories() = viewModelScope.launch {
    state.value = Loading

    val cachedCategoryList = kiwixDataStore.cachedOnlineCategoryList.first()
    val isOnline = connectivityBroadcastReceiver.networkStates.value == NetworkState.CONNECTED
    if (CategorySessionCache.hasFetched && !cachedCategoryList.isNullOrEmpty()) {
      actions.emit(UpdateCategory(cachedCategoryList))
      return@launch
    }

    if (isOnline) {
      runCatching {
        val fetched = fetchCategories()
        if (!fetched.isNullOrEmpty()) {
          kiwixDataStore.saveOnlineCategoryList(fetched)
          CategorySessionCache.hasFetched = true
          actions.emit(UpdateCategory(fetched))
          return@launch
        }
      }.onFailure { it.printStackTrace() }
    }

    if (!cachedCategoryList.isNullOrEmpty()) {
      actions.emit(UpdateCategory(cachedCategoryList))
    } else {
      val errorMessage = if (isOnline) {
        context.getString(string.no_category_available)
      } else {
        context.getString(R.string.no_network_connection)
      }
      actions.emit(Error(errorMessage))
    }
  }

  private suspend fun fetchCategories(): List<Category>? =
    runCatching {
      kiwixService =
        KiwixService.ServiceCreator.newHackListService(getOkHttpClient(), KIWIX_LANGUAGE_URL)
      val feed = kiwixService.getCategories()

      val categories = feed.entries.orEmpty().mapIndexed { index, entry ->
        Category(
          category = entry.title,
          active = kiwixDataStore.selectedOnlineContentCategory.first() == entry.title,
          id = (index + 1).toLong()
        )
      }

      buildList {
        add(
          Category(
            category = "",
            active = kiwixDataStore.selectedOnlineContentCategory.first().isEmpty(),
            id = 0L
          )
        )
        addAll(categories)
      }
    }.onFailure { it.printStackTrace() }.getOrNull()

  private fun reduce(
    action: Action,
    currentState: State
  ): State {
    return when (action) {
      is Error -> State.Error(action.errorMessage)

      is UpdateCategory ->
        when (currentState) {
          Loading -> Content(action.categories)
          else -> currentState
        }

      is Filter -> {
        when (currentState) {
          is Content -> filterContent(action.filter, currentState)
          else -> currentState
        }
      }

      is Select ->
        when (currentState) {
          is Content -> {
            val newState = updateSelection(action.category, currentState)
            save(newState)
          }

          else -> currentState
        }
    }
  }

  private fun filterContent(
    filter: String,
    currentState: Content
  ) = currentState.updateFilter(filter)

  private fun updateSelection(
    categoryItem: CategoryItem,
    currentState: Content
  ) = currentState.select(categoryItem)

  private fun save(currentState: Content): State {
    val selectedCategory = currentState.items.first { it.active }
    effects.tryEmit(
      SaveCategoryAndFinish(
        selectedCategory,
        kiwixDataStore,
        viewModelScope
      )
    )
    return Saving
  }

  private fun getOkHttpClient() = OkHttpClient().newBuilder()
    .followRedirects(true)
    .followSslRedirects(true)
    .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
    .readTimeout(READ_TIMEOUT, SECONDS)
    .callTimeout(CALL_TIMEOUT, SECONDS)
    .addNetworkInterceptor(
      HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) BASIC else NONE
      }
    )
    .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
    .build()

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
    super.onCleared()
  }
}

object CategorySessionCache {
  var hasFetched: Boolean = false
}
