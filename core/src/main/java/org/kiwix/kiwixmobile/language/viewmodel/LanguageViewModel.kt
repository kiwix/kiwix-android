package org.kiwix.kiwixmobile.language.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.SaveAll
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects.SideEffect
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
