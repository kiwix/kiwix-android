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

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.appcompat.R

object DimenUtils {
  @JvmStatic fun Context.getToolbarHeight(): Int {
    return resources.getDimensionPixelSize(TypedValue().apply {
      theme.resolveAttribute(R.attr.actionBarSize, this, true)
    }.resourceId)
  }

  @JvmStatic fun Activity.getWindowHeight(): Int =
    computedDisplayMetric.heightPixels

  @JvmStatic fun Activity.getWindowWidth(): Int =
    computedDisplayMetric.widthPixels

  private val Activity.computedDisplayMetric
    get() = DisplayMetrics().apply { windowManager.defaultDisplay.getMetrics(this) }
}
