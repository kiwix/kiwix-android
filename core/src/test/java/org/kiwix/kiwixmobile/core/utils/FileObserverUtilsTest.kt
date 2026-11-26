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
package org.kiwix.kiwixmobile.core.utils

import android.os.FileObserver
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FileObserverUtilsTest {
  @Test
  fun `startWatchingWithRetry retries after ConcurrentModificationException`() = runTest {
    val observer: FileObserver = mockk()
    every { observer.startWatching() } throws ConcurrentModificationException() andThen Unit

    assertThat(observer.startWatchingWithRetry()).isTrue()
    verify(exactly = 2) { observer.startWatching() }
  }

  @Test
  fun `startWatchingWithRetry returns false when all attempts fail`() = runTest {
    val observer: FileObserver = mockk()
    every { observer.startWatching() } throws ConcurrentModificationException()

    assertThat(observer.startWatchingWithRetry(attempts = 3)).isFalse()
    verify(exactly = 3) { observer.startWatching() }
  }

  @Test
  fun `safeStopWatching retries and does not throw`() {
    val observer: FileObserver = mockk()
    every { observer.stopWatching() } throws ConcurrentModificationException() andThen Unit

    observer.safeStopWatching()
    verify(exactly = 2) { observer.stopWatching() }
  }

  @Test
  fun `safeStopWatching swallows exception when all attempts fail`() {
    val observer: FileObserver = mockk()
    every { observer.stopWatching() } throws ConcurrentModificationException()

    observer.safeStopWatching(attempts = 2)
    verify(exactly = 2) { observer.stopWatching() }
  }

  @Test
  fun `startWatchingWithRetry succeeds on first attempt`() = runTest {
    val observer: FileObserver = mockk()
    every { observer.startWatching() } just runs

    assertThat(observer.startWatchingWithRetry()).isTrue()
    verify(exactly = 1) { observer.startWatching() }
  }
}
