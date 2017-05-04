package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;


import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.TextView;

import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreAdapter;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreItem;
import org.dyndns.gill_roxrud.frodeg.gridwalking.HighscoreList;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;

import java.util.ArrayList;

public class HighscoreActivity extends AppCompatActivity {

    public static final String HIGHSCORE_LIST = "highscore_list";


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_highscore);

        ListView highscoreListView = (ListView) findViewById(R.id.highscoreList);
        ArrayList<HighscoreItem> highscoreArrayList = new ArrayList<>();
        HighscoreAdapter highscoreListViewAdapter = new HighscoreAdapter(this, highscoreArrayList);

        highscoreListView.setAdapter(highscoreListViewAdapter);

        Intent intent = getIntent();
        Parcelable highscoreParcelable = intent.getParcelableExtra(HIGHSCORE_LIST);
        HighscoreList highscoreList = (HighscoreList) highscoreParcelable;
        if (highscoreList != null) {
            TextView view = (TextView) findViewById(R.id.total_players);
            view.setText(Integer.toString(highscoreList.getTotalPlayerCount()));

            view = (TextView) findViewById(R.id.your_position);
            view.setText(Integer.toString(highscoreList.getPlayerPosition()));

            highscoreArrayList.clear();
            highscoreArrayList.addAll(highscoreList.getHighscoreItemList());
            highscoreListViewAdapter.notifyDataSetChanged();
        }
    }
}
