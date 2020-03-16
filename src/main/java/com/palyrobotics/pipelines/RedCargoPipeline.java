package com.palyrobotics.pipelines;

import com.palyrobotics.Pipeline;
import com.palyrobotics.Stream;
import com.palyrobotics.util.ColorConstants;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.*;

/**
 * Example Pipeline that detects orange balls.
 */
public class RedCargoPipeline implements Pipeline {
    private HashMap<String, Integer> mData = new HashMap<>();
    public int indexOfCameraCapture = 0;
    private Mat mCaptureCopy;
    private List<MatOfPoint> mContoursCandidates = new ArrayList<>();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start() {
        mCaptureCopy = Stream.getCameraCaptures()[indexOfCameraCapture].clone();
        Imgproc.blur(mCaptureCopy, mCaptureCopy, new Size(25, 25));
        Imgproc.cvtColor(mCaptureCopy, mCaptureCopy, Imgproc.COLOR_BGR2HSV);
    }

    @Override
    public void update() {
        Core.inRange(mCaptureCopy, ColorConstants.kRedLowerBoundHSV, ColorConstants.kRedUpperBoundHSV, mCaptureCopy); // masks image to only allow orange objects
        Imgproc.findContours(mCaptureCopy, mContoursCandidates, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); // Takes the top level contour in image
        if (mContoursCandidates.size() != 0) {
            Mat largestContour = mContoursCandidates.stream().max(Comparator.comparingDouble(Imgproc::contourArea)).orElseThrow(NoSuchElementException::new);
            Imgproc.drawContours(Stream.getCameraCaptures()[indexOfCameraCapture], mContoursCandidates, mContoursCandidates.indexOf(largestContour), ColorConstants.kWhite, 5);
        }
        mContoursCandidates.clear();
    }

    @Override
    public String getName() {
        return "RedCargoPipeline";
    }

    @Override
    public HashMap<String, Integer> getData() {
        return mData;
    }
}
