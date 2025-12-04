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

package org.kiwix.kiwixmobile.core.settings.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.DEFAULT_ZOOM
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

const val ZOOM_OFFSET = 2
const val ZOOM_SCALE = 25

class SettingsViewModel @Inject constructor(
  private val context: Application,
  val sharedPreferenceUtil: SharedPreferenceUtil,
  val kiwixDataStore: KiwixDataStore
) : ViewModel() {
  private val _actions = MutableSharedFlow<Action>()
  val actions: SharedFlow<Action> = _actions
  private val appTheme: StateFlow<ThemeConfig.Theme> = sharedPreferenceUtil.appThemes()
    .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, sharedPreferenceUtil.appTheme)

  val themeLabel: StateFlow<String> = appTheme
    .map { mode ->
      when (mode) {
        ThemeConfig.Theme.DARK -> context.getString(R.string.theme_dark)
        ThemeConfig.Theme.LIGHT -> context.getString(R.string.theme_light)
        ThemeConfig.Theme.SYSTEM -> context.getString(R.string.theme_system)
      }
    }
    .stateIn(
      viewModelScope,
      SharingStarted.Companion.Eagerly,
      getLabelFor(sharedPreferenceUtil.appTheme)
    )

  val backToTopEnabled = kiwixDataStore.backToTop
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = false
    )

  val externalLinkPopup = kiwixDataStore.externalLinkPopup
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = true
    )

  val textZoom: StateFlow<Int> = kiwixDataStore.textZoom
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = DEFAULT_ZOOM
    )

  val newTabInBackground = kiwixDataStore.openNewTabInBackground
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = false
    )

  val wifiOnly: StateFlow<Boolean> = kiwixDataStore.wifiOnly
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Companion.Eagerly,
      initialValue = true
    )

  fun sendAction(action: Action) =
    viewModelScope.launch {
      _actions.emit(action)
    }

  private fun getLabelFor(theme: ThemeConfig.Theme): String {
    return when (theme) {
      ThemeConfig.Theme.DARK -> context.getString(R.string.theme_dark)
      ThemeConfig.Theme.LIGHT -> context.getString(R.string.theme_light)
      ThemeConfig.Theme.SYSTEM -> context.getString(R.string.theme_system)
    }
  }

  fun setAppTheme(selectedMode: String) {
    sharedPreferenceUtil.updateAppTheme(selectedMode)
  }

  fun setBackToTop(enabled: Boolean) {
    viewModelScope.launch {
      kiwixDataStore.setPrefBackToTop(enabled)
    }
  }

  fun setTextZoom(position: Int) {
    viewModelScope.launch {
      kiwixDataStore.setTextZoom((position + ZOOM_OFFSET) * ZOOM_SCALE)
    }
  }

  fun setNewTabInBackground(enabled: Boolean) {
    viewModelScope.launch {
      kiwixDataStore.setOpenNewInBackground(enabled)
    }
  }

  fun setExternalLinkPopup(enabled: Boolean) {
    viewModelScope.launch {
      kiwixDataStore.setExternalLinkPopup(enabled)
    }
  }

  fun setWifiOnly(wifiOnly: Boolean) {
    viewModelScope.launch {
      kiwixDataStore.setWifiOnly(wifiOnly)
    }
  }
}
