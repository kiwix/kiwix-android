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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Toast;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.DatabaseHelper;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.views.SliderPreference;

import java.util.Locale;

public class KiwixSettingsActivity extends AppCompatActivity {

  public static final int RESULT_RESTART = 1236;

  public static final String PREF_LANG = "pref_language_chooser";

  public static final String PREF_VERSION = "pref_version";

  public static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

  public static final String PREF_ZOOM = "pref_zoom_slider";

  public static final String PREF_RECENT_SEARCH = "pref_clear_recent_searches";

  public static String zimFile;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    zimFile = getIntent().getStringExtra("zim_file");

    getFragmentManager()
        .beginTransaction().
        replace(R.id.content_frame, new PrefsFragment())
        .commit();

    setUpToolbar();
  }

  private void setUpToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
  }

  public static class PrefsFragment extends PreferenceFragment implements
      SharedPreferences.OnSharedPreferenceChangeListener {

    private SliderPreference mSlider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);

      if (Constants.CUSTOM_APP_ENFORCED_LANG.equals("")) {
        setUpLanguageChooser(PREF_LANG);
      } else {
        getPreferenceScreen().removePreference(getPrefrence("pref_language"));
      }
      mSlider = (SliderPreference) getPrefrence(PREF_ZOOM);
      setSliderState();
      setUpSettings();
      new LanguageUtils(getActivity()).changeFont(getActivity().getLayoutInflater());
    }


    private void recentSearchConfirmDialog() {
      new AlertDialog.Builder(getActivity())
          .setTitle("Clear History")
          .setMessage(getResources().getString(R.string.history_dialog))
          .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              deleteSearchHistoryFromDb();
              Toast.makeText(getActivity(), "Recent searches cleared", Toast.LENGTH_SHORT).show();
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

    private void deleteSearchHistoryFromDb() {
      DatabaseHelper mDatabaseHelper = new DatabaseHelper(getActivity(), zimFile);
      SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

      mDatabaseHelper.deleteSearchTable(db);
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

      ListPreference languageList = (ListPreference) getPrefrence(preferenceId);
      LanguageUtils languageUtils = new LanguageUtils(getActivity());

      languageList.setTitle(Locale.getDefault().getDisplayLanguage());
      languageList.setEntries(languageUtils.getValues().toArray(new String[0]));
      languageList.setEntryValues(languageUtils.getKeys().toArray(new String[0]));
      languageList.setDefaultValue(Locale.getDefault().toString());
      languageList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

          if (!newValue.equals(Locale.getDefault().toString())) {

            LanguageUtils.handleLocaleChange(getActivity(), newValue.toString());
            // Request a restart when the user returns to the Activity, that called this Activity
            restartActivity();
          }
          return true;
        }
      });
    }

    private void restartActivity() {
      getActivity().setResult(RESULT_RESTART);
      getActivity().finish();
      getActivity().startActivity(new Intent(getActivity(), getActivity().getClass()));
    }

    private void setAppVersionNumber() {
      String version;

      try {
        version = getActivity().getPackageManager()
            .getPackageInfo("org.kiwix.kiwixmobile", 0).versionName;
      } catch (PackageManager.NameNotFoundException e) {
        return;
      }
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

    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {

      if (preference.getKey().equalsIgnoreCase(PREF_RECENT_SEARCH)) {
        recentSearchConfirmDialog();

      }

      return true;
    }
  }
}