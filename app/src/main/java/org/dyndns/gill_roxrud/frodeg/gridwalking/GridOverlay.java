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
        if (shadow) {
            return;
        }

        float line_halfwidth = Math.min(canvas.getWidth(), canvas.getHeight()) / 320;

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

        Paint gridColour = GameState.getInstance().getGrid().gridColours[gridLevel];
        gridColour.setAlpha(255);

        android.graphics.Point upperLeftPixel = GridToPixel(leftGrid, topGrid+11, projection, new android.graphics.Point());
        android.graphics.Point lowerRightPixel = GridToPixel(rightGrid+1, bottomGrid, projection, new android.graphics.Point());

        int x, y;
        android.graphics.Point point = new android.graphics.Point();
        for (y=bottomGrid; y<=(topGrid+stepping); y+=stepping) {
            point = GridToPixel(leftGrid, y, projection, point);
            canvas.drawRect(new RectF(upperLeftPixel.x, point.y-line_halfwidth,
                                      lowerRightPixel.x, point.y+line_halfwidth), gridColour);
        }
        for (x=leftGrid; x<=(rightGrid+stepping); x+=stepping) {
            point = GridToPixel(x, topGrid, projection, point);
            canvas.drawRect(new RectF(point.x-line_halfwidth, upperLeftPixel.y,
                                      point.x+line_halfwidth, lowerRightPixel.y), gridColour);
        }

        canvas.scale(4,4);
        canvas.drawText("Level:"+gridLevel, 100, 100, new Paint(Color.rgb(0,0,0)));
        canvas.scale(1,1);
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
