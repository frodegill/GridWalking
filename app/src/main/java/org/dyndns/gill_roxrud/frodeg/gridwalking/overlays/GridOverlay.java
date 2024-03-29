package org.dyndns.gill_roxrud.frodeg.gridwalking.overlays;

import android.graphics.*;
import android.view.MotionEvent;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Grid;
import org.dyndns.gill_roxrud.frodeg.gridwalking.GridWalkingDBHelper;
import org.dyndns.gill_roxrud.frodeg.gridwalking.InvalidPositionException;
import org.dyndns.gill_roxrud.frodeg.gridwalking.activities.MapFragment;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.Iterator;
import java.util.Set;


public class GridOverlay extends Overlay {

    static final private int DRAW_LEVEL_DEPTH = 5;

    private final MapFragment mapFragment;


    public GridOverlay(final MapFragment mapFragment) {
        super();
        this.mapFragment = mapFragment;
    }

    private void draw(final Canvas canvas, final MapView mapView, final IGeoPoint ne, final IGeoPoint sw) {
        Grid grid = GameState.getInstance().getGrid();
        Projection projection = mapView.getProjection();
        byte gridLevel = grid.OsmToGridLevel((int)mapView.getZoomLevelDouble());

        drawGrid(canvas, projection, sw, ne, gridLevel);
        drawSquares(canvas, projection, sw, ne, gridLevel);
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

    private void drawGrid(final Canvas canvas, final Projection projection, final IGeoPoint sw, final IGeoPoint ne, final byte gridLevel) {
        Grid grid = GameState.getInstance().getGrid();

        Paint gridColour = new Paint(grid.gridColours[gridLevel]);
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

    private void drawSquares(final Canvas canvas, final Projection projection, final IGeoPoint sw, final IGeoPoint ne, final byte gridLevel) {
        GameState gameState = GameState.getInstance();
        if (!gameState.getShowGrids()) {
            return;
        }

        Grid grid = gameState.getGrid();
        byte fromLevel = (byte) Math.max(gridLevel-DRAW_LEVEL_DEPTH, Grid.LEVEL_0);
        Integer selectedGridX = null;
        Integer selectedGridY = null;
        Integer selectedGridKey = gameState.getSelectedGridKey();
        if (null != selectedGridKey) {
            try {
                selectedGridX = grid.XFromKey(selectedGridKey);
                selectedGridY = grid.YFromKey(selectedGridKey);
            } catch (InvalidPositionException e) {
                selectedGridX = selectedGridY = null;
                selectedGridKey = null;
            }
        }

        byte currentLevel;
        int y;
        GridWalkingDBHelper db = GameState.getInstance().getDB();
        for (currentLevel = fromLevel; currentLevel < Grid.LEVEL_COUNT; currentLevel++) {
            if (db.GetLevelCount(currentLevel)==0 &&
                !(0==currentLevel && null!=selectedGridKey)) {
                continue;
            }

            int currentStepping = 1<<currentLevel;
            int currentTopGrid = grid.ToVerticalGridBounded(ne.getLatitude(), currentLevel);
            int currentLeftGrid = grid.ToHorizontalGrid(sw.getLongitude(), currentLevel);
            int currentBottomGrid = grid.ToVerticalGridBounded(sw.getLatitude(), currentLevel);
            int currentRightGrid = grid.ToHorizontalGrid(ne.getLongitude(), currentLevel);

            android.graphics.Point tmpPoint1 = new android.graphics.Point();
            android.graphics.Point tmpPoint2 = new android.graphics.Point();

            Set<Integer> currentSet;
            Iterator<Integer> currentKeyIterator;
            for (y=currentBottomGrid; y<=(currentTopGrid+currentStepping); y+=currentStepping) {
                try {
                    int gridLeftKey = grid.ToKey(currentLeftGrid, y);
                    int gridRightKey = grid.ToKey(currentRightGrid, y);
                    currentSet = db.ContainsGrid(gridLeftKey, gridRightKey+currentStepping, currentLevel);
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

            if (0==currentLevel && null!=selectedGridX && null!=selectedGridY &&
                selectedGridX>=currentLeftGrid && selectedGridX<=currentRightGrid &&
                selectedGridY>=currentBottomGrid && selectedGridY<=currentTopGrid) {

                drawSquare(canvas, projection, grid, selectedGridKey, 0, grid.getSelectedGridColour(), tmpPoint1, tmpPoint2);
            }
        }
    }

    private void drawSquare(final Canvas canvas, final Projection projection,
                            final Grid grid, final int gridKey, final int gridLevel,
                            final Paint squareColour,
                            final android.graphics.Point reusePoint1, final android.graphics.Point reusePoint2) {
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


    private android.graphics.Point GridToPixel(final Grid grid, final int x, final int y, final Projection projection,
                                               android.graphics.Point reusePoint) {
        return projection.toPixels(new GeoPoint(grid.FromVerticalGrid(y), grid.FromHorizontalGrid(x)), reusePoint, true);
    }

    @Override
    public boolean onLongPress(final MotionEvent e, final MapView mapView)
    {
        IGeoPoint longpressPoint = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
        mapFragment.onLongPress(longpressPoint);
        return true;
    }

}
