package com.palyrobotics;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Stream {
    private VideoCapture[] mCameras;
    private Mat[] mCameraCaptures;

    private static VideoCapture mFrontCamera = new VideoCapture(0);
    private static VideoCapture mBackCamera = new VideoCapture(1);

    Stream() {
        setupCameras();
        mCameras = new VideoCapture[] {mFrontCamera, mBackCamera};
        mCameraCaptures = new Mat[mCameras.length];
        for (int i = 0; i < mCameras.length; i++) {
            mCameraCaptures[i] = new Mat();
        }
    }

    private void setupCameras() {
        mFrontCamera.set(Videoio.CAP_PROP_FRAME_WIDTH, 360);
        mFrontCamera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 240);
        mFrontCamera.set(Videoio.CAP_PROP_FPS, 30);
        mBackCamera.set(Videoio.CAP_PROP_FRAME_WIDTH, 360);
        mBackCamera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 240);
        mBackCamera.set(Videoio.CAP_PROP_FPS, 30);
    }

    public Mat[] capture() {
        for (int i = 0; i < mCameras.length; i++) {
            if (mCameras[i].isOpened()) {
                mCameras[i].read(mCameraCaptures[i]);
            }
        }
        return mCameraCaptures;
    }
}
