package com.palyrobotics;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import org.opencv.core.Mat;

public class KumquatVision {

    public static void main(String... arguments) {
        new KumquatVision();
    }

    private KumquatVision()
    {
        var camera = new UsbCamera("Vision", "/dev/video0");
        var sink = new CvSink(camera.getName());
        Mat inputFrame = new Mat();
        while (camera.isConnected()) {
            sink.setSource(camera);
            sink.grabFrame(inputFrame);
        }
    }
}
