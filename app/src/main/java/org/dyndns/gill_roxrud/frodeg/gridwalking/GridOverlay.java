package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.support.v7.widget.PopupMenu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;


public class GridOverlay extends Overlay {

    private MapActivity mapActivity;

    public GridOverlay(Context ctx, MapActivity activity) {
        super(ctx);
        this.mapActivity = activity;
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (Grid.gridColours == null) {
            return;
        }

        if (shadow) {
            return;
        }

        Projection projection = mapView.getProjection();
        IGeoPoint ne = projection.getNorthEast();
        IGeoPoint sw = projection.getSouthWest();
        if (Grid.GRID_MAX_NORTH<sw.getLatitude() || Grid.GRID_MAX_SOUTH>ne.getLatitude()) {
            return;
        }

        int gridLevel = Grid.OsmToGridLevel(mapView.getZoomLevel());
        int stepping = 1<<gridLevel;
        int mask = ~(stepping-1);
        int topGrid = Grid.ToVerticalGridBounded(ne.getLatitude()) & mask;
        int leftGrid = Grid.ToHorizontalGrid(sw.getLongitude()) & mask;
        int bottomGrid = Grid.ToVerticalGridBounded(sw.getLatitude()) & mask;
        int rightGrid = Grid.ToHorizontalGrid(ne.getLongitude()) & mask;

        android.graphics.Point point = new android.graphics.Point();

        Paint gridColour = Grid.gridColours[gridLevel];

        android.graphics.Point upperLeftPixel = GridToPixel(leftGrid, topGrid, projection, point);
        android.graphics.Point lowerRightPixel = GridToPixel(rightGrid, bottomGrid, projection, point);

        int x, y;
        for (y=bottomGrid; y<=topGrid; y+=stepping) {
            point = GridToPixel(leftGrid, y, projection, point);
            canvas.drawLine(upperLeftPixel.x, point.y, lowerRightPixel.x, point.y, gridColour);
        }
        for (x=leftGrid; x<=rightGrid; x+=stepping) {
            point = GridToPixel(x, topGrid, projection, point);
            canvas.drawLine(point.x, upperLeftPixel.y, point.x, lowerRightPixel.y, gridColour);
        }
    }

    private android.graphics.Point GridToPixel(int x, int y, Projection projection, android.graphics.Point reusePoint) {
        GeoPoint geoPoint = new GeoPoint(Bonus.FromVerticalBonusGrid(y), Bonus.FromHorizontalBonusGrid(x));
        reusePoint = projection.toProjectedPixels(geoPoint, reusePoint);
        return projection.toPixelsFromProjected(reusePoint, reusePoint);
    }

    @Override
    public boolean onLongPress(final MotionEvent e, final MapView mapView)
    {
        mapActivity.onLongPress();
        return true;
    }

}
