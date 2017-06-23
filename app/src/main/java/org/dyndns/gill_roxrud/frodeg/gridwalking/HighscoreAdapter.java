package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class HighscoreAdapter extends BaseAdapter {

    private final ArrayList<HighscoreItem> data;
    private static LayoutInflater inflater = null;

    public HighscoreAdapter(Activity activity, ArrayList<HighscoreItem> data) {
        this.data = data;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;
        if (view == null) {
            v = inflater.inflate(R.layout.highscorerow, null);
        }

        HighscoreItem item = (HighscoreItem) getItem(i);

        TextView textView = v.findViewById(R.id.position);
        textView.setText(Integer.toString(item.getPosition()));

        textView = v.findViewById(R.id.nickname);
        textView.setText(item.getUsername());

        textView = v.findViewById(R.id.levels);
        textView.setText(item.getLevelsString());

        textView = v.findViewById(R.id.score);
        textView.setText(Long.toString(item.getScore()));

        return v;
    }
}
