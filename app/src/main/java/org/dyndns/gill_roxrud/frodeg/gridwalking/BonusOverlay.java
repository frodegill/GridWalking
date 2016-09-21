package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.Context;
import android.graphics.*;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;


public class BonusOverlay extends Overlay {

    public BonusOverlay(Context ctx) {
        super(ctx);
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        int gridLevel = Grid.OsmToGridLevel(mapView.getZoomLevel());
        if (4<=gridLevel) {
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

        Paint white = new Paint();
        white.setColor(Color.argb(128, 255, 255, 255));
        white.setStyle(Paint.Style.STROKE);
        white.setStrokeWidth(radius/4);

        Paint black = new Paint();
        black.setColor(Color.argb(128, 0, 0, 0));

        int x, y;
        Bonus  bonus = GameState.getInstance().getBonus();
        for (y=bottomGrid; y<=topGrid; y++) {
            for (x=leftGrid; x<=rightGrid; x++) {
                if (bonus.Contains(x, y)) {
                    continue;
                }

                geoPoint = new GeoPoint(Bonus.FromVerticalBonusGrid(y), Bonus.FromHorizontalBonusGrid(x));
                point = projection.toProjectedPixels(geoPoint, point);
                point = projection.toPixelsFromProjected(point, point);

                canvas.drawCircle(point.x, point.y, 2*radius, white);
                canvas.drawCircle(point.x, point.y, radius, black);
            }
        }
    }

}
