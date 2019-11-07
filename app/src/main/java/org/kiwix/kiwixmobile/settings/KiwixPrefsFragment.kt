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
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_LANG
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_STORAGE
import java.io.File

class KiwixPrefsFragment : CorePrefsFragment() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setUpLanguageChooser(PREF_LANG)
  }

  override fun setStorage() {
    if (Environment.isExternalStorageEmulated()) {
      findPreference(PREF_STORAGE).title = sharedPreferenceUtil.getPrefStorageTitle("Internal")
    } else {
      findPreference(PREF_STORAGE).title = sharedPreferenceUtil.getPrefStorageTitle("External")
    }
    findPreference(PREF_STORAGE).summary =
      storageCalculator.calculateAvailableSpace(File(sharedPreferenceUtil.prefStorage))
  }
}
