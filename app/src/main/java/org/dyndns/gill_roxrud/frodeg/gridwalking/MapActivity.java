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


}
*/