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
import android.view.View
import androidx.lifecycle.lifecycleScope
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.settings.CoreSettingsFragment
import org.kiwix.kiwixmobile.main.KiwixMainActivity

class KiwixSettingsFragment : CoreSettingsFragment() {
  private var storageDeviceList: List<StorageDevice> = listOf()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    settingsScreenState.value.update {
      copy(
        shouldShowExternalLinkPreference = true,
        shouldShowLanguageCategory = true,
        shouldShowPrefWifiOnlyPreference = true,
      )
    }
    lifecycleScope.launch {
      setMangeExternalStoragePermission()
    }
  }

  override suspend fun setStorage() {
    settingsScreenState.value.update { copy(shouldShowStorageCategory = true) }
    if (storageDeviceList.isNotEmpty()) {
      // update the storage when user switch to other storage.
      setUpStoragePreference()
      return
    }
    showHideProgressBarWhileFetchingStorageInfo(true)
    storageDeviceList = (requireActivity() as KiwixMainActivity).getStorageDeviceList()
    showHideProgressBarWhileFetchingStorageInfo(false)
    setUpStoragePreference()
  }

  private fun setUpStoragePreference() {
    settingsScreenState.value.value =
      settingsScreenState.value.value.copy(storageDeviceList = emptyList())
    settingsScreenState.value.value =
      settingsScreenState.value.value.copy(storageDeviceList = storageDeviceList)
  }

  /**
   * Shows or hides the progress bar while the application is fetching
   * storage information in the background. The progress bar is displayed
   * with a title indicating that the storage information is being retrieved.
   *
   * @param show If true, the progress bar will be displayed; otherwise, it will be hidden.
   */
  private fun showHideProgressBarWhileFetchingStorageInfo(show: Boolean) {
    settingsScreenState.value.update { copy(isLoadingStorageDetails = show) }
  }

  private suspend fun setMangeExternalStoragePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
      kiwixDataStore?.isPlayStoreBuild?.first() == false
    ) {
      val externalStorageManager = Environment.isExternalStorageManager()
      val permissionSummary = if (externalStorageManager) {
        getString(R.string.allowed)
      } else {
        getString(R.string.not_allowed)
      }
      settingsScreenState.value.update { copy(permissionItem = true to permissionSummary) }
    }
  }

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as KiwixMainActivity).cachedComponent.inject(this)
  }
}
