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

package org.kiwix.kiwixmobile.core.reader

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ZimReaderContainerTest {
  private val factory: ZimFileReader.Factory = mockk()
  private val container = ZimReaderContainer(factory, Dispatchers.Unconfined)

  @Test
  fun `accessors return safe defaults when no reader is set`() {
    assertFalse(container.isRedirect("A/foo"))
    assertEquals("", container.getRedirect("A/foo"))
    assertNull(container.getPageUrlFromTitle("foo"))
    assertNull(container.getRandomPageUrl())
    assertNull(container.zimReaderSource)
    assertNull(container.zimFileTitle)
    assertNull(container.mainPage)
    assertNull(container.id)
    assertEquals(0L, container.fileSize)
    assertNull(container.creator)
    assertNull(container.publisher)
    assertNull(container.name)
    assertNull(container.date)
    assertNull(container.description)
    assertNull(container.favicon)
    assertNull(container.language)
  }

  @Test
  fun `setting a new reader disposes the previous one exactly once`() = runTest {
    val firstSource: ZimReaderSource = mockk()
    val secondSource: ZimReaderSource = mockk()
    val firstReader: ZimFileReader = mockk(relaxed = true) {
      every { zimReaderSource } returns firstSource
    }
    coEvery { firstSource.exists(any()) } returns true
    coEvery { firstSource.canOpenInLibkiwix(any()) } returns true
    coEvery { secondSource.exists(any()) } returns false
    coEvery { factory.create(firstSource, any()) } returns firstReader

    container.setZimReaderSource(firstSource)
    verify(exactly = 0) { firstReader.dispose() }

    // secondSource fails exists(), so the new reader is null - the old one
    // must still be disposed exactly once when it's swapped out.
    container.setZimReaderSource(secondSource)
    verify(exactly = 1) { firstReader.dispose() }
  }
}
