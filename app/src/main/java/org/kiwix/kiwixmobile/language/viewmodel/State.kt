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

import org.kiwix.kiwixmobile.language.adapter.LanguageListItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.zim_manager.Language

sealed class State {
  object Loading : State()
  object Saving : State()
  data class Content(
    val items: List<Language>,
    val filter: String = "",
    val viewItems: List<LanguageListItem> = createViewList(
      items, filter
    )
  ) : State() {
    fun select(languageItem: LanguageItem) = Content(
      items.map { if (it.id == languageItem.id) it.copy(active = !it.active) else it },
      filter
    )

    fun updateFilter(filter: String) = Content(items, filter)

    companion object {
      internal fun createViewList(
        items: List<Language>,
        filter: String
      ) = activeItems(
        items, filter
      ) + otherItems(items, filter)

      private fun activeItems(
        items: List<Language>,
        filter: String
      ) =
        createLanguageSection(
          items, filter, Language::active, HeaderItem.SELECTED
        )

      private fun otherItems(
        items: List<Language>,
        filter: String
      ) =
        createLanguageSection(
          items, filter, { !it.active }, HeaderItem.OTHER
        )

      private fun createLanguageSection(
        items: List<Language>,
        filter: String,
        filterCondition: (Language) -> Boolean,
        headerId: Long
      ) = items.filter(filterCondition)
        .filter { filter.isEmpty() or it.matches(filter) }
        .takeIf { it.isNotEmpty() }
        ?.let { listOf(HeaderItem(headerId)) + it.map { language -> LanguageItem(language) } }
        ?: emptyList()
    }
  }
}
