package com.potato.esp;

import android.content.Context;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.nio.ByteBuffer;

public class FrameProcessor {
    private final Detector detector;

    public FrameProcessor(Context context) {
        this.detector = new Detector();
    }

    public Detector.Player[] processFrame(ByteBuffer buffer, int width, int height, int pixelStride, int rowStride) {
        if (buffer == null) return new Detector.Player[0];
        try {
            buffer.position(0);
            Mat rawMat = new Mat(height, width, CvType.CV_8UC4, buffer, rowStride);
            Detector.Player[] players = detector.detectMat(rawMat);
            rawMat.release();
            return players;
        } catch (Throwable t) {
            return new Detector.Player[0];
        }
    }
}

