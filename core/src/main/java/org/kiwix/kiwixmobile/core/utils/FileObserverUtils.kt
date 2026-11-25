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
import kotlinx.coroutines.delay

const val FILE_OBSERVER_RETRY_ATTEMPTS = 5
const val FILE_OBSERVER_RETRY_DELAY_MS = 500L

suspend fun FileObserver.startWatchingWithRetry(
  attempts: Int = FILE_OBSERVER_RETRY_ATTEMPTS,
  retryDelayMs: Long = FILE_OBSERVER_RETRY_DELAY_MS
): Boolean {
  repeat(attempts) { attempt ->
    try {
      startWatching()
      return true
    } catch (_: ConcurrentModificationException) {
      if (attempt < attempts - 1) delay(retryDelayMs)
    }
  }
  return false
}

fun FileObserver.safeStopWatching(attempts: Int = FILE_OBSERVER_RETRY_ATTEMPTS) {
  repeat(attempts) {
    try {
      stopWatching()
      return
    } catch (_: ConcurrentModificationException) {
      Thread.yield()
    }
  }
}
