package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.osmdroid.config.Configuration;


public class MapActivity extends AppCompatActivity {
    private static final String MAP_FRAGMENT_TAG = "org.dyndns.gill_roxrud.frodeg.gridwalking.MAP_FRAGMENT_TAG";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = GridWalkingApplication.getContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setMapViewHardwareAccelerated(true);

        this.setContentView(R.layout.activity_map);

        FragmentManager fm = this.getSupportFragmentManager();
        if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            MapFragment mapFragment = new MapFragment();
            fm.beginTransaction().add(R.id.activity_map_layout, mapFragment, MAP_FRAGMENT_TAG).commit();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == GridWalkingApplication.RequestCode.SYNC_HIGHSCORE.ordinal()) {
            if (responseCode == GridWalkingApplication.NetworkResponseCode.OK.ordinal()) {
                Parcelable response = data.getParcelableExtra(HighscoreIntentService.RESPONSE_EXTRA);
                HighscoreList highscoreList = (HighscoreList) response;

                Intent intent = new Intent(this, HighscoreActivity.class);
                intent.putExtra(HighscoreActivity.HIGHSCORE_LIST, highscoreList);
                ContextCompat.startActivity(this, intent, null);
            } else {
                String msg = data.getStringExtra(HighscoreIntentService.RESPONSE_MSG_EXTRA);
                Toast.makeText(GridWalkingApplication.getContext(), "Syncing highscore failed: " + msg, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, responseCode, data);
    }

    void syncHighscore() {
        PendingIntent pendingResult = createPendingResult(GridWalkingApplication.RequestCode.SYNC_HIGHSCORE.ordinal(), new Intent(), 0);
        Intent intent = new Intent(GridWalkingApplication.getContext(), HighscoreIntentService.class);
        intent.putExtra(HighscoreIntentService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
    }

}
