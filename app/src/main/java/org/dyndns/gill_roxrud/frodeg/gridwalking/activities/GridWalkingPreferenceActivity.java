package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.dyndns.gill_roxrud.frodeg.gridwalking.InsetHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;


public class GridWalkingPreferenceActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        InsetHelper.applySystemBarInsets(findViewById(R.id.activity_preferences_layout));
    }

}
