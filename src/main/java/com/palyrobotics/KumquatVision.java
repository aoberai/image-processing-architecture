package com.palyrobotics;

import com.esotericsoftware.kryonet.Server;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;

public class KumquatVision {

    private static final int PORT = 5809;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String... arguments) {
        new KumquatVision();
    }

    private KumquatVision() {
        var server = new Server();
        server.start();
        try {
            server.bind(PORT);
        } catch (IOException connectException) {
            connectException.printStackTrace();
        }

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
