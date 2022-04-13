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

import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_STORAGE

class KiwixPrefsFragment : CorePrefsFragment() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setUpLanguageChooser(SharedPreferenceUtil.PREF_LANG)
    setMangeExternalStoragePermission()
  }

  override fun setStorage() {
    findPreference<Preference>(PREF_STORAGE)?.title = getString(
      if (sharedPreferenceUtil.prefStorage == internalStorage()?.let(sharedPreferenceUtil::getActualPath)) R.string.internal_storage
      else R.string.external_storage
    )
    findPreference<Preference>(PREF_STORAGE)?.summary = storageCalculator.calculateAvailableSpace()
  }

  private fun internalStorage(): String? =
    ContextCompat.getExternalFilesDirs(requireContext(), null).firstOrNull()?.path

  private fun setMangeExternalStoragePermission() {
    val permissionPref = findPreference<Preference>(PREF_MANAGE_EXTERNAL_STORAGE_PERMISSION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      showPermissionPreference()
      val externalStorageManager = Environment.isExternalStorageManager()
      if (externalStorageManager) {
        permissionPref!!.setSummary(org.kiwix.kiwixmobile.core.R.string.allowed)
      } else {
        permissionPref!!.setSummary(org.kiwix.kiwixmobile.core.R.string.not_allowed)
      }
      permissionPref.onPreferenceClickListener =
        Preference.OnPreferenceClickListener {
          activity?.let(FragmentActivity::navigateToSettings)
          true
        }
    }
  }

  private fun showPermissionPreference() {
    val preferenceCategory = findPreference<PreferenceCategory>(
      PREF_PERMISSION
    )
    preferenceCategory!!.isVisible = true
  }

  companion object {
    const val PREF_MANAGE_EXTERNAL_STORAGE_PERMISSION =
      "pref_manage_external_storage"
  }
}
