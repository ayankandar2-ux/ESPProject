package com.potato.esp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_OVERLAY = 100;
    private static final int REQUEST_CODE_SCREEN = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        }

        Button btn = findViewById(R.id.btn_start);
        btn.setOnClickListener(v -> {
            MediaProjectionManager mp = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent screenIntent = mp.createScreenCaptureIntent();
            startActivityForResult(screenIntent, REQUEST_CODE_SCREEN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN && resultCode == Activity.RESULT_OK) {
            ESPApplication.getInstance().setProjectionData(data);
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            startService(serviceIntent);
        }
    }
}
