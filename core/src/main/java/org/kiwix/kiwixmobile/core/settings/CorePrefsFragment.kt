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
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.OpenCredits
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
        .getPackageInfo(requireActivity().packageName, 0).versionCode
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }
  private val versionName: String
    @Suppress("TooGenericExceptionThrown")
    get() = try {
      requireActivity().packageManager
        .getPackageInfo(requireActivity().packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
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
    return true
  }

  companion object {
    const val PREF_VERSION = "pref_version"
    const val PREF_CLEAR_ALL_HISTORY = "pref_clear_all_history"
    const val PREF_CLEAR_ALL_NOTES = "pref_clear_all_notes"
    const val PREF_CREDITS = "pref_credits"
    private const val ZOOM_OFFSET = 2
    private const val ZOOM_SCALE = 25
    private const val INTERNAL_TEXT_ZOOM = "text_zoom"
  }
}
