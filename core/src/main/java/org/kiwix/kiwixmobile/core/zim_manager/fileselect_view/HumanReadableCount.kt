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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private val UNIT_STEP = BigDecimal(1000)
private val DECIMAL_THRESHOLD = BigDecimal.TEN
private val UNITS = listOf("", "K", "M", "B", "T")

@JvmInline
value class HumanReadableCount(private val countStr: String?) {
  val humanReadable
    get() = countStr?.toLongOrNull()?.let(::format).orEmpty()

  fun toHumanReadable(context: Context, @StringRes resId: Int): String {
    val formatted = humanReadable
    return if (formatted.isEmpty()) "" else context.getString(resId, formatted)
  }

  private fun format(count: Long): String {
    if (count <= 0L) return ""
    var unitIndex = 0
    var value = BigDecimal(count)
    while (value >= UNIT_STEP && unitIndex < UNITS.lastIndex) {
      value = value.divide(UNIT_STEP)
      unitIndex++
    }
    var rounded = round(value)
    if (rounded >= UNIT_STEP && unitIndex < UNITS.lastIndex) {
      rounded = round(value.divide(UNIT_STEP))
      unitIndex++
    }
    return "${DecimalFormat("#,##0.#").format(rounded)}${UNITS[unitIndex]}"
  }

  private fun round(value: BigDecimal): BigDecimal {
    val scale = if (value < DECIMAL_THRESHOLD) 1 else 0
    return value.setScale(scale, RoundingMode.HALF_UP)
  }
}
