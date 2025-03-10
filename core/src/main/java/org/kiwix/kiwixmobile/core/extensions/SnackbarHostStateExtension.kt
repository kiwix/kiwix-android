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

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun SnackbarHostState.snack(
  message: String,
  actionLabel: String? = null,
  actionClick: (() -> Unit)? = null,
  // Default duration is 4 seconds.
  snackbarDuration: SnackbarDuration = SnackbarDuration.Short,
  lifecycleScope: CoroutineScope
) {
  lifecycleScope.launch {
    val result = showSnackbar(
      message = message,
      actionLabel = actionLabel?.uppercase(),
      duration = snackbarDuration
    )
    if (result == SnackbarResult.ActionPerformed) {
      actionClick?.invoke()
    }
  }
}
