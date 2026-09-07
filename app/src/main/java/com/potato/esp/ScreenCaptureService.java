package com.potato.esp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private FrameProcessor processor;
    private OverlayManager overlay;
    private int width, height, density;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        processor = new FrameProcessor(this);
        overlay = new OverlayManager(this);

        backgroundThread = new HandlerThread("CaptureThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        Intent data = ESPApplication.getInstance().getProjectionData();
        if (data != null) setMediaProjection(data);
    }

    public void setMediaProjection(Intent data) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(android.app.Activity.RESULT_OK, data);
        startCapture();
    }

    private void startCapture() {
        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            android.media.Image img = reader.acquireLatestImage();
            if (img != null) {
                android.media.Image.Plane plane = img.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                img.close();
                backgroundHandler.post(() -> {
                    Detector.Player[] players = processor.processFrame(data, width, height);
                    overlay.updatePlayers(players);
                });
            }
        }, backgroundHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ESPDisplay",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, backgroundHandler
        );
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, "esp_channel")
                .setContentTitle("ESP Overlay")
                .setContentText("Capture running")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("esp_channel",
                    "ESP Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        overlay.destroy();
        backgroundThread.quitSafely();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
