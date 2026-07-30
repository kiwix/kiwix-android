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
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.utils.LocaleHelper
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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect

import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.helper.ObserveLanguages
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Cancel
import org.kiwix.kiwixmobile.language.viewmodel.Action.Error
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.Save
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import javax.inject.Inject

class LanguageViewModel @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  private val observeLanguages: ObserveLanguages,
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

  private fun observeLanguages() = viewModelScope.launch {
    state.value = Loading
    when (
      val result = observeLanguages(
        errorNoLanguage = context.getString(R.string.no_language_available),
        errorNoNetwork = context.getString(R.string.no_network_connection)
      )
    ) {
      is ObserveLanguages.Result.Success -> {
        val sortedLanguages = sortLanguages(result.languages)
        actions.emit(UpdateLanguages(sortedLanguages))
      }

      is ObserveLanguages.Result.Error ->
        actions.emit(Error(result.message))
    }
  }

  private fun sortLanguages(languages: List<Language>): List<Language> {
    val allLanguagesItem = languages.firstOrNull { it.id == 0L || it.languageCode.isEmpty() }
    val otherLanguages = languages.filter { it.id != 0L && it.languageCode.isNotEmpty() }

    val systemLanguageLocale = LocaleHelper.getAppLocale(context, kiwixDataStore)

    val systemLanguageISO3 = try {
      systemLanguageLocale.isO3Language
    } catch (_: Exception) {
      ""
    }
    val systemLanguageISO2 = systemLanguageLocale.language

    val sortedOthers = otherLanguages.sortedWith(
      compareByDescending<Language> {
        it.languageCodeISO2.equals(systemLanguageISO2, ignoreCase = true) ||
          it.languageCode.equals(systemLanguageISO2, ignoreCase = true) ||
          it.languageCodeISO2.equals(systemLanguageISO3, ignoreCase = true) ||
          it.languageCode.equals(systemLanguageISO3, ignoreCase = true)
      }.thenBy { it.languageLocalized }
    ).mapIndexed { index, language ->
      language.copy(id = (index + 1).toLong())
    }

    return if (allLanguagesItem != null) {
      buildList {
        add(allLanguagesItem)
        addAll(sortedOthers)
      }
    } else {
      sortedOthers
    }
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
    super.onCleared()
  }

  private fun reduce(
    action: Action,
    currentState: State
  ): State {
    return when (action) {
      is Error -> State.Error(action.errorMessage)
      is UpdateLanguages -> updateLanguages(action, currentState)
      is Filter -> filter(action, currentState)
      is Select -> select(action, currentState)
      Save -> saveAction(currentState)
      Cancel -> cancel(currentState)
    }
  }

  private fun cancel(currentState: State): State {
    if (currentState !is Content) return currentState
    effects.tryEmit(object : SideEffect<Unit> {
      override fun invokeWith(activity: AppCompatActivity) {
        activity.onBackPressedDispatcher.onBackPressed()
      }
    })
    return currentState
  }

  private fun updateLanguages(action: UpdateLanguages, currentState: State): State =
    if (currentState is Loading) Content(action.languages) else currentState

  private fun filter(action: Filter, currentState: State): State =
    if (currentState is Content) filterContent(action.filter, currentState) else currentState

  private fun select(action: Select, currentState: State): State =
    if (currentState is Content) updateSelection(action.language, currentState) else currentState

  private fun saveAction(currentState: State): State =
    if (currentState is Content) save(currentState) else currentState

  private fun save(currentState: Content): State {
    val selectedLanguages = currentState.items.filter { it.active }
    effects.tryEmit(
      SaveLanguagesAndFinish(
        selectedLanguages,
        kiwixDataStore,
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
}
