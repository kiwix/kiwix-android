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

import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.CoroutineScope
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

data class SettingScreenState(
  /**
   * Manages the showing of storage list in setting screen.
   */
  val storageDeviceList: List<StorageDevice>,
  /**
   * Manages the showing of storage loading preference.
   */
  val isLoadingStorageDetails: Boolean,
  /**
   * Manages the show/hide of storage category.
   */
  val shouldShowStorageCategory: Boolean,
  val storageCalculator: StorageCalculator,
  val sharedPreferenceUtil: SharedPreferenceUtil,
  val kiwixDataStore: KiwixDataStore,
  val lifeCycleScope: CoroutineScope?,
  /**
   * Controls the visibility and summary of the "Permission" preference.
   *
   * A [Pair] containing:
   *  - [Boolean]: The first boolean shows/hides the "Permission" preference.
   *  - [String]: The second string is summary the "Permission" preference
   *              whether the permission is allowed or not.
   */
  val permissionItem: Pair<Boolean, String>,
  /**
   * Manages the showing of language category.
   */
  val shouldShowLanguageCategory: Boolean,
  /**
   * Callback when app's language changed.
   */
  val onLanguageChanged: () -> Unit,
  val versionInformation: String,
  /**
   * Manages the showing of external link popup preference.
   */
  val shouldShowExternalLinkPreference: Boolean,
  /**
   * Manages the showing of wifi only preference.
   */
  val shouldShowPrefWifiOnlyPreference: Boolean
)
