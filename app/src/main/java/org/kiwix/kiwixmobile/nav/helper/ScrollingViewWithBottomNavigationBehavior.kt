/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.helper

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.kiwix.kiwixmobile.R

/**
 * Inspired by James @ https://stackoverflow.com/users/165783/james
 * for his answer on https://stackoverflow.com/questions/47917354/coordinatorlayout-content-child-overlaps-bottomnavigationview
 * 2020-07-15
 */
class ScrollingViewWithBottomNavigationBehavior(context: Context, attrs: AttributeSet) :
  AppBarLayout.ScrollingViewBehavior(context, attrs) {
  private var bottomMargin = 0

  companion object {
    const val DEFAULT_BOTTOM_NAVIGATION_MARGIN_ON_HIDDEN = -2
    const val EXTRA_MARGIN_FOR_BOTTOM_BAR = 30
  }

  override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean =
    super.layoutDependsOn(parent, child, dependency) || dependency is BottomNavigationView

  override fun onDependentViewChanged(
    parent: CoordinatorLayout,
    child: View,
    dependency: View
  ): Boolean {
    val result = super.onDependentViewChanged(parent, child, dependency)
    if (dependency is BottomNavigationView) {
      if (moveFragmentWithNavigationBar(child, dependency, parent)) return true
    }
    return result
  }

  private fun moveFragmentWithNavigationBar(
    fragmentContainer: View,
    navigationBar: BottomNavigationView,
    coordinatorLayout: CoordinatorLayout
  ): Boolean {
    val readerBottomAppBar: BottomAppBar? =
      fragmentContainer.findViewById(org.kiwix.kiwixmobile.core.R.id.bottom_toolbar)
    if (readerBottomAppBar != null) {
      val newBottomMargin = calculateNewBottomMargin(navigationBar, coordinatorLayout)
      if (newBottomMargin != bottomMargin) {
        changeReaderToolbarMargin(newBottomMargin, fragmentContainer)
        return true
      }
    }
    return false
  }

  private fun calculateNewBottomMargin(
    navigationBar: BottomNavigationView,
    coordinatorLayout: CoordinatorLayout
  ): Int {
    var newBottomMargin = (coordinatorLayout.height - navigationBar.y).toInt()
    if (newBottomMargin >= DEFAULT_BOTTOM_NAVIGATION_MARGIN_ON_HIDDEN) {
      newBottomMargin -= EXTRA_MARGIN_FOR_BOTTOM_BAR
    }
    return newBottomMargin
  }

  private fun changeReaderToolbarMargin(
    newBottomMargin: Int,
    fragmentContainer: View
  ) {
    bottomMargin = newBottomMargin
    val layout = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
    layout.bottomMargin = bottomMargin
    fragmentContainer.requestLayout()
  }
}
