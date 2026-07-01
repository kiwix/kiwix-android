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

package org.kiwix.kiwixmobile.core.main.reader.helper

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource

class ZimFileManagerTest {
  private lateinit var manager: ZimFileManager

  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)
  private val zimReader = mockk<ZimFileReader>(relaxed = true)
  private val destroyWebViews: () -> Unit = mockk(relaxed = true)

  @Before
  fun setup() {
    manager = ZimFileManager(zimReaderContainer)
  }

  @Test
  fun `close clears current zim`() = runTest {
    manager.close()

    coVerify {
      zimReaderContainer.setZimReaderSource(null)
    }
  }

  @Test
  fun `zimFileReader delegates to container`() {
    every { zimReaderContainer.zimFileReader } returns zimReader

    assertThat(manager.zimFileReader).isEqualTo(zimReader)
  }

  @Test
  fun `zimReaderSource delegates to container`() {
    val source = mockk<ZimReaderSource>()

    every { zimReaderContainer.zimReaderSource } returns source

    assertThat(manager.zimReaderSource).isEqualTo(source)
  }

  @Test
  fun `invalid source returns InvalidFile`() = runTest {
    val source = mockk<ZimReaderSource>()

    coEvery { source.canOpenInLibkiwix() } returns false

    val result = manager.openZimFileInReader(
      source,
      false,
      destroyWebViews
    )

    assertThat(result).isEqualTo(ZimFileManager.OpenZimResult.InvalidFile)

    coVerify(exactly = 0) {
      zimReaderContainer.setZimReaderSource(any(), any())
    }
  }

  @Test
  fun `success returns reader`() = runTest {
    val source = mockk<ZimReaderSource>()

    coEvery { source.canOpenInLibkiwix() } returns true
    every { zimReaderContainer.zimReaderSource } returns null
    every { zimReaderContainer.zimFileReader } returns zimReader

    val result = manager.openZimFileInReader(
      source,
      true,
      destroyWebViews
    )

    assertThat(result)
      .isEqualTo(ZimFileManager.OpenZimResult.Success(zimReader))

    coVerify {
      zimReaderContainer.setZimReaderSource(source, true)
    }
  }

  @Test
  fun `returns InvalidFile when reader is null`() = runTest {
    val source = mockk<ZimReaderSource>()

    coEvery { source.canOpenInLibkiwix() } returns true
    every { zimReaderContainer.zimReaderSource } returns null
    every { zimReaderContainer.zimFileReader } returns null

    val result = manager.openZimFileInReader(
      source,
      false,
      destroyWebViews
    )

    assertThat(result)
      .isEqualTo(ZimFileManager.OpenZimResult.InvalidFile)
  }

  @Test
  fun `changing zim destroys webviews`() = runTest {
    val oldSource = mockk<ZimReaderSource>()
    val newSource = mockk<ZimReaderSource>()

    coEvery { oldSource.canOpenInLibkiwix() } returns true
    coEvery { newSource.canOpenInLibkiwix() } returns true

    every { zimReaderContainer.zimReaderSource } returns oldSource
    every { zimReaderContainer.zimFileReader } returns zimReader

    manager.openZimFileInReader(
      newSource,
      false,
      destroyWebViews
    )

    verify {
      destroyWebViews.invoke()
    }
  }

  @Test
  fun `opening same zim does not destroy webviews`() = runTest {
    val source = mockk<ZimReaderSource>()

    coEvery { source.canOpenInLibkiwix() } returns true
    every { zimReaderContainer.zimReaderSource } returns source
    every { zimReaderContainer.zimFileReader } returns zimReader

    manager.openZimFileInReader(
      source,
      false,
      destroyWebViews
    )

    verify(exactly = 0) {
      destroyWebViews.invoke()
    }
  }
}
