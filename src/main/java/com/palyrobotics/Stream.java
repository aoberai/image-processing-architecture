package com.palyrobotics;

import com.palyrobotics.config.Configs;
import com.palyrobotics.config.VisionConfig;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Stream {
    public static VideoCapture frontCamera = new VideoCapture(0);
    public static VideoCapture backCamera = new VideoCapture(1);
    public static VideoCapture[] mCameras = new VideoCapture[] {frontCamera, backCamera};
    private static Mat[] mCameraCaptures;

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    Stream() {
        setupCameras();
        mCameraCaptures = new Mat[mCameras.length];
        for (int i = 0; i < mCameras.length; i++) {
            mCameraCaptures[i] = new Mat();
        }
    }

    private void setupCameras() {
        for (VideoCapture camera : mCameras) {
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, Configs.get(VisionConfig.class).captureWidth);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, Configs.get(VisionConfig.class).captureHeight);
            camera.set(Videoio.CAP_PROP_FPS, Configs.get(VisionConfig.class).captureFps);
        }
    }

    public Mat[] capture() {
        for (int i = 0; i < mCameras.length; i++) {
            if (mCameras[i].isOpened()) {
                mCameras[i].read(mCameraCaptures[i]);
            }
        }
        return mCameraCaptures;
    }

    public static Mat[] getCameraCaptures() {
        return mCameraCaptures;
    }
}
