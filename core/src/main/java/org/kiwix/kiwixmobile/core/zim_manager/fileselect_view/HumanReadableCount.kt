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

package org.kiwix.kiwixmobile.core.zim_manager.fileselect_view

import android.content.Context
import androidx.annotation.StringRes
import org.kiwix.kiwixmobile.core.utils.formatHumanReadable

private val COUNT_UNITS = listOf("", "K", "M", "B", "T")

@JvmInline
value class HumanReadableCount(private val countStr: String?) {
  val humanReadable: String
    get() = formatHumanReadable(
      rawValue = countStr,
      units = COUNT_UNITS,
      includeSpace = false,
      allowZeroOrNegative = false,
    )

  fun toHumanReadable(context: Context, @StringRes resId: Int): String {
    val formatted = humanReadable
    return if (formatted.isEmpty()) "" else context.getString(resId, formatted)
  }
}
