/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

import android.content.res.Resources
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class RecyclerViewMatcher(private val recyclerViewId: Int) {

  fun atPosition(position: Int): Matcher<View?>? = atPositionOnView(position, -1)

  fun atPositionOnView(position: Int, targetViewId: Int): Matcher<View?>? {
    return object : TypeSafeMatcher<View?>() {
      var resources: Resources? = null
      var childView: View? = null

      override fun describeTo(description: Description) {
        var idDescription = recyclerViewId.toString()
        resources?.let { resource ->
          idDescription = try {
            resource.getResourceName(recyclerViewId)
          } catch (resourcesNotFoundException: Resources.NotFoundException) {
            "${arrayOf<Any>(Integer.valueOf(recyclerViewId))} (resource name not found)"
          }
        }
        description.appendText("with id: $idDescription")
      }

      override fun matchesSafely(view: View?): Boolean {
        resources = view?.resources
        if (childView == null) {
          val recyclerView =
            view?.rootView?.findViewById(recyclerViewId) as RecyclerView
          childView = if (recyclerView.id == recyclerViewId) {
            recyclerView.findViewHolderForAdapterPosition(position)?.itemView
          } else {
            return false
          }
        }
        return if (targetViewId == -1) {
          view == childView
        } else {
          val targetView: View? = childView?.findViewById(targetViewId)
          view == targetView
        }
      }
    }
  }
}
