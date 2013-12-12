package org.kiwix.kiwixmobile;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class KiwixSettings extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            prepareListPreferenceForAutoSummary("pref_zoom");

            // Set version
            String version;
            try {
                version = getPackageManager().getPackageInfo("org.kiwix.kiwixmobile", 0).versionName;
            } catch (NameNotFoundException e) {
                return;
            }
            EditTextPreference versionPref = (EditTextPreference) findPreference("pref_version");
            versionPref.setSummary(version);
        }

        private void prepareListPreferenceForAutoSummary(String preferenceID) {
            ListPreference prefList = (ListPreference) findPreference(preferenceID);
            prefList.setDefaultValue(prefList.getEntryValues()[0]);
            String ss = prefList.getValue();
            if (ss == null) {
                prefList.setValue((String) prefList.getEntryValues()[0]);
                ss = prefList.getValue();
            }
            prefList.setSummary(prefList.getEntries()[prefList.findIndexOfValue(ss)]);
            prefList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    if (preference instanceof ListPreference) {
                        preference.setSummary(((ListPreference) preference)
                                .getEntries()[((ListPreference) preference)
                                .findIndexOfValue(newValue.toString())]);
                    }
                    return true;
                }
            });
        }
    }
}