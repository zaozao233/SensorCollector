package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.widget.Button;
import android.widget.TextView;
import android.content.BroadcastReceiver;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;

import static java.lang.System.currentTimeMillis;

public class MainActivity extends WearableActivity {
    //Button startbutton, endbutton;
    //TextView info;
    protected Handler myHandler;
    private TextView mTextView;
    Date currentTime ;
    DateFormat dateFormat  = new SimpleDateFormat("yyyyMMddhhmmssSSS");

    //final String choices[] =  { "walking", "walking without swing", "swing", "swipe left","swipe right" };  // Where we track the selected items
    final String[] perms = {"Manifest.permission.WRITE_EXTERNAL_STORAGE","Manifest.permission.VIBRATE","Manifest.permission.READ_EXTERNAL_STORAGE"};
    File[] folder_name;
    SensorInfo sensorInfo;
    Time_management time_management;
    SensorDescriptor[] dsc_lst;
    SensorManager sensorManager;
    private long timestamp;
    private LocalBroadcastManager localBroadcastManager;
    private Receiver messageReceiver;

    static final String TAG = "path name:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < perms.length; i++) {
            if (checkPermission(perms[i])) {

            } else {
                requestPermission(perms[i]);
            }
        }
        folder_name = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        Log.i(TAG,folder_name[0].toString());//get primary external storage
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        dsc_lst=new SensorDescriptor[5];

        dsc_lst[0]=new SensorDescriptor(Sensor.TYPE_LINEAR_ACCELERATION,"acc_watch");
        dsc_lst[1]=new SensorDescriptor(Sensor.TYPE_GYROSCOPE,"gyro_watch");
        dsc_lst[2]=new SensorDescriptor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,"mag_watch");
        dsc_lst[3]=new SensorDescriptor(Sensor.TYPE_GAME_ROTATION_VECTOR,"game_watch");
        dsc_lst[4]=new SensorDescriptor(Sensor.TYPE_GRAVITY,"grav_watch");

        sensorInfo=new SensorInfo(dsc_lst,sensorManager);
        time_management = new Time_management(); //instantiate a time management

        mTextView =  findViewById(R.id.textView2);

        myHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("message"));
                return true;
            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new Receiver();//for local broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
        // Enables Always-on
        setAmbientEnabled();
    }

    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            mTextView.append("\n" + newinfo);
        }
    }

    private boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, permission);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestPermission(String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permission)) {
            Toast.makeText(MainActivity.this, "Please grante the permission", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("DEBUG", "PERMISSION_GRANTED");
                } else {
                    Log.e("DEBUG", "PERMISSION_DENIED");
                }
                break;
        }
    }

//Define a nested class that extends BroadcastReceiver//
    public class Time_management{
        public long start_r,start_s,end_r,end_s;
        public File f;
        public Time_management(){
            start_r = 0;
            start_s = 0;
            end_r = 0;
            end_s = 0;
        }
        public void set_file(String ss){
            f = new File (ss+"watch_log.txt");
        }
        public void write_file (){
            StringBuilder content = new StringBuilder();
            content.append(start_r);
            content.append(",");
            content.append(start_s);
            content.append(",");
            content.append(end_r);
            content.append(",");
            content.append(end_s);

            try {
                //create new file if not exists
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
        public void close_file(){
            if(f != null && f.exists()){
                f = null;
            }
        }
    }
    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//Upon receiving each message from the wearable, display the following text//
            timestamp = currentTimeMillis();
            String info = intent.getExtras().getString("message");
            String action;
            String time;
            String[] sub1,sub2;

            if (info.indexOf("test")==0 ){

                report(Long.toString(timestamp),"test");
            }
            else {
                sub1 = info.split(" at ");
                time = dateFormat.format(Long.parseLong(sub1[1]));//need to be test
                sub2 = sub1[0].split(" recording ");
                action = sub2[1];
                mTextView.setText(info);

                if (info.charAt(7) == 's') {
                    time_management.start_r = timestamp;
                    Log.d("test", "find start");
                    start_collect(action,time);
                } else if (info.charAt(7) == 'e') {
                    Log.d("test", "find end");
                    time_management.end_r = timestamp;
                    stop_collect(Long.toString(timestamp));
                }
            }
        }
    }
/*
    public void startClick(View v) {
        String message = "Sending message.... ";
        mTextView.setText(message);

//Sending a message can block the main UI thread, so use a new thread//

        new NewThread("/my_path", message).start();
        //the other way is to declare a class that implements the Runnable interface

    }

    public void endClick(View v) {
        String message = "Sending message.... ";
        mTextView.setText(message);

//Sending a message can block the main UI thread, so use a new thread//

        new NewThread("/my_path", message).start();
        //the other way is to declare a class that implements the Runnable interface

    }



//Use a Bundle to encapsulate our message//

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);

    }
*/
    public void report(String time ,String state){
        String message = "watch reports the " + state + " time at "+ time;
        mTextView.append("\n"+message);
        NewThread foo = new NewThread("/my_path", message);
        foo.setName(state + " repo");
        foo.start();
    }

    public void start_collect(String actname, String time){
        Log.d("start_collect","find action "+actname+" start at "+time);
        long[] pattern = {0, 1000};
        run_vibration(pattern);
        String watch_folder = folder_name[0].toString()+ "/" + actname +"/" +time +"/";
        Log.d("path",watch_folder+"\n");
        time_management.set_file(watch_folder);
        sensorInfo.set_filename(watch_folder);
        sensorInfo.register_listener(sensorManager,SensorManager.SENSOR_DELAY_GAME);//Begin sensor data collection

//        currentTime = Calendar.getInstance().getTime();
//        String watch_time = dateFormat.format(currentTime);
        time_management.start_s = currentTimeMillis();
        String watch_time = Long.toString(time_management.start_s);
        report(watch_time, "start");
    }

    public void stop_collect(String time){
        Log.d("stop_collect", "called at "+time);
        long[] pattern = {400, 200};
        run_vibration(pattern);
        sensorInfo.unregister_listener(sensorManager);
        sensorInfo.close_files();

//        currentTime = Calendar.getInstance().getTime();
//        String watch_time = dateFormat.format(currentTime);
        time_management.end_s = currentTimeMillis();
        String watch_time = Long.toString(time_management.end_s);
        report(watch_time,"end");
        time_management.write_file();
        time_management.close_file();
    }

    private void run_vibration(long[] pattern) {
        Log.d("watch", "try to vibrate");
        Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(pattern, -1);
        } else {
            Log.d("watch", "Do not have vibration service ");
        }
    }


    class NewThread extends Thread {
        String path;
        String message;

        //Constructor for sending information to the Data Layer//

        NewThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

            //Retrieve the connected devices, known as nodes//

            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
       //             Task<Integer> sendMessageTask =

                            //Send the message//

                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());
                    //Integer result = Tasks.await(sendMessageTask);
//                    if (result==1) {
//                        return;
//                    }
//                    try {
//
//                        //Block on a task and get the result synchronously//
////                        currentTime = Calendar.getInstance().getTime();
////                        String time = dateFormat.format(currentTime);
////                        Log.d("taskawait_bft",time);
//
//                        //sendmessage("I just sent the wearable a message " + sentMessageNumber++);
////                        currentTime = Calendar.getInstance().getTime();
////                        time = dateFormat.format(currentTime);
////                        Log.d("taskawait_aft",time);
//                        //if the Task fails, then…..//
//
//                    } catch (ExecutionException exception) {
//
//                        //TO DO: Handle the exception//
//
//                    } catch (InterruptedException exception) {
//
//                        //TO DO: Handle the exception//
//
//                    }

                }

            } catch (ExecutionException exception) {

                //TO DO: Handle the exception//

            } catch (InterruptedException exception) {

                //TO DO: Handle the exception//
            }

        }
    }
}
