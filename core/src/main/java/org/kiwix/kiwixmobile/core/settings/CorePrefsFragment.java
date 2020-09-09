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

package org.kiwix.kiwixmobile.core.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.snackbar.Snackbar;
import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.StorageSelectDialog;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.NightModeConfig;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.main.AddNoteDialog;
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower;
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;

import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.RESULT_RESTART;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_NIGHT_MODE;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_STORAGE;

public abstract class CorePrefsFragment extends PreferenceFragmentCompat implements
  SettingsContract.View,
  SharedPreferences.OnSharedPreferenceChangeListener {

  public static final String PREF_VERSION = "pref_version";
  public static final String PREF_CLEAR_ALL_HISTORY = "pref_clear_all_history";
  public static final String PREF_CLEAR_ALL_NOTES = "pref_clear_all_notes";
  public static final String PREF_CREDITS = "pref_credits";
  private static final int ZOOM_OFFSET = 2;
  private static final int ZOOM_SCALE = 25;
  private static final String INTERNAL_TEXT_ZOOM = "text_zoom";
  @Inject
  SettingsPresenter presenter;
  @Inject
  protected SharedPreferenceUtil sharedPreferenceUtil;
  @Inject
  protected StorageCalculator storageCalculator;
  @Inject
  protected NightModeConfig nightModeConfig;
  @Inject
  protected DialogShower alertDialogShower;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    CoreApp.getCoreComponent()
      .activityComponentBuilder()
      .activity(getActivity())
      .build()
      .inject(this);
    addPreferencesFromResource(R.xml.preferences);
    setStorage();
    setUpSettings();
    setupZoom();
    new LanguageUtils(getActivity()).changeFont(getActivity().getLayoutInflater(),
      sharedPreferenceUtil);
  }

  private void setupZoom() {
    final Preference textZoom = findPreference(INTERNAL_TEXT_ZOOM);
    textZoom.setOnPreferenceChangeListener(
      (preference, newValue) -> {
        sharedPreferenceUtil.setTextZoom((((Integer) newValue) + ZOOM_OFFSET) * ZOOM_SCALE);
        updateTextZoomSummary(textZoom);
        return true;
      });
    updateTextZoomSummary(textZoom);
  }

  private void updateTextZoomSummary(Preference textZoom) {
    textZoom.setSummary(getString(R.string.percentage, sharedPreferenceUtil.getTextZoom()));
  }

  protected abstract void setStorage();

  @Override
  public void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences()
      .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences()
      .unregisterOnSharedPreferenceChangeListener(this);
  }

  public void setUpSettings() {
    setAppVersionNumber();
  }

  protected void setUpLanguageChooser(String preferenceId) {
    ListPreference languagePref = findPreference(preferenceId);
    List<String> languageCodeList = new LanguageUtils(getActivity()).getKeys();
    languageCodeList.add(0, Locale.ROOT.getLanguage());
    final String selectedLang =
      selectedLanguage(languageCodeList, sharedPreferenceUtil.getPrefLanguage());
    languagePref.setEntries(languageDisplayValues(languageCodeList));
    languagePref.setEntryValues(languageCodeList.toArray(new String[0]));
    languagePref.setDefaultValue(selectedLang);
    languagePref.setValue(selectedLang);
    languagePref.setTitle(selectedLang.equals(Locale.ROOT.toString())
      ? getString(R.string.device_default)
      : new Locale(selectedLang).getDisplayLanguage());
    languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
      String languageCode = (String) newValue;
      LanguageUtils.handleLocaleChange(getActivity(), languageCode);
      sharedPreferenceUtil.putPrefLanguage(languageCode);
      restartActivity();
      return true;
    });
  }

  private void restartActivity() {
    getActivity().setResult(RESULT_RESTART);
    getActivity().finish();
    getActivity().startActivity(new Intent(getActivity(), getActivity().getClass()));
  }

  @NotNull private String selectedLanguage(List<String> languageCodeList, String langPref) {
    return languageCodeList.contains(langPref) ? langPref : "en";
  }

  @NotNull private String[] languageDisplayValues(List<String> languageCodeList) {
    String[] entries = new String[languageCodeList.size()];
    entries[0] = getString(R.string.device_default);
    for (int i = 1; i < languageCodeList.size(); i++) {
      Locale locale = new Locale(languageCodeList.get(i));
      entries[i] = locale.getDisplayLanguage() + " (" + locale.getDisplayLanguage(locale) + ") ";
    }
    return entries;
  }

  private void setAppVersionNumber() {
    EditTextPreference versionPref = findPreference(PREF_VERSION);
    versionPref.setSummary(getVersionName() + " Build: " + getVersionCode());
  }

  private int getVersionCode() {
    try {
      return getActivity().getPackageManager()
        .getPackageInfo(getActivity().getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private String getVersionName() {
    try {
      return getActivity().getPackageManager()
        .getPackageInfo(getActivity().getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(PREF_NIGHT_MODE)) {
      sharedPreferenceUtil.updateNightMode();
      restartActivity();
    }
  }

  private void clearAllHistoryDialog() {
    alertDialogShower.show(KiwixDialog.ClearAllHistory.INSTANCE, () -> {
      presenter.clearHistory();
      CoreSettingsActivity.allHistoryCleared = true;
      Snackbar.make(getView(), R.string.all_history_cleared, Snackbar.LENGTH_SHORT).show();
      return Unit.INSTANCE;
    });
  }

  private void showClearAllNotesDialog() {
    alertDialogShower.show(KiwixDialog.ClearAllNotes.INSTANCE, () -> {
      clearAllNotes();
      return Unit.INSTANCE;
    });
  }

  private void clearAllNotes() {
    if (CoreApp.getInstance().isExternalStorageWritable()) {
      if (ContextCompat.checkSelfPermission(getActivity(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
        Snackbar.make(getView(), R.string.ext_storage_permission_not_granted, Snackbar.LENGTH_SHORT)
          .show();
        return;
      }

      if (FilesKt.deleteRecursively(new File(AddNoteDialog.NOTES_DIRECTORY))) {
        Snackbar.make(getView(), R.string.notes_deletion_successful, Snackbar.LENGTH_SHORT).show();
        return;
      }
    }
    Snackbar.make(getView(), R.string.notes_deletion_unsuccessful, Snackbar.LENGTH_SHORT).show();
  }

  @SuppressLint("SetJavaScriptEnabled")
  public void openCredits() {
    @SuppressLint("InflateParams") WebView view =
      (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.credits_webview, null);
    view.loadUrl("file:///android_asset/credits.html");
    if (nightModeConfig.isNightModeActive()) {
      view.getSettings().setJavaScriptEnabled(true);
      view.setBackgroundColor(0);
    }
    alertDialogShower.show(new KiwixDialog.OpenCredits(() -> view));
  }

  @Override public boolean onPreferenceTreeClick(Preference preference) {
    if (preference.getKey().equalsIgnoreCase(PREF_CLEAR_ALL_HISTORY)) {
      clearAllHistoryDialog();
    }
    if (preference.getKey().equalsIgnoreCase(PREF_CLEAR_ALL_NOTES)) {
      showClearAllNotesDialog();
    }

    if (preference.getKey().equalsIgnoreCase(PREF_CREDITS)) {
      openCredits();
    }

    if (preference.getKey().equalsIgnoreCase(PREF_STORAGE)) {
      openFolderSelect();
    }

    return true;
  }

  public void openFolderSelect() {
    StorageSelectDialog dialogFragment = new StorageSelectDialog();
    dialogFragment.setOnSelectAction(this::onStorageDeviceSelected);
    dialogFragment.show(getActivity().getSupportFragmentManager(),
      getResources().getString(R.string.pref_storage));
  }

  private Unit onStorageDeviceSelected(StorageDevice storageDevice) {
    findPreference(PREF_STORAGE).setSummary(
      storageCalculator.calculateAvailableSpace(storageDevice.getFile())
    );
    sharedPreferenceUtil.putPrefStorage(storageDevice.getName());
    if (storageDevice.isInternal()) {
      findPreference(PREF_STORAGE).setTitle(getString(R.string.internal_storage));
    } else {
      findPreference(PREF_STORAGE).setTitle(getString(R.string.external_storage));
    }
    return Unit.INSTANCE;
  }
}
