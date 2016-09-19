package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;


public class MapActivity extends AppCompatActivity {
    private static final String MAP_FRAGMENT_TAG = "org.dyndns.gill_roxrud.frodeg.gridwalking.MAP_FRAGMENT_TAG";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_map);
        FragmentManager fm = this.getSupportFragmentManager();
        if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            MapFragment mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG).commit();
        }
    }
}
/*
public class MapActivity extends Activity {

    private MapView mapView;
    private TilesOverlay tilesOverlay;
    private MapTileProviderBasic mapProvider;

    private boolean useDataConnection = true;

    private View tempPopupMenuParentView = null;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //OpenStreetMapTileProviderConstants.DEBUG_TILE_PROVIDERS = true;
        OpenStreetMapTileProviderConstants.DEBUGMODE = true;

        // Setup base map
        final RelativeLayout rl = new RelativeLayout(this);

        // Add tiles layer
        this.mapView = new MapView(this);
        this.mapView.setTilesScaledToDpi(true);
        rl.addView(this.mapView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        this.mapView.setBuiltInZoomControls(true);

        mapProvider = new MapTileProviderBasic(getApplicationContext());
        mapProvider.setTileSource(TileSourceFactory.MAPNIK);
        this.tilesOverlay = new TilesOverlay(mapProvider, this.getBaseContext());
        this.mapView.getOverlays().add(this.tilesOverlay);
        this.mapView.getOverlays().add(new GridOverlay(this.getBaseContext(), this));
        this.mapView.getOverlays().add(new BonusOverlay(this.getBaseContext()));
        this.mapView.setBuiltInZoomControls(true);

        this.setContentView(rl);

        this.mapView.getController().setCenter(new GeoPoint(59675530, 10663472));
        this.mapView.getController().setZoom(15);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_map, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline:
                useDataConnection = !useDataConnection;
                item.setChecked(!useDataConnection);
                if (this.mapView != null) {
                    this.mapView.setUseDataConnection(useDataConnection);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void toggleUseDataConnection() {
        useDataConnection = !useDataConnection;
        //item.setChecked(!useDataConnection);
        if (this.mapView != null) {
            this.mapView.setUseDataConnection(useDataConnection);
        }
    }

    public void onLongPress()
    {
        if (false) {
            PopupMenu popup = new PopupMenu(this.getBaseContext(), mapView);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.fragment_map, popup.getMenu());
            popup.show();
        } else {
            showContextMenu(new GeoPoint(59.675330, 10.663672));
        }
    }

    protected boolean showContextMenu(final GeoPoint geoPosition) {
        MenuInflater inflater = getMenuInflater();
        if (tempPopupMenuParentView != null) {
            mapView.removeView(tempPopupMenuParentView);
            tempPopupMenuParentView = null;
        }

        createTempPopupParentMenuView(geoPosition);
        PopupMenu menu = new PopupMenu(this, tempPopupMenuParentView);

        inflater.inflate(R.menu.fragment_map, menu.getMenu());

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
        tempPopupMenuParentView = new View(this);
        MapView.LayoutParams lp = new MapView.LayoutParams(1, 1, position, MapView.LayoutParams.CENTER, 0, 0);
        tempPopupMenuParentView.setVisibility(View.VISIBLE);
        mapView.addView(tempPopupMenuParentView, lp);
        return tempPopupMenuParentView;
    }
}
*/