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

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

class ZimFileManagerTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()
  private val testDispatcher = mainDispatcherRule.dispatcher
  private lateinit var manager: ZimFileManager

  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>()
  private val zimReader = mockk<ZimFileReader>(relaxed = true)

  @BeforeEach
  fun setup() {
    // The test class uses JUnit5's per-class lifecycle, so mocks are shared across test
    // methods; clear recorded calls/stubs between tests to keep them independent.
    clearAllMocks()
    manager = ZimFileManager(zimReaderContainer, kiwixDataStore, testDispatcher)
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

    coEvery { source.canOpenInLibkiwix(testDispatcher) } returns false

    val result = manager.openZimFileInReader(source, false)

    assertThat(result).isEqualTo(ZimFileManager.OpenZimResult.InvalidFile)

    coVerify(exactly = 0) {
      zimReaderContainer.setZimReaderSource(any(), any())
    }
  }

  @Test
  fun `success returns reader`() = runTest {
    val source = mockk<ZimReaderSource>()

    coEvery { source.canOpenInLibkiwix(testDispatcher) } returns true
    every { zimReaderContainer.zimReaderSource } returns null
    every { zimReaderContainer.zimFileReader } returns zimReader

    val result = manager.openZimFileInReader(source, true)

    assertThat(result)
      .isEqualTo(ZimFileManager.OpenZimResult.Success(zimReader))

    coVerify {
      zimReaderContainer.setZimReaderSource(source, true)
    }
  }

  @Test
  fun `returns InvalidFile when reader is null`() = runTest {
    val source = mockk<ZimReaderSource>()

    coEvery { source.canOpenInLibkiwix(testDispatcher) } returns true
    every { zimReaderContainer.zimReaderSource } returns null
    every { zimReaderContainer.zimFileReader } returns null

    val result = manager.openZimFileInReader(source, false)

    assertThat(result)
      .isEqualTo(ZimFileManager.OpenZimResult.InvalidFile)
  }

  @Nested
  inner class RestoreLastOpenedZimFileIfNeeded {
    @Test
    fun `does nothing when a reader is already open`() = runTest {
      every { zimReaderContainer.zimFileReader } returns zimReader

      val result = manager.restoreLastOpenedZimFileIfNeeded(false)

      assertThat(result).isNull()
      verify(exactly = 0) { kiwixDataStore.currentZimFile }
      coVerify(exactly = 0) { zimReaderContainer.setZimReaderSource(any(), any()) }
    }

    @Test
    fun `does nothing when there is no stored zim file`() = runTest {
      every { zimReaderContainer.zimFileReader } returns null
      every { kiwixDataStore.currentZimFile } returns flowOf(null)

      val result = manager.restoreLastOpenedZimFileIfNeeded(false)

      assertThat(result).isNull()
      coVerify(exactly = 0) { zimReaderContainer.setZimReaderSource(any(), any()) }
    }

    @Test
    fun `reopens the last used zim file honouring the spell check flag`() = runTest {
      val zimFile = File.createTempFile("zim_file_manager_test", ".zim").apply {
        deleteOnExit()
      }
      every { zimReaderContainer.zimFileReader } returnsMany listOf(null, zimReader)
      every { kiwixDataStore.currentZimFile } returns flowOf(zimFile.absolutePath)

      val result = manager.restoreLastOpenedZimFileIfNeeded(true)

      assertThat(result).isEqualTo(ZimFileManager.OpenZimResult.Success(zimReader))
      coVerify {
        zimReaderContainer.setZimReaderSource(
          match { it?.file?.path == zimFile.absolutePath },
          true
        )
      }
    }
  }
}
