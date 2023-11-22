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
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf.allOf

class RecyclerViewSelectedCheckBoxCountAssertion(
  private val recyclerViewId: Int,
  private val checkBoxId: Int
) {
  fun countCheckedCheckboxes(): Int {
    var checkedCount = 0

    // Find the RecyclerView
    val recyclerViewMatcher: Matcher<View> = allOf(
      isAssignableFrom(RecyclerView::class.java),
      isDisplayed(),
      withId(recyclerViewId)
    )

    // Use a custom ViewMatcher to find checkboxes that are checked
    val checkBoxMatcher: Matcher<View> = object : TypeSafeMatcher<View>() {
      override fun matchesSafely(view: View): Boolean =
        view is CheckBox && view.isChecked

      override fun describeTo(description: Description) {
        description.appendText("is checked")
      }
    }

    // Count the checked checkboxes
    onView(recyclerViewMatcher).check { view, noViewFoundException ->
      if (noViewFoundException != null) {
        throw noViewFoundException
      }
      val recyclerView = view as RecyclerView
      (0 until recyclerView.childCount)
        .asSequence()
        .map {
          // Check the checkbox directly without using inRoot
          recyclerView.getChildAt(it).findViewById<CheckBox>(checkBoxId)
        }
        .filter { it != null && checkBoxMatcher.matches(it) }
        .forEach { _ -> checkedCount++ }
    }

    return checkedCount
  }
}
