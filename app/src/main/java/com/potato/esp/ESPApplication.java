package com.potato.esp;

import android.app.Application;
import android.content.Intent;

public class ESPApplication extends Application {
    private static ESPApplication instance;
    private Intent projectionData;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static ESPApplication getInstance() { return instance; }

    public void setProjectionData(Intent data) { this.projectionData = data; }
    public Intent getProjectionData() { return projectionData; }
}
