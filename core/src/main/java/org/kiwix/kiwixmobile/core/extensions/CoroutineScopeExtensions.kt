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

package org.kiwix.kiwixmobile.core.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.utils.files.Log
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Safely runs a suspend function inside a lifecycle-aware CoroutineScope.
 *
 * This helper ensures that:
 * - The coroutine only starts if the Lifecycle's CoroutineScope is valid.
 * - Execution is skipped if the scope is `null` (lifecycle destroyed or not yet created).
 * - Execution is skipped if the scope is inactive/cancelled (e.g., view destroyed).
 * - Exceptions inside the launched coroutine are **not swallowed** and will propagate normally.
 *
 * Use this method when you want to launch work in a lifecycle scope without
 * risking crashes due to calling `launch` after the lifecycle has already ended.
 *
 * @param context Optional [CoroutineContext] used to launch the coroutine.
 * @param func The suspend function to run within the active CoroutineScope.
 */
fun CoroutineScope?.runSafelyInLifecycleScope(
  context: CoroutineContext = EmptyCoroutineContext,
  func: suspend CoroutineScope.() -> Unit
) {
  if (this == null || !this.isActive) {
    Log.w("Lifecycle", "Skipping execution: lifecycle not active")
    return
  }
  runCatching {
    launch(context) {
      func(this)
    }
  }.onFailure {
    when (it) {
      is CancellationException -> {
        // Expected lifecycle cancellation → ignore
        Log.d("Lifecycle", "Coroutine cancelled", it)
      }

      else -> {
        // Real bug
        Log.e("Lifecycle", "Unhandled coroutine error", it)
        throw it
      }
    }
  }
}
