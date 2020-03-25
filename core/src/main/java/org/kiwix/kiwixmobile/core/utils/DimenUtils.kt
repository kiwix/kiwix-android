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
import androidx.appcompat.R.attr

/**
 * Created by hritik on 03/23/20.
 */
object DimenUtils {
  @JvmStatic fun getToolbarHeight(context: Context): Int {
    val t = TypedValue()
    context.theme.resolveAttribute(attr.actionBarSize, t, true)
    return context.resources.getDimensionPixelSize(t.resourceId)
  }

  @JvmStatic fun getWindowHeight(activity: Activity): Int {
    val displayMetrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
  }

  @JvmStatic fun getWindowWidth(activity: Activity): Int {
    val displayMetrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
  }
}
