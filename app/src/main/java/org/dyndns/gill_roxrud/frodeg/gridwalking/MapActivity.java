package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;


public class MapActivity extends AppCompatActivity {
    private static final String MAP_FRAGMENT_TAG = "org.dyndns.gill_roxrud.frodeg.gridwalking.MAP_FRAGMENT_TAG";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_map);

        FragmentManager fm = this.getSupportFragmentManager();
        if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            MapFragment mapFragment = new MapFragment();
            fm.beginTransaction().add(R.id.activity_map_layout, mapFragment, MAP_FRAGMENT_TAG).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == GridWalkingApplication.RequestCode.SYNC_HIGHSCORE.ordinal()) {
            if (responseCode == GridWalkingApplication.NetworkResponseCode.OK.ordinal()) {
                Parcelable response = data.getParcelableExtra(HighscoreIntentService.RESPONSE_EXTRA);
//                DomainObj domainObj = (DomainObj) response;
//                setTextValue(R.id.domainText, domainObj.domain);
                Toast.makeText(GridWalkingApplication.getContext(), "Syncing highscore succeeded", Toast.LENGTH_LONG).show();
            } else {
//                setTextValue(R.id.domainText, "");
                String msg = data.getStringExtra(HighscoreIntentService.RESPONSE_MSG_EXTRA);
//                Toast.makeText(ApplicationWrapper.getContext(), "Getting subdomains failed: " + msg, Toast.LENGTH_LONG).show();
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
