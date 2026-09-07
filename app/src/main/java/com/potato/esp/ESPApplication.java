package com.potato.esp;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import org.opencv.android.OpenCVLoader;

public class ESPApplication extends Application {
    private static final String TAG = "ESPApplication";
    private static ESPApplication instance;
    private Intent projectionData;
    private int projectionResultCode;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV initialized successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
        }
    }

    public static ESPApplication getInstance() { return instance; }

    public void setProjectionData(int resultCode, Intent data) {
        this.projectionResultCode = resultCode;
        this.projectionData = data;
    }

    public Intent getProjectionData() { return projectionData; }
    public int getProjectionResultCode() { return projectionResultCode; }
}

