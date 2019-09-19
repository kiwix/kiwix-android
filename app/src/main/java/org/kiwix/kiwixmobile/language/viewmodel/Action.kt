package org.kiwix.kiwixmobile.language.viewmodel

import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.zim_manager.Language

sealed class Action {
  data class UpdateLanguages(val languages: List<Language>) : Action()
  data class Filter(val filter: String) : Action()
  data class Select(val language: LanguageItem) : Action()
  object SaveAll : Action()
}
