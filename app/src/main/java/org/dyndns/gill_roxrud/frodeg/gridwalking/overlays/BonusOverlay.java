package org.dyndns.gill_roxrud.frodeg.gridwalking.overlays;


import android.graphics.*;

import org.dyndns.gill_roxrud.frodeg.gridwalking.Bonus;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Grid;
import org.dyndns.gill_roxrud.frodeg.gridwalking.InvalidPositionException;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.HashSet;


public class BonusOverlay extends Overlay {

    static final private int MAX_DRAW_LEVEL = 4;

    private static final Paint white = new Paint();
    private static final Paint black = new Paint();

    public BonusOverlay() {
        super();
        white.setColor(Color.argb(0x80, 0xFF, 0xFF, 0xFF));
        white.setStyle(Paint.Style.STROKE);

        black.setColor(Color.argb(0x80, 0x00, 0x00, 0x00));
    }

    private void draw(Canvas canvas, MapView mapView, IGeoPoint ne, IGeoPoint sw) {
        Grid grid = GameState.getInstance().getGrid();
        Bonus bonus = GameState.getInstance().getBonus();

        int gridLevel = grid.OsmToGridLevel(mapView.getZoomLevel());
        if (MAX_DRAW_LEVEL < gridLevel) {
            return;
        }

        int topGrid = bonus.ToVerticalBonusGridBounded(ne.getLatitude());
        int leftGrid = bonus.ToHorizontalBonusGrid(sw.getLongitude());
        int bottomGrid = bonus.ToVerticalBonusGridBounded(sw.getLatitude());
        int rightGrid = bonus.ToHorizontalBonusGrid(ne.getLongitude());

        android.graphics.Point point = null;
        GeoPoint geoPoint;

        Projection projection = mapView.getProjection();
        float radius = projection.metersToPixels(Bonus.BONUS_SIZE_RADIUS);

        white.setStrokeWidth(radius/4);

        HashSet<Integer> drawnBonuses = new HashSet<>();
        int x, y, key;
        for (y=bottomGrid; y<=(topGrid+1); y++) {
            for (x=leftGrid; x<=(rightGrid+1); x++) {
                try {
                    key = bonus.ToBonusKey(x, y);
                } catch (InvalidPositionException e) {
                    continue;
                }

                if (bonus.Contains(key) || drawnBonuses.contains(key)) {
                    continue;
                }

                geoPoint = new GeoPoint(bonus.FromVerticalBonusGrid(y), bonus.FromHorizontalBonusGrid(x));
                point = projection.toProjectedPixels(geoPoint, point);
                point = projection.toPixelsFromProjected(point, point);

                canvas.drawCircle(point.x, point.y, 2*radius, white);
                canvas.drawCircle(point.x, point.y, radius, black);
                drawnBonuses.add(key);
            }
        }
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        Projection projection = mapView.getProjection();
        IGeoPoint ne = projection.getNorthEast();
        IGeoPoint sw = projection.getSouthWest();
        if (Grid.GRID_MAX_NORTH<sw.getLatitude() || Grid.GRID_MAX_SOUTH>ne.getLatitude()) {
            return;
        }

        if (ne.getLongitude() < sw.getLongitude()) { //Across date-line?
            GeoPoint neEastBorder = new GeoPoint(ne.getLatitude(), Grid.EAST);
            GeoPoint swWestBorder = new GeoPoint(sw.getLatitude(), Grid.WEST);
            draw(canvas, mapView, neEastBorder, sw);
            draw(canvas, mapView, ne, swWestBorder);
        } else {
            draw(canvas, mapView, ne, sw);
        }
    }

}
