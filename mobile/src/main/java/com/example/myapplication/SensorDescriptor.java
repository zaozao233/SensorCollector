package com.example.myapplication;

/**
 * Created by harry on 9/9/17.
 */

public class SensorDescriptor {
    int sensorType;
    String storageName;

    public SensorDescriptor(int type, String name){
        this.sensorType=type;
        this.storageName=name;
    }
}
