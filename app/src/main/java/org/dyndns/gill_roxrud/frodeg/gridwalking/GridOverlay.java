package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.Rect;
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

    private MapFragment mapFragment;


    public GridOverlay(Context ctx, MapFragment mapFragment) {
        super(ctx);
        this.mapFragment = mapFragment;
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

        Paint gridColour = Grid.gridColours[gridLevel];

        android.graphics.Point upperLeftPixel = GridToPixel(leftGrid, topGrid, projection, new android.graphics.Point());
        android.graphics.Point lowerRightPixel = GridToPixel(rightGrid, bottomGrid, projection, new android.graphics.Point());

        int x, y;
        android.graphics.Point point = new android.graphics.Point();
        for (y=bottomGrid; y<=(topGrid+stepping); y+=stepping) {
            point = GridToPixel(leftGrid, y, projection, point);
            canvas.drawRect(new Rect(upperLeftPixel.x, point.y-2, lowerRightPixel.x, point.y+2), gridColour);
        }
        for (x=leftGrid; x<=(rightGrid+stepping); x+=stepping) {
            point = GridToPixel(x, topGrid, projection, point);
            canvas.drawRect(new Rect(point.x-2, upperLeftPixel.y, point.x+2, lowerRightPixel.y), gridColour);
        }
    }

    private android.graphics.Point GridToPixel(int x, int y, Projection projection, android.graphics.Point reusePoint) {
        GeoPoint geoPoint = new GeoPoint(Grid.FromVerticalGrid(y), Grid.FromHorizontalGrid(x));
        reusePoint = projection.toProjectedPixels(geoPoint, reusePoint);
        return projection.toPixelsFromProjected(reusePoint, reusePoint);
    }

    @Override
    public boolean onLongPress(final MotionEvent e, final MapView mapView)
    {
        mapFragment.onLongPress();
        return true;
    }

}
