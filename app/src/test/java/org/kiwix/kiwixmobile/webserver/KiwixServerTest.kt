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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KiwixServerTest {
  @Test
  fun `returns first address when it is valid`() {
    assertEquals(
      "http://192.168.43.1:8080/",
      firstValidServerAddress(arrayOf("http://192.168.43.1:8080/", "http://10.0.2.15:8080/"))
    )
  }

  @Test
  fun `skips empty and blank addresses`() {
    assertEquals(
      "http://10.0.2.15:8080/",
      firstValidServerAddress(arrayOf("", "  ", null, "http://10.0.2.15:8080/"))
    )
  }

  @Test
  fun `returns null when all addresses are blank`() {
    assertNull(firstValidServerAddress(arrayOf("", " ")))
  }

  @Test
  fun `returns null when there are no addresses`() {
    assertNull(firstValidServerAddress(emptyArray()))
  }

  @Test
  fun `returns null when urls array is null`() {
    assertNull(firstValidServerAddress(null))
  }
}
