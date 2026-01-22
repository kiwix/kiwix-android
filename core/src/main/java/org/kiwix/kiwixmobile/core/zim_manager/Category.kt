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

package org.kiwix.kiwixmobile.core.zim_manager

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category constructor(
  val id: Long = 0,
  var active: Boolean,
  val category: String
) : Parcelable {
  override fun equals(other: Any?): Boolean =
    (other as Category).category == category && other.active == active

  override fun hashCode(): Int {
    var result = active.hashCode()
    result = 31 * result + category.hashCode()
    return result
  }

  fun matches(filter: String) =
    category.contains(filter, true)
}
