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

import org.kiwix.kiwixmobile.LanguageUtils;
import org.kiwix.kiwixmobile.R;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

public class KiwixSettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment())
                .commit();
    }

    private class PrefsFragment extends PreferenceFragment {

        public static final int RESULT_RESTART = 1236;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            setUpSettings();
            new LanguageUtils(getActivity()).changeFont(getLayoutInflater());
        }

        public void setUpSettings() {
            setUpLanguageChooser("pref_language_chooser");
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
                    .findPreference("pref_version");
            versionPref.setSummary(version);
        }

        private Preference getPrefrence(String preferenceId) {

            return PrefsFragment.this.findPreference(preferenceId);

        }
    }
}