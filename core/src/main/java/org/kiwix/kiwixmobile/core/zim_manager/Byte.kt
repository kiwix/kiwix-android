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

package org.kiwix.kiwixmobile.core.zim_manager

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

@JvmInline
value class Byte(private val byteString: String?) {
  val humanReadable
    get() = byteString?.toLongOrNull()?.let {
      val units = arrayOf("B", "KB", "MB", "GB", "TB")
      val conversion = (log10(it.toDouble()) / log10(1000.0)).toInt()
      DecimalFormat("#,##0.##")
        .format(it / 1000.0.pow(conversion.toDouble())) +
        " " +
        units[conversion]
    }.orEmpty()
}
