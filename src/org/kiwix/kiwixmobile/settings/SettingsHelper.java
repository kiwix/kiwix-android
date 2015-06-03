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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.Locale;

import static org.kiwix.kiwixmobile.BackwardsCompatibilityTools.newApi;


public class SettingsHelper {

    public static final int RESULT_RESTART = 1236;

    private Object mPreference;

    public SettingsHelper(Object preference) {
        mPreference = preference;
        setUpSettings();
    }

    public void setUpSettings() {
        setUpLanguageChooser("pref_language_chooser");
        setAppVersionNumber();
    }

    private void setUpLanguageChooser(String preferenceId) {

        ListPreference languageList = (ListPreference) getPrefrence(preferenceId);
        LanguageUtils languageUtils = new LanguageUtils(getContext());

        languageList.setTitle(Locale.getDefault().getDisplayLanguage());
        languageList.setEntries(languageUtils.getValues().toArray(new String[0]));
        languageList.setEntryValues(languageUtils.getKeys().toArray(new String[0]));
        languageList.setDefaultValue(Locale.getDefault().toString());
        languageList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (!newValue.equals(Locale.getDefault().toString())) {

                    LanguageUtils.handleLocaleChange(getContext(), newValue.toString());
                    // Request a restart when the user returns to the Activity, that called this Activity
                    restartActivity();
                }
                return true;
            }
        });
    }

    private void restartActivity() {
        getContext().setResult(RESULT_RESTART);
        getContext().finish();
        getContext().startActivity(new Intent(getContext(), getContext().getClass()));
    }

    private void setAppVersionNumber() {
        String version;

        try {
            version = getContext().getPackageManager()
                    .getPackageInfo("org.kiwix.kiwixmobile", 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        EditTextPreference versionPref = (EditTextPreference) getPrefrence("pref_version");
        versionPref.setSummary(version);
    }

    private Activity getContext() {
        if (newApi()) {
            return ((PreferenceFragment) mPreference).getActivity();
        }

        return ((PreferenceActivity) mPreference);
    }

    private Preference getPrefrence(String preferenceId) {

        if (newApi()) {
            return ((PreferenceFragment) mPreference).findPreference(preferenceId);
        }

        return ((PreferenceActivity) mPreference).findPreference(preferenceId);
    }
}
