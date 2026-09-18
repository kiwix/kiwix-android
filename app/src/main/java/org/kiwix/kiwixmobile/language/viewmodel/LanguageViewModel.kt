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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.LocaleHelper
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityObserver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.helper.ObserveLanguages
import org.kiwix.kiwixmobile.language.viewmodel.Action.Cancel
import org.kiwix.kiwixmobile.language.viewmodel.Action.Error
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.MoveDown
import org.kiwix.kiwixmobile.language.viewmodel.Action.MoveUp
import org.kiwix.kiwixmobile.language.viewmodel.Action.Reorder
import org.kiwix.kiwixmobile.language.viewmodel.Action.Save
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import javax.inject.Inject

@HiltViewModel
open class LanguageViewModel @Inject constructor(
  private val context: Application,
  private val kiwixDataStore: KiwixDataStore,
  private val observeLanguages: ObserveLanguages,
  private val connectivityObserver: ConnectivityObserver
) : ViewModel() {
  val state = MutableStateFlow<State>(Loading)
  val actions = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)
  val effects = MutableSharedFlow<SideEffect<*>>(extraBufferCapacity = Int.MAX_VALUE)
  private val coroutineJobs = mutableListOf<Job>()

  init {
    connectivityObserver.register()
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
        errorNoNetwork = context.getString(R.string.no_network_connection),
        isOnline = connectivityObserver.networkStates.value == NetworkState.CONNECTED
      )
    ) {
      is ObserveLanguages.Result.Success -> {
        val sortedLanguages = sortLanguages(result.languages)
        val savedLangPref = kiwixDataStore.selectedOnlineContentLanguage.first()
        val initialOrder =
          if (savedLangPref.isEmpty() || savedLangPref.equals("all", ignoreCase = true)) {
            sortedLanguages.filter { it.active && it.languageCode.isNotEmpty() }.map { it.languageCode }
          } else {
            savedLangPref
              .split(",")
              .map { it.trim() }
              .filter { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
          }
        actions.emit(UpdateLanguages(sortedLanguages, initialOrder))
      }

      is ObserveLanguages.Result.Error ->
        actions.emit(Error(result.message))
    }
  }

  private suspend fun sortLanguages(languages: List<Language>): List<Language> {
    val systemLanguageLocale = LocaleHelper.getAppLocale(context, kiwixDataStore)
    val systemLanguageISO3 = try {
      systemLanguageLocale.isO3Language
    } catch (_: Exception) {
      ""
    }
    val systemLanguageISO2 = systemLanguageLocale.language

    val savedLangPref = kiwixDataStore.selectedOnlineContentLanguage.first()
    val isExplicitAll = savedLangPref.equals("all", ignoreCase = true)
    val savedLanguagesSet = if (savedLangPref.isNotEmpty() && !isExplicitAll) {
      savedLangPref.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    } else {
      emptySet()
    }
    val isDefault = savedLangPref.isEmpty() || isExplicitAll

    return languages.map { lang ->
      lang.copy(
        active = isLanguageActive(
          lang,
          savedLanguagesSet,
          isDefault,
          systemLanguageISO3,
          systemLanguageISO2
        )
      )
    }.sortedBy { it.languageLocalized }
      .mapIndexed { index, language ->
        language.copy(id = (index + 1).toLong())
      }
  }

  private fun isLanguageActive(
    lang: Language,
    savedLanguagesSet: Set<String>,
    isDefault: Boolean,
    systemLanguageISO3: String,
    systemLanguageISO2: String
  ): Boolean = when {
    savedLanguagesSet.isNotEmpty() -> lang.languageCode in savedLanguagesSet
    isDefault -> matchesSystemLanguage(lang, systemLanguageISO3, systemLanguageISO2)
    else -> false
  }

  private fun matchesSystemLanguage(
    lang: Language,
    iso3: String,
    iso2: String
  ): Boolean {
    if (lang.languageCode.isBlank()) return false
    val matchesIso3 = iso3.isNotBlank() &&
      (
        lang.languageCode.equals(iso3, ignoreCase = true) ||
          lang.languageCodeISO2.equals(iso3, ignoreCase = true)
      )
    val matchesIso2 = iso2.isNotBlank() &&
      (
        lang.languageCode.equals(iso2, ignoreCase = true) ||
          lang.languageCodeISO2.equals(iso2, ignoreCase = true)
      )
    return matchesIso3 || matchesIso2
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

  private fun reduce(
    action: Action,
    currentState: State
  ): State =
    when (action) {
      is Error -> State.Error(action.errorMessage)
      is UpdateLanguages -> updateLanguages(action, currentState)
      is Filter -> filter(action, currentState)
      is Select -> select(action, currentState)
      is MoveUp -> moveUp(action, currentState)
      is MoveDown -> moveDown(action, currentState)
      is Reorder -> reorder(action, currentState)
      Save -> saveAction(currentState)
      Cancel -> cancel(currentState)
    }

  private fun moveUp(action: MoveUp, currentState: State): State =
    if (currentState is Content) currentState.moveUp(action.language) else currentState

  private fun moveDown(action: MoveDown, currentState: State): State =
    if (currentState is Content) currentState.moveDown(action.language) else currentState

  private fun reorder(action: Reorder, currentState: State): State =
    if (currentState is Content) currentState.reorder(action.fromIndex, action.toIndex) else currentState

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
    if (currentState === Loading) {
      val activeLanguages = action.languages.filter { it.active && it.languageCode.isNotEmpty() }
      val order = if (action.initialSelectedOrder.isNotEmpty()) {
        val activeCodes = activeLanguages.map { it.languageCode }.toSet()
        val validInitial = action.initialSelectedOrder.filter { it in activeCodes }
        val remaining =
          activeLanguages.filter { it.languageCode !in validInitial }.map { it.languageCode }
        validInitial + remaining
      } else {
        activeLanguages.map { it.languageCode }
      }
      Content(action.languages, selectedLanguageOrder = order)
    } else {
      currentState
    }

  private fun filter(action: Filter, currentState: State): State =
    if (currentState is Content) filterContent(action.filter, currentState) else currentState

  private fun select(action: Select, currentState: State): State =
    if (currentState is Content) updateSelection(action.language, currentState) else currentState

  private fun saveAction(currentState: State): State =
    if (currentState is Content) save(currentState) else currentState

  private fun save(currentState: Content): State {
    val activeLanguages = currentState.items.filter { it.active }
    val orderMap =
      currentState.selectedLanguageOrder.withIndex().associate { it.value to it.index }
    val sortedSelectedLanguages = activeLanguages.sortedBy {
      orderMap[it.languageCode] ?: Int.MAX_VALUE
    }
    effects.tryEmit(
      SaveLanguagesAndFinish(
        sortedSelectedLanguages,
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
