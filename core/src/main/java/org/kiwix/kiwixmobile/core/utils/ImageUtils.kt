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

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.view.View
import android.view.View.MeasureSpec

object ImageUtils {
  @JvmStatic
  fun getBitmapFromView(
    viewToDrawFrom: View,
    width: Int,
    height: Int
  ): Bitmap? {
    return getBitmapFromView(
      width,
      height,
      viewToDrawFrom
    )
  }

  private fun getBitmapFromView(width: Int, height: Int, viewToDrawFrom: View): Bitmap? {
    if (width <= 0 || height <= 0) {
      if (viewToDrawFrom.width <= 0 || viewToDrawFrom.height <= 0) {
        return viewToDrawFrom.measure(0, 0, MeasureSpec.UNSPECIFIED).let {
          if (it.width <= 0 || it.height <= 0) viewToDrawFrom.createBitmap()
          else layoutAndCreateBitmap(
            viewToDrawFrom,
            it
          )
        }
      }
      return viewToDrawFrom.createBitmap()
    }
    return layoutAndCreateBitmap(
      viewToDrawFrom,
      viewToDrawFrom.measure(width, height, MeasureSpec.EXACTLY)
    )
  }

  private fun layoutAndCreateBitmap(
    viewToDrawFrom: View,
    measuredView: MeasuredView
  ): Bitmap? = viewToDrawFrom.createBitmap(measuredView.height, measuredView.width)

  private fun View.createBitmap(): Bitmap? {
    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    draw(Canvas(bitmap))
    return bitmap
  }

  private fun View.createBitmap(height: Int, width: Int): Bitmap? {
    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    draw(Canvas(bitmap))
    return bitmap
  }

  private fun View.measure(
    width: Int,
    height: Int,
    measureSpec: Int = MeasureSpec.EXACTLY
  ): MeasuredView {
    measure(
      MeasureSpec.makeMeasureSpec(width, measureSpec),
      MeasureSpec.makeMeasureSpec(height, measureSpec)
    )
    return MeasuredView(this)
  }
}

@JvmInline
value class MeasuredView(private val view: View) {
  val width: Int
    get() = view.measuredWidth
  val height: Int
    get() = view.measuredHeight
}
