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
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;


public class MapFragment extends Fragment implements LocationListener {
    static final long  LOCATION_UPDATE_INTERVAL = 30l;
    static final float LOCATION_UPDATE_DISTANCE = 25.0f;

    static final String PREFS_NAME = "org.dyndns.gill_roxrud.frodeg.gridwalking.prefs";
    static final String PREFS_SCROLL_X = "scrollX";
    static final String PREFS_SCROLL_Y = "scrollY";
    static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    static final String PREFS_USE_DATA_CONNECTION = "useDataConnection";

    private boolean useDataConnection = true;

    private SharedPreferences preferences;
    private MapView mapView;
    private View tempPopupMenuParentView = null;

    private Location location = null;


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
    }

    @Override
    public void onPause() {
        final SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(PREFS_SCROLL_X, mapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mapView.getZoomLevel());
        edit.putBoolean(PREFS_USE_DATA_CONNECTION, mapView.useDataConnection());
        edit.commit();

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
    public void onPrepareOptionsMenu(final Menu pMenu) {
        super.onPrepareOptionsMenu(pMenu);
        MenuItem offlineItem = pMenu.findItem(R.id.offline);
        if (offlineItem != null) {
            offlineItem.setChecked(!useDataConnection);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline:
                toggleUseDataConnection(item);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggleUseDataConnection() {
        toggleUseDataConnection(null);
    }

    private void toggleUseDataConnection(MenuItem item) {
        useDataConnection = !useDataConnection;
        if (item != null) {
            item.setChecked(useDataConnection);
        }
        if (this.mapView != null) {
            this.mapView.setUseDataConnection(useDataConnection);
        }
    }

    protected boolean showContextMenu(final GeoPoint geoPosition) {
        MenuInflater inflater = getActivity().getMenuInflater();
        if (tempPopupMenuParentView != null) {
            mapView.removeView(tempPopupMenuParentView);
            tempPopupMenuParentView = null;
        }

        createTempPopupParentMenuView(geoPosition);
        PopupMenu menu = new PopupMenu(getActivity(), tempPopupMenuParentView);

        inflater.inflate(R.menu.activity_map, menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (tempPopupMenuParentView != null) {
                    mapView.removeView(tempPopupMenuParentView);
                    tempPopupMenuParentView = null;
                }

                switch (item.getItemId()) {
                    case R.id.offline:
                        toggleUseDataConnection();
                        return true;
                    default:
                        return false;
                }
            }
        });
        menu.show();
        return true;
    }

    // inspired by org.osmdroid.bonuspack.overlays.InfoWindow
    private View createTempPopupParentMenuView(final GeoPoint position) {
        if (tempPopupMenuParentView != null) {
            mapView.removeView(tempPopupMenuParentView);
        }
        tempPopupMenuParentView = new View(getActivity());
        MapView.LayoutParams lp = new MapView.LayoutParams(1, 1, position, MapView.LayoutParams.CENTER, 0, 0);
        tempPopupMenuParentView.setVisibility(View.VISIBLE);
        mapView.addView(tempPopupMenuParentView, lp);
        return tempPopupMenuParentView;
    }

    public void onLongPress()
    {
        if (false) {
            PopupMenu popup = new PopupMenu(getActivity(), mapView);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.activity_map, popup.getMenu());
            popup.show();
        } else {
            showContextMenu(new GeoPoint(59.675330, 10.663672));
        }
    }

    public void onScoreUpdated() {
        TextView view = (TextView) getView().findViewById(R.id.score);
        view.setText(GameState.getInstance().getGrid().getScoreString());

        view = (TextView) getView().findViewById(R.id.bonus);
        view.setText(GameState.getInstance().getBonus().getBonusString());
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        GameState.getInstance().onPositionChanged(this, location.getLatitude(), location.getLongitude());

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
