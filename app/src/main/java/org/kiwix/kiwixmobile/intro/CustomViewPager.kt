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
package org.kiwix.kiwixmobile.intro

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.viewpager.widget.ViewPager

/**
 * A custom implementation of [ViewPager] to decrease the speed of auto-scroll animation
 * of [ViewPager].
 */
const val DURATION_MULTIPLIER = 3

class CustomViewPager : ViewPager {
  constructor(context: Context?) : super(context!!) {
    postInitViewPager()
  }

  constructor(context: Context?, attrs: AttributeSet?) : super(
    context!!,
    attrs
  ) {
    postInitViewPager()
  }

  /**
   * Override the [Scroller] instance with our own class so we can change the
   * duration
   */
  private fun postInitViewPager() {
    try {
      val scroller = ViewPager::class.java.getDeclaredField("mScroller")
      scroller.isAccessible = true
      val interpolator =
        ViewPager::class.java.getDeclaredField("sInterpolator")
      interpolator.isAccessible = true
      val customScroller = CustomScroller(
        context,
        interpolator[null] as Interpolator
      )
      scroller[this] = customScroller
    } catch (numberFormatException: NumberFormatException) {
      Log.e("CustomViewPager", "$numberFormatException")
    }
  }

  internal inner class CustomScroller(
    context: Context?,
    interpolator: Interpolator?
  ) : Scroller(context, interpolator) {
    override fun startScroll(
      startX: Int,
      startY: Int,
      dx: Int,
      dy: Int,
      duration: Int
    ) {
      super.startScroll(startX, startY, dx, dy, duration * DURATION_MULTIPLIER)
    }
  }
}
