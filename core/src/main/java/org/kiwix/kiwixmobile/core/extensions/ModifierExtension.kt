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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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
