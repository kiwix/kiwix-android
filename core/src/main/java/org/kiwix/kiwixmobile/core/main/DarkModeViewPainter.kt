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
package org.kiwix.kiwixmobile.core.main

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import org.kiwix.kiwixmobile.core.DarkModeConfig
import javax.inject.Inject

/**
 * DarkModeViewPainter class is used to apply respective filters to the views
 * depending whether the app is in dark mode or not
 * Created by yashk2000 on 24/03/2020.
 */

class DarkModeViewPainter @Inject constructor(
  private val darkModeConfig: DarkModeConfig
) {

  private val invertedPaint =
    Paint().apply { colorFilter = ColorMatrixColorFilter(KiwixWebView.DARK_MODE_COLORS) }

  @JvmOverloads
  fun <T : View?> update(
    view: T,
    shouldActivateCriteria: ((T) -> Boolean) = { true },
    vararg additionalViews: View? = emptyArray()
  ) {
    if (darkModeConfig.isDarkModeActive()) {
      if (shouldActivateCriteria(view)) {
        activateDarkMode(view, *additionalViews)
      }
    } else {
      deactivateDarkMode(view, *additionalViews)
    }
  }

  private fun deactivateDarkMode(vararg additionalViews: View?) {
    additionalViews.filterNotNull()
      .forEach { it.setLayerType(View.LAYER_TYPE_NONE, null) }
  }

  private fun activateDarkMode(vararg additionalViews: View?) {
    additionalViews.filterNotNull()
      .forEach { it.setLayerType(View.LAYER_TYPE_HARDWARE, invertedPaint) }
  }
}
