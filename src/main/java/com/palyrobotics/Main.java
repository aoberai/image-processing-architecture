package com.palyrobotics;

import org.opencv.core.Core;
import org.opencv.highgui.HighGui;

public class Main {
    private static Stream mStream;

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        mStream = new Stream();
        while (true) {
            HighGui.imshow("Vision", mStream.capture()[0]);
            HighGui.waitKey(1);
        }
    }
}
