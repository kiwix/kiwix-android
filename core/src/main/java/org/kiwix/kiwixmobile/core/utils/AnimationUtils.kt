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
package org.kiwix.kiwixmobile.core.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat

private const val HEIGHT_ANIM_DURATION = 100L
private const val ROTATE_ANIM_DURATION = 200L
private const val FULL_ROTATION = 360.0f

object AnimationUtils {
  @JvmStatic fun View.expand() {
    measure(MATCH_PARENT, WRAP_CONTENT)

    // Older versions of android (pre API 21) cancel animations for views with a height of 0.
    layoutParams.height = 1
    visibility = View.VISIBLE

    animateHeight(1, measuredHeight) {
      layoutParams.height = WRAP_CONTENT
    }
  }

  @JvmStatic fun View.collapse() {
    animateHeight(measuredHeight, 0) { visibility = View.GONE }
  }

  @SuppressLint("Recycle")
  private fun View.animateHeight(start: Int, end: Int, onEndAction: () -> Unit) {
    ValueAnimator.ofInt(start, end).apply {
      addUpdateListener {
        layoutParams.height = it.animatedValue as Int
        requestLayout()
      }
      addListener(onEnd = { onEndAction.invoke() })
      duration = HEIGHT_ANIM_DURATION
      interpolator = LinearInterpolator()
    }.start()
  }

  @JvmStatic fun FloatingActionButton.rotate() {
    animate().apply {
      withStartAction { setImageDrawableCompat(R.drawable.ic_close_black_24dp) }
      withEndAction { setImageDrawableCompat(R.drawable.ic_done_white_24dp) }
      rotationBy(FULL_ROTATION)
      duration = ROTATE_ANIM_DURATION
    }.start()
  }
}
