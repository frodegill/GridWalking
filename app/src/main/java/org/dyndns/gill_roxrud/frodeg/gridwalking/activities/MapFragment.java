package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.location.GnssStatusCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingApplication;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.R;
import org.dyndns.gill_roxrud.frodeg.gridwalking.overlays.BonusOverlay;
import org.dyndns.gill_roxrud.frodeg.gridwalking.overlays.GridOverlay;
import org.dyndns.gill_roxrud.frodeg.gridwalking.overlays.MyLocationOverlay;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import java.util.ArrayList;


public class MapFragment extends Fragment implements MenuProvider, LocationListenerCompat, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final long  LOCATION_UPDATE_INTERVAL = 30L;
    private static final float LOCATION_UPDATE_DISTANCE = 25.0f;

    private MapView mapView;

    private GnssStatusCompat.Callback gnssStatusCallback = null;


    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, null);
        mapView = view.findViewById(R.id.mapview);
        mapView.setDestroyMode(false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();

        Configuration.getInstance().setDebugTileProviders(false);
        Configuration.getInstance().setDebugMode(false);

        mapView.setTileSource(gameState.getMapSource());
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);

        mapView.getOverlays().add(new CopyrightOverlay(GridWalkingApplication.getContext()));
        mapView.getOverlays().add(new GridOverlay(this));
        mapView.getOverlays().add(new BonusOverlay());
        mapView.getOverlays().add(new MyLocationOverlay());
        mapView.getOverlays().add(new ScaleBarOverlay(mapView));

        repositionAndEnableMap();

        if (db.GetProperty(GridWalkingDBHelper.PROPERTY_BUGFIX_PURGE_DUPLICATES) != 0) {
            GameState.getInstance().getGrid().BugfixPurgeDuplicatesT();
        }
        if (db.GetProperty(GridWalkingDBHelper.PROPERTY_BUGFIX_ADJUST_LEVELCOUNT) != 0) {
            GameState.getInstance().getGrid().BugfixAdjustLevelCountT();
        }

        onScoreUpdated();

        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onPause() {
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();

        boolean successful = true;
        SQLiteDatabase dbInTransaction = db.StartTransaction();
        try {
            db.SetStringProperty(dbInTransaction, GridWalkingDBHelper.PROPERTY_LATITUDE_POS, String.valueOf(mapView.getMapCenter().getLatitude()));
            db.SetStringProperty(dbInTransaction, GridWalkingDBHelper.PROPERTY_LONGITUDE_POS, String.valueOf(mapView.getMapCenter().getLongitude()));
            db.SetProperty(dbInTransaction, GridWalkingDBHelper.PROPERTY_ZOOM_LEVEL, (int)mapView.getZoomLevelDouble());

        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR12: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        db.EndTransaction(dbInTransaction, successful);

        super.onPause();
        DisableLocationUpdates();
        mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GridWalkingApplication.getContext());
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mapView.onResume();

        repositionAndEnableMap();
    }

    private void repositionAndEnableMap() {
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();
        mapView.getController().setZoom((double)db.GetProperty(GridWalkingDBHelper.PROPERTY_ZOOM_LEVEL));
        final String latitudeString = db.GetStringPropertyR(GridWalkingDBHelper.PROPERTY_LATITUDE_POS);
        final String longitudeString = db.GetStringPropertyR(GridWalkingDBHelper.PROPERTY_LONGITUDE_POS);
        if (latitudeString!=null && longitudeString!=null) {
            mapView.setExpectedCenter(new GeoPoint(Double.parseDouble(latitudeString), Double.parseDouble(longitudeString)));
        }
        mapView.setUseDataConnection(gameState.getUseDataConnection());

        IGeoPoint positionHint = gameState.popPositionHint();
        if (positionHint != null) {
            mapView.setExpectedCenter(positionHint);
        }

        EnableLocationUpdates();
    }

    private void discoverSelectedGridT() {
        if (GameState.getInstance().getGrid().DiscoverSelectedGridT()) {
            mapView.postInvalidate();
            onScoreUpdated();
        }
    }

    public void onLongPress(IGeoPoint geoPoint)
    {
        GameState gameState = GameState.getInstance();
        if (!gameState.getShowGrids()) {
            return;
        }

        if (gameState.getBonus().GetUnusedBonusCount() == 0) {
            return;
        }

        if (gameState.getGrid().SelectGridIfValid(geoPoint, true)) {
            mapView.postInvalidate();
            if (gameState.getSelectedGridKey() != null) {
                this.getActivity().openOptionsMenu();
            }
        }
    }

    public void onScoreUpdated() {
        TextView view;
        try {
            view = getView().findViewById(R.id.score);
            view.setText(GameState.getInstance().getGrid().getScoreString());
        }
        catch(NullPointerException e) {}

        try {
            view = getView().findViewById(R.id.bonus);
            view.setText(GameState.getInstance().getBonus().getBonusString());
        } catch(NullPointerException e) {}
    }

    public void onSpeedAltitudeUpdated(final int speed, final int altitude) {
        TextView view;
        try {
            view = getView().findViewById(R.id.speed);
            view.setText(altitude == 0.0 ? String.format(getString(R.string.speed), speed) : String.format(getString(R.string.speed_and_height), speed, altitude));
        }
        catch(NullPointerException e) {}
    }

    private static final int MAX_QUALITY_COUNT = 8;
    public void onGpsQualityUpdated(ArrayList<Integer> quality) {
        String s = "";
        for (int index=0; index<MAX_QUALITY_COUNT && index<quality.size(); index++) {
            s += Integer.toString(quality.get(index));
        }

        if (quality.size() > MAX_QUALITY_COUNT) {
            s += "+";
        }

        TextView view;
        try {
            view = getView().findViewById(R.id.gpsquality);
            view.setText(s);
        }
        catch(NullPointerException e) {}
    }

    @Override
    public void onLocationChanged(Location location) {
        GameState gameState = GameState.getInstance();
        gameState.onPositionChangedT(this, location.getLongitude(), location.getLatitude(), location.getAltitude());

        if (gameState.getSnapToCentre()) {
            GeoPoint position = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapView.setExpectedCenter(position);
        }
        mapView.postInvalidate();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    private void DisableLocationUpdates() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);

            LocationManagerCompat.unregisterGnssStatusCallback(locationManager, gnssStatusCallback);
        }
    }

    private void EnableLocationUpdates() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (!locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(getContext(), "Failed to find a Location Provider (GPS)", Toast.LENGTH_LONG).show();
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE, this);

            gnssStatusCallback = new GnssStatusCompat.Callback() {
                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatusCompat status) {
                    GameState gameState = GameState.getInstance();
                    gameState.updateGpsQualityDisplay(MapFragment.this, status);
                }
            };

            LocationManagerCompat.registerGnssStatusCallback(locationManager, gnssStatusCallback, new Handler(Looper.getMainLooper()));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("map_source".equals(key)) {
            ITileSource oldMapSource = mapView.getTileProvider().getTileSource();

            GameState gameState = GameState.getInstance();
            ITileSource newMapSource = gameState.getMapSource();

            if (oldMapSource != newMapSource) {
                mapView.getTileProvider().clearTileCache();
                mapView.setTileSource(newMapSource);
                mapView.postInvalidate();
            }
        } else if ("offline_preference".equals(key)) {
            GameState gameState = GameState.getInstance();
            mapView.setUseDataConnection(gameState.getUseDataConnection());
        }
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem item = menu.findItem(R.id.mark_visited);
        if (item != null) {
            GameState gameState = GameState.getInstance();
            boolean showGrids = gameState.getShowGrids();
            item.setVisible(showGrids && gameState.getSelectedGridKey()!=null);
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.activity_map, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.mark_visited) {
            discoverSelectedGridT();
            return true;
        } else if (itemId == R.id.sync_highscore) {
            ((MapActivity) getActivity()).syncHighscore();
            return true;
        } else if (itemId == R.id.settings) {
            Intent i = new Intent(getContext(), GridWalkingPreferenceActivity.class);
            startActivity(i);
            return true;
        }
        return false;
    }

}
