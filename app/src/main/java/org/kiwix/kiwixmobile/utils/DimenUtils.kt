/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * Created by gmon on 1/13/17.
 */

object DimenUtils {
  @JvmStatic
  fun getToolbarHeight(context: Context): Int {
    val t = TypedValue()
    context.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, t, true)
    return context.resources.getDimensionPixelSize(t.resourceId)
  }

  @JvmStatic
  fun getWindowHeight(activity: Activity) = activity.displayMetrics.heightPixels

  @JvmStatic
  fun getWindowWidth(activity: Activity) = activity.displayMetrics.widthPixels

  private val Activity.displayMetrics: DisplayMetrics
    get() = DisplayMetrics().apply { windowManager.defaultDisplay.getMetrics(this) }
}
