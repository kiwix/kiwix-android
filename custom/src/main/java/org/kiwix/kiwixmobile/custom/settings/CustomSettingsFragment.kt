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

package org.kiwix.kiwixmobile.custom.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.settings.CoreSettingsFragment
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.main.CustomMainActivity

class CustomSettingsFragment : CoreSettingsFragment() {
  override suspend fun setStorage() {
    settingsScreenState.value.update { copy(shouldShowStorageCategory = false) }
  }

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as CustomMainActivity).cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (BuildConfig.DISABLE_EXTERNAL_LINK) {
      hideExternalLinksPreference()
    }
    settingsScreenState.value.update {
      copy(
        shouldShowPrefWifiOnlyPreference = false,
        shouldShowLanguageCategory = BuildConfig.ENFORCED_LANG.isEmpty(),
        permissionItem = false to ""
      )
    }
  }

  /**
   * If "external links" are disabled in a custom app,
   * this function hides the external links preference from settings
   * and sets the shared preference to not show the external link popup
   * when opening external links.
   */
  private fun hideExternalLinksPreference() {
    settingsScreenState.value.update { copy(shouldShowExternalLinkPreference = false) }
    lifecycleScope.launch {
      kiwixDataStore?.setExternalLinkPopup(false)
    }
  }
}
