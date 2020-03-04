package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;

import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.GridWalkingPreferenceActivity;
import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.MapFragment;
import org.dyndns.gill_roxrud.frodeg.gridwalking.network.HttpsClient;
import org.dyndns.gill_roxrud.frodeg.gridwalking.network.HttpsClientCompat6;
import org.dyndns.gill_roxrud.frodeg.gridwalking.network.HttpsClientCompat7;
import org.osmdroid.api.IGeoPoint;

import java.util.ArrayList;
import java.util.Collections;


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
    private long currentPosSetTime = 0L;


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

    public HttpsClient getHttpsClient() {
        if (Build.VERSION.SDK_INT < 24) { // Older than 7.0
            return new HttpsClientCompat6();
        } else {
            return new HttpsClientCompat7();
        }
    }

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
        prefsEditor.apply();
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

    public void onPositionChangedT(MapFragment mapFragment, double x_pos, double y_pos, double z_pos) {
        long now = System.currentTimeMillis();
        if (currentPosSetTime != 0L) {
            float[] result = new float[1];
            Location.distanceBetween(currentPos.getY(), currentPos.getX(), y_pos, x_pos, result);
            float kmPrHour = (result[0] / ((now - currentPosSetTime)/1000.0f)) * 3.6f;
            mapFragment.onSpeedAltitudeUpdated((int)kmPrHour, (int)z_pos);
        }

        currentPos.set(x_pos, y_pos);
        currentPosSetTime = now;

        try {
            if (grid.DiscoverT(currentPos, false) || null!=bonus.ValidBonusKeyFromPosT(currentPos)) { //Two transactions, OK
                mapFragment.onScoreUpdated();
            }
        } catch (InvalidPositionException e) {
        }
    }

    public void updateGpsQualityDisplay(MapFragment mapFragment, Object gpsStatusObject) {
        ArrayList<Integer> results = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Iterable<GpsSatellite> satellites = ((GpsStatus)gpsStatusObject).getSatellites();
            for (GpsSatellite satellite : satellites) {
                float snr = satellite.getSnr();

                if (snr > 500) results.add(9);
                else if (snr > 250) results.add(8);
                else if (snr > 100) results.add(7);
                else if (snr > 75) results.add(6);
                else if (snr > 50) results.add(5);
                else if (snr > 25) results.add(4);
                else if (snr > 10) results.add(3);
                else if (snr > 5) results.add(2);
                else if (snr > 1) results.add(1);
                else results.add(0);
            }
        } else {
            GnssStatus gnssStatus = (GnssStatus)gpsStatusObject;
            int satelliteCount = gnssStatus.getSatelliteCount();
            for (int i = 0; i<satelliteCount; i++) {
                float snr = gnssStatus.getCn0DbHz(i);

                if (snr > 54) results.add(9);
                else if (snr > 48) results.add(8);
                else if (snr > 42) results.add(7);
                else if (snr > 36) results.add(6);
                else if (snr > 30) results.add(5);
                else if (snr > 24) results.add(4);
                else if (snr > 18) results.add(3);
                else if (snr > 12) results.add(2);
                else if (snr > 6) results.add(1);
                else results.add(0);
            }
        }
        Collections.sort(results, Collections.<Integer>reverseOrder());
        mapFragment.onGpsQualityUpdated(results);
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
