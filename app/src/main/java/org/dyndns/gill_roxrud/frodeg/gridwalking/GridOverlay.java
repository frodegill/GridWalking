package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.Context;
import android.graphics.*;
import android.graphics.Rect;
import android.view.MotionEvent;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.Iterator;
import java.util.SortedSet;


public class GridOverlay extends Overlay {

    static final private int DRAW_LEVEL_DEPTH = 5;

    private MapFragment mapFragment;


    public GridOverlay(Context ctx, MapFragment mapFragment) {
        super(ctx);
        this.mapFragment = mapFragment;
    }

    private void draw(Canvas canvas, MapView mapView, IGeoPoint ne, IGeoPoint sw) {
        Grid grid = GameState.getInstance().getGrid();
        Projection projection = mapView.getProjection();
        byte gridLevel = grid.OsmToGridLevel(mapView.getZoomLevel());

        drawGrid(canvas, projection, sw, ne, gridLevel);
        drawSquares(canvas, projection, sw, ne, gridLevel);
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
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
            GeoPoint neEastBorder = new GeoPoint(ne.getLatitudeE6(), Grid.EAST*1E6-1);
            GeoPoint swWestBorder = new GeoPoint(sw.getLatitudeE6(), Grid.WEST*1E6);
            draw(canvas, mapView, neEastBorder, sw);
            draw(canvas, mapView, ne, swWestBorder);
        } else {
            draw(canvas, mapView, ne, sw);
        }
    }

    private void drawGrid(Canvas canvas, Projection projection, IGeoPoint sw, IGeoPoint ne, byte gridLevel) {
        Grid grid = GameState.getInstance().getGrid();
        Paint gridColour = grid.gridColours[gridLevel];
        gridColour.setAlpha(0xFF);

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        float line_halfwidth = Math.min(canvasWidth, canvasHeight) / 320;

        int topGrid = grid.ToVerticalGridBounded(ne.getLatitude(), gridLevel);
        int leftGrid = grid.ToHorizontalGrid(sw.getLongitude(), gridLevel);
        int bottomGrid = grid.ToVerticalGridBounded(sw.getLatitude(), gridLevel);
        int rightGrid = grid.ToHorizontalGrid(ne.getLongitude(), gridLevel);

        int x, y;
        android.graphics.Point point = new android.graphics.Point();
        int stepping = 1<<gridLevel;
        for (y=bottomGrid; y<=(topGrid+stepping); y+=stepping) {
            point = GridToPixel(grid, leftGrid, y, projection, point);
            canvas.drawRect(new RectF(0, point.y-line_halfwidth, canvasWidth-1, point.y+line_halfwidth), gridColour);
        }
        for (x=leftGrid; x<=(rightGrid+stepping); x+=stepping) {
            point = GridToPixel(grid, x, topGrid, projection, point);
            canvas.drawRect(new RectF(point.x-line_halfwidth, 0, point.x+line_halfwidth, canvasHeight-1), gridColour);
        }
    }

    private void drawSquares(Canvas canvas, Projection projection, IGeoPoint sw, IGeoPoint ne, byte gridLevel) {
        Grid grid = GameState.getInstance().getGrid();
        byte fromLevel = (byte) Math.max(gridLevel-DRAW_LEVEL_DEPTH, Grid.LEVEL_0);

        byte currentLevel;
        int y;
        synchronized (Grid.gridsLock) {
            for (currentLevel = fromLevel; currentLevel < Grid.LEVEL_COUNT; currentLevel++) {
                if (Grid.grids[currentLevel].isEmpty()) {
                    continue;
                }

                int currentStepping = 1<<currentLevel;
                int currentTopGrid = grid.ToVerticalGridBounded(ne.getLatitude(), currentLevel);
                int currentLeftGrid = grid.ToHorizontalGrid(sw.getLongitude(), currentLevel);
                int currentBottomGrid = grid.ToVerticalGridBounded(sw.getLatitude(), currentLevel);
                int currentRightGrid = grid.ToHorizontalGrid(ne.getLongitude(), currentLevel);

                android.graphics.Point tmpPoint1 = new android.graphics.Point();
                android.graphics.Point tmpPoint2 = new android.graphics.Point();

                SortedSet<Long> currentSet;
                Iterator<Long> currentKeyIterator;
                for (y=currentBottomGrid; y<=(currentTopGrid+currentStepping); y+=currentStepping) {
                    try {
                        Long gridLeftKey = grid.ToKey(currentLeftGrid, y);
                        Long gridRightKey = grid.ToKey(currentRightGrid, y);
                        currentSet = Grid.grids[currentLevel].subSet(gridLeftKey, gridRightKey+currentStepping);
                    } catch (InvalidPositionException e) {
                        continue;
                    }

                    currentKeyIterator = currentSet.iterator();
                    while (currentKeyIterator.hasNext()) {
                        drawSquare(canvas, projection, grid, currentKeyIterator.next(), currentLevel,
                                   grid.gridColours[currentLevel],
                                   tmpPoint1, tmpPoint2);
                    }
                }
            }
        }
    }

    private void drawSquare(Canvas canvas, Projection projection,
                            Grid grid, long gridKey, int gridLevel,
                            Paint squareColour,
                            android.graphics.Point reusePoint1, android.graphics.Point reusePoint2) {
        int gridStepping = 1<<gridLevel;

        int gridX;
        int gridY;
        try {
            gridX = grid.XFromKey(gridKey);
            gridY = grid.YFromKey(gridKey);
        }
        catch (InvalidPositionException e) {
            return;
        }

        android.graphics.Point currentLowerLeft = GridToPixel(grid, gridX, gridY, projection, reusePoint1);
        android.graphics.Point currentUpperRight = GridToPixel(grid, gridX+gridStepping, gridY+gridStepping, projection, reusePoint2);

        canvas.drawRect(new Rect(currentLowerLeft.x, currentUpperRight.y, currentUpperRight.x, currentLowerLeft.y), squareColour);
    }


    private android.graphics.Point GridToPixel(Grid grid, int x, int y, Projection projection, android.graphics.Point reusePoint) {
        GeoPoint geoPoint = new GeoPoint(grid.FromVerticalGrid(y), grid.FromHorizontalGrid(x));
        reusePoint = projection.toProjectedPixels(geoPoint, reusePoint);
        return projection.toPixelsFromProjected(reusePoint, reusePoint);
    }

    @Override
    public boolean onLongPress(final MotionEvent e, final MapView mapView)
    {
        IGeoPoint longpressPoint = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
        mapFragment.onLongPress(longpressPoint);
        return true;
    }

}
