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

package org.kiwix.kiwixmobile.webserver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KiwixServerTest {
  @Test
  fun testFirstValidServerAddress() {
    val validAddress = "http://192.168.43.1:8080/"
    val fallbackAddress = "http://10.0.2.15:8080/"
    listOf(
      arrayOf(validAddress, fallbackAddress) to validAddress,
      arrayOf("", fallbackAddress) to fallbackAddress,
      arrayOf("  ", null, fallbackAddress) to fallbackAddress,
      arrayOf("", " ") to null,
      emptyArray<String?>() to null,
      null to null
    ).forEach { (urls, expected) ->
      assertEquals(expected, firstValidServerAddress(urls), "urls=${urls?.toList()}")
    }
  }
}
