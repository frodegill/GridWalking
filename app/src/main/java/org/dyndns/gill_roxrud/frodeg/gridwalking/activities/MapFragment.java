package org.dyndns.gill_roxrud.frodeg.gridwalking.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;


public class MapFragment extends Fragment implements LocationListener {
    private static final long  LOCATION_UPDATE_INTERVAL = 30L;
    private static final float LOCATION_UPDATE_DISTANCE = 25.0f;

    private MapView mapView;


    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, null);
        mapView = view.findViewById(R.id.mapview);
        return view;
    }

    private void setHardwareAccelerationOff() {
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();

        Configuration.getInstance().setDebugTileProviders(false);
        Configuration.getInstance().setDebugMode(false);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);

        mapView.getOverlays().add(new CopyrightOverlay(GridWalkingApplication.getContext()));
        mapView.getOverlays().add(new GridOverlay(this));
        mapView.getOverlays().add(new BonusOverlay());
        mapView.getOverlays().add(new MyLocationOverlay());
        mapView.getOverlays().add(new ScaleBarOverlay(mapView));

        setHasOptionsMenu(true);

        if (db.GetProperty(GridWalkingDBHelper.PROPERTY_BUGFIX_PURGE_DUPLICATES) != 0) {
            GameState.getInstance().getGrid().BugfixPurgeDuplicatesT();
        }
        if (db.GetProperty(GridWalkingDBHelper.PROPERTY_BUGFIX_ADJUST_LEVELCOUNT) != 0) {
            GameState.getInstance().getGrid().BugfixAdjustLevelCountT();
        }

        onScoreUpdated();
    }

    @Override
    public void onPause() {
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();

        boolean successful = true;
        SQLiteDatabase dbInTransaction = db.StartTransaction();
        try {
            db.SetProperty(dbInTransaction, GridWalkingDBHelper.PROPERTY_X_POS, mapView.getScrollX());
            db.SetProperty(dbInTransaction, GridWalkingDBHelper.PROPERTY_Y_POS, mapView.getScrollY());
            db.SetProperty(dbInTransaction, GridWalkingDBHelper.PROPERTY_ZOOM_LEVEL, mapView.getZoomLevel());

        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR12: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        db.EndTransaction(dbInTransaction, successful);

        super.onPause();
        DisableLocationUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();
        mapView.getController().setZoom(db.GetProperty(GridWalkingDBHelper.PROPERTY_ZOOM_LEVEL));
        mapView.scrollTo(db.GetProperty(GridWalkingDBHelper.PROPERTY_X_POS), db.GetProperty(GridWalkingDBHelper.PROPERTY_Y_POS));
        mapView.setUseDataConnection(gameState.getUseDataConnection());

        IGeoPoint positionHint = gameState.popPositionHint();
        if (positionHint != null) {
            mapView.getController().setCenter(positionHint);
        }

        EnableLocationUpdates();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        GameState gameState = GameState.getInstance();
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.mark_visited);
        if (item != null) {
            GameState.ShowGridState showGridState = GameState.getInstance().getShowGridState();
            item.setVisible(GameState.ShowGridState.SELF==showGridState && gameState.getSelectedGridKey()!=null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mark_visited:
                discoverSelectedGridT();
                return true;
            case R.id.sync_highscore:
                ((MapActivity)getActivity()).syncHighscore();
                return true;
            case R.id.settings:
                Intent i = new Intent(getContext(), GridWalkingPreferenceActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    public void onLocationChanged(Location location) {
        GameState gameState = GameState.getInstance();
        gameState.onPositionChangedT(this, location.getLongitude(), location.getLatitude());

        if (gameState.getSnapToCentre()) {
            GeoPoint position = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapView.getController().setCenter(position);
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
        }
    }
}
