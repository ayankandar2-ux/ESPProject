package com.potato.esp;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import java.util.concurrent.atomic.AtomicReference;

public class OverlayManager {
    private static final String TAG = "OverlayManager";
    private final WindowManager wm;
    private View overlayView;
    private final AtomicReference<Detector.Player[]> currentPlayers = new AtomicReference<>(new Detector.Player[0]);
    private Paint boxPaint, textPaint, dotPaint;
    private Paint statusBgPaint, statusDotPaint, statusTextPaint;
    private RectF statusRect;

    public OverlayManager(Context context) {
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initPaints();
        createOverlay(context);
    }

    private void initPaints() {
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setShadowLayer(4, 2, 2, Color.BLACK);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setAntiAlias(true);

        dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        statusBgPaint = new Paint();
        statusBgPaint.setColor(Color.argb(170, 20, 20, 20));
        statusBgPaint.setStyle(Paint.Style.FILL);
        statusBgPaint.setAntiAlias(true);

        statusDotPaint = new Paint();
        statusDotPaint.setColor(Color.rgb(76, 175, 80));
        statusDotPaint.setStyle(Paint.Style.FILL);
        statusDotPaint.setAntiAlias(true);

        statusTextPaint = new Paint();
        statusTextPaint.setColor(Color.WHITE);
        statusTextPaint.setTextSize(24f);
        statusTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextPaint.setAntiAlias(true);

        statusRect = new RectF(30, 40, 320, 95);
    }

    private void createOverlay(Context context) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot draw overlay: permission not granted");
            return;
        }

        overlayView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                // Status indicator badge
                canvas.drawRoundRect(statusRect, 14, 14, statusBgPaint);
                canvas.drawCircle(55, 68, 8, statusDotPaint);

                Detector.Player[] players = currentPlayers.get();
                int count = (players != null) ? players.length : 0;
                String status = "ESP Active (" + count + ")";
                canvas.drawText(status, 75, 76, statusTextPaint);

                // Draw bounding boxes around detected players
                if (players != null) {
                    for (Detector.Player p : players) {
                        boxPaint.setColor(p.isEnemy ? Color.RED : Color.GREEN);
                        canvas.drawRect(p.left, p.top, p.right, p.bottom, boxPaint);

                        String dist = String.format("%.1fm", p.distance);
                        canvas.drawText(dist, p.left, Math.min(canvas.getHeight() - 10, p.bottom + 30), textPaint);

                        int headX = (p.left + p.right) / 2;
                        int headY = Math.max(10, p.top - 6);
                        canvas.drawCircle(headX, headY, 8, dotPaint);
                    }
                }
            }
        };

        int windowType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        try {
            wm.addView(overlayView, params);
            Log.i(TAG, "Overlay view added successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add overlay view", e);
        }
    }

    public void updatePlayers(Detector.Player[] players) {
        currentPlayers.set(players != null ? players : new Detector.Player[0]);
        if (overlayView != null) {
            overlayView.postInvalidate();
        }
    }

    public void destroy() {
        if (overlayView != null) {
            try {
                wm.removeView(overlayView);
            } catch (Exception ignored) {}
            overlayView = null;
        }
    }
}
