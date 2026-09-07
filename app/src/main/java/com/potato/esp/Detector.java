package com.potato.esp;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class Detector {
    static { System.loadLibrary("opencv_java4"); }

    public static class Player {
        public int left, top, right, bottom;
        public float distance;
        public boolean isEnemy;
    }

    public Player[] detect(Bitmap frame) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(frame, rgba);
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGBA2HSV);

        // Red health bars (enemy)
        Mat redMask = new Mat();
        Core.inRange(hsv, new Scalar(0, 100, 100), new Scalar(10, 255, 255), redMask);
        Mat redMask2 = new Mat();
        Core.inRange(hsv, new Scalar(170, 100, 100), new Scalar(180, 255, 255), redMask2);
        Core.bitwise_or(redMask, redMask2, redMask);

        // Green health bars (ally)
        Mat greenMask = new Mat();
        Core.inRange(hsv, new Scalar(40, 100, 100), new Scalar(80, 255, 255), greenMask);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(redMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        List<MatOfPoint> greenContours = new ArrayList<>();
        Imgproc.findContours(greenMask, greenContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Player> players = new ArrayList<>();

        for (MatOfPoint c : contours) {
            Rect rect = Imgproc.boundingRect(c);
            if (rect.width > 15 && rect.height > 5) {
                int left = Math.max(0, rect.x - 20);
                int top = Math.max(0, rect.y + rect.height + 5);
                int right = Math.min(frame.getWidth(), rect.x + rect.width + 20);
                int bottom = Math.min(frame.getHeight(), top + (rect.height * 6));
                Player p = new Player();
                p.left = left; p.top = top; p.right = right; p.bottom = bottom;
                p.isEnemy = true;
                float actualHeight = bottom - top;
                if (actualHeight > 10) p.distance = (180f / actualHeight) * 10f;
                else p.distance = 99f;
                players.add(p);
            }
        }

        for (MatOfPoint c : greenContours) {
            Rect rect = Imgproc.boundingRect(c);
            if (rect.width > 15 && rect.height > 5) {
                int left = Math.max(0, rect.x - 20);
                int top = Math.max(0, rect.y + rect.height + 5);
                int right = Math.min(frame.getWidth(), rect.x + rect.width + 20);
                int bottom = Math.min(frame.getHeight(), top + (rect.height * 6));
                Player p = new Player();
                p.left = left; p.top = top; p.right = right; p.bottom = bottom;
                p.isEnemy = false;
                float actualHeight = bottom - top;
                if (actualHeight > 10) p.distance = (180f / actualHeight) * 10f;
                else p.distance = 99f;
                players.add(p);
            }
        }

        return players.toArray(new Player[0]);
    }
}
