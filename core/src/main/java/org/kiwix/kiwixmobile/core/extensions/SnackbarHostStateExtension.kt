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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberSnackbarHostState(): SnackbarHostState = remember { SnackbarHostState() }

fun SnackbarHostState.snack(
  message: String,
  actionLabel: String? = null,
  actionClick: (() -> Unit)? = null,
  lifecycleScope: CoroutineScope
) {
  lifecycleScope.launch {
    val result = showSnackbar(
      message = message,
      actionLabel = actionLabel
    )
    if (result == SnackbarResult.ActionPerformed) {
      actionClick?.invoke()
    }
  }
}
