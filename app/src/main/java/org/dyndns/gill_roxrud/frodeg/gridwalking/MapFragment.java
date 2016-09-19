package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;


public class MapFragment extends Fragment {
    static final String PREFS_NAME = "org.dyndns.gill_roxrud.frodeg.gridwalking.prefs";
    static final String PREFS_SCROLL_X = "scrollX";
    static final String PREFS_SCROLL_Y = "scrollY";
    static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    static final String PREFS_USE_DATA_CONNECTION = "useDataConnection";

    private boolean useDataConnection = true;

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
        mapView = new MapView(inflater.getContext());
        return mapView;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff() {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(false);

        mapView.getOverlays().add(new GridOverlay(context));
        mapView.getOverlays().add(new BonusOverlay(context));

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.setTileSource(TileSourceFactory.MAPNIK);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu pMenu) {
        super.onPrepareOptionsMenu(pMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.menu.activity_map:
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
}
