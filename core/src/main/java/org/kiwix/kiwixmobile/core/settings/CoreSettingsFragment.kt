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
package org.kiwix.kiwixmobile.core.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action
import org.kiwix.kiwixmobile.core.settings.viewmodel.SettingsViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.OpenCredits
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

const val ZERO_POINT_SEVEN = 0.7

abstract class CoreSettingsFragment : SettingsContract.View, BaseFragment() {
  private var composeView: ComposeView? = null

  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory

  @JvmField
  @Inject
  internal var presenter: SettingsPresenter? = null

  @JvmField
  @Inject
  var sharedPreferenceUtil: SharedPreferenceUtil? = null

  @JvmField
  @Inject
  var storageCalculator: StorageCalculator? = null

  @JvmField
  @Inject
  var themeConfig: ThemeConfig? = null

  @JvmField
  @Inject
  var alertDialogShower: DialogShower? = null

  @JvmField
  @Inject
  internal var libkiwixBookmarks: LibkiwixBookmarks? = null
  private val settingViewModel by lazy {
    requireActivity().viewModel<SettingsViewModel>(viewModelFactory)
  }
  protected val settingsScreenState = lazy {
    mutableStateOf(
      SettingScreenState(
        storageDeviceList = emptyList(),
        isLoadingStorageDetails = true,
        storageCalculator = storageCalculator
          ?: throw IllegalStateException("Storage calculator is null"),
        sharedPreferenceUtil = sharedPreferenceUtil
          ?: throw IllegalStateException("SharedPreferenceUtils is null"),
        permissionItem = false to "",
        shouldShowLanguageCategory = false,
        onLanguageChanged = { restartActivity() },
        versionInformation = "",
        shouldShowStorageCategory = false,
        shouldShowExternalLinkPreference = false,
        shouldShowPrefWifiOnlyPreference = false
      )
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    composeView?.apply {
      setContent {
        SettingsScreen(
          settingScreenState = settingsScreenState.value.value,
          settingsViewModel = settingViewModel
        ) {
          NavigationIcon(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() })
        }
        DialogHost(alertDialogShower as AlertDialogShower)
      }
    }
    settingsScreenState.value.update {
      copy(versionInformation = "$versionName Build: $versionCode")
    }
    sharedPreferenceUtil?.let {
      LanguageUtils(requireActivity()).changeFont(
        requireActivity(),
        it
      )
    }
    settingViewModel.actions.onEach {
      when (it) {
        Action.AllowPermission -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          requireActivity().navigateToSettings()
        }

        Action.ClearAllHistory -> clearAllHistoryDialog()
        Action.ClearAllNotes -> showClearAllNotesDialog()
        Action.ExportBookmarks -> if (requestExternalStorageWritePermissionForExportBookmark()) {
          showExportBookmarkDialog()
        }

        Action.ImportBookmarks -> showImportBookmarkDialog()
        is Action.OnStorageItemClick -> onStorageDeviceSelected(it.storageDevice)
        Action.OpenCredits -> openCredits()
      }
    }.launchIn(lifecycleScope)
    lifecycleScope.launch {
      setStorage()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).also {
    composeView = it
  }

  /**
   * Restarts the Settings screen by popping it from the back stack and reopening it.
   *
   * This is useful when we need to refresh the Settings UI (e.g., after a app's language
   * change) without fully recreating the activity.
   *
   * Steps:
   * 1. Get the CoreMainActivity reference to access the NavController.
   * 2. Pop the Settings fragment from the navigation back stack.
   * 3. Wait for one frame so the back stack can settle after the pop operation.
   * 4. Navigate back to the Settings fragment route.
   */
  private fun restartActivity() {
    val coreMainActivity = activity as? CoreMainActivity ?: return
    val navController = coreMainActivity.navController
    navController.popBackStack()
    coreMainActivity.uiCoroutineScope.launch {
      // Wait for one frame to ensure the back stack has settled before navigation
      // Bug fix #4387
      withFrameNanos { }
      navController.navigate(coreMainActivity.settingsFragmentRoute)
    }
  }

  private val versionCode: Int
    @Suppress("TooGenericExceptionThrown")
    get() = try {
      requireActivity().packageManager
        .getPackageInformation(requireActivity().packageName, ZERO).getVersionCode()
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }
  private val versionName: String
    @Suppress("TooGenericExceptionThrown")
    get() = try {
      requireActivity().packageManager
        .getPackageInformation(requireActivity().packageName, ZERO).versionName.toString()
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }

  override fun onDestroyView() {
    storagePermissionForNotesLauncher?.unregister()
    storagePermissionForNotesLauncher = null
    super.onDestroyView()
    composeView?.disposeComposition()
    composeView = null
  }

  protected abstract suspend fun setStorage()

  private fun clearAllHistoryDialog() {
    alertDialogShower?.show(KiwixDialog.ClearAllHistory, {
      lifecycleScope.launch {
        presenter?.clearHistory()
        Snackbar.make(requireView(), R.string.all_history_cleared, Snackbar.LENGTH_SHORT).show()
      }
    })
  }

  private fun showClearAllNotesDialog() {
    alertDialogShower?.show(KiwixDialog.ClearAllNotes, ::clearAllNotes)
  }

  private fun clearAllNotes() {
    if (instance.isExternalStorageWritable) {
      if (ContextCompat.checkSelfPermission(
          requireActivity(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        != PackageManager.PERMISSION_GRANTED &&
        sharedPreferenceUtil?.isPlayStoreBuildWithAndroid11OrAbove() == false &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
      ) {
        Snackbar.make(
          requireView(),
          R.string.ext_storage_permission_not_granted,
          Snackbar.LENGTH_SHORT
        )
          .show()
        return
      }
      if (File(AddNoteDialog.NOTES_DIRECTORY).deleteRecursively()) {
        Snackbar.make(requireView(), R.string.notes_deletion_successful, Snackbar.LENGTH_SHORT)
          .show()
        return
      }
    }
    Snackbar.make(requireView(), R.string.notes_deletion_unsuccessful, Snackbar.LENGTH_SHORT).show()
  }

  @SuppressLint("SetJavaScriptEnabled")
  fun openCredits() {
    @SuppressLint("InflateParams") val view =
      LayoutInflater.from(
        requireActivity()
      ).inflate(R.layout.credits_webview, null) as WebView
    val maxHeightInPx =
      (Resources.getSystem().displayMetrics.heightPixels * ZERO_POINT_SEVEN).toInt()
    view.layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      maxHeightInPx
    )
    view.loadUrl("file:///android_asset/credits.html")
    if (themeConfig?.isDarkTheme() == true) {
      view.settings.javaScriptEnabled = true
      view.setBackgroundColor(0)
    }
    alertDialogShower?.show(OpenCredits { AndroidView(factory = { view }) })
  }

  @Suppress("NestedBlockDepth")
  private fun requestExternalStorageWritePermissionForExportBookmark(): Boolean {
    var isPermissionGranted = false
    if (sharedPreferenceUtil?.isPlayStoreBuildWithAndroid11OrAbove() == false &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED
      ) {
        isPermissionGranted = true
      } else {
        storagePermissionForNotesLauncher?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      }
    } else {
      isPermissionGranted = true
    }
    return isPermissionGranted
  }

  private var storagePermissionForNotesLauncher: ActivityResultLauncher<String>? =
    registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) { isGranted ->
      if (isGranted) {
        // Successfully granted permission, so opening the export bookmark Dialog
        showExportBookmarkDialog()
      } else {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          requireActivity().toast(
            R.string.ext_storage_permission_rationale_export_bookmark,
            Toast.LENGTH_LONG
          )
        } else {
          requireActivity().toast(
            R.string.ext_storage_write_permission_denied_export_bookmark,
            Toast.LENGTH_LONG
          )
          alertDialogShower?.show(
            KiwixDialog.ReadPermissionRequired,
            requireActivity()::navigateToAppSettings
          )
        }
      }
    }

  private fun showExportBookmarkDialog() {
    alertDialogShower?.show(
      KiwixDialog.YesNoDialog.ExportBookmarks,
      {
        lifecycleScope.launch {
          libkiwixBookmarks?.exportBookmark()
        }
      }
    )
  }

  private fun showImportBookmarkDialog() {
    alertDialogShower?.show(
      KiwixDialog.ImportBookmarks,
      ::showFileChooser
    )
  }

  private fun showFileChooser() {
    val intent = Intent().apply {
      action = Intent.ACTION_GET_CONTENT
      type = "*/*"
      addCategory(Intent.CATEGORY_OPENABLE)
    }
    try {
      fileSelectLauncher.launch(Intent.createChooser(intent, "Select a bookmark file"))
    } catch (_: ActivityNotFoundException) {
      activity.toast(
        resources.getString(R.string.no_app_found_to_select_bookmark_file),
        Toast.LENGTH_SHORT
      )
    }
  }

  private val fileSelectLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        result.data?.data?.let { uri ->
          val contentResolver = requireActivity().contentResolver
          if (!isValidBookmarkFile(contentResolver.getType(uri))) {
            activity.toast(
              resources.getString(R.string.error_invalid_bookmark_file),
              Toast.LENGTH_SHORT
            )
            return@registerForActivityResult
          }

          createTempFile(contentResolver.openInputStream(uri)).apply {
            if (isValidXmlFile(this)) {
              lifecycleScope.launch {
                libkiwixBookmarks?.importBookmarks(this@apply)
              }
            } else {
              activity.toast(
                resources.getString(R.string.error_invalid_bookmark_file),
                Toast.LENGTH_SHORT
              )
            }
          }
        }
      }
    }

  private fun isValidXmlFile(file: File): Boolean {
    return try {
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
      true
    } catch (ignore: Exception) {
      Log.e("IMPORT_BOOKMARKS", "Invalid XML file", ignore)
      false
    }
  }

  private fun createTempFile(inputStream: InputStream?): File {
    // create a temp file for importing the saved bookmarks
    val tempFile = File(requireActivity().externalCacheDir, "bookmark.xml")
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

  @Suppress("NestedBlockDepth")
  private fun onStorageDeviceSelected(storageDevice: StorageDevice) {
    lifecycleScope.launch {
      sharedPreferenceUtil?.let { sharedPreferenceUtil ->
        sharedPreferenceUtil.putPrefStorage(
          sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
        )
        sharedPreferenceUtil.putStoragePosition(
          if (storageDevice.isInternal) {
            INTERNAL_SELECT_POSITION
          } else {
            EXTERNAL_SELECT_POSITION
          }
        )
        setShowStorageOption()
        setStorage()
      }
    }
  }

  private fun setShowStorageOption() {
    sharedPreferenceUtil?.showStorageOption = false
  }
}
