package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
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

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;


public class MapFragment extends Fragment implements LocationListener {
    static final long  LOCATION_UPDATE_INTERVAL = 30L;
    static final float LOCATION_UPDATE_DISTANCE = 25.0f;

    static final String PREFS_NAME = "org.dyndns.gill_roxrud.frodeg.gridwalking.prefs";
    static final String PREFS_SCROLL_X = "scrollX";
    static final String PREFS_SCROLL_Y = "scrollY";
    static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    static final String PREFS_USE_DATA_CONNECTION = "useDataConnection";

    private SharedPreferences preferences;
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

        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        //OpenStreetMapTileProviderConstants.DEBUG_TILE_PROVIDERS = true;
        OpenStreetMapTileProviderConstants.DEBUGMODE = true;

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);

        mapView.getOverlays().add(new GridOverlay(context, this));
        mapView.getOverlays().add(new BonusOverlay(context));
        mapView.getOverlays().add(new MyLocationOverlay(context));

        mapView.getController().setZoom(preferences.getInt(PREFS_ZOOM_LEVEL, 8));
        mapView.scrollTo(preferences.getInt(PREFS_SCROLL_X, 0), preferences.getInt(PREFS_SCROLL_Y, 0));

        mapView.setUseDataConnection(preferences.getBoolean(PREFS_USE_DATA_CONNECTION, true));

        setHasOptionsMenu(true);

        onScoreUpdated();
    }

    @Override
    public void onPause() {
        final SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(PREFS_SCROLL_X, mapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mapView.getZoomLevel());
        edit.putBoolean(PREFS_USE_DATA_CONNECTION, mapView.useDataConnection());
        edit.apply();

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

        item = menu.findItem(R.id.mark_visitted);
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
            case R.id.mark_visitted:
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

    private void discoverSelectedGrid() {
        if (GameState.getInstance().getGrid().DiscoverSelectedGrid()) {
            mapView.postInvalidate();
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
                //this.getActivity().openOptionsMenu();
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
        GameState.getInstance().onPositionChanged(this, location.getLongitude(), location.getLatitude());

        GeoPoint position = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().setCenter(position);
        mapView.postInvalidate(); //Is this needed (after calling setCenter)?
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE, this);
        }
    }
}
