package com.potato.esp;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class Detector {

    public static class Player {
        public int left, top, right, bottom;
        public float distance;
        public boolean isEnemy;
    }

    public Player[] detect(Bitmap frame) {
        if (frame == null) return new Player[0];
        Mat rgba = new Mat();
        Utils.bitmapToMat(frame, rgba);
        Player[] players = detectMat(rgba);
        rgba.release();
        return players;
    }

    public Player[] detectMat(Mat rgba) {
        if (rgba == null || rgba.empty()) return new Player[0];

        Mat rgb = new Mat();
        Mat hsv = new Mat();
        Mat redMask1 = new Mat();
        Mat redMask2 = new Mat();
        Mat redMask = new Mat();
        Mat greenMask = new Mat();
        Mat hierarchyRed = new Mat();
        Mat hierarchyGreen = new Mat();

        List<Player> players = new ArrayList<>();

        try {
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);
            Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);

            // Red health bars (enemy) - broad HSV range for game lighting
            Core.inRange(hsv, new Scalar(0, 60, 60), new Scalar(10, 255, 255), redMask1);
            Core.inRange(hsv, new Scalar(160, 60, 60), new Scalar(180, 255, 255), redMask2);
            Core.bitwise_or(redMask1, redMask2, redMask);

            // Green health bars (ally)
            Core.inRange(hsv, new Scalar(35, 60, 60), new Scalar(85, 255, 255), greenMask);

            List<MatOfPoint> contoursRed = new ArrayList<>();
            Imgproc.findContours(redMask, contoursRed, hierarchyRed, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            List<MatOfPoint> contoursGreen = new ArrayList<>();
            Imgproc.findContours(greenMask, contoursGreen, hierarchyGreen, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            int frameWidth = rgba.cols();
            int frameHeight = rgba.rows();

            for (MatOfPoint c : contoursRed) {
                Rect rect = Imgproc.boundingRect(c);
                c.release();
                if (rect.width >= 12 && rect.height >= 3 && (float) rect.width / rect.height >= 1.2f) {
                    int left = Math.max(0, rect.x - 15);
                    int top = Math.max(0, rect.y + rect.height + 2);
                    int right = Math.min(frameWidth, rect.x + rect.width + 15);
                    int bottom = Math.min(frameHeight, top + (rect.height * 7));
                    Player p = new Player();
                    p.left = left;
                    p.top = top;
                    p.right = right;
                    p.bottom = bottom;
                    p.isEnemy = true;
                    float actualHeight = bottom - top;
                    p.distance = actualHeight > 10 ? (180f / actualHeight) * 10f : 99f;
                    players.add(p);
                }
            }

            for (MatOfPoint c : contoursGreen) {
                Rect rect = Imgproc.boundingRect(c);
                c.release();
                if (rect.width >= 12 && rect.height >= 3 && (float) rect.width / rect.height >= 1.2f) {
                    int left = Math.max(0, rect.x - 15);
                    int top = Math.max(0, rect.y + rect.height + 2);
                    int right = Math.min(frameWidth, rect.x + rect.width + 15);
                    int bottom = Math.min(frameHeight, top + (rect.height * 7));
                    Player p = new Player();
                    p.left = left;
                    p.top = top;
                    p.right = right;
                    p.bottom = bottom;
                    p.isEnemy = false;
                    float actualHeight = bottom - top;
                    p.distance = actualHeight > 10 ? (180f / actualHeight) * 10f : 99f;
                    players.add(p);
                }
            }
        } finally {
            rgb.release();
            hsv.release();
            redMask1.release();
            redMask2.release();
            redMask.release();
            greenMask.release();
            hierarchyRed.release();
            hierarchyGreen.release();
        }

        return players.toArray(new Player[0]);
    }
}
