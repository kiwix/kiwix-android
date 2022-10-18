/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.NoMatchingViewException
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

class RecyclerViewItemCountAssertion private constructor(private val matcher: Matcher<Int>) :
  ViewAssertion {
  override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
    if (noViewFoundException != null) {
      throw noViewFoundException
    }
    val recyclerView = view as RecyclerView
    val adapter = recyclerView.adapter
    ViewMatchers.assertThat(adapter?.itemCount, matcher)
  }

  companion object {
    fun withItemCount(expectedCount: Int): RecyclerViewItemCountAssertion =
      withItemCount(CoreMatchers.`is`(expectedCount))

    private fun withItemCount(matcher: Matcher<Int>): RecyclerViewItemCountAssertion =
      RecyclerViewItemCountAssertion(matcher)
  }
}
