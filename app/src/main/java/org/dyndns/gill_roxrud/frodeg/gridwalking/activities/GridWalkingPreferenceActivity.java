package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.dyndns.gill_roxrud.frodeg.gridwalking.InsetHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;


public class GridWalkingPreferenceActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_preferences);

        InsetHelper.applySystemBarInsets(findViewById(R.id.activity_preferences_layout));
    }

}
