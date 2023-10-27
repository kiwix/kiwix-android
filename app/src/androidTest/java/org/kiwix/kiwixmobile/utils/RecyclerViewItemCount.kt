/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.matcher.ViewMatchers.withId

class RecyclerViewItemCount(private val recyclerViewId: Int) {
  fun checkRecyclerViewCount(): Int {
    var recyclerViewItemCount = 0
    onView(withId(recyclerViewId))
      .check { view: View, noViewFoundException: NoMatchingViewException? ->
        if (noViewFoundException != null) {
          throw noViewFoundException
        }
        val recyclerView = view as RecyclerView
        // Get the item count from the RecyclerView
        recyclerViewItemCount = recyclerView.adapter?.itemCount ?: 0
      }
    return recyclerViewItemCount
  }
}
