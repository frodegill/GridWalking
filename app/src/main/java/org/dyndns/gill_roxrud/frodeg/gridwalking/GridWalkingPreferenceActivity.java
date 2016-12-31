package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class GridWalkingPreferenceActivity extends PreferenceActivity {

    public static final String OFFLINE_PREFERENCE = "offline_preference";
    public static final String SNAP_TO_CENTRE_PREFERENCE = "snap_to_centre_preference";
    public static final String HIGHSCORE_NICKNAME_PREFERENCE = "highscore_nickname_preference";

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_main); //API 10-
    }
}
