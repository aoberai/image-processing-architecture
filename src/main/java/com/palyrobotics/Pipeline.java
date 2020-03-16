package com.palyrobotics;

import java.util.HashMap;

public interface Pipeline {
    public void start();
    public void update();
    public String getName();
    public HashMap<String, Integer> getData();
}
