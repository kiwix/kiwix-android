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

package org.kiwix.kiwixmobile.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import org.kiwix.kiwixmobile.core.R

/**
 * Taken from
 * https://stackoverflow.com/questions/34181372/coordinatorlayout-inside-another-coordinatorlayout
 * By user Fabian https://stackoverflow.com/users/5343268/fabian
 * 2020-07-15
 */
class NestedCoordinatorLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  @AttrRes
  @SuppressLint("PrivateResource")
  defStyleAttr: Int = R.attr.coordinatorLayoutStyle
) : CoordinatorLayout(context, attrs, defStyleAttr), NestedScrollingChild3 {

  private val helper = NestedScrollingChildHelper(this)

  init {
    isNestedScrollingEnabled = true
  }

  override fun isNestedScrollingEnabled(): Boolean = helper.isNestedScrollingEnabled

  override fun setNestedScrollingEnabled(enabled: Boolean) {
    helper.isNestedScrollingEnabled = enabled
  }

  override fun hasNestedScrollingParent(type: Int): Boolean = helper.hasNestedScrollingParent(type)

  override fun hasNestedScrollingParent(): Boolean = helper.hasNestedScrollingParent()

  override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
    val superResult = super.onStartNestedScroll(child, target, axes, type)
    return startNestedScroll(axes, type) || superResult
  }

  override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
    val superResult = super.onStartNestedScroll(child, target, axes)
    return startNestedScroll(axes) || superResult
  }

  override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
    val superConsumed = intArrayOf(0, 0)
    super.onNestedPreScroll(target, dx, dy, superConsumed, type)
    val thisConsumed = intArrayOf(0, 0)
    dispatchNestedPreScroll(dx, dy, consumed, null, type)
    consumed[0] = superConsumed[0] + thisConsumed[0]
    consumed[1] = superConsumed[1] + thisConsumed[1]
  }

  override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
    val superConsumed = intArrayOf(0, 0)
    super.onNestedPreScroll(target, dx, dy, superConsumed)
    val thisConsumed = intArrayOf(0, 0)
    dispatchNestedPreScroll(dx, dy, consumed, null)
    consumed[0] = superConsumed[0] + thisConsumed[0]
    consumed[1] = superConsumed[1] + thisConsumed[1]
  }

  override fun onNestedScroll(
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray
  ) {
    dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type)
    super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
  }

  override fun onNestedScroll(
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int
  ) {
    super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
    dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type)
  }

  override fun onNestedScroll(
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int
  ) {
    super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
    dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null)
  }

  override fun onStopNestedScroll(target: View, type: Int) {
    super.onStopNestedScroll(target, type)
    stopNestedScroll(type)
  }

  override fun onStopNestedScroll(target: View) {
    super.onStopNestedScroll(target)
    stopNestedScroll()
  }

  override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
    val superResult = super.onNestedPreFling(target, velocityX, velocityY)
    return dispatchNestedPreFling(velocityX, velocityY) || superResult
  }

  override fun onNestedFling(
    target: View,
    velocityX: Float,
    velocityY: Float,
    consumed: Boolean
  ): Boolean {
    val superResult = super.onNestedFling(target, velocityX, velocityY, consumed)
    return dispatchNestedFling(velocityX, velocityY, consumed) || superResult
  }

  override fun startNestedScroll(axes: Int, type: Int): Boolean =
    helper.startNestedScroll(axes, type)

  override fun startNestedScroll(axes: Int): Boolean = helper.startNestedScroll(axes)

  override fun stopNestedScroll(type: Int) {
    helper.stopNestedScroll(type)
  }

  override fun stopNestedScroll() {
    helper.stopNestedScroll()
  }

  override fun dispatchNestedScroll(
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    offsetInWindow: IntArray?,
    type: Int,
    consumed: IntArray
  ) {
    helper.dispatchNestedScroll(
      dxConsumed,
      dyConsumed,
      dxUnconsumed,
      dyUnconsumed,
      offsetInWindow,
      type,
      consumed
    )
  }

  override fun dispatchNestedScroll(
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    offsetInWindow: IntArray?,
    type: Int
  ): Boolean = helper.dispatchNestedScroll(
    dxConsumed,
    dyConsumed,
    dxUnconsumed,
    dyUnconsumed,
    offsetInWindow,
    type
  )

  override fun dispatchNestedScroll(
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    offsetInWindow: IntArray?
  ): Boolean =
    helper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)

  override fun dispatchNestedPreScroll(
    dx: Int,
    dy: Int,
    consumed: IntArray?,
    offsetInWindow: IntArray?,
    type: Int
  ): Boolean = helper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

  override fun dispatchNestedPreScroll(
    dx: Int,
    dy: Int,
    consumed: IntArray?,
    offsetInWindow: IntArray?
  ): Boolean =
    helper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

  override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
    helper.dispatchNestedPreFling(velocityX, velocityY)

  override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean =
    helper.dispatchNestedFling(velocityX, velocityY, consumed)
}
