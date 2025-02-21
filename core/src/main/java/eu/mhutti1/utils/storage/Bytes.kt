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
package eu.mhutti1.utils.storage

import java.text.DecimalFormat

const val KB = 1 * 1024L
const val MB = KB * 1024
const val GB = MB * 1024
const val TB = GB * 1024
const val PB = TB * 1024
const val EB = PB * 1024

@JvmInline
value class Bytes(val size: Long) {
  val humanReadable
    get() =
      when {
        size < KB -> "$size Bytes"
        size in KB until MB -> format(size.toDouble() / KB) + " KB"
        size in MB until GB -> format(size.toDouble() / MB) + " MB"
        size in GB until TB -> format(size.toDouble() / GB) + " GB"
        size in TB until PB -> format(size.toDouble() / TB) + " TB"
        size in PB until EB -> format(size.toDouble() / PB) + " PB"
        size >= EB -> format(size.toDouble() / EB) + " EB"
        else -> throw RuntimeException("impossible value $size")
      }

  fun format(size: Double): String = DecimalFormat("#.#").format(size)
}
