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
package org.kiwix.kiwixmobile.core.main

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

open class OnSwipeTouchListener @SuppressLint("SyntheticAccessor")
constructor(ctx: Context?) : OnTouchListener {
  private val gestureDetector: GestureDetector

  init {
    gestureDetector = GestureDetector(ctx, GestureListener())
  }

  companion object {
    private const val SWIPE_THRESHOLD = 100
    private const val SWIPE_VELOCITY_THRESHOLD = 100
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(
    v: View,
    event: MotionEvent
  ): Boolean = gestureDetector.onTouchEvent(event)

  private inner class GestureListener : SimpleOnGestureListener() {
    override fun onDown(e: MotionEvent): Boolean = true

    override fun onSingleTapUp(e: MotionEvent): Boolean {
      onTap(e)
      return super.onSingleTapUp(e)
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    override fun onFling(
      e1: MotionEvent,
      e2: MotionEvent,
      velocityX: Float,
      velocityY: Float
    ): Boolean {
      var result = false
      try {
        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x
        if (abs(diffX) > abs(diffY)) {
          if (abs(diffX) > Companion.SWIPE_THRESHOLD && abs(velocityX)
            > Companion.SWIPE_VELOCITY_THRESHOLD
          ) {
            if (diffX > 0) {
              onSwipeRight()
            } else {
              onSwipeLeft()
            }
            result = true
          }
        } else if (abs(diffY) > Companion.SWIPE_THRESHOLD &&
          abs(velocityY) > Companion.SWIPE_VELOCITY_THRESHOLD
        ) {
          if (diffY > 0) {
            onSwipeBottom()
          } else {
            onSwipeTop()
          }
          result = true
        }
      } catch (exception: Exception) {
        exception.printStackTrace()
      }
      return result
    }
  }

  open fun onSwipeRight() {}
  open fun onSwipeLeft() {}

  @SuppressWarnings("EmptyFunctionBlock")
  fun onSwipeTop() {
  }

  open fun onSwipeBottom() {}
  open fun onTap(e: MotionEvent?) {}
}
