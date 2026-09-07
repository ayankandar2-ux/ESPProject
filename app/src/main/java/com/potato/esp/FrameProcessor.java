package com.potato.esp;

import android.content.Context;
import android.graphics.Bitmap;
import java.nio.ByteBuffer;

public class FrameProcessor {
    private Detector detector;
    private Bitmap cacheBitmap;

    public FrameProcessor(Context context) {
        detector = new Detector();
        cacheBitmap = Bitmap.createBitmap(1080, 2340, Bitmap.Config.ARGB_8888);
    }

    public Detector.Player[] processFrame(byte[] rgbaData, int w, int h) {
        if (cacheBitmap.getWidth() != w || cacheBitmap.getHeight() != h) {
            cacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        ByteBuffer buffer = ByteBuffer.wrap(rgbaData);
        cacheBitmap.copyPixelsFromBuffer(buffer);
        return detector.detect(cacheBitmap);
    }
}
