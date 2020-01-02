package com.palyrobotics;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.palyrobotics.config.Configs;
import com.palyrobotics.config.VisionConfig;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

public class KumquatVision {

    private static final int BUFFER_SIZE = 50000;
    private static final long IDLE_SLEEP_MS = 200L;

    private static final String VERSION = "0.1";

    static {
        // The OpenCV jar just contains a wrapper that allows us to interface with the implementation of OpenCV written in C++
        // So, we have to load those C++ libraries explicitly and linked them properly.
        // Just having the jars is not sufficient, OpenCV must be installed into the filesystem manually.
        // I prefer to build it from source using CMake
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private Point centroidPoint = new Point();
    private final VisionConfig m_VisionConfig = Configs.get(VisionConfig.class);
    private Mat mCaptureMatHSV = new Mat();
    private Mat mFrameHSV = new Mat();
    private ArrayList<MatOfPoint> mContoursCandidates = new ArrayList<>();
    private final VideoCapture m_Capture = new VideoCapture(0);
    private final Server m_Server = new Server(BUFFER_SIZE, BUFFER_SIZE);
    private final MatOfByte m_StreamMat = new MatOfByte();
    private int largestContourIndex = -1;
    private final Scalar kBlack = new Scalar(0, 0, 0); // colors used to point out objects within live video feed
    private final Scalar kWhite = new Scalar(256, 256, 256);
    private final Scalar kRed = new Scalar(0, 0, 256);
    private final Scalar kPink = new Scalar(100, 100, 256);


    public static void main(String... arguments) {
        new KumquatVision();
    }

    private KumquatVision() {
        greatUser();
        setupServer();
        handleCapture();
    }

    private void handleCapture() {
        m_Capture.set(Videoio.CAP_PROP_FRAME_WIDTH, m_VisionConfig.captureWidth);
        m_Capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, m_VisionConfig.captureHeight);
        m_Capture.set(Videoio.CAP_PROP_FPS, m_VisionConfig.captureFps);
        while (m_Capture.isOpened()) {
            boolean shouldCapture = m_Server.getConnections().length > 0 || m_VisionConfig.showImage;
            if (shouldCapture) {
                if (readFrame()) {
                    sendFrameToConnectedClients();
                } else {
                    break;
                }
            } else {
                try {
                    Thread.sleep(IDLE_SLEEP_MS);
                } catch (InterruptedException sleepException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        HighGui.destroyAllWindows();
        m_Capture.release();
    }

    private void setupServer() {
        m_Server.getKryo().register(byte[].class);
        m_Server.start();
        m_Server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Connected");
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Disconnected");
            }
        });
        try {
            m_Server.bind(m_VisionConfig.port, m_VisionConfig.port);
        } catch (IOException connectException) {
            connectException.printStackTrace();
        }
    }

    private void greatUser() {
        String greeting = String.format("Starting Kumquat Vision Version: %s", VERSION);
        printSeparator(greeting.length());
        System.out.println(greeting);
        printSeparator(greeting.length());
    }

    private void printSeparator(int length) {
        Stream.generate(() -> "/").limit(length).forEach(System.out::print);
        System.out.println();
    }

    private boolean readFrame() {
        if (m_Capture.read(mCaptureMatHSV)) {
            if (m_VisionConfig.showImage) {
                mFrameHSV = mCaptureMatHSV.clone();
                Imgproc.blur(mFrameHSV, mFrameHSV, new Size(25, 25));
                Imgproc.cvtColor(mFrameHSV, mFrameHSV, Imgproc.COLOR_BGR2HSV);
                final Scalar lowerBoundHSV = new Scalar(5, 150, 153);
                final Scalar upperBoundHSV = new Scalar(15, 206, 255);
                Core.inRange(mFrameHSV, lowerBoundHSV, upperBoundHSV, mFrameHSV); // masks image to only allow orange objects
                Imgproc.findContours(mFrameHSV, mContoursCandidates, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); // Takes the top level contour in image

                if (mContoursCandidates.size() > 0) {
                    for (int i = 0; i < mContoursCandidates.size(); i++) {
                        if (largestContourIndex == -1) {
                            largestContourIndex = i;
                        } else if (Imgproc.contourArea(mContoursCandidates.get(i)) > Imgproc
                                .contourArea(mContoursCandidates.get(largestContourIndex))) {
                            largestContourIndex = i;
                        }
                    }
                    Imgproc.drawContours(mCaptureMatHSV, mContoursCandidates, largestContourIndex, kWhite, 10);
                    Imgproc.circle(mCaptureMatHSV, centroidPoint, 5, kPink, 20); // draws black circle at contour centroid
                    Imgproc.line(mCaptureMatHSV, new Point(centroidPoint.x, 0),
                            new Point(centroidPoint.x, mCaptureMatHSV.rows()), kBlack, 5);
                    Imgproc.line(mCaptureMatHSV, new Point(mCaptureMatHSV.cols() / 2, 0),
                            new Point(mCaptureMatHSV.cols() / 2, mCaptureMatHSV.rows()), kRed, 5); // draws center line
                }
//                mContourPointGetter.clear();
                mContoursCandidates.clear();
                largestContourIndex = -1;
                HighGui.imshow("Vision", mCaptureMatHSV);
                HighGui.waitKey(1);
            }
            return Imgcodecs.imencode(".jpg", mCaptureMatHSV, m_StreamMat, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));
        } else {
            System.err.println("Opened camera, but could not read from it.");
            return false;
        }
    }

    private void sendFrameToConnectedClients() {
        for (Connection connection : m_Server.getConnections()) {
            if (connection.isConnected()) {
                final var bytes = m_StreamMat.toArray();
                if (bytes.length < BUFFER_SIZE)
                    connection.sendUDP(bytes);
                else
                    System.err.println("Too big!");
            }
        }
    }
}
