/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private val UNIT_STEP = BigDecimal(1000)
private val DECIMAL_THRESHOLD = BigDecimal.TEN

/**
 * Formats a raw numeric string representation into a human-readable string with unit scaling.
 *
 * @param rawValue The raw string representation of a long value.
 * @param units List of unit labels (e.g. `["B", "KB", "MB", "GB", "TB"]`).
 * @param includeSpace Whether to insert a space between the formatted number and unit.
 * @param allowZeroOrNegative If false, non-positive numbers (<= 0) return an empty string.
 */
fun formatHumanReadable(
  rawValue: String?,
  units: List<String>,
  includeSpace: Boolean = true,
  allowZeroOrNegative: Boolean = true,
): String {
  val count = parseValidCount(rawValue, allowZeroOrNegative) ?: return ""
  val (formattedNumber, unit) = scaleToUnit(count, units)
  val space = if (includeSpace) " " else ""

  return "$formattedNumber$space$unit"
}

private fun parseValidCount(rawValue: String?, allowZeroOrNegative: Boolean): Long? {
  val count = rawValue?.toLongOrNull() ?: return null
  return if (allowZeroOrNegative || count > 0L) count else null
}

private fun scaleToUnit(count: Long, units: List<String>): Pair<String, String> {
  var unitIndex = 0
  var value = BigDecimal(count)

  while (value >= UNIT_STEP && unitIndex < units.lastIndex) {
    value = value.divide(UNIT_STEP)
    unitIndex++
  }

  var rounded = round(value)
  if (rounded >= UNIT_STEP && unitIndex < units.lastIndex) {
    rounded = round(value.divide(UNIT_STEP))
    unitIndex++
  }

  val formattedNumber = DecimalFormat("#,##0.#").format(rounded)
  return formattedNumber to units[unitIndex]
}

private fun round(value: BigDecimal): BigDecimal {
  val scale = if (value < DECIMAL_THRESHOLD) 1 else 0
  return value.setScale(scale, RoundingMode.HALF_UP)
}
