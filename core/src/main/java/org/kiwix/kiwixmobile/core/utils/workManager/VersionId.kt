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

package org.kiwix.kiwixmobile.core.utils.workManager

@Suppress("all")
data class VersionId(
  val major: Int,
  val minor: Int,
  val build: Int,
  val variantType: String,
  val variantNumber: Int,
) : Comparable<VersionId> {
  override fun compareTo(other: VersionId): Int {
    var diff = major.compareTo(other.major)
    if (diff != 0) {
      return diff
    }
    diff = minor.compareTo(other.minor)
    if (diff != 0) {
      return diff
    }
    diff = build.compareTo(other.build)
    if (diff != 0) {
      return diff
    }

    return variantNumber.compareTo(other.variantNumber)
  }
}

fun VersionId(versionName: String): VersionId {
  val parts = versionName.substringBeforeLast('-').split('.')
  val variant = versionName.substringAfterLast('-', "")
  return VersionId(
    major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
    minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
    build = parts.getOrNull(2)?.toIntOrNull() ?: 0,
    variantType = variant.filter(Char::isLetter),
    variantNumber = variant.filter(Char::isDigit).toIntOrNull() ?: 0,
  )
}
