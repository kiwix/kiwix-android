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
package org.kiwix.kiwixmobile.testutils

import android.view.View
import androidx.test.espresso.ViewAction
import android.widget.Checkable
import androidx.test.espresso.UiController
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.Description

object ViewActions {
  fun setChecked(checked: Boolean): ViewAction {
    return object : ViewAction {
      override fun getConstraints(): BaseMatcher<View?> {
        return object : BaseMatcher<View?>() {
          override fun matches(item: Any): Boolean =
            CoreMatchers.isA(Checkable::class.java).matches(item)

          override fun describeMismatch(item: Any, mismatchDescription: Description) {}
          override fun describeTo(description: Description) {}
        }
      }

      override fun getDescription(): String? = null

      override fun perform(uiController: UiController, view: View) {
        val checkableView = view as Checkable
        checkableView.isChecked = checked
      }
    }
  }
}
