/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.app.Application
import kotlinx.coroutines.flow.update
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.settings.viewmodel.CoreSettingsViewModel
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import javax.inject.Inject

class CustomSettingsViewModel @Inject constructor(
  context: Application,
  kiwixDataStore: KiwixDataStore,
  dataSource: DataSource,
  storageCalculator: StorageCalculator,
  themeConfig: ThemeConfig,
  alertDialogShower: DialogShower,
  libkiwixBookmarks: LibkiwixBookmarks,
  kiwixPermissionChecker: KiwixPermissionChecker
) : CoreSettingsViewModel(
  context,
  kiwixDataStore,
  dataSource,
  storageCalculator,
  themeConfig,
  alertDialogShower,
  libkiwixBookmarks,
  kiwixPermissionChecker
) {
  override suspend fun setStorage() {
    _uiState.update {
      it.copy(
        shouldShowStorageCategory = true,
        isLoadingStorageDetails = false
      )
    }
  }
}
