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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.SaveAll
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import javax.inject.Inject

class LanguageViewModel @Inject constructor(
  private val languageDao: NewLanguagesDao
) : ViewModel() {

  val state = MutableLiveData<State>().apply { value = Loading }
  val actions = PublishProcessor.create<Action>()
  val effects = PublishProcessor.create<SideEffect<*>>()

  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(
      actions.map { reduce(it, state.value!!) }
        .distinctUntilChanged()
        .subscribe(state::postValue, Throwable::printStackTrace),
      languageDao.languages().filter { it.isNotEmpty() }
        .subscribe(
          {
            actions.offer(UpdateLanguages(it))
          },
          Throwable::printStackTrace
        )
    )
  }

  private fun reduce(
    action: Action,
    currentState: State
  ): State {
    return when (action) {
      is UpdateLanguages -> when (currentState) {
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
      SaveAll ->
        when (currentState) {
          is Content -> saveAll(currentState)
          else -> currentState
        }
    }
  }

  private fun saveAll(currentState: Content): State {
    effects.offer(
      SaveLanguagesAndFinish(
        currentState.items, languageDao
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
