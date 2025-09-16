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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class ThemeConfig @Inject constructor(
  val sharedPreferenceUtil: SharedPreferenceUtil,
  val context: Context
) {
  fun init() {
    CoroutineScope(Dispatchers.Main).launch {
      sharedPreferenceUtil.appThemes().collect {
        setMode(it)
      }
    }
  }

  fun isDarkTheme() =
    when (sharedPreferenceUtil.appTheme) {
      Theme.DARK -> true
      Theme.LIGHT -> false
      Theme.SYSTEM -> uiMode() == UiMode.DARK
    }

  private fun setMode(theme: Theme) {
    AppCompatDelegate.setDefaultNightMode(theme.value)
  }

  private fun uiMode() = UiMode.from(context.uiMode)

  enum class Theme(val value: Int) {
    DARK(AppCompatDelegate.MODE_NIGHT_YES),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
      @JvmStatic fun from(theme: Int) =
        Theme.entries.firstOrNull { it.value == theme }
          ?: throw RuntimeException("Invalid theme $theme")
    }
  }

  enum class UiMode(val value: Int) {
    DARK(Configuration.UI_MODE_NIGHT_YES),
    LIGHT(Configuration.UI_MODE_NIGHT_NO),
    NOT_SET(Configuration.UI_MODE_NIGHT_UNDEFINED),
    UNKNOWN(Configuration.UI_MODE_NIGHT_MASK); // Value returned from amazon devices

    companion object {
      @JvmStatic
      fun from(uiMode: Int) =
        UiMode.entries.firstOrNull { it.value == uiMode }
          ?: throw RuntimeException("Invalid theme $uiMode")
    }
  }
}

private val Context.uiMode: Int
  get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
