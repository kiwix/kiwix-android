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
import android.preference.PreferenceCategory
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_LANG
import org.kiwix.kiwixmobile.custom.BuildConfig

class CustomPrefsFragment : CorePrefsFragment() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (BuildConfig.ENFORCED_LANG == "") {
      setUpLanguageChooser(PREF_LANG)
    } else {
      preferenceScreen.removePreference(findPreference("pref_language"))
    }

    val notificationsCategory = findPreference("pref_extras") as PreferenceCategory
    notificationsCategory.removePreference(findPreference("pref_wifi_only"))
  }

  override fun setStorage() {
    preferenceScreen.removePreference(findPreference("pref_storage"))
  }
}
