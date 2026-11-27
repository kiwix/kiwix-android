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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Factory

@OptIn(ExperimentalCoroutinesApi::class)
class ZimReaderContainerTest {
  private val dispatcher = UnconfinedTestDispatcher()
  private val factory: Factory = mockk()
  private val container = ZimReaderContainer(factory, dispatcher)

  @Test
  fun `setZimReaderSource serializes concurrent restores instead of racing disposal`() =
    runTest(dispatcher) {
      val sourceA = mockk<ZimReaderSource>()
      val sourceB = mockk<ZimReaderSource>()
      val readerA = mockk<ZimFileReader>(relaxed = true)
      val readerB = mockk<ZimFileReader>(relaxed = true)
      every { readerA.zimReaderSource } returns sourceA
      every { readerB.zimReaderSource } returns sourceB

      val openingA = CompletableDeferred<Unit>()
      val releaseA = CompletableDeferred<Unit>()

      coEvery { sourceA.exists(dispatcher) } returns true
      coEvery { sourceA.canOpenInLibkiwix(dispatcher) } returns true
      coEvery { factory.create(sourceA, false) } coAnswers {
        // Simulates a slow native open still in flight.
        openingA.complete(Unit)
        releaseA.await()
        readerA
      }

      coEvery { sourceB.exists(dispatcher) } returns true
      coEvery { sourceB.canOpenInLibkiwix(dispatcher) } returns true
      coEvery { factory.create(sourceB, false) } returns readerB

      // Simulates the Search screen and the Reader screen both trying to restore the
      // last used ZIM file around the same time after process death.
      val jobA = async { container.setZimReaderSource(sourceA) }
      openingA.await()

      val jobB = async { container.setZimReaderSource(sourceB) }
      // B must block on the mutex while A's open is still in flight. Without the lock,
      // B could run to completion here, assigning (and disposing) a reader while A is
      // still relying on the container being mid-restore.
      assertThat(jobB.isCompleted).isFalse()

      releaseA.complete(Unit)
      advanceUntilIdle()
      jobA.await()
      jobB.await()

      assertThat(container.zimFileReader).isEqualTo(readerB)
    }

  @Test
  fun `setZimReaderSource is a no-op when the source is unchanged`() = runTest(dispatcher) {
    val source = mockk<ZimReaderSource>()
    val reader = mockk<ZimFileReader>(relaxed = true)
    every { reader.zimReaderSource } returns source
    coEvery { source.exists(dispatcher) } returns true
    coEvery { source.canOpenInLibkiwix(dispatcher) } returns true
    coEvery { factory.create(source, false) } returns reader

    container.setZimReaderSource(source)
    container.setZimReaderSource(source)

    coVerify(exactly = 1) { factory.create(source, false) }
    assertThat(container.zimFileReader).isEqualTo(reader)
  }
}
