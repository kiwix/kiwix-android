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
import org.kiwix.kiwixmobile.core.NightModeConfig
import javax.inject.Inject

/**
 * NightModeViewPainter class is used to apply respective filters to the views
 * depending whether the app is in dark mode or not
 * Created by yashk2000 on 24/03/2020.
 */

class NightModeViewPainter @Inject constructor(
  private val nightModeConfig: NightModeConfig
) {

  private val invertedPaint =
    Paint().apply { colorFilter = ColorMatrixColorFilter(KiwixWebView.NIGHT_MODE_COLORS) }

  @JvmOverloads
  fun <T : View?> update(
    view: T,
    shouldActivateCriteria: ((T) -> Boolean) = { true },
    vararg additionalViews: View? = emptyArray()
  ) {
    if (nightModeConfig.isNightModeActive()) {
      if (shouldActivateCriteria(view)) {
        activateNightMode(view, *additionalViews)
      }
    } else {
      deactivateNightMode(view, *additionalViews)
    }
  }

  fun deactivateNightMode(vararg additionalViews: View?) {
    additionalViews.filterNotNull()
      .forEach { it.setLayerType(View.LAYER_TYPE_NONE, null) }
  }

  private fun activateNightMode(vararg additionalViews: View?) {
    additionalViews.filterNotNull()
      .forEach { it.setLayerType(View.LAYER_TYPE_HARDWARE, invertedPaint) }
  }
}
