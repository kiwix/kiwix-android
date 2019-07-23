/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.StorageSelectDialog;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.utils.StyleUtils;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryUtils;

import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_WEBVIEWS_LIST;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_AUTONIGHTMODE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_CLEAR_ALL_HISTORY;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_CREDITS;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_LANG;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_NIGHTMODE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_STORAGE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_VERSION;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_WIFI_ONLY;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_ZOOM;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_ZOOM_ENABLED;
import static org.kiwix.kiwixmobile.utils.Constants.RESULT_HISTORY_CLEARED;
import static org.kiwix.kiwixmobile.utils.Constants.RESULT_RESTART;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class KiwixSettingsActivity extends BaseActivity {

  public static boolean allHistoryCleared = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    allHistoryCleared = false;

    getFragmentManager()
        .beginTransaction().
        replace(R.id.content_frame, new PrefsFragment())
        .commit();

    setUpToolbar();
  }

  @Override
  public void onBackPressed() {
    getWindow().setWindowAnimations(0);
    if (allHistoryCleared) {
      Intent data = new Intent();
      data.putExtra(EXTRA_WEBVIEWS_LIST, allHistoryCleared);
      setResult(RESULT_HISTORY_CLEARED, data);
    }
    super.onBackPressed();
  }

  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_settings));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  public static class PrefsFragment extends PreferenceFragment implements
      SettingsContract.View,
      SharedPreferences.OnSharedPreferenceChangeListener, StorageSelectDialog.OnSelectListener {

    @Inject
    SettingsPresenter presenter;
    @Inject
    SharedPreferenceUtil sharedPreferenceUtil;

    private SliderPreference mSlider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      KiwixApplication.getApplicationComponent().inject(this);
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);

      boolean auto_night_mode = sharedPreferenceUtil.getPrefAutoNightMode();

      if (auto_night_mode) {
        getPreferenceScreen().findPreference(PREF_NIGHTMODE).setEnabled(false);
      }

      if (BuildConfig.ENFORCED_LANG.equals("")) {
        setUpLanguageChooser(PREF_LANG);
      } else {
        getPreferenceScreen().removePreference(findPreference("pref_language"));
      }

      if (BuildConfig.IS_CUSTOM_APP) {
        PreferenceCategory notificationsCategory =
            (PreferenceCategory) findPreference("pref_extras");
        notificationsCategory.removePreference(findPreference("pref_wifi_only"));
      }

      mSlider = (SliderPreference) findPreference(PREF_ZOOM);
      setSliderState();
      setStorage();
      setUpSettings();
      new LanguageUtils(getActivity()).changeFont(getActivity().getLayoutInflater(),
          sharedPreferenceUtil);
    }

    private void setStorage() {
      if (BuildConfig.IS_CUSTOM_APP) {
        getPreferenceScreen().removePreference(findPreference("pref_storage"));
      } else {
        if (Environment.isExternalStorageEmulated()) {
          findPreference(PREF_STORAGE).setTitle(
              sharedPreferenceUtil.getPrefStorageTitle("Internal"));
        } else {
          findPreference(PREF_STORAGE).setTitle(
              sharedPreferenceUtil.getPrefStorageTitle("External"));
        }
        findPreference(PREF_STORAGE).setSummary(LibraryUtils.bytesToHuman(
            new File(sharedPreferenceUtil.getPrefStorage()).getFreeSpace()));
      }
    }

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

    private void setUpLanguageChooser(String preferenceId) {
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
      versionPref.setSummary(BuildConfig.VERSION_NAME + " Build: " + getVersionCode());
    }

    private int getVersionCode()  {
      try {
        return getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
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
      if (key.equals(PREF_NIGHTMODE)) {
        MainActivity.refresh = true;
        MainActivity.nightMode = sharedPreferenceUtil.nightMode();
        getActivity().finish();
        startActivity(new Intent(getActivity(), KiwixSettingsActivity.class));
        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
      }
      if (key.equals(PREF_WIFI_ONLY)) {
        MainActivity.wifiOnly = sharedPreferences.getBoolean(PREF_WIFI_ONLY, true);
      }
      if (key.equals(PREF_AUTONIGHTMODE)) {
        MainActivity.refresh = true;
        MainActivity.nightMode = sharedPreferenceUtil.nightMode();
        getActivity().finish();
        startActivity(new Intent(getActivity(), KiwixSettingsActivity.class));
        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
      }
    }

    private void clearAllHistoryDialog() {
      int warningResId;
      if (sharedPreferenceUtil.nightMode()) {
        warningResId = R.drawable.ic_warning_white;
      } else {
        warningResId = R.drawable.ic_warning_black;
      }
      new AlertDialog.Builder(getActivity(), dialogStyle())
          .setTitle(getResources().getString(R.string.clear_all_history_dialog_title))
          .setMessage(getResources().getString(R.string.clear_recent_and_tabs_history_dialog))
          .setPositiveButton(android.R.string.yes, (dialog, which) -> {
            presenter.clearHistory();
            allHistoryCleared = true;
            Toast.makeText(getActivity(),
                getResources().getString(R.string.all_history_cleared_toast), Toast.LENGTH_SHORT)
                .show();
          })
          .setNegativeButton(android.R.string.no, (dialog, which) -> {
            // do nothing
          })
          .setIcon(warningResId)
          .show();
    }

    public void openCredits() {
      WebView view =
          (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.credits_webview, null);
      view.loadUrl("file:///android_asset/credits.html");
      if (sharedPreferenceUtil.nightMode()) {
        view.getSettings().setJavaScriptEnabled(true);
        view.setBackgroundColor(0);
      }
      new AlertDialog.Builder(getActivity(), dialogStyle())
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
      Bundle b = new Bundle();
      b.putString(StorageSelectDialog.STORAGE_DIALOG_INTERNAL,
          getResources().getString(R.string.internal_storage));
      b.putString(StorageSelectDialog.STORAGE_DIALOG_EXTERNAL,
          getResources().getString(R.string.external_storage));
      b.putInt(StorageSelectDialog.STORAGE_DIALOG_THEME, StyleUtils.dialogStyle());
      dialogFragment.setArguments(b);
      dialogFragment.setOnSelectListener(this);
      dialogFragment.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), getResources().getString(R.string.pref_storage));
    }

    @Override
    public void selectionCallback(StorageDevice storageDevice) {
      findPreference(PREF_STORAGE).setSummary(storageDevice.getSize());
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
    }
  }
}
