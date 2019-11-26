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
package org.kiwix.kiwixmobile.core

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import org.kiwix.kiwixmobile.core.NightModeConfig.UiMode.OFF
import org.kiwix.kiwixmobile.core.NightModeConfig.UiMode.ON
import org.kiwix.kiwixmobile.core.NightModeConfig.UiMode.NOT_SET
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class NightModeConfig @Inject constructor(
  val sharedPreferenceUtil: SharedPreferenceUtil,
  val context: Context
) {

  fun init() {
    sharedPreferenceUtil.nightModes().subscribe(::setMode, Throwable::printStackTrace)
  }

  fun isNightModeActive() =
    when (uiMode()) {
      ON -> true
      OFF, NOT_SET -> false
    }

  private fun setMode(nightMode: Mode) {
    AppCompatDelegate.setDefaultNightMode(nightMode.value)
  }

  private fun uiMode() = UiMode.from(context.uiMode)

  enum class Mode(val value: Int) {
    ON(AppCompatDelegate.MODE_NIGHT_YES),
    OFF(AppCompatDelegate.MODE_NIGHT_NO),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
      @JvmStatic fun from(nightMode: Int) =
        values().firstOrNull { it.value == nightMode }
          ?: throw RuntimeException("Invalid night mode $nightMode")
    }
  }

  enum class UiMode(val value: Int) {
    ON(Configuration.UI_MODE_NIGHT_YES),
    OFF(Configuration.UI_MODE_NIGHT_NO),
    NOT_SET(Configuration.UI_MODE_NIGHT_UNDEFINED);

    companion object {
      @JvmStatic
      fun from(uiMode: Int) = values().firstOrNull { it.value == uiMode }
        ?: throw RuntimeException("Invalid night mode $uiMode")
    }
  }
}

private val Context.uiMode: Int
  get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
