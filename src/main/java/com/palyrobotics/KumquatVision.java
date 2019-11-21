package com.palyrobotics;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.IOException;

public class KumquatVision {

    private static final int PORT = 5809;

    private static final int BUFFER_SIZE = 50000;
    private static final long IDLE_SLEEP_MS = 200L;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String... arguments) throws InterruptedException {
        new KumquatVision();
    }

    private KumquatVision() {
        var server = new Server(BUFFER_SIZE, BUFFER_SIZE);
        server.getKryo().register(byte[].class);
        server.start();
        server.addListener(new Listener() {
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
            server.bind(PORT, PORT);
        } catch (IOException connectException) {
            connectException.printStackTrace();
        }

        var mat = new Mat();
        var capture = new VideoCapture(0);
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 320); // TODO config variables
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 240);
        capture.set(Videoio.CAP_PROP_FPS, 60);
        var streamMat = new MatOfByte();
        while (capture.isOpened()) {
            if (server.getConnections().length > 0) {
                if (capture.read(mat)) {
                    boolean encoded = Imgcodecs.imencode(".jpg", mat, streamMat, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));
                    if (encoded) {
                        try {
                            for (Connection connection : server.getConnections()) {
                                if (connection.isConnected()) {
                                    final var bytes = streamMat.toArray();
                                    if (bytes.length < BUFFER_SIZE)
                                        connection.sendUDP(bytes);
                                    else
                                        System.err.println("Too big!");
                                }
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                } else {
                    System.err.println("Opened camera, but could not read from it.");
                    break;
                }
            } else {
                try {
                    Thread.sleep(IDLE_SLEEP_MS);
                } catch (InterruptedException sleepException) {
                    break;
                }
            }
        }
        capture.release();
    }
}
