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

package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

const val ONE_THOUSAND = 1000

@Composable
fun rememberLazyListScrollListener(
  onScrollChanged: (ScrollDirection) -> Unit,
  scrollThreshold: Int = 20
): LazyListState {
  val lazyListState = rememberLazyListState()
  val updatedOnScrollChanged = rememberUpdatedState(onScrollChanged)

  var previousScrollPosition by remember { mutableIntStateOf(0) }
  var lastScrollDirection by remember { mutableStateOf(ScrollDirection.IDLE) }

  LaunchedEffect(lazyListState) {
    snapshotFlow {
      lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
    }.collect { (index, offset) ->
      val currentScrollPosition = index * ONE_THOUSAND + offset
      val scrollDelta = currentScrollPosition - previousScrollPosition

      val scrollDirection = when {
        scrollDelta > scrollThreshold -> ScrollDirection.SCROLL_DOWN
        scrollDelta < -scrollThreshold -> ScrollDirection.SCROLL_UP
        else -> lastScrollDirection
      }

      if (scrollDirection != lastScrollDirection) {
        lastScrollDirection = scrollDirection
        updatedOnScrollChanged.value(scrollDirection)
      }

      previousScrollPosition = currentScrollPosition
    }
  }

  return lazyListState
}

enum class ScrollDirection {
  SCROLL_UP,
  SCROLL_DOWN,
  IDLE
}
