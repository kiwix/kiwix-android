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
package org.kiwix.kiwixmobile.core.utils.dialog

import android.content.Context
import android.content.SharedPreferences

class RateAppCounter internal constructor(context: Context) {
  private var visitCounter: SharedPreferences

  init {
    visitCounter = context.getSharedPreferences(NO_THANKS_CLICKED, 0)
  }

  var noThanksState: Boolean
    get() = visitCounter.getBoolean(NO_THANKS_CLICKED, false)
    set(value) {
      visitCounter.edit().apply {
        putBoolean(NO_THANKS_CLICKED, value)
        apply()
      }
    }

  var count: Int
    get() = visitCounter.getInt("count", 0)
    set(count) {
      visitCounter.edit().apply {
        putInt("count", count)
        apply()
      }
    }

  companion object {
    private const val NO_THANKS_CLICKED = "clickedNoThanks"
  }
}
