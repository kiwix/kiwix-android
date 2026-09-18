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

import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.language.composables.LanguageListItem.LanguageItem

sealed class State {
  data class Error(val errorMessage: String) : State()
  object Loading : State()
  object Saving : State()
  data class Content(
    val items: List<Language>,
    val filter: String = "",
    val selectedLanguageOrder: List<String> =
      items.filter { it.active && it.languageCode.isNotEmpty() }.map { it.languageCode },
    val viewItems: List<LanguageListItem> =
      createViewList(
        items,
        filter,
        selectedLanguageOrder
      )
  ) : State() {
    fun select(languageItem: LanguageItem): Content {
      val target = items.firstOrNull { it.id == languageItem.language.id } ?: languageItem.language
      val isCurrentlyActive = target.active
      val targetCode = target.languageCode
      val updatedItems = items.map { item ->
        if (item.id == target.id) {
          item.copy(active = !isCurrentlyActive)
        } else {
          item
        }
      }
      val updatedOrder = if (!isCurrentlyActive) {
        if (targetCode.isNotEmpty() && targetCode !in selectedLanguageOrder) {
          selectedLanguageOrder + targetCode
        } else {
          selectedLanguageOrder
        }
      } else {
        selectedLanguageOrder.filter { it != targetCode }
      }
      return Content(
        items = updatedItems,
        filter = filter,
        selectedLanguageOrder = updatedOrder
      )
    }

    fun moveUp(languageItem: LanguageItem): Content {
      val code = languageItem.language.languageCode
      val index = selectedLanguageOrder.indexOf(code)
      if (index <= 0) return this
      val mutableOrder = selectedLanguageOrder.toMutableList()
      val temp = mutableOrder[index]
      mutableOrder[index] = mutableOrder[index - 1]
      mutableOrder[index - 1] = temp
      return Content(
        items = items,
        filter = filter,
        selectedLanguageOrder = mutableOrder
      )
    }

    fun moveDown(languageItem: LanguageItem): Content {
      val code = languageItem.language.languageCode
      val index = selectedLanguageOrder.indexOf(code)
      if (index < 0 || index >= selectedLanguageOrder.size - 1) return this
      val mutableOrder = selectedLanguageOrder.toMutableList()
      val temp = mutableOrder[index]
      mutableOrder[index] = mutableOrder[index + 1]
      mutableOrder[index + 1] = temp
      return Content(
        items = items,
        filter = filter,
        selectedLanguageOrder = mutableOrder
      )
    }

    fun reorder(fromIndex: Int, toIndex: Int): Content {
      if (fromIndex !in selectedLanguageOrder.indices || toIndex !in selectedLanguageOrder.indices) {
        return this
      }
      val mutableOrder = selectedLanguageOrder.toMutableList()
      val item = mutableOrder.removeAt(fromIndex)
      mutableOrder.add(toIndex, item)
      return Content(
        items = items,
        filter = filter,
        selectedLanguageOrder = mutableOrder
      )
    }

    fun updateFilter(filter: String) =
      Content(items, filter, selectedLanguageOrder = selectedLanguageOrder)

    companion object {
      internal fun createViewList(
        items: List<Language>,
        filter: String,
        selectedLanguageOrder: List<String>
      ): List<LanguageListItem> =
        activeItems(items, filter, selectedLanguageOrder) + otherItems(items, filter)

      private fun activeItems(
        items: List<Language>,
        filter: String,
        selectedLanguageOrder: List<String>
      ): List<LanguageListItem> {
        val activeLanguages = items.filter { it.active }
        val activeMap = activeLanguages.associateBy { it.languageCode }
        val orderedActive = selectedLanguageOrder.mapNotNull { activeMap[it] } +
          activeLanguages.filter { it.languageCode !in selectedLanguageOrder }
        val filtered = orderedActive.filter { filter.isEmpty() || it.matches(filter) }
        return if (filtered.isNotEmpty()) {
          listOf(HeaderItem(HeaderItem.SELECTED)) + filtered.mapIndexed { index, language ->
            LanguageItem(
              language = language,
              isSelectedSection = true,
              rank = index + 1,
              canMoveUp = index > 0,
              canMoveDown = index < filtered.size - 1
            )
          }
        } else {
          emptyList()
        }
      }

      private fun otherItems(
        items: List<Language>,
        filter: String
      ): List<LanguageListItem> {
        val filtered = items
          .filter { !it.active }
          .filter { filter.isEmpty() || it.matches(filter) }
        return if (filtered.isNotEmpty()) {
          listOf(HeaderItem(HeaderItem.OTHER)) + filtered.map { language ->
            LanguageItem(
              language = language,
              isSelectedSection = false
            )
          }
        } else {
          emptyList()
        }
      }
    }
  }
}
