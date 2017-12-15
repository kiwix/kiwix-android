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

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.RecentSearchDao;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.StyleUtils;
import org.kiwix.kiwixmobile.views.LanguageSelectDialog;
import org.kiwix.kiwixmobile.views.SliderPreference;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.StorageSelectDialog;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class KiwixSettingsActivity extends AppCompatActivity {

  public static final int RESULT_RESTART = 1236;

  public static final int RESULT_HISTORY_CLEARED = 1239;

  public static final String PREF_LANG = "pref_language_chooser";

  public static final String PREF_VERSION = "pref_version";

  public static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

  public static final String PREF_ZOOM = "pref_zoom_slider";

  public static final String PREF_CLEAR_ALL_HISTORY = "pref_clear_all_history";

  public static final String PREF_CREDITS = "pref_credits";

  public static final String PREF_STORAGE = "pref_select_folder";

  public static final String PREF_AUTONIGHTMODE = "pref_auto_nightmode";

  public static final String PREF_NIGHTMODE = "pref_nightmode";

  public static final String PREF_WIFI_ONLY = "pref_wifi_only";

  public static final String PREF_BOTTOM_TOOLBAR = "pref_bottomtoolbar";

  public static String zimFile;

  public static boolean allHistoryCleared = false;


  private static final int DAWN_HOUR = 6;
  private static final int DUSK_HOUR = 18;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    if(nightMode(PreferenceManager.getDefaultSharedPreferences(this))){
      setTheme(R.style.AppTheme_Night);
    }

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
    if (allHistoryCleared) {
      Intent data = new Intent();
      data.putExtra("webviewsList", allHistoryCleared);
      setResult(RESULT_HISTORY_CLEARED, data);
    }
    super.onBackPressed();
  }

  private void setUpToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_settings));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  public static boolean nightMode(SharedPreferences preferences){
    boolean autoNightMode = preferences.getBoolean(PREF_AUTONIGHTMODE, false);
    if(autoNightMode){
      Calendar cal = Calendar.getInstance();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      return hour < DAWN_HOUR || hour > DUSK_HOUR;
    } else{
      return preferences.getBoolean(PREF_NIGHTMODE, false);
    }
  }

  public static class PrefsFragment extends PreferenceFragment implements
      SharedPreferences.OnSharedPreferenceChangeListener, StorageSelectDialog.OnSelectListener {

    private SliderPreference mSlider;
    private RecentSearchDao recentSearchDao;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);

      boolean auto_night_mode = PreferenceManager.getDefaultSharedPreferences(getActivity())
              .getBoolean(PREF_AUTONIGHTMODE, false);

      if(auto_night_mode){
        getPreferenceScreen().findPreference(PREF_NIGHTMODE).setEnabled(false);
      }

      if (BuildConfig.ENFORCED_LANG.equals("")) {
        setUpLanguageChooser(PREF_LANG);
      } else {
        getPreferenceScreen().removePreference(getPrefrence("pref_language"));
      }

      if (BuildConfig.IS_CUSTOM_APP) {
        PreferenceCategory notificationsCategory = (PreferenceCategory) findPreference("pref_extras");
        notificationsCategory.removePreference(getPrefrence("pref_wifi_only"));
      }

      mSlider = (SliderPreference) getPrefrence(PREF_ZOOM);
      setSliderState();
      setStorage();
      setUpSettings();
      new LanguageUtils(getActivity()).changeFont(getActivity().getLayoutInflater());
      recentSearchDao = new RecentSearchDao(KiwixDatabase.getInstance(getActivity()));

    }

    private void deleteSearchHistoryFromDb() {
      recentSearchDao.deleteSearchHistory();
    }

    private void setStorage(){
      if (BuildConfig.IS_CUSTOM_APP) {
        getPreferenceScreen().removePreference(getPrefrence("pref_storage"));
      } else {
        if (Environment.isExternalStorageEmulated()) {
          getPrefrence(PREF_STORAGE).setTitle(PreferenceManager.getDefaultSharedPreferences(getActivity())
              .getString(KiwixMobileActivity.PREF_STORAGE_TITLE, "Internal"));
        } else {
          getPrefrence(PREF_STORAGE).setTitle(PreferenceManager.getDefaultSharedPreferences(getActivity())
              .getString(KiwixMobileActivity.PREF_STORAGE_TITLE, "External"));
        }
        getPrefrence(PREF_STORAGE).setSummary(LibraryUtils.bytesToHuman( new File(PreferenceManager.getDefaultSharedPreferences(getActivity())
            .getString(KiwixMobileActivity.PREF_STORAGE, Environment.getExternalStorageDirectory().getPath())).getFreeSpace()));
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
      Preference languagePref = getPrefrence(preferenceId);
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
      String selectedLang = sharedPreferences.getString(PREF_LANG, Locale.getDefault().toString());

      languagePref.setTitle(new Locale(selectedLang).getDisplayLanguage());
    }

    private void restartActivity() {
      getActivity().setResult(RESULT_RESTART);
      getActivity().finish();
      getActivity().startActivity(new Intent(getActivity(), getActivity().getClass()));
    }

    private void setAppVersionNumber() {
      String version;
      version = BuildConfig.VERSION_NAME + " Build: " + BuildConfig.VERSION_CODE;
      EditTextPreference versionPref = (EditTextPreference) PrefsFragment.this
          .findPreference(PREF_VERSION);
      versionPref.setSummary(version);
    }

    private Preference getPrefrence(String preferenceId) {
      return PrefsFragment.this.findPreference(preferenceId);
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
        KiwixMobileActivity.refresh = true;
        getActivity().recreate();
      }
      if (key.equals(PREF_WIFI_ONLY)) {
        KiwixMobileActivity.wifiOnly = sharedPreferences.getBoolean(PREF_WIFI_ONLY, true);
      }
      if(key.equals(PREF_AUTONIGHTMODE)){
        KiwixMobileActivity.refresh = true;
        getActivity().recreate();
      }

    }

    private void clearAllHistoryDialog() {
      new AlertDialog.Builder(getActivity(), dialogStyle())
          .setTitle(getResources().getString(R.string.clear_all_history_dialog_title))
          .setMessage(getResources().getString(R.string.clear_recent_and_tabs_history_dialog))
          .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              deleteSearchHistoryFromDb();
              allHistoryCleared = true;
              Toast.makeText(getActivity(), getResources().getString(R.string.all_history_cleared_toast), Toast.LENGTH_SHORT).show();
            }
          })
          .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // do nothing
            }
          })
          .setIcon(android.R.drawable.ic_dialog_alert)
          .show();
    }

    public void openCredits(){
      WebView view = (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.credits_webview, null);
      view.loadUrl("file:///android_asset/credits.html");
      new AlertDialog.Builder(getActivity(), dialogStyle())
          .setView(view)
          .setPositiveButton(android.R.string.ok, null)
          .show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
      if (preference.getKey().equalsIgnoreCase(PREF_CLEAR_ALL_HISTORY))
        clearAllHistoryDialog();

      if (preference.getKey().equalsIgnoreCase(PREF_CREDITS))
        openCredits();

      if (preference.getKey().equalsIgnoreCase(PREF_STORAGE))
        openFolderSelect();

      if (preference.getKey().equalsIgnoreCase(PREF_LANG))
        openLanguageSelect();

      return true;
    }

    public void openLanguageSelect() {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
      LanguageUtils languageUtils = new LanguageUtils(getActivity());
      String selectedLang = sharedPreferences.getString(PREF_LANG, Locale.getDefault().toString());

      new LanguageSelectDialog.Builder(getActivity(), dialogStyle())
          .setLanguages(languageUtils.getLanguageList())
          .setSingleSelect(true)
          .setSelectedLanguage(selectedLang)
          .setOnLanguageSelectedListener((languageCode -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PREF_LANG, languageCode);
            editor.apply();

            LanguageUtils.handleLocaleChange(getActivity(), languageCode);
            // Request a restart when the user returns to the Activity, that called this Activity
            restartActivity();
          }))
          .show();
    }

    public void openFolderSelect(){
      FragmentManager fm = getFragmentManager();
      StorageSelectDialog dialogFragment = new StorageSelectDialog();
      Bundle b = new Bundle();
      b.putString(StorageSelectDialog.STORAGE_DIALOG_INTERNAL, getResources().getString(R.string.internal_storage));
      b.putString(StorageSelectDialog.STORAGE_DIALOG_EXTERNAL, getResources().getString(R.string.external_storage));
      b.putInt(StorageSelectDialog.STORAGE_DIALOG_THEME, StyleUtils.dialogStyle());
      dialogFragment.setArguments(b);
      dialogFragment.setOnSelectListener(this);
      dialogFragment.show(fm, getResources().getString(R.string.pref_storage));

    }

    @Override
    public void selectionCallback(StorageDevice storageDevice) {
      findPreference(PREF_STORAGE).setSummary(storageDevice.getSize());
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putString(KiwixMobileActivity.PREF_STORAGE,storageDevice.getName());
      if (storageDevice.isInternal()) {
        findPreference(PREF_STORAGE).setTitle(getResources().getString(R.string.internal_storage));
        editor.putString(KiwixMobileActivity.PREF_STORAGE_TITLE, getResources().getString(R.string.internal_storage));
      } else {
        findPreference(PREF_STORAGE).setTitle(getResources().getString(R.string.external_storage));
        editor.putString(KiwixMobileActivity.PREF_STORAGE_TITLE, getResources().getString(R.string.external_storage));
      }
      editor.apply();
    }
  }
}
