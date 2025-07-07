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

package org.kiwix.kiwixmobile.language.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
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
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Error
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.Save
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

class LanguageViewModel @Inject constructor(
  private val context: Application,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
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
      add(observeLanguages())
    }
  }

  private fun observeActions() =
    actions
      .map { action -> reduce(action, state.value) }
      .distinctUntilChanged()
      .onEach { newState -> state.value = newState }
      .launchIn(viewModelScope)

  private suspend fun fetchLanguages(): List<Language>? =
    runCatching {
      kiwixService =
        KiwixService.ServiceCreator.newHackListService(getOkHttpClient(), KIWIX_LANGUAGE_URL)
      val feed = kiwixService.getLanguages()
      var allBooksCount = ZERO

      val languages = feed.entries.orEmpty().mapIndexedNotNull { index, entry ->
        allBooksCount += entry.count
        runCatching {
          Language(
            languageCode = entry.languageCode,
            active = sharedPreferenceUtil.selectedOnlineContentLanguage == entry.languageCode,
            occurrencesOfLanguage = entry.count,
            id = (index + 1).toLong()
          )
        }.onFailure {
          Log.w(TAG_KIWIX, "Unsupported locale code: ${entry.languageCode}", it)
        }.getOrNull()
      }

      buildList {
        add(
          Language(
            languageCode = "",
            active = sharedPreferenceUtil.selectedOnlineContentLanguage.isEmpty(),
            occurrencesOfLanguage = allBooksCount,
            id = 0L
          )
        )
        addAll(languages)
      }
    }.onFailure { it.printStackTrace() }.getOrNull()

  private fun observeLanguages() = viewModelScope.launch {
    state.value = Loading

    val cachedLanguageList = sharedPreferenceUtil.getCachedLanguageList()
    val isOnline = connectivityBroadcastReceiver.networkStates.value != NetworkState.NOT_CONNECTED

    if (LanguageSessionCache.hasFetched && !cachedLanguageList.isNullOrEmpty()) {
      actions.emit(UpdateLanguages(cachedLanguageList))
      return@launch
    }

    if (isOnline) {
      runCatching {
        val fetched = fetchLanguages()
        if (!fetched.isNullOrEmpty()) {
          sharedPreferenceUtil.saveLanguageList(fetched)
          LanguageSessionCache.hasFetched = true
          actions.emit(UpdateLanguages(fetched))
          return@launch
        }
      }.onFailure { it.printStackTrace() }
    }

    if (!cachedLanguageList.isNullOrEmpty()) {
      actions.emit(UpdateLanguages(cachedLanguageList))
    } else {
      actions.emit(Error(context.getString(R.string.no_language_available)))
    }
  }

  override fun onCleared() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    context.unregisterReceiver(connectivityBroadcastReceiver)
    super.onCleared()
  }

  private fun reduce(
    action: Action,
    currentState: State
  ): State {
    return when (action) {
      is Error -> State.Error(action.errorMessage)

      is UpdateLanguages ->
        when (currentState) {
          Loading -> Content(action.languages)
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
          is Content -> updateSelection(action.language, currentState)
          else -> currentState
        }

      Save ->
        when (currentState) {
          is Content -> save(currentState)
          else -> currentState
        }
    }
  }

  private fun save(currentState: Content): State {
    val selectedLanguage = currentState.items.first { it.active }
    effects.tryEmit(
      SaveLanguagesAndFinish(
        selectedLanguage,
        sharedPreferenceUtil,
        viewModelScope
      )
    )
    return Saving
  }

  private fun updateSelection(
    languageItem: LanguageItem,
    currentState: Content
  ) = currentState.select(languageItem)

  private fun filterContent(
    filter: String,
    currentState: Content
  ) = currentState.updateFilter(filter)

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
}

object LanguageSessionCache {
  var hasFetched: Boolean = false
}
