package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class GameState {

    private static GameState instance = null;
    private final Grid grid;
    private final Bonus bonus;
    private final GridWalkingDBHelper db;

    private Integer selectedGridKey = null;

    private final Point<Double> currentPos = new Point<>(Grid.EAST+1.0, Grid.NORTH+1.0);


    private GameState() {
        grid = new Grid();
        bonus = new Bonus();
        db = new GridWalkingDBHelper(GridWalkingApplication.getContext());
    }

    static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    Grid getGrid() {
        return grid;
    }

    Bonus getBonus() {
        return bonus;
    }

    GridWalkingDBHelper getDB() {return db;}

    boolean getUseDataConnection() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        return !sharedPrefs.getBoolean(GridWalkingPreferenceActivity.OFFLINE_PREFERENCE, false);
    }

    boolean getSnapToCentre() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        return sharedPrefs.getBoolean(GridWalkingPreferenceActivity.SNAP_TO_CENTRE_PREFERENCE, true);
    }

    String getHighscoreNickname() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        return sharedPrefs.getString(GridWalkingPreferenceActivity.HIGHSCORE_NICKNAME_PREFERENCE, "Anonymous");
    }

    Integer getSelectedGridKey() {
        return selectedGridKey;
    }

    void setSelectedGridKey(Integer selectedGridKey) {
        this.selectedGridKey = selectedGridKey;
    }

    Point<Double> getCurrentPos() {
        return currentPos;
    }

    void onPositionChanged(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (grid.Discover(currentPos, false) || null!=bonus.ValidBonusKeyFromPos(currentPos)) {
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }
}
