package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.GridWalkingPreferenceActivity;
import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.MapFragment;


public class GameState {
    public enum ShowGridState {
        NONE,
        SELF,
        SYNCED
    }

    private static GameState instance = null;
    private final Grid grid;
    private final Bonus bonus;
    private final GridWalkingDBHelper db;

    private Integer selectedGridKey = null;

    private final Point<Double> currentPos = new Point<>(Grid.EAST+1.0, Grid.NORTH+1.0);


    private GameState() {
        Context context = GridWalkingApplication.getContext();
        grid = new Grid();
        bonus = new Bonus();
        db = new GridWalkingDBHelper(context);
        //String s = db.DumpDB();
        //System.out.println(s);
    }

    public static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    public Grid getGrid() {
        return grid;
    }

    public Bonus getBonus() {
        return bonus;
    }

    public GridWalkingDBHelper getDB() {return db;}

    public boolean getUseDataConnection() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        return !sharedPrefs.getBoolean(GridWalkingPreferenceActivity.OFFLINE_PREFERENCE, false);
    }

    public boolean getSnapToCentre() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        return sharedPrefs.getBoolean(GridWalkingPreferenceActivity.SNAP_TO_CENTRE_PREFERENCE, true);
    }

    public ShowGridState getShowGridState() {
        return ShowGridState.SELF;
    }

    public String getHighscoreNickname() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        return sharedPrefs.getString(GridWalkingPreferenceActivity.HIGHSCORE_NICKNAME_PREFERENCE, "Anonymous");
    }

    public Integer getSelectedGridKey() {
        return selectedGridKey;
    }

    void setSelectedGridKey(Integer selectedGridKey) {
        this.selectedGridKey = selectedGridKey;
    }

    public Point<Double> getCurrentPos() {
        return currentPos;
    }

    public void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (grid.Discover(currentPos, false) || null!=bonus.ValidBonusKeyFromPos(currentPos)) {
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
