package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.Context;
import android.graphics.*;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.HashSet;


public class BonusOverlay extends Overlay {

    static final private int MAX_DRAW_LEVEL = 4;

    static Paint white = new Paint();
    static Paint black = new Paint();

    public BonusOverlay(Context ctx) {
        super(ctx);

        white.setColor(Color.argb(0x80, 0xFF, 0xFF, 0xFF));
        white.setStyle(Paint.Style.STROKE);

        black.setColor(Color.argb(0x80, 0x00, 0x00, 0x00));
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        int gridLevel = Grid.OsmToGridLevel(mapView.getZoomLevel());
        if (MAX_DRAW_LEVEL < gridLevel) {
            return;
        }

        Projection projection = mapView.getProjection();
        float radius = projection.metersToPixels(Bonus.BONUS_SIZE_RADIUS);

        IGeoPoint ne = projection.getNorthEast();
        IGeoPoint sw = projection.getSouthWest();
        if (Grid.GRID_MAX_NORTH<sw.getLatitude() || Grid.GRID_MAX_SOUTH>ne.getLatitude()) {
            return;
        }

        int topGrid = Bonus.ToVerticalBonusGridBounded(ne.getLatitude());
        int leftGrid = Bonus.ToHorizontalBonusGrid(sw.getLongitude());
        int bottomGrid = Bonus.ToVerticalBonusGridBounded(sw.getLatitude());
        int rightGrid = Bonus.ToHorizontalBonusGrid(ne.getLongitude());

        android.graphics.Point point = null;
        GeoPoint geoPoint;

        white.setStrokeWidth(radius/4);

        HashSet<Integer> drawnBonuses = new HashSet();
        int x, y, key;
        Bonus bonus = GameState.getInstance().getBonus();
        for (y=bottomGrid; y<=(topGrid+1); y++) {
            for (x=leftGrid; x<=(rightGrid+1); x++) {
                try {
                    key = Bonus.ToBonusKey(x, y);
                } catch (InvalidPositionException e) {
                    continue;
                }

                if (bonus.Contains(key) || drawnBonuses.contains(key)) {
                    continue;
                }

                geoPoint = new GeoPoint(Bonus.FromVerticalBonusGrid(y), Bonus.FromHorizontalBonusGrid(x));
                point = projection.toProjectedPixels(geoPoint, point);
                point = projection.toPixelsFromProjected(point, point);

                canvas.drawCircle(point.x, point.y, 2*radius, white);
                canvas.drawCircle(point.x, point.y, radius, black);
                drawnBonuses.add(key);
            }
        }
    }

}
