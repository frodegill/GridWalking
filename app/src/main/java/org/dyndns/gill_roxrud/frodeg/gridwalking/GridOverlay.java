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

import java.util.Iterator;
import java.util.SortedSet;


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

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        float line_halfwidth = Math.min(canvasWidth, canvasHeight) / 320;

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

        int x, y;
        //Draw grid
        android.graphics.Point point = new android.graphics.Point();
        for (y=bottomGrid; y<=(topGrid+stepping); y+=stepping) {
            point = GridToPixel(leftGrid, y, projection, point);
            canvas.drawRect(new RectF(0, point.y-line_halfwidth, canvasWidth-1, point.y+line_halfwidth), gridColour);
        }
        for (x=leftGrid; x<=(rightGrid+stepping); x+=stepping) {
            point = GridToPixel(x, topGrid, projection, point);
            canvas.drawRect(new RectF(point.x-line_halfwidth, 0, point.x+line_halfwidth, canvasHeight-1), gridColour);
        }

        //Draw visited squares
        int currentLevel;
        int fromLevel = gridLevel - 3;
        if (0 > fromLevel) {
            fromLevel = 0;
        }
        synchronized (Grid.gridsLock) {
            for (currentLevel = fromLevel; currentLevel < Grid.LEVEL_COUNT; currentLevel++) {
                if (Grid.grids[currentLevel].isEmpty()) {
                    continue;
                }

                int currentStepping = 1<<currentLevel;
                int currentMask = ~(currentStepping-1);
                int currentTopGrid = Grid.ToVerticalGridBounded(ne.getLatitude()) & currentMask;
                int currentLeftGrid = Grid.ToHorizontalGrid(sw.getLongitude()) & currentMask;
                int currentBottomGrid = Grid.ToVerticalGridBounded(sw.getLatitude()) & currentMask;
                int currentRightGrid = Grid.ToHorizontalGrid(ne.getLongitude()) & currentMask;

                int currentX;
                int currentY;
                android.graphics.Point currentUpperLeft = new android.graphics.Point();
                android.graphics.Point currentLowerRight = new android.graphics.Point();

                SortedSet<Long> currentSet;
                Iterator<Long> currentKeyIterator;
                long currentKey;
                for (y=currentBottomGrid; y<=(currentTopGrid+currentStepping); y+=currentStepping) {
                    try {
                        Long gridLeftKey = Grid.ToKey(currentLeftGrid, y);
                        Long gridRightKey = Grid.ToKey(currentRightGrid, y);
                        currentSet = Grid.grids[currentLevel].subSet(gridLeftKey, gridRightKey);
                    } catch (InvalidPositionException e) {
                        continue;
                    }

                    currentKeyIterator = currentSet.iterator();
                    while (currentKeyIterator.hasNext()) {
                        currentKey = currentKeyIterator.next();
                        try {
                            currentX = Grid.XFromKey(currentKey);
                            currentY = Grid.YFromKey(currentKey);
                        }
                        catch (InvalidPositionException e) {
                            continue;
                        }

                        currentUpperLeft = GridToPixel(currentX, currentY, projection, currentUpperLeft);
                        if (currentUpperLeft.x<0) {
                            currentUpperLeft.x=0;
                        } else if (currentUpperLeft.x>=canvasWidth) {
                            currentUpperLeft.x=canvasWidth;
                        }
                        if (currentUpperLeft.y<0) {
                            currentUpperLeft.y=0;
                        } else if (currentUpperLeft.y>canvasHeight) {
                            currentUpperLeft.y=canvasHeight;
                        }

                        currentLowerRight = GridToPixel(currentX+currentStepping, currentY-currentStepping, projection, currentLowerRight);
                        if (currentLowerRight.x<0) {
                            currentLowerRight.x=0;
                        } else if (currentLowerRight.x>=canvasWidth) {
                            currentLowerRight.x=canvasWidth;
                        }
                        if (currentLowerRight.y<0) {
                            currentLowerRight.y=0;
                        } else if (currentLowerRight.y>canvasHeight) {
                            currentLowerRight.y=canvasHeight;
                        }

                        canvas.drawRect(new Rect(currentUpperLeft.x, currentUpperLeft.y, currentLowerRight.x, currentLowerRight.y),
                                Grid.gridColours[currentLevel]);
                    }
                }
            }
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
