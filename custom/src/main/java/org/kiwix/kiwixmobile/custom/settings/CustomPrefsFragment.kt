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
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_LANG
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_WIFI_ONLY
import org.kiwix.kiwixmobile.custom.BuildConfig

class CustomPrefsFragment : CorePrefsFragment() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    if (BuildConfig.ENFORCED_LANG.isEmpty()) {
      setUpLanguageChooser(PREF_LANG)
    } else {
      preferenceScreen.removePreference(findPreference("pref_language"))
    }
    preferenceScreen.removePreference(findPreference(PREF_WIFI_ONLY))
  }

  override fun setStorage() {
    preferenceScreen.removePreference(findPreference("pref_storage"))
  }
}
