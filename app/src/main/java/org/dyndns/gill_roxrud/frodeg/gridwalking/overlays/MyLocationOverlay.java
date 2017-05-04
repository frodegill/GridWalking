package org.dyndns.gill_roxrud.frodeg.gridwalking.overlays;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.dyndns.gill_roxrud.frodeg.gridwalking.GameState;
import org.dyndns.gill_roxrud.frodeg.gridwalking.Point;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;


public class MyLocationOverlay extends Overlay {

    private static final Paint red = new Paint();
    private static final Paint black = new Paint();

    public MyLocationOverlay() {
        super();
        red.setColor(Color.argb(0x80, 0xC0, 0x00, 0x00));
        red.setStyle(Paint.Style.STROKE);

        black.setColor(Color.argb(0x80, 0x00, 0x00, 0x00));
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        float line_halfwidth = Math.min(canvasWidth, canvasHeight) / 320;

        red.setStrokeWidth(line_halfwidth);

        Projection projection = mapView.getProjection();
        Point<Double> currentPos = GameState.getInstance().getCurrentPos();
        GeoPoint geoPoint = new GeoPoint(currentPos.getY(), currentPos.getX());
        android.graphics.Point reusePoint = null;
        reusePoint = projection.toProjectedPixels(geoPoint, reusePoint);
        reusePoint = projection.toPixelsFromProjected(reusePoint, reusePoint);

        canvas.drawCircle(reusePoint.x, reusePoint.y, line_halfwidth*16, red);
        canvas.drawCircle(reusePoint.x, reusePoint.y, line_halfwidth*4, black);
    }

}
