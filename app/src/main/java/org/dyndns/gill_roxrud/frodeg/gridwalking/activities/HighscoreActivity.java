package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;


import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Grid;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreAdapter;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreItem;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreList;
import org.dyndns.gill_roxrud.frodeg.gridwalking.InvalidPositionException;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;
import org.dyndns.gill_roxrud.frodeg.gridwalking.intents.SyncGridsIntentService;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class HighscoreActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener {

    public static final String HIGHSCORE_LIST = "highscore_list";


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_highscore);

        final ListView highscoreListView = findViewById(R.id.highscoreList);
        ArrayList<HighscoreItem> highscoreArrayList = new ArrayList<>();
        HighscoreAdapter highscoreListViewAdapter = new HighscoreAdapter(this, highscoreArrayList);

        highscoreListView.setAdapter(highscoreListViewAdapter);
        highscoreListView.setOnItemLongClickListener(this);

        Intent intent = getIntent();
        Parcelable highscoreParcelable = intent.getParcelableExtra(HIGHSCORE_LIST);
        HighscoreList highscoreList = (HighscoreList) highscoreParcelable;
        if (highscoreList != null) {
            TextView view = findViewById(R.id.total_players);
            view.setText(Integer.toString(highscoreList.getTotalPlayerCount()));

            view = findViewById(R.id.your_position);
            view.setText(Integer.toString(highscoreList.getPlayerPosition()));

            highscoreArrayList.clear();
            highscoreArrayList.addAll(highscoreList.getHighscoreItemList());
            highscoreListViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == GridWalkingApplication.RequestCode.SYNC_GRIDS.ordinal()) {
            if (responseCode == GridWalkingApplication.NetworkResponseCode.OK.ordinal()) {
                GameState gameState = GameState.getInstance();
                gameState.setShowGridState(GameState.ShowGridState.SYNCED);
                if (data.hasExtra(SyncGridsIntentService.RESPONSE_EXTRA)) {
                    int aGrid = data.getIntExtra(SyncGridsIntentService.RESPONSE_EXTRA, 0);
                    Grid grid = GameState.getInstance().getGrid();
                    try {
                        GeoPoint geoPoint = new GeoPoint(grid.FromVerticalGrid(grid.YFromKey(aGrid)),
                                grid.FromHorizontalGrid(grid.XFromKey(aGrid)));
                        gameState.pushPositionHint(geoPoint);
                    } catch (InvalidPositionException e) {
                    }
                }
                this.finish();
            } else {
                String msg = data.getStringExtra(SyncGridsIntentService.RESPONSE_MSG_EXTRA);
                Toast.makeText(this, "Syncing grids failed: " + msg, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, responseCode, data);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final ListView highscoreListView = findViewById(R.id.highscoreList);
        HighscoreItem item = (HighscoreItem) highscoreListView.getItemAtPosition(position);

        PendingIntent pendingResult = createPendingResult(GridWalkingApplication.RequestCode.SYNC_GRIDS.ordinal(), new Intent(), 0);
        Intent intent = new Intent(HighscoreActivity.this, SyncGridsIntentService.class);
        intent.putExtra(SyncGridsIntentService.PARAM_GUID, item.getGuid());
        intent.putExtra(SyncGridsIntentService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
        return true;
    }
}
