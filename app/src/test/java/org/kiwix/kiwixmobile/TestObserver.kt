/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat

fun <T> Flow<T>.test(scope: TestScope, itemCountsToEmitInFlow: Int = 1): TestObserver<T> {
  val observer = TestObserver(scope, this, itemCountsToEmitInFlow)
  observer.startCollecting()
  return observer
}

class TestObserver<T>(
  private val scope: TestScope,
  private val flow: Flow<T>,
  private val itemCountsToEmitInFlow: Int
) {
  private val values = mutableListOf<T>()
  private val completionChannel = Channel<Unit>()
  private var job: Job? = null

  fun startCollecting() {
    job = scope.launch {
      flow.collect {
        values.add(it)
        completionChannel.send(Unit)
      }
    }
  }

  /**
   * Returns the list of values collected from the flow.
   *
   * If [shouldAwaitCompletion] is true, this method will suspend until the flow
   * signals completion, ensuring all values have been collected before returning.
   *
   * @param shouldAwaitCompletion Whether to wait for the flow to finish collecting before returning the results.
   * @return A mutable list of values emitted by the flow.
   */
  suspend fun getValues(shouldAwaitCompletion: Boolean = true): MutableList<T> {
    if (shouldAwaitCompletion) {
      awaitCompletion()
    }
    return values
  }

  private suspend fun awaitCompletion() {
    repeat(itemCountsToEmitInFlow) {
      completionChannel.receive()
    }
  }

  suspend fun assertValues(listValues: MutableList<T>): TestObserver<T> {
    awaitCompletion()
    assertThat(listValues).containsExactlyElementsOf(values)
    return this
  }

  suspend fun containsExactlyInAnyOrder(
    listValues: MutableList<T>,
    vararg values: T
  ): TestObserver<T> {
    awaitCompletion()
    assertThat(listValues).containsExactlyInAnyOrder(*values)
    return this
  }

  suspend fun assertLastValue(value: T): TestObserver<T> {
    awaitCompletion()
    assertThat(values.last()).isEqualTo(value)
    return this
  }

  suspend fun finish() {
    job?.cancelAndJoin()
  }

  suspend fun assertLastValue(value: (T) -> Boolean): TestObserver<T> {
    awaitCompletion()
    assertThat(values.last()).satisfies({ value(it) })
    return this
  }
}
