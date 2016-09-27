package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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


public class MapFragment extends Fragment {
    static final String PREFS_NAME = "org.dyndns.gill_roxrud.frodeg.gridwalking.prefs";
    static final String PREFS_SCROLL_X = "scrollX";
    static final String PREFS_SCROLL_Y = "scrollY";
    static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    static final String PREFS_USE_DATA_CONNECTION = "useDataConnection";

    private boolean useDataConnection = true;

    private SharedPreferences preferences;
    private MapView mapView;
    private View tempPopupMenuParentView = null;


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
        // Turn off hardware acceleration here, or in manifest
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

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
//        this.mapView.setTilesScaledToDpi(true);

        mapView.getOverlays().add(new GridOverlay(context, this));
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

    public void onUpdateScoreUpdated() {
        TextView scoreView = (TextView) getView().findViewById(R.id.score);
        scoreView.setText("Test");
    }
}
