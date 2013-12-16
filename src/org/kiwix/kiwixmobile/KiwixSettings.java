package org.kiwix.kiwixmobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.Locale;

public class KiwixSettings extends Activity {

    public static final int RESULT_RESTART = 1236;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();

        new LanguageUtils(this).changeFont(getLayoutInflater());
    }

    public class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            prepareListPreferenceForAutoSummary("pref_zoom");
            setUpLanguageChooser("pref_language_chooser");
            setAppVersionNumber();
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

            LanguageUtils languageUtils = new LanguageUtils(getActivity());

            languageList.setTitle(Locale.getDefault().getDisplayLanguage());

            languageList.setEntries(languageUtils.getValues().toArray(new String[0]));
            languageList.setEntryValues(languageUtils.getKeys().toArray(new String[0]));

            languageList.setDefaultValue(Locale.getDefault().toString());

            languageList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    if (!newValue.equals(Locale.getDefault().toString())) {

                        LanguageUtils.handleLocaleChange(getActivity(), newValue.toString());
                        // Request a restart when the user returns to the Activity, that called this Activity
                        setResult(RESULT_RESTART);
                        finish();
                        startActivity(new Intent(getActivity(), KiwixSettings.class));
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
}