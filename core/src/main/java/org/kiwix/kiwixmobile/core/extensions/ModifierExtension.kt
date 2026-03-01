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

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val STORAGE_CHECK_CLICK_THROTTLE_DELAY_MS = 1500L

/**
 * A [Modifier] that prevents rapid repeated click or long-click events by
 * automatically throttling interactions for a given time window.
 *
 * Once a click or long-click is triggered, the modifier becomes temporarily
 * disabled and ignores further interactions until [delayMs] has elapsed.
 * This helps avoid:
 * - Multiple rapid taps triggering the same action
 * - Accidental double navigation
 * - Repeated permission dialogs or storage checks
 *
 * Throttling is handled internally by the modifier, so callers do not need
 * to manually check or invoke any throttle logic. This makes the behavior
 * safe and hard to misuse.
 *
 * ### Usage:
 *
 * ```
 * Modifier.throttledClickable {
 *   onItemClicked()
 * }
 * ```
 *
 * @param delayMs Duration (in milliseconds) for which subsequent clicks
 * are ignored after a click or long-click occurs.
 * @param enabled Whether the click handling is enabled. When `false`,
 * all click interactions are disabled regardless of throttle state.
 * @param onClick Callback invoked when the user performs a click.
 * @param onLongClick Optional callback invoked when the user performs
 * a long-click. Long-clicks are throttled using the same delay window.
 *
 * @return A modified [Modifier] that applies throttled click handling.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.throttledClickable(
  delayMs: Long = STORAGE_CHECK_CLICK_THROTTLE_DELAY_MS,
  enabled: Boolean = true,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null
): Modifier = composed {
  var isThrottled by remember { mutableStateOf(false) }

  LaunchedEffect(isThrottled) {
    if (isThrottled) {
      delay(delayMs)
      isThrottled = false
    }
  }

  combinedClickable(
    enabled = enabled && !isThrottled,
    onClick = {
      isThrottled = true
      onClick()
    },
    onLongClick = onLongClick?.let {
      {
        isThrottled = true
        it()
      }
    }
  )
}

@Composable
fun Modifier.hideKeyboardOnLazyColumnScroll(lazyListState: LazyListState): Modifier {
  val context = LocalContext.current

  LaunchedEffect(lazyListState) {
    snapshotFlow { lazyListState.isScrollInProgress }
      .distinctUntilChanged()
      .filter { it }
      .collectLatest {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow((context as? Activity)?.currentFocus?.windowToken, 0)
      }
  }

  return this
}

fun Modifier.bottomShadow(shadow: Dp) =
  clip(
    GenericShape { size, _ ->
      lineTo(size.width, 0f)
      lineTo(size.width, Float.MAX_VALUE)
      lineTo(0f, Float.MAX_VALUE)
    }
  ).shadow(shadow)
