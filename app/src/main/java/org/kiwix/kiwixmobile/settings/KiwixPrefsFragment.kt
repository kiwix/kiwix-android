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

package org.kiwix.kiwixmobile.settings

import android.os.Bundle
import android.os.Environment
import androidx.preference.Preference
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_STORAGE

class KiwixPrefsFragment : CorePrefsFragment() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setUpLanguageChooser(SharedPreferenceUtil.PREF_LANG)
  }

  override fun setStorage() {
    if (Environment.isExternalStorageEmulated()) {
      findPreference<Preference>(PREF_STORAGE)?.title =
        sharedPreferenceUtil.getPrefStorageTitle("Internal")
    } else {
      findPreference<Preference>(PREF_STORAGE)?.title =
        sharedPreferenceUtil.getPrefStorageTitle("External")
    }
    findPreference<Preference>(PREF_STORAGE)?.summary = storageCalculator.calculateAvailableSpace()
  }
}
