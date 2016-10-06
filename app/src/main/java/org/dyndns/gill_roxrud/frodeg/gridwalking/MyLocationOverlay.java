package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;


public class MyLocationOverlay extends Overlay {


    public MyLocationOverlay(Context ctx) {
        super(ctx);
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        float line_halfwidth = Math.min(canvasWidth, canvasHeight) / 320;

        Paint red = new Paint();
        red.setColor(Color.argb(0x80, 0xC0, 0x00, 0x00));
        red.setStyle(Paint.Style.STROKE);
        red.setStrokeWidth(line_halfwidth);

        Paint black = new Paint();
        black.setColor(Color.argb(0x80, 0x00, 0x00, 0x00));

        canvas.drawCircle(canvasWidth/2, canvasHeight/2, line_halfwidth*16, red);
        canvas.drawCircle(canvasWidth/2, canvasHeight/2, line_halfwidth*4, black);
    }

}
