package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;


public class MapActivity extends AppCompatActivity {
    private static final String MAP_FRAGMENT_TAG = "org.dyndns.gill_roxrud.frodeg.gridwalking.MAP_FRAGMENT_TAG";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_map);
        FragmentManager fm = this.getSupportFragmentManager();
        if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            MapFragment mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG).commit();
        }
    }
}
