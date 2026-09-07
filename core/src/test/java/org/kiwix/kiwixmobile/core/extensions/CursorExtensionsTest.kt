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

package org.kiwix.kiwixmobile.core.extensions

import android.database.Cursor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CursorExtensionsTest {
  @Test
  fun `forEachRow closes the cursor even when block throws`() {
    val cursor = mockk<Cursor>(relaxed = true)
    every { cursor.moveToNext() } returns true

    assertThrows(IllegalStateException::class.java) {
      cursor.forEachRow { throw IllegalStateException("boom") }
    }

    verify(exactly = 1) { cursor.close() }
  }

  @Test
  fun `forEachRow closes the cursor after iterating every row`() {
    val cursor = mockk<Cursor>(relaxed = true)
    every { cursor.moveToNext() } returnsMany listOf(true, true, false)

    var rowsSeen = 0
    cursor.forEachRow { rowsSeen++ }

    assertEquals(2, rowsSeen)
    verify(exactly = 1) { cursor.close() }
  }
}
