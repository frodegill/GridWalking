package org.dyndns.gill_roxrud.frodeg.gridwalking;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;


public class ScoreOverlay extends Overlay {

    public ScoreOverlay(Context ctx) {
        super(ctx);
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        Paint white = new Paint();
        white.setColor(Color.argb(128, 255, 255, 255));

        Paint black = new Paint();
        black.setColor(Color.argb(128, 0, 0, 0));

    }

}
