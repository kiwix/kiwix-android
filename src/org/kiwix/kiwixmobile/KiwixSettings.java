package org.kiwix.kiwixmobile;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class KiwixSettings extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}