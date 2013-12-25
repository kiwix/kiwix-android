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

package org.kiwix.kiwixmobile;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import java.util.Locale;

public class KiwixSettingsActivity extends PreferenceActivity {

    public static final int RESULT_RESTART = 1236;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        prepareListPreferenceForAutoSummary("pref_zoom");
        setUpLanguageChooser("pref_language_chooser");
        setAppVersionNumber();
        new LanguageUtils(this).changeFont(getLayoutInflater());
    }

    private void prepareListPreferenceForAutoSummary(String preferenceId) {

        ListPreference prefList = (ListPreference) findPreference(preferenceId);

        prefList.setDefaultValue(prefList.getEntryValues()[0]);
        String summary = prefList.getValue();

        if (summary == null) {
            prefList.setValue((String) prefList.getEntryValues()[0]);
            summary = prefList.getValue();
        }

        prefList.setSummary(prefList.getEntries()[prefList.findIndexOfValue(summary)]);
        prefList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference instanceof ListPreference) {
                    preference.setSummary(((ListPreference) preference)
                            .getEntries()[((ListPreference) preference)
                            .findIndexOfValue(newValue.toString())]);
                }
                return true;
            }
        });
    }

    private void setUpLanguageChooser(String preferenceId) {

        ListPreference languageList = (ListPreference) findPreference(preferenceId);

        LanguageUtils languageUtils = new LanguageUtils(KiwixSettingsActivity.this);

        languageList.setTitle(Locale.getDefault().getDisplayLanguage());

        languageList.setEntries(languageUtils.getValues().toArray(new String[0]));

        languageList.setEntryValues(languageUtils.getKeys().toArray(new String[0]));

        languageList.setDefaultValue(Locale.getDefault().toString());

        languageList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (!newValue.equals(Locale.getDefault().toString())) {

                    LanguageUtils.handleLocaleChange(KiwixSettingsActivity.this, newValue.toString());
                    // Request a restart when the user returns to the Activity, that called this Activity
                    setResult(RESULT_RESTART);
                    finish();
                    startActivity(new Intent(KiwixSettingsActivity.this, KiwixSettingsActivity.class));
                }
                return true;
            }
        });
    }

    private void setAppVersionNumber() {
        String version;

        try {
            version = getPackageManager().getPackageInfo("org.kiwix.kiwixmobile", 0).versionName;
        } catch (NameNotFoundException e) {
            return;
        }
        EditTextPreference versionPref = (EditTextPreference) findPreference("pref_version");
        versionPref.setSummary(version);
    }
}
