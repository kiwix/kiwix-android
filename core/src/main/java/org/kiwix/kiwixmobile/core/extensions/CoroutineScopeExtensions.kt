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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.utils.files.Log

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
 * @param func The suspend function to run within the active CoroutineScope.
 */
fun CoroutineScope?.runSafelyInLifecycleScope(func: suspend CoroutineScope.() -> Unit) {
  // If lifecycleScope is null or already cancelled, skip execution safely.
  if (this == null || !this.isActive) {
    Log.w("Lifecycle", "Skipping execution: lifecycle not active")
    return
  }
  // Launch the block normally. Errors inside this coroutine are not swallowed.
  launch {
    func(this)
  }
}
