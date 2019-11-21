package com.palyrobotics;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.palyrobotics.config.Configs;
import com.palyrobotics.config.VisionConfig;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.IOException;

public class KumquatVision {

    private static final int BUFFER_SIZE = 50000;
    private static final long IDLE_SLEEP_MS = 200L;

    static {
        // The OpenCV jar just contains a wrapper that allows us to interface with the implementation of OpenCV written in C++
        // So, we have to load those C++ libraries explicitly and linked them properly.
        // Just having the jars is not sufficient, OpenCV must be installed into the filesystem manually.
        // I prefer to build it from source using CMake
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private final VisionConfig m_VisionConfig = Configs.get(VisionConfig.class);
    private final Mat m_CaptureMat = new Mat();
    private final VideoCapture m_Capture = new VideoCapture(0);
    private final Server m_Server = new Server(BUFFER_SIZE, BUFFER_SIZE);
    private final MatOfByte m_StreamMat = new MatOfByte();

    public static void main(String... arguments) {
        new KumquatVision();
    }

    private KumquatVision() {
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

    private boolean readFrame() {
        if (m_Capture.read(m_CaptureMat)) {
            if (m_VisionConfig.showImage) {
                HighGui.imshow("Vision", m_CaptureMat);
                HighGui.waitKey(1);
            }
            return Imgcodecs.imencode(".jpg", m_CaptureMat, m_StreamMat, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));
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
