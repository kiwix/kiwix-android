package org.kiwix.kiwixmobile;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;


public class KiwixSettings extends PreferenceActivity {
	ListPreference prefList;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        prefList = (ListPreference) findPreference("pref_zoom");
        prefList.setDefaultValue(prefList.getEntryValues()[0]);
        String ss = prefList.getValue();
        if (ss == null) {
            prefList.setValue((String)prefList.getEntryValues()[0]);
            ss = prefList.getValue();
        }
        prefList.setSummary(prefList.getEntries()[prefList.findIndexOfValue(ss)]);


        prefList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefList.setSummary(prefList.getEntries()[prefList.findIndexOfValue(newValue.toString())]);
                return true;
            }
        });     
    }

}