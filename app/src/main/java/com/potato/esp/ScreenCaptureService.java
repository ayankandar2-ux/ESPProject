package com.potato.esp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    public static final String ACTION_STOP = "com.potato.esp.ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_DATA = "data";

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private FrameProcessor processor;
    private OverlayManager overlay;
    private int width, height, density;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForegroundCompat();

        calculateScreenDimensions();

        processor = new FrameProcessor(this);
        overlay = new OverlayManager(this);

        backgroundThread = new HandlerThread("CaptureThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_STOP.equals(intent.getAction())) {
                stopSelf();
                return START_NOT_STICKY;
            }

            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            if (data == null) {
                data = ESPApplication.getInstance().getProjectionData();
                resultCode = ESPApplication.getInstance().getProjectionResultCode();
            }

            if (data != null && mediaProjection == null) {
                initMediaProjection(resultCode, data);
            }
        }
        return START_STICKY;
    }

    private void startForegroundCompat() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }
    }

    private void calculateScreenDimensions() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowMetrics windowMetrics = wm.getMaximumWindowMetrics();
            android.graphics.Rect bounds = windowMetrics.getBounds();
            width = bounds.width();
            height = bounds.height();
            density = getResources().getConfiguration().densityDpi;
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(metrics);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
            density = metrics.densityDpi;
        }
    }

    private void initMediaProjection(int resultCode, Intent data) {
        try {
            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to obtain MediaProjection");
                stopSelf();
                return;
            }

            // Android 14 requires registering a callback before creating a VirtualDisplay
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    Log.i(TAG, "MediaProjection stopped by system");
                    stopSelf();
                }
            }, backgroundHandler);

            startCapture();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MediaProjection", e);
            stopSelf();
        }
    }

    private void startCapture() {
        if (mediaProjection == null) return;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            if (!isProcessing.compareAndSet(false, true)) {
                // Drop frame to prevent processing lag
                Image dropped = reader.acquireLatestImage();
                if (dropped != null) dropped.close();
                return;
            }

            Image img = null;
            try {
                img = reader.acquireLatestImage();
                if (img == null) return;

                Image.Plane[] planes = img.getPlanes();
                if (planes != null && planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();

                    Detector.Player[] players = processor.processFrame(buffer, width, height, pixelStride, rowStride);
                    if (overlay != null) {
                        overlay.updatePlayers(players);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing frame", e);
            } finally {
                if (img != null) {
                    img.close();
                }
                isProcessing.set(false);
            }
        }, backgroundHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ESPDisplay",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, backgroundHandler
        );
        Log.i(TAG, "VirtualDisplay created: " + width + "x" + height);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, "esp_channel")
                .setContentTitle("ESP Overlay")
                .setContentText("Overlay active and running")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("esp_channel",
                    "ESP Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background screen capture for ESP overlay");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping ScreenCaptureService");
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (overlay != null) {
            overlay.destroy();
            overlay = null;
        }
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
