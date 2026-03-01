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

import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.HeaderItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.CategoryItem

sealed class State {
  data class Error(val errorMessage: String) : State()
  object Loading : State()
  object Saving : State()

  data class Content(
    val items: List<Category>,
    val filter: String = "",
    val viewItems: List<CategoryListItem> =
      createViewList(
        items,
        filter
      )
  ) : State() {
    fun select(category: CategoryItem): Content {
      val isAllCategories = category.id == 0L
      return Content(
        items.map {
          when {
            // Toggling the "All Categories" item
            isAllCategories && it.id == 0L -> it.copy(active = !it.active)
            // Deselect all others when "All Categories" is toggled on
            isAllCategories -> it.copy(active = false)
            // Toggling a specific category
            it.id == category.id -> it.copy(active = !it.active)
            // Deselect "All Categories" when a specific category is selected
            it.id == 0L -> it.copy(active = false)
            else -> it
          }
        },
        filter
      )
    }

    fun updateFilter(filter: String) =
      Content(items, filter)

    companion object {
      internal fun createViewList(
        items: List<Category>,
        filter: String
      ) = activeItems(
        items, filter
      ) +
        otherItems(
          items,
          filter
        )

      private fun activeItems(
        items: List<Category>,
        filter: String
      ) =
        createCategorySection(
          items,
          filter,
          Category::active,
          HeaderItem.SELECTED
        )

      private fun otherItems(
        items: List<Category>,
        filter: String
      ) =
        createCategorySection(
          items,
          filter,
          { !it.active },
          HeaderItem.OTHER
        )

      private fun createCategorySection(
        items: List<Category>,
        filter: String,
        filterCondition: (Category) -> Boolean,
        headerId: Long
      ) = items.filter(filterCondition)
        .filter { filter.isEmpty() or it.matches(filter) }
        .takeIf { it.isNotEmpty() }
        ?.let { listOf(HeaderItem(headerId)) + it.map { category -> CategoryItem(category) } }
        .orEmpty()
    }
  }
}
