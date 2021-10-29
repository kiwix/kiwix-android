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

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView

class CloseKeyboardOnScroll(
  private val onLayoutScrollListener: (RecyclerView, Int) -> Unit // here we are calling callback
) :
  RecyclerView.OnScrollListener() {
  override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
    super.onScrollStateChanged(recyclerView, newState)
    onLayoutScrollListener(
      recyclerView,
      newState
    ) // implement the callback by passing recyclerview and  newState
    super.onScrollStateChanged(recyclerView, newState)
  }

  companion object {
    fun classImplement(recyclerView: RecyclerView) {
      // This is called when scroll list changes and callback will be called here
      recyclerView.addOnScrollListener(CloseKeyboardOnScroll { _, newState ->
        // Then check here if new state is scroll then implement the hide method
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          recyclerView.hideKeyboard()
        }
      })
    }
  }
}

fun View.hideKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(windowToken, 0)
}
