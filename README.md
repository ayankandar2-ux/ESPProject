# ESPProject

An Android screen-capture overlay application that uses OpenCV to detect target health bars in real time and render bounding boxes on an overlay window.

## Features

- **Real-Time Screen Capture**: Uses Android `MediaProjection` API via a Foreground Service.
- **Computer Vision Detection**: HSV color segmentation and contour detection with OpenCV (`libopencv_java4.so`) to locate ally and enemy players.
- **Floating Overlay**: System alert window rendering bounding boxes and estimated distances directly over the screen.

## Prerequisites

- Android SDK (API level 24 minimum, target 34)
- JDK 17
- OpenCV 4.8.0 Android SDK native libraries (`armeabi-v7a`, `arm64-v8a`)

## Setup

1. **Download OpenCV Native Libraries**:
   Run the setup script to download and extract the required OpenCV `.so` files:
   ```bash
   chmod +x download_opencv.sh
   ./download_opencv.sh
   ```

2. **Build the Project**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install & Permissions**:
   Install the APK on an Android device (API 24+) and grant:
   - "Display over other apps" (Overlay permission)
   - Screen capture / recording permission when prompted

## Project Structure

```
ESPProject/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/potato/esp/
│       │   ├── Detector.java              # OpenCV HSV detection & distance calculation
│       │   ├── ESPApplication.java        # Application singleton & projection data holder
│       │   ├── FrameProcessor.java        # Frame extraction & processing pipeline
│       │   ├── MainActivity.java          # Permission request & service launcher
│       │   ├── OverlayManager.java        # System alert window rendering
│       │   └── ScreenCaptureService.java  # Foreground service capturing screen
│       ├── jniLibs/                       # Native OpenCV libraries (.so)
│       └── res/layout/activity_main.xml
├── download_opencv.sh                     # Helper script to fetch OpenCV binaries
├── settings.gradle
└── build.gradle
```

## License

This project is for educational and research purposes.
