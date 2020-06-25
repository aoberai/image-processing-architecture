package com.palyrobotics;

import org.opencv.core.Core;
import org.opencv.highgui.HighGui;

public class Main {
    private static PipelineManager mPipelineManager = new PipelineManager();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        Stream stream = new Stream();
        while (true) {
            stream.capture();
            mPipelineManager.runPipelines();
            HighGui.imshow("Vision", Stream.getCameraCaptures()[0]);
            HighGui.waitKey(1);
        }
    }
}
