package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class HighscoreActivity extends AppCompatActivity {

    public static final String HIGHSCORE_LIST = "highscore_list";

    private ListView highscoreListView;
    private ArrayList<HighscoreItem> highscoreArrayList;
    private HighscoreAdapter highscoreListViewAdapter;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_highscore);

        highscoreListView = (ListView) findViewById(R.id.highscoreList);
        highscoreArrayList = new ArrayList<>();
        highscoreListViewAdapter = new HighscoreAdapter(this, highscoreArrayList);

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
