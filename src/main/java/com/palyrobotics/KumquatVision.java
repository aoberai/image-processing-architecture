package com.palyrobotics;

import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpiutil.RuntimeLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;

public class KumquatVision {

    static {
        var openCvLoader = new RuntimeLoader<>(Core.NATIVE_LIBRARY_NAME, RuntimeLoader.getDefaultExtractionRoot(), Core.class);
        try {
            openCvLoader.loadLibraryHashed();
        } catch (IOException loadException) {
            loadException.printStackTrace();
        }
    }

    public static void main(String... arguments) {
        new KumquatVision();
    }

    private KumquatVision() {
        var mat = new Mat();
        var capture = new VideoCapture(0);
        while (capture.isOpened()) {
            if (capture.read(mat)) {
            }
        }
        capture.release();
//        HighGui.destroyAllWindows();
    }
}
