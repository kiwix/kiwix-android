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
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.StorageSelectDialog;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.io.FilesKt;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.NightModeConfig;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt;
import org.kiwix.kiwixmobile.core.main.AddNoteDialog;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;

import static org.kiwix.kiwixmobile.core.utils.Constants.RESULT_RESTART;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_NIGHT_MODE;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_STORAGE;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_ZOOM;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_ZOOM_ENABLED;

public abstract class CorePrefsFragment extends PreferenceFragment implements
  SettingsContract.View,
  SharedPreferences.OnSharedPreferenceChangeListener {

  public static final String PREF_VERSION = "pref_version";
  public static final String PREF_CLEAR_ALL_HISTORY = "pref_clear_all_history";
  public static final String PREF_CLEAR_ALL_NOTES = "pref_clear_all_notes";
  public static final String PREF_CREDITS = "pref_credits";
  @Inject
  SettingsPresenter presenter;
  @Inject
  protected SharedPreferenceUtil sharedPreferenceUtil;
  @Inject
  protected StorageCalculator storageCalculator;
  @Inject
  protected NightModeConfig nightModeConfig;

  private SliderPreference mSlider;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    CoreApp.getCoreComponent().inject(this);
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);

    mSlider = (SliderPreference) findPreference(PREF_ZOOM);
    setSliderState();
    setStorage();
    setUpSettings();
    new LanguageUtils(getActivity()).changeFont(getActivity().getLayoutInflater(),
      sharedPreferenceUtil);
  }

  protected abstract void setStorage();

  private void setSliderState() {
    boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean(
      PREF_ZOOM_ENABLED, false);
    if (enabled) {
      mSlider.setEnabled(true);
    } else {
      mSlider.setEnabled(false);
    }
  }

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
    ListPreference languagePref = (ListPreference) findPreference(preferenceId);
    String selectedLang = sharedPreferenceUtil.getPrefLanguage(Locale.getDefault().toString());
    List<String> languageCodeList = new LanguageUtils(getActivity()).getKeys();
    selectedLang = languageCodeList.contains(selectedLang) ? selectedLang : "en";
    String code[] = languageCodeList.toArray(new String[0]);
    String[] entries = new String[code.length];
    for (int index = 0; index < code.length; index++) {
      Locale locale = new Locale(code[index]);
      entries[index] =
        locale.getDisplayLanguage() + " (" + locale.getDisplayLanguage(locale) + ") ";
    }
    languagePref.setEntries(entries);
    languagePref.setEntryValues(code);
    languagePref.setDefaultValue(selectedLang);
    languagePref.setValue(selectedLang);
    languagePref.setTitle(new Locale(selectedLang).getDisplayLanguage());
    languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
      String languageCode = (String) newValue;
      LanguageUtils.handleLocaleChange(getActivity(), languageCode);
      preference.setTitle(new Locale(languageCode).getLanguage());
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

  private void setAppVersionNumber() {
    EditTextPreference versionPref = (EditTextPreference) findPreference(PREF_VERSION);
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

    if (key.equals(PREF_ZOOM_ENABLED)) {
      setSliderState();
    }
    if (key.equals(PREF_ZOOM)) {
      mSlider.setSummary(mSlider.getSummary());
      ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }
    if (key.equals(PREF_NIGHT_MODE)) {
      sharedPreferenceUtil.updateNightMode();
    }
  }

  private void clearAllHistoryDialog() {
    new AlertDialog.Builder(getActivity())
      .setTitle(getResources().getString(R.string.clear_all_history_dialog_title))
      .setMessage(getResources().getString(R.string.clear_recent_and_tabs_history_dialog))
      .setPositiveButton(android.R.string.yes, (dialog, which) -> {
        presenter.clearHistory();
        CoreSettingsActivity.allHistoryCleared = true;
        Toast.makeText(getActivity(),
          getResources().getString(R.string.all_history_cleared_toast), Toast.LENGTH_SHORT)
          .show();
      })
      .setNegativeButton(android.R.string.no, (dialog, which) -> {
        // do nothing
      })
      .setIcon(R.drawable.ic_warning)
      .show();
  }

  private void showClearAllNotesDialog() {
    new AlertDialog.Builder(getActivity()).setMessage(R.string.delete_notes_confirmation_msg)
      .setNegativeButton(android.R.string.cancel, null) // Do nothing for 'Cancel' button
      .setPositiveButton(R.string.yes, (dialog, which) -> clearAllNotes())
      .show();
  }

  private void clearAllNotes() {
    if (CoreApp.getInstance().isExternalStorageWritable()) {
      if (ContextCompat.checkSelfPermission(getActivity(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
        ContextExtensionsKt.toast(getActivity(), R.string.ext_storage_permission_not_granted,
          Toast.LENGTH_LONG);
        return;
      }

      if (FilesKt.deleteRecursively(new File(AddNoteDialog.NOTES_DIRECTORY))) {
        ContextExtensionsKt.toast(getActivity(), R.string.notes_deletion_successful,
          Toast.LENGTH_SHORT);
        return;
      }
    }
    ContextExtensionsKt.toast(getActivity(), R.string.notes_deletion_unsuccessful,
      Toast.LENGTH_SHORT);
  }

  @SuppressLint("SetJavaScriptEnabled")
  public void openCredits() {
    WebView view =
      (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.credits_webview, null);
    view.loadUrl("file:///android_asset/credits.html");
    if (nightModeConfig.isNightModeActive()) {
      view.getSettings().setJavaScriptEnabled(true);
      view.setBackgroundColor(0);
    }
    new AlertDialog.Builder(getActivity())
      .setView(view)
      .setPositiveButton(android.R.string.ok, null)
      .show();
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    Preference preference) {
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
    dialogFragment.setOnSelectListener(this::onStorageDeviceSelected);
    dialogFragment.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(),
      getResources().getString(R.string.pref_storage));
  }

  private Unit onStorageDeviceSelected(StorageDevice storageDevice) {
    findPreference(PREF_STORAGE).setSummary(
      storageCalculator.calculateAvailableSpace(storageDevice.getFile())
    );
    sharedPreferenceUtil.putPrefStorage(storageDevice.getName());
    if (storageDevice.isInternal()) {
      findPreference(PREF_STORAGE).setTitle(getResources().getString(R.string.internal_storage));
      sharedPreferenceUtil.putPrefStorageTitle(
        getResources().getString(R.string.internal_storage));
    } else {
      findPreference(PREF_STORAGE).setTitle(getResources().getString(R.string.external_storage));
      sharedPreferenceUtil.putPrefStorageTitle(
        getResources().getString(R.string.external_storage));
    }
    return Unit.INSTANCE;
  }
}
