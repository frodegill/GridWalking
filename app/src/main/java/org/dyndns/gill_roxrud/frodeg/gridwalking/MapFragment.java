package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
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

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;


public class MapFragment extends Fragment implements LocationListener {
    static final long  LOCATION_UPDATE_INTERVAL = 30L;
    static final float LOCATION_UPDATE_DISTANCE = 25.0f;

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
        mapView = (MapView) view.findViewById(R.id.mapview);
        return view;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();

        //OpenStreetMapTileProviderConstants.DEBUG_TILE_PROVIDERS = true;
        OpenStreetMapTileProviderConstants.DEBUGMODE = false;

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);

        mapView.getOverlays().add(new GridOverlay(context, this));
        mapView.getOverlays().add(new BonusOverlay(context));
        mapView.getOverlays().add(new MyLocationOverlay(context));

        gameState.setUseDataConnection(1 == db.getProperty(GridWalkingDBHelper.PROPERTY_USE_DATA_CONNECTION));
        mapView.setUseDataConnection(gameState.getUseDataConnection());

        gameState.setSnapToCentre(1 == db.getProperty(GridWalkingDBHelper.PROPERTY_SNAP_TO_CENTRE));

        mapView.getController().setZoom(db.getProperty(GridWalkingDBHelper.PROPERTY_ZOOM_LEVEL));
        if (!gameState.getSnapToCentre()) {
            mapView.scrollTo(db.getProperty(GridWalkingDBHelper.PROPERTY_X_POS), db.getProperty(GridWalkingDBHelper.PROPERTY_Y_POS));
        }

        setHasOptionsMenu(true);

        onScoreUpdated();
    }

    @Override
    public void onPause() {
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();
        db.setProperty(GridWalkingDBHelper.PROPERTY_X_POS, mapView.getScrollX());
        db.setProperty(GridWalkingDBHelper.PROPERTY_Y_POS, mapView.getScrollY());
        db.setProperty(GridWalkingDBHelper.PROPERTY_ZOOM_LEVEL, mapView.getZoomLevel());
        db.setProperty(GridWalkingDBHelper.PROPERTY_USE_DATA_CONNECTION, gameState.getUseDataConnection()?1:0);
        db.setProperty(GridWalkingDBHelper.PROPERTY_SNAP_TO_CENTRE, gameState.getSnapToCentre()?1:0);

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
        MenuItem item = menu.findItem(R.id.offline);
        if (item != null) {
            item.setChecked(!gameState.getUseDataConnection());
        }

        item = menu.findItem(R.id.snap_to_centre);
        if (item != null) {
            item.setChecked(gameState.getSnapToCentre());
        }

        item = menu.findItem(R.id.mark_visited);
        if (item != null) {
            item.setVisible(gameState.getSelectedGridKey() !=null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline:
                toggleUseDataConnection(item);
                return true;
            case R.id.snap_to_centre:
                toggleSnapToCentre(item);
                return true;
            case R.id.mark_visited:
                discoverSelectedGrid();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleUseDataConnection(MenuItem item) {
        GameState gameState = GameState.getInstance();
        gameState.setUseDataConnection(!gameState.getUseDataConnection());
        if (item != null) {
            item.setChecked(gameState.getUseDataConnection());
        }
        if (this.mapView != null) {
            this.mapView.setUseDataConnection(gameState.getUseDataConnection());
        }
    }

    private void toggleSnapToCentre(MenuItem item) {
        GameState gameState = GameState.getInstance();
        gameState.setSnapToCentre(!gameState.getSnapToCentre());
        if (item != null) {
            item.setChecked(gameState.getSnapToCentre());
        }
    }

    private void discoverSelectedGrid() {
        if (GameState.getInstance().getGrid().DiscoverSelectedGrid()) {
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
            view = (TextView) getView().findViewById(R.id.score);
            view.setText(GameState.getInstance().getGrid().getScoreString());
        }
        catch(NullPointerException e) {}

        try {
            view = (TextView) getView().findViewById(R.id.bonus);
            view.setText(GameState.getInstance().getBonus().getBonusString());
        } catch(NullPointerException e) {}
    }

    @Override
    public void onLocationChanged(Location location) {
        GameState gameState = GameState.getInstance();
        gameState.onPositionChanged(this, location.getLongitude(), location.getLatitude());

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
                Toast.makeText(GridWalkingApplication.getContext(), "Failed to find a Location Provider (GPS)", Toast.LENGTH_LONG).show();
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE, this);
        }
    }
}
