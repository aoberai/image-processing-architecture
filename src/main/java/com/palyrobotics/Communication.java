package com.palyrobotics;

import com.esotericsoftware.kryonet.Server;

import java.util.HashMap;

public class Communication {
    private static final int BUFFER_SIZE = 5000;
    private final Server mStreamServer = new Server(BUFFER_SIZE, BUFFER_SIZE);
    private final Server mDataServer = new Server(BUFFER_SIZE, BUFFER_SIZE);
    public static HashMap<String, HashMap<String, Integer>> dataToSend = new HashMap<>();
}
