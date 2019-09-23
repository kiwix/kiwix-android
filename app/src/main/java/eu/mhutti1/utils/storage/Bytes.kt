/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package eu.mhutti1.utils.storage

import java.text.DecimalFormat

const val Kb = 1 * 1024L
const val Mb = Kb * 1024
const val Gb = Mb * 1024
const val Tb = Gb * 1024
const val Pb = Tb * 1024
const val Eb = Pb * 1024

inline class Bytes(val size: Long) {
  val humanReadable
    get() = when {
      size < Kb -> "$size Bytes"
      size in Kb until Mb -> format(size.toDouble() / Kb) + " KB"
      size in Mb until Gb -> format(size.toDouble() / Mb) + " MB"
      size in Gb until Tb -> format(size.toDouble() / Gb) + " GB"
      size in Tb until Pb -> format(size.toDouble() / Tb) + " TB"
      size in Pb until Eb -> format(size.toDouble() / Pb) + " PB"
      size >= Eb -> format(size.toDouble() / Eb) + " EB"
      else -> throw RuntimeException("impossible value $size")
    }

  fun format(size: Double): String = DecimalFormat("#.#").format(size)
}
