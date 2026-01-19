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
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore.Companion.DEFAULT_ZOOM
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File

const val ZOOM_OFFSET = 2
const val ZOOM_SCALE = 25

abstract class CoreSettingsViewModel(
  val context: Application,
  val kiwixDataStore: KiwixDataStore,
  val dataSource: DataSource,
  val storageCalculator: StorageCalculator,
  val themeConfig: ThemeConfig,
  val alertDialogShower: DialogShower,
  val libkiwixBookmarks: LibkiwixBookmarks,
  val kiwixPermissionChecker: KiwixPermissionChecker
) : ViewModel() {
  data class SettingsUiState(
    val storageDeviceList: List<StorageDevice> = emptyList(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    val isLoadingStorageDetails: Boolean = true,
    val shouldShowLanguageCategory: Boolean = false,
    val shouldShowStorageCategory: Boolean = false,
    val shouldShowExternalLinkPreference: Boolean = false,
    val shouldShowPrefWifiOnlyPreference: Boolean = false,
    val versionInformation: String = "",
    val permissionItem: Pair<Boolean, String> = false to "",
  )

  abstract suspend fun setStorage()
  protected val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
  private val _actions = MutableSharedFlow<Action>()
  val actions: SharedFlow<Action> = _actions

  val themeLabel: StateFlow<String> = kiwixDataStore.appTheme
    .map { theme -> getLabelFor(theme) }
    .stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      getLabelFor(ThemeConfig.Theme.SYSTEM)
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
    viewModelScope.launch {
      kiwixDataStore.updateAppTheme(selectedMode)
    }
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

  fun setVersionCodeInformation() {
    _uiState.update { it.copy(versionInformation = "$versionName Build: $versionCode") }
  }

  private val versionCode: Int =
    context.packageManager
      .getPackageInformation(context.packageName, ZERO).getVersionCode()

  private val versionName: String =
    context.packageManager
      .getPackageInformation(context.packageName, ZERO).versionName.toString()

  fun clearHistory() {
    runCatching {
      viewModelScope.launch { dataSource.clearHistory() }
    }.onFailure {
      Log.e("SettingsPresenter", it.message, it)
    }
  }

  fun clearAllNotes() {
    viewModelScope.launch {
      if (!kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
        sendAction(
          Action.ShowSnackbar(
            context.getString(R.string.ext_storage_permission_not_granted),
            viewModelScope
          )
        )
        return@launch
      }
      if (File(AddNoteDialog.NOTES_DIRECTORY).deleteRecursively()) {
        sendAction(
          Action.ShowSnackbar(
            context.getString(R.string.notes_deletion_successful),
            viewModelScope
          )
        )
      }
    }
  }

  fun updateAppLanguage(selectedLangCode: String) {
    viewModelScope.launch {
      kiwixDataStore.setPrefLanguage(selectedLangCode)
    }
  }
}
