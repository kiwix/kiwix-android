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
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

open class OnSwipeTouchListener constructor(context: Context) : OnTouchListener {
  private val gestureDetector: GestureDetector

  init {
    gestureDetector = GestureDetector(context, GestureListener())
  }

  @SuppressLint("ClickableViewAccessibility") override fun onTouch(
    view: View,
    event: MotionEvent
  ): Boolean = gestureDetector.onTouchEvent(event)

  private inner class GestureListener : SimpleOnGestureListener() {
    private val tag = "GestureListener"
    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100

    override fun onDown(event: MotionEvent): Boolean = true

    override fun onSingleTapUp(event: MotionEvent): Boolean {
      onTap(event)
      return super.onSingleTapUp(event)
    }

    // See:- https://stackoverflow.com/questions/73463685/gesturedetector-ongesturelistener-overridden-methods-are-not-working-in-android
    @Suppress("NOTHING_TO_OVERRIDE", "ACCIDENTAL_OVERRIDE")
    @SuppressLint("NestedBlockDepth, ReturnCount")
    override fun onFling(
      e1: MotionEvent,
      e2: MotionEvent,
      velocityX: Float,
      velocityY: Float
    ): Boolean {
      try {
        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x
        if (abs(diffX) > abs(diffY)) {
          if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
            if (diffX > 0) {
              onSwipeRight()
            } else {
              onSwipeLeft()
            }
            return true
          }
        } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
          if (diffY > 0) {
            onSwipeBottom()
          }
          return true
        }
      } catch (exception: Exception) {
        Log.e(tag, "Exception in onFling", exception)
      }
      return false
    }
  }

  open fun onSwipeRight() {}
  open fun onSwipeLeft() {}
  open fun onSwipeBottom() {}
  open fun onTap(e: MotionEvent?) {}
}
