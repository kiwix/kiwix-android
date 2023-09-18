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
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.OpenCredits
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.SelectFolder
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getPathFromUri
import java.io.File
import java.util.Locale
import javax.inject.Inject

abstract class CorePrefsFragment :
  PreferenceFragmentCompat(),
  SettingsContract.View,
  OnSharedPreferenceChangeListener {

  @JvmField
  @Inject
  internal var presenter: SettingsPresenter? = null

  @JvmField
  @Inject
  protected var sharedPreferenceUtil: SharedPreferenceUtil? = null

  @JvmField
  @Inject
  protected var storageCalculator: StorageCalculator? = null

  @JvmField
  @Inject
  protected var nightModeConfig: NightModeConfig? = null

  @JvmField
  @Inject
  protected var alertDialogShower: DialogShower? = null
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    coreComponent
      .activityComponentBuilder()
      .activity(requireActivity())
      .build()
      .inject(this)
    addPreferencesFromResource(R.xml.preferences)
    setStorage()
    setUpSettings()
    setupZoom()
    sharedPreferenceUtil?.let {
      LanguageUtils(requireActivity()).changeFont(
        requireActivity(),
        it
      )
    }
  }

  private fun setupZoom() {
    val textZoom = findPreference<Preference>(INTERNAL_TEXT_ZOOM)
    textZoom?.onPreferenceChangeListener =
      Preference.OnPreferenceChangeListener { _, newValue ->
        sharedPreferenceUtil?.textZoom = (newValue as Int + ZOOM_OFFSET) * ZOOM_SCALE
        updateTextZoomSummary(textZoom)
        true
      }
    updateTextZoomSummary(textZoom)
  }

  private fun updateTextZoomSummary(textZoom: Preference?) {
    textZoom?.summary = getString(R.string.percentage, sharedPreferenceUtil?.textZoom)
  }

  protected abstract fun setStorage()
  override fun onResume() {
    super.onResume()
    preferenceScreen.sharedPreferences
      .registerOnSharedPreferenceChangeListener(this)
  }

  override fun onPause() {
    super.onPause()
    preferenceScreen.sharedPreferences
      .unregisterOnSharedPreferenceChangeListener(this)
  }

  override fun onDestroyView() {
    presenter?.dispose()
    super.onDestroyView()
  }

  private fun setUpSettings() {
    setAppVersionNumber()
  }

  protected fun setUpLanguageChooser(preferenceId: String) {
    findPreference<ListPreference>(
      preferenceId
    )?.let { languagePref ->
      var languageCodeList = LanguageUtils(requireActivity()).keys
      languageCodeList = listOf(Locale.ROOT.language) + languageCodeList
      sharedPreferenceUtil?.let { sharedPreferenceUtil ->
        val selectedLang = selectedLanguage(languageCodeList, sharedPreferenceUtil.prefLanguage)
        languagePref.entries = languageDisplayValues(languageCodeList)
        languagePref.entryValues = languageCodeList.toTypedArray()
        languagePref.setDefaultValue(selectedLang)
        languagePref.value = selectedLang
        languagePref.title =
          if (selectedLang == Locale.ROOT.toString())
            getString(R.string.device_default)
          else Locale(
            selectedLang
          ).displayLanguage
        languagePref.onPreferenceChangeListener =
          Preference.OnPreferenceChangeListener { _, newValue ->
            val languageCode = newValue as String?
            languageCode?.let {
              handleLocaleChange(requireActivity(), it, sharedPreferenceUtil)
              sharedPreferenceUtil.putPrefLanguage(it)
              restartActivity()
            }
            true
          }
      }
    }
  }

  private fun restartActivity() {
    (activity as CoreMainActivity?)?.let {
      it.navController.apply {
        popBackStack()
        navigate(it.settingsFragmentResId)
      }
    }
  }

  private fun selectedLanguage(
    languageCodeList: List<String>,
    langPref: String
  ): String =
    if (languageCodeList.contains(langPref)) langPref else "en"

  private fun languageDisplayValues(languageCodeList: List<String>): Array<String?> {
    val entries = arrayOfNulls<String>(languageCodeList.size)
    entries[0] = getString(R.string.device_default)
    for (i in 1 until languageCodeList.size) {
      val locale = Locale(languageCodeList[i])
      entries[i] = locale.displayLanguage + " (" + locale.getDisplayLanguage(locale) + ") "
    }
    return entries
  }

  private fun setAppVersionNumber() {
    findPreference<EditTextPreference>(PREF_VERSION)?.apply {
      summary = "$versionName Build: $versionCode"
    }
  }

  private val versionCode: Int
    @Suppress("TooGenericExceptionThrown")
    get() = try {
      requireActivity().packageManager
        .getPackageInformation(requireActivity().packageName, 0).getVersionCode()
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }
  private val versionName: String
    @Suppress("TooGenericExceptionThrown")
    get() = try {
      requireActivity().packageManager
        .getPackageInformation(requireActivity().packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    if (key == SharedPreferenceUtil.PREF_NIGHT_MODE) {
      sharedPreferenceUtil?.updateNightMode()
    }
  }

  private fun clearAllHistoryDialog() {
    alertDialogShower?.show(KiwixDialog.ClearAllHistory, {
      presenter?.clearHistory()
      Snackbar.make(requireView(), R.string.all_history_cleared, Snackbar.LENGTH_SHORT).show()
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
    view.loadUrl("file:///android_asset/credits.html")
    if (nightModeConfig?.isNightModeActive() == true) {
      view.settings.javaScriptEnabled = true
      view.setBackgroundColor(0)
    }
    alertDialogShower?.show(OpenCredits { view })
  }

  override fun onPreferenceTreeClick(preference: Preference): Boolean {
    if (preference.key.equals(PREF_CLEAR_ALL_HISTORY, ignoreCase = true)) {
      clearAllHistoryDialog()
    }
    if (preference.key.equals(PREF_CLEAR_ALL_NOTES, ignoreCase = true)) {
      showClearAllNotesDialog()
    }
    if (preference.key.equals(PREF_CREDITS, ignoreCase = true)) {
      openCredits()
    }
    if (preference.key.equals(SharedPreferenceUtil.PREF_STORAGE, ignoreCase = true)) {
      openFolderSelect()
    }
    return true
  }

  private fun openFolderSelect() {
    val dialogFragment = StorageSelectDialog()
    dialogFragment.onSelectAction =
      ::onStorageDeviceSelected
    dialogFragment.show(
      requireActivity().supportFragmentManager,
      resources.getString(R.string.pref_storage)
    )
  }

  @Suppress("NestedBlockDepth")
  private fun onStorageDeviceSelected(storageDevice: StorageDevice) {
    sharedPreferenceUtil?.let { sharedPreferenceUtil ->
      findPreference<Preference>(SharedPreferenceUtil.PREF_STORAGE)?.summary =
        storageCalculator?.calculateAvailableSpace(storageDevice.file)
      if (storageDevice.isInternal) {
        sharedPreferenceUtil.putPrefStorage(
          sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
        )
        findPreference<Preference>(SharedPreferenceUtil.PREF_STORAGE)?.title =
          getString(R.string.internal_storage)
        sharedPreferenceUtil.putStoragePosition(INTERNAL_SELECT_POSITION)
        setShowStorageOption()
      } else {
        if (sharedPreferenceUtil.isPlayStoreBuild) {
          setExternalStoragePath(storageDevice)
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
          ) {
            @SuppressLint("InflateParams") val view = LayoutInflater.from(
              activity
            ).inflate(R.layout.select_folder_dialog, null)
            alertDialogShower?.show(SelectFolder { view }, ::selectFolder)
          } else {
            setExternalStoragePath(storageDevice)
          }
        }
      }
    }
    return
  }

  private fun setExternalStoragePath(storageDevice: StorageDevice) {
    sharedPreferenceUtil?.putPrefStorage(storageDevice.name)
    findPreference<Preference>(SharedPreferenceUtil.PREF_STORAGE)?.title =
      getString(R.string.external_storage)
    sharedPreferenceUtil?.putStoragePosition(EXTERNAL_SELECT_POSITION)
    setShowStorageOption()
  }

  private fun selectFolder() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
          or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
          or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
      )
    }
    selectFolderLauncher.launch(intent)
  }

  private val selectFolderLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        result.data?.let { intent ->
          getPathFromUri(requireActivity(), intent)?.let { path ->
            sharedPreferenceUtil?.putPrefStorage(path)
            findPreference<Preference>(SharedPreferenceUtil.PREF_STORAGE)?.title =
              getString(R.string.external_storage)
            sharedPreferenceUtil?.putStoragePosition(EXTERNAL_SELECT_POSITION)
            setShowStorageOption()
          }
        }
      }
    }

  private fun setShowStorageOption() {
    sharedPreferenceUtil?.showStorageOption = false
  }

  companion object {
    const val PREF_VERSION = "pref_version"
    const val PREF_CLEAR_ALL_HISTORY = "pref_clear_all_history"
    const val PREF_CLEAR_ALL_NOTES = "pref_clear_all_notes"
    const val PREF_CREDITS = "pref_credits"
    const val PREF_PERMISSION = "pref_permissions"
    private const val ZOOM_OFFSET = 2
    private const val ZOOM_SCALE = 25
    private const val INTERNAL_TEXT_ZOOM = "text_zoom"
  }
}
