#!/bin/bash
# Download OpenCV Android SDK and extract only the .so files for armeabi-v7a and arm64-v8a
echo "Downloading OpenCV 4.8.0 Android SDK..."
wget -q --show-progress https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip
unzip -q opencv-4.8.0-android-sdk.zip -d opencv_temp
cp opencv_temp/OpenCV-android-sdk/sdk/native/libs/armeabi-v7a/libopencv_java4.so app/src/main/jniLibs/armeabi-v7a/
cp opencv_temp/OpenCV-android-sdk/sdk/native/libs/arm64-v8a/libopencv_java4.so app/src/main/jniLibs/arm64-v8a/
rm -rf opencv_temp opencv-4.8.0-android-sdk.zip
echo "OpenCV .so files placed in jniLibs."
