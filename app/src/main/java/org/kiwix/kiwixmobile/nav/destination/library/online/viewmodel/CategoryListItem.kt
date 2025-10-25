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

sealed class CategoryListItem {
  abstract val id: Long

  data class HeaderItem constructor(
    override val id: Long
  ) : CategoryListItem() {
    companion object {
      const val SELECTED = Long.MAX_VALUE
      const val OTHER = Long.MIN_VALUE
    }
  }

  data class CategoryItem(
    val category: Category,
    override val id: Long = category.id
  ) : CategoryListItem()
}
