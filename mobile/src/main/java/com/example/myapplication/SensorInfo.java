package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by harry on 9/9/17.
 */

public class SensorInfo {

    Sensor[] m_sensor_lst;
    File[] m_file_lst;
    String[] m_filename_lst;
    SensorEventListener[] m_listen_lst;


    String TAG="SensorInfo";
    public  SensorInfo(SensorDescriptor[] dsc_lst, SensorManager sensorManager){


        m_file_lst=new File[dsc_lst.length];
        m_sensor_lst=new Sensor[dsc_lst.length];
        m_listen_lst=new SensorEventListener[dsc_lst.length];
        m_filename_lst=new String[dsc_lst.length];

        for(int i=0;i<dsc_lst.length;i++){
            Sensor snr_temp= sensorManager.getDefaultSensor(dsc_lst[i].sensorType);
            m_sensor_lst[i]=snr_temp;

            final int idx=i;
            SensorEventListener listener = new SensorEventListener() {
                public void onAccuracyChanged(Sensor sensor, int acc) {}
                public void onSensorChanged(SensorEvent event) {
                    write_file(m_file_lst[idx], event);
                }
            };
            m_listen_lst[i]=listener;
            m_filename_lst[i]=dsc_lst[i].storageName;

        }

    }

    public void register_listener(SensorManager mgr, int delay){
        for(int i=0;i<m_listen_lst.length;i++){
            mgr.registerListener(m_listen_lst[i], m_sensor_lst[i],delay);
        }
    }
    public void unregister_listener(SensorManager mgr){
        for(int i=0;i<m_listen_lst.length;i++){
            mgr.unregisterListener(m_listen_lst[i]);
        }
    }
    public void close_files(){
        for(int i=0;i<m_file_lst.length;i++){
            if (m_file_lst[i] != null && m_file_lst[i].exists()) {
                m_file_lst[i] = null;
            }
        }
    }


    public void set_filename(String foldername){
        for (int i=0;i<m_file_lst.length;i++){
            m_file_lst[i]=new File(foldername+m_filename_lst[i]+".csv");
        }
    }


    private void write_file(File f, SensorEvent event) {
        long time = event.timestamp;
        float x ;
        float y ;
        float z ;


        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            float[] orientationVals = new float[3];
            float[] mRotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);
            // Optionally convert the result from radians to degrees
            x = (float) Math.toDegrees(orientationVals[0]);
            y = (float) Math.toDegrees(orientationVals[1]);
            z = (float) Math.toDegrees(orientationVals[2]);
        } else {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        }


        StringBuilder content = new StringBuilder();
        content.append(time);
        content.append(",");

        content.append(x);
        content.append(",");

        content.append(y);
        content.append(",");

        content.append(z);
        content.append("\n");

        try { //create new file if not exists
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                Log.i("parentfile",f.getParentFile().toString());
                f.createNewFile();
            }

            FileWriter fw = new FileWriter(f.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append(content.toString());
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
