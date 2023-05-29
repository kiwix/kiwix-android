/*
 * Kiwix Android
 * Copyright (c) 2021 Kiwix <android.kiwix.org>
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

import androidx.recyclerview.widget.RecyclerView

class SimpleRecyclerViewScrollListener(
  private val onLayoutScrollListener: (RecyclerView, Int) -> Unit
) :
  RecyclerView.OnScrollListener() {
  override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
    super.onScrollStateChanged(recyclerView, newState)
    onLayoutScrollListener(
      recyclerView,
      newState
    )
  }

  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    super.onScrolled(recyclerView, dx, dy)
    val currentScrollPosition = recyclerView.computeVerticalScrollOffset()

    if (currentScrollPosition > previousScrollPosition) {
      onLayoutScrollListener(
        recyclerView,
        SCROLL_DOWN
      )
    } else if (currentScrollPosition < previousScrollPosition) {
      onLayoutScrollListener(
        recyclerView,
        SCROLL_UP
      )
    }

    previousScrollPosition = currentScrollPosition
  }

  private var previousScrollPosition = 0

  companion object {
    const val SCROLL_DOWN = 2000
    const val SCROLL_UP = 2001
  }
}
