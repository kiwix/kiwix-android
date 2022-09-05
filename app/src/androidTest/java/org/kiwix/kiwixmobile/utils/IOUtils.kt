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
package org.kiwix.kiwixmobile.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object IOUtils {
  private const val DEFAULT_BUFFER_SIZE = 1024 * 4
  private const val EOF = -1

  @Throws(IOException::class)
  fun toByteArray(input: InputStream): ByteArray {
    ByteArrayOutputStream().use { output ->
      copy(input, output)
      return@toByteArray output.toByteArray()
    }
  }

  @Throws(IOException::class)
  private fun copy(input: InputStream, output: OutputStream): Int {
    val count = copyLarge(input, output)
    return if (count > Int.MAX_VALUE) {
      -1
    } else count.toInt()
  }

  @Throws(IOException::class)
  private fun copyLarge(
    input: InputStream,
    output: OutputStream
  ): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var count: Long = 0
    var n: Int
    while (EOF != input.read(buffer).also { n = it }) {
      output.write(buffer, 0, n)
      count += n.toLong()
    }
    return count
  }
}
