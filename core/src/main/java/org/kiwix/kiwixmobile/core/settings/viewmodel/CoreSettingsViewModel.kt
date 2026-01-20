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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mhutti1.utils.storage.StorageDevice
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
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.extensions.runSafelyInLifecycleScope
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ExportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.RequestWriteStoragePermission
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ShowSnackbar
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore.Companion.DEFAULT_ZOOM
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.OpenCredits
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

const val ZOOM_OFFSET = 2
const val ZOOM_SCALE = 25
const val ZERO_POINT_SEVEN = 0.7

@Suppress("LongParameterList")
abstract class CoreSettingsViewModel(
  val context: Application,
  val kiwixDataStore: KiwixDataStore,
  val dataSource: DataSource,
  val storageCalculator: StorageCalculator,
  val themeConfig: ThemeConfig,
  val libkiwixBookmarks: LibkiwixBookmarks,
  val kiwixPermissionChecker: KiwixPermissionChecker
) : ViewModel() {
  data class PermissionLaunchersForSettingScreen(
    val writeStoragePermission: ManagedActivityResultLauncher<String, Boolean>,
    val filePicker: ManagedActivityResultLauncher<Intent, ActivityResult>
  )

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

  abstract suspend fun setStorage(coreMainActivity: CoreMainActivity)
  abstract suspend fun showExternalLinksPreference()
  abstract suspend fun showPrefWifiOnlyPreference()
  abstract suspend fun showPermissionItem()
  abstract suspend fun showLanguageCategory()

  protected val settingsUiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = settingsUiState.asStateFlow()
  private val _actions = MutableSharedFlow<Action>()
  val actions: SharedFlow<Action> = _actions
  lateinit var alertDialogShower: AlertDialogShower

  suspend fun initialize(activity: CoreMainActivity) {
    setStorage(activity)
    showExternalLinksPreference()
    showPrefWifiOnlyPreference()
    showPermissionItem()
    showLanguageCategory()
    setVersionCodeInformation()
  }

  fun setAlertDialog(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

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

  private fun setVersionCodeInformation() {
    settingsUiState.update { it.copy(versionInformation = "$versionName Build: $versionCode") }
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
      sendAction(
        ShowSnackbar(
          context.getString(R.string.all_history_cleared),
          viewModelScope
        )
      )
    }.onFailure {
      Log.e("SettingsPresenter", it.message, it)
    }
  }

  fun clearAllNotes() {
    viewModelScope.launch {
      if (!instance.isExternalStorageWritable) {
        sendAction(
          ShowSnackbar(
            context.getString(R.string.notes_deletion_unsuccessful),
            viewModelScope
          )
        )
        return@launch
      }
      if (!kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
        sendAction(
          ShowSnackbar(
            context.getString(R.string.ext_storage_permission_not_granted),
            viewModelScope
          )
        )
        return@launch
      }
      if (File(AddNoteDialog.NOTES_DIRECTORY).deleteRecursively()) {
        sendAction(
          ShowSnackbar(
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

  fun exportBookmark() {
    viewModelScope.launch {
      libkiwixBookmarks.exportBookmark()
    }
  }

  suspend fun requestExternalStorageWritePermissionForExportBookmark(): Boolean =
    if (kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
      true
    } else {
      sendAction(RequestWriteStoragePermission)
      false
    }

  fun onStoragePermissionResult(isGranted: Boolean, coreMainActivity: CoreMainActivity) {
    if (isGranted) {
      // Successfully granted permission, so opening the export bookmark Dialog
      sendAction(ExportBookmarks)
      return
    }
    if (kiwixPermissionChecker.shouldShowRationale(
        coreMainActivity,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
    ) {
      /* shouldShowRationale() returns false when:
       *  1) User has previously checked on "Don't ask me again", and/or
       *  2) Permission has been disabled on device
       */
      context.toast(
        R.string.ext_storage_permission_rationale_export_bookmark,
        Toast.LENGTH_LONG
      )
    } else {
      context.toast(
        R.string.ext_storage_write_permission_denied_export_bookmark,
        Toast.LENGTH_LONG
      )
      sendAction(Action.NavigateToAppSettingsDialog)
    }
  }

  fun onBookmarkFileSelected(result: ActivityResult) {
    result.data?.data?.let { uri ->
      val contentResolver = context.contentResolver
      if (!isValidBookmarkFile(contentResolver.getType(uri))) {
        context.toast(
          context.getString(R.string.error_invalid_bookmark_file),
          Toast.LENGTH_SHORT
        )
        return@let
      }

      createTempFile(contentResolver.openInputStream(uri)).apply {
        if (isValidXmlFile(this)) {
          viewModelScope.launch {
            libkiwixBookmarks.importBookmarks(this@apply)
          }
        } else {
          context.toast(
            context.getString(R.string.error_invalid_bookmark_file),
            Toast.LENGTH_SHORT
          )
        }
      }
    }
  }

  private fun isValidXmlFile(file: File): Boolean {
    return try {
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
      true
    } catch (ignore: Exception) {
      android.util.Log.e("IMPORT_BOOKMARKS", "Invalid XML file", ignore)
      false
    }
  }

  private fun createTempFile(inputStream: InputStream?): File {
    // create a temp file for importing the saved bookmarks
    val tempFile = File(context.externalCacheDir, "bookmark.xml")
    if (tempFile.exists()) {
      tempFile.delete()
    }
    tempFile.createNewFile()
    inputStream?.let {
      tempFile.outputStream().use(inputStream::copyTo)
    }
    return tempFile
  }

  private fun isValidBookmarkFile(mimeType: String?) =
    mimeType == "application/xml" || mimeType == "text/xml"

  fun showFileChooser(fileSelectLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    val intent = Intent().apply {
      action = Intent.ACTION_GET_CONTENT
      type = "*/*"
      addCategory(Intent.CATEGORY_OPENABLE)
    }
    try {
      fileSelectLauncher.launch(Intent.createChooser(intent, "Select a bookmark file"))
    } catch (_: ActivityNotFoundException) {
      context.toast(
        context.getString(R.string.no_app_found_to_select_bookmark_file),
        Toast.LENGTH_SHORT
      )
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  fun openCredits() {
    alertDialogShower.show(
      OpenCredits {
        AndroidView(factory = {
          WebView(it).apply {
            val maxHeightInPx =
              (Resources.getSystem().displayMetrics.heightPixels * ZERO_POINT_SEVEN).toInt()

            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              maxHeightInPx
            )
            viewModelScope.launch {
              if (themeConfig.isDarkTheme()) {
                settings.javaScriptEnabled = true
                setBackgroundColor(0)
              }
              loadUrl("file:///android_asset/credits.html")
            }
          }
        })
      }
    )
  }

  @Suppress("NestedBlockDepth")
  fun onStorageDeviceSelected(storageDevice: StorageDevice, coreMainActivity: CoreMainActivity) {
    viewModelScope.runSafelyInLifecycleScope {
      kiwixDataStore.apply {
        setSelectedStorage(getPublicDirectoryPath(storageDevice.name))
        setSelectedStoragePosition(
          if (storageDevice.isInternal) {
            INTERNAL_SELECT_POSITION
          } else {
            EXTERNAL_SELECT_POSITION
          }
        )
        setShowStorageOption()
        setStorage(coreMainActivity)
      }
    }
  }

  private suspend fun setShowStorageOption() {
    kiwixDataStore.setShowStorageOption(false)
  }
}
