/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.settings

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.kiwix.kiwixmobile.core.DarkModeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
  private val context: Application,
  val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {
  private val darkMode: StateFlow<DarkModeConfig.Mode> = sharedPreferenceUtil.darkModes()
    .stateIn(viewModelScope, SharingStarted.Eagerly, sharedPreferenceUtil.darkMode)

  val darkModeLabel: StateFlow<String> = darkMode
    .map { mode ->
      when (mode) {
        DarkModeConfig.Mode.ON -> context.getString(R.string.on)
        DarkModeConfig.Mode.OFF -> context.getString(R.string.off)
        DarkModeConfig.Mode.SYSTEM -> context.getString(R.string.auto)
      }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, getLabelFor(sharedPreferenceUtil.darkMode))

  var backToTopEnabled = mutableStateOf(sharedPreferenceUtil.prefBackToTop)

  var externalLinkPopup = mutableStateOf(sharedPreferenceUtil.prefExternalLinkPopup)

  val textZoom: StateFlow<Int> = sharedPreferenceUtil.textZooms
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = sharedPreferenceUtil.textZoom
    )

  var newTabInBackground = mutableStateOf(sharedPreferenceUtil.prefNewTabBackground)

  val wifiOnly: StateFlow<Boolean> = sharedPreferenceUtil.prefWifiOnlys
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = sharedPreferenceUtil.prefWifiOnly
    )

  private fun getLabelFor(mode: DarkModeConfig.Mode): String {
    return when (mode) {
      DarkModeConfig.Mode.ON -> context.getString(R.string.on)
      DarkModeConfig.Mode.OFF -> context.getString(R.string.off)
      DarkModeConfig.Mode.SYSTEM -> context.getString(R.string.auto)
    }
  }

  fun setDarkMode(selectedMode: String) {
    sharedPreferenceUtil.updateDarkMode(selectedMode)
  }

  fun setBackToTop(enabled: Boolean) {
    sharedPreferenceUtil.prefBackToTop = enabled
    backToTopEnabled.value = enabled
  }

  fun setTextZoom(zoom: Int) {
    sharedPreferenceUtil.textZoom = zoom
  }

  fun setNewTabInBackground(enabled: Boolean) {
    sharedPreferenceUtil.prefNewTabBackground = enabled
    newTabInBackground.value = enabled
  }

  fun setExternalLinkPopup(enabled: Boolean) {
    sharedPreferenceUtil.putPrefExternalLinkPopup(enabled)
    externalLinkPopup.value = enabled
  }

  fun setWifiOnly(wifiOnly: Boolean) {
    sharedPreferenceUtil.putPrefWifiOnly(wifiOnly)
  }
}
