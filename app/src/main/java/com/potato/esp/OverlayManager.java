package com.potato.esp;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.concurrent.atomic.AtomicReference;

public class OverlayManager {
    private final WindowManager wm;
    private View overlayView;
    private final AtomicReference<Detector.Player[]> currentPlayers = new AtomicReference<>(new Detector.Player[0]);
    private Paint boxPaint, textPaint, dotPaint;

    public OverlayManager(Context context) {
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initPaints();
        createOverlay(context);
    }

    private void initPaints() {
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3f);
        boxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        textPaint.setShadowLayer(3, 1, 1, Color.BLACK);
        textPaint.setTypeface(Typeface.MONOSPACE);

        dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    private void createOverlay(Context context) {
        overlayView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                Detector.Player[] players = currentPlayers.get();
                for (Detector.Player p : players) {
                    boxPaint.setColor(p.isEnemy ? Color.RED : Color.GREEN);
                    canvas.drawRect(p.left, p.top, p.right, p.bottom, boxPaint);
                    String dist = String.format("%.1fm", p.distance);
                    canvas.drawText(dist, p.left, p.bottom + 30, textPaint);
                    int headX = (p.left + p.right) / 2;
                    int headY = p.top - 5;
                    canvas.drawCircle(headX, headY, 6, dotPaint);
                }
            }
        };

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        wm.addView(overlayView, params);
    }

    public void updatePlayers(Detector.Player[] players) {
        currentPlayers.set(players);
        overlayView.postInvalidate();
    }

    public void destroy() {
        if (overlayView != null) {
            wm.removeView(overlayView);
            overlayView = null;
        }
    }
}
