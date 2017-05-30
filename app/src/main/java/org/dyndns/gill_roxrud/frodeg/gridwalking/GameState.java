package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.GridWalkingPreferenceActivity;
import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.MapFragment;
import org.osmdroid.api.IGeoPoint;


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
    private IGeoPoint positionHint = null;

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
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        if (!sharedPrefs.getBoolean(GridWalkingPreferenceActivity.SHOW_GRIDS_PREFERENCE, true)) {
            return ShowGridState.NONE;
        } else {
            return sharedPrefs.getBoolean(GridWalkingPreferenceActivity.SHOW_SYNCED_GRIDS_PREFERENCE, false) ? ShowGridState.SYNCED : ShowGridState.SELF;
        }
    }

    public void setShowGridState(final ShowGridState showGridState) {
        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext()).edit();
        prefsEditor.putBoolean(GridWalkingPreferenceActivity.SHOW_GRIDS_PREFERENCE, ShowGridState.NONE!=showGridState);
        prefsEditor.putBoolean(GridWalkingPreferenceActivity.SHOW_SYNCED_GRIDS_PREFERENCE, ShowGridState.SYNCED==showGridState);
        prefsEditor.putBoolean(GridWalkingPreferenceActivity.SNAP_TO_CENTRE_PREFERENCE, ShowGridState.SYNCED!=showGridState);
        prefsEditor.commit();
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

    public void onPositionChangedT(MapFragment mapFragment, double x_pos, double y_pos) {
        currentPos.set(x_pos, y_pos);
        try {
            if (grid.DiscoverT(currentPos, false) || null!=bonus.ValidBonusKeyFromPosT(currentPos)) { //Two transactions, OK
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }

    public void pushPositionHint(final IGeoPoint positionHint) {
        this.positionHint = positionHint;
    }

    public IGeoPoint popPositionHint() {
        IGeoPoint tmp = this.positionHint;
        this.positionHint = null;
        return tmp;
    }
}
