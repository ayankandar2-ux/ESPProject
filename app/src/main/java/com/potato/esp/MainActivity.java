package com.potato.esp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_OVERLAY = 100;
    private static final int REQUEST_CODE_SCREEN = 101;
    private static final int REQUEST_CODE_NOTIF = 102;

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);

        // Check overlay permission
        checkOverlayPermission();

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIF);
            }
        }

        btnStart.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show();
                requestOverlayPermission();
                return;
            }

            MediaProjectionManager mp = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent screenIntent = mp.createScreenCaptureIntent();
            startActivityForResult(screenIntent, REQUEST_CODE_SCREEN);
        });

        btnStop.setOnClickListener(v -> {
            Intent stopIntent = new Intent(this, ScreenCaptureService.class);
            stopIntent.setAction(ScreenCaptureService.ACTION_STOP);
            startService(stopIntent);
            if (tvStatus != null) {
                tvStatus.setText("Status: Stopped");
                tvStatus.setTextColor(0xFFF44336);
            }
            Toast.makeText(this, "ESP Overlay stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Overlay permission is required for ESP to work", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_SCREEN) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ESPApplication.getInstance().setProjectionData(resultCode, data);

                Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenCaptureService.EXTRA_DATA, data);

                ContextCompat.startForegroundService(this, serviceIntent);

                if (tvStatus != null) {
                    tvStatus.setText("Status: Running");
                    tvStatus.setTextColor(0xFF4CAF50);
                }

                Toast.makeText(this, "ESP Overlay started! Switch to your game.", Toast.LENGTH_LONG).show();
                moveTaskToBack(true);
            } else {
                Toast.makeText(this, "Screen capture permission was cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

