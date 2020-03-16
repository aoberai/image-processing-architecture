package com.palyrobotics;

import com.esotericsoftware.kryonet.Server;

public class Communication {
    private static final int BUFFER_SIZE = 5000;
    private final Server mStreamServer = new Server(BUFFER_SIZE, BUFFER_SIZE);
    private final Server mDataServer = new Server(BUFFER_SIZE, BUFFER_SIZE);
}
