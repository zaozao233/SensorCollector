package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
//import android.app.Activity;
//import android.Manifest;
//import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.lang.System.currentTimeMillis;


public class MainActivity extends AppCompatActivity {

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
                    Task<Integer> sendMessageTask =
                            //Send the message//
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

//                    try {
//
//                        //Block on a task and get the result synchronously//
//                        Integer result = Tasks.await(sendMessageTask);
//                        /*String messagetext = "start sending message 1234";
//                        Task<Integer> sendMessageTask2 =
//
//                                //Send the message//
//                                Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, messagetext.getBytes());
//                        Integer result2 = Tasks.await(sendMessageTask);
///                       sendmessage("start sending message 1234"); //+ dateFormat.format(currentTime) */
//
//                        //if the Task fails, then…..//
//                    } catch (ExecutionException exception) {
//                        //TO DO: Handle the exception//
//                        Log.d("error1","shit happens");
//
//                    } catch (InterruptedException exception) {
//                        Log.d("error2","shit happens");
//
//                        //TO DO: Handle the exception//
//                    }
                }
            } catch (ExecutionException exception) {

                //TO DO: Handle the exception//

            } catch (InterruptedException exception) {

                //TO DO: Handle the exception//
            }
        }
    }

    Button talkbutton,startbutton,endbutton;
    private Spinner spinner1,spinner2;
    TextView textview;
    protected Handler myHandler;

    Date currentTime ;
    DateFormat dateFormat  = new SimpleDateFormat("yyyyMMddhhmmssSSS");

    final String choices[] =  { "up", "down", "left", "right","back" ,"counterclockwise","clockwise","walking","WGW"};  // Where we track the selected items
    final String[] perms = {"Manifest.permission.WRITE_EXTERNAL_STORAGE","Manifest.permission.VIBRATE","Manifest.permission.READ_EXTERNAL_STORAGE"};
    File[] folder_name;
    SensorInfo sensorInfo;
    SensorDescriptor[] dsc_lst;
    SensorManager sensorManager;
    CountDownTimer countDownTimer;
    String state,action;
    int duration;
    long timestamp1, timestamp2, delay,bias,sync_start,sync;

    private LocalBroadcastManager localBroadcastManager;
    private Receiver messageReceiver;
    Time_management time_management;

    // boolean is_collecting_data=false;
    static final String TAG = "path name:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

 //       if (Build.VERSION.SDK_INT >= 23) {
        for (int i = 0; i < perms.length; i++) {
            if (checkPermission(perms[i])) {

            } else {
                requestPermission(perms[i]);
            }
        }
  //      }
        folder_name = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);

        Log.i(TAG,folder_name[0].toString());//get primary external storage

        //textview.setText(folder_name[0].toString());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        dsc_lst=new SensorDescriptor[5];

        dsc_lst[0]=new SensorDescriptor(Sensor.TYPE_LINEAR_ACCELERATION,"acc_mobile");
        dsc_lst[1]=new SensorDescriptor(Sensor.TYPE_GYROSCOPE,"gyro_mobile");
        dsc_lst[2]=new SensorDescriptor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,"mag_mobile");
        dsc_lst[3]=new SensorDescriptor(Sensor.TYPE_GAME_ROTATION_VECTOR,"game_mobile");
        dsc_lst[4]=new SensorDescriptor(Sensor.TYPE_GRAVITY,"grav_mobile");

        sensorInfo=new SensorInfo(dsc_lst,sensorManager);
        time_management = new Time_management(); //instantiate a time management

        talkbutton = findViewById(R.id.talkButton);
        startbutton = findViewById(R.id.startbutton);
        endbutton = findViewById(R.id.endbutton);
        textview = findViewById(R.id.textView); //refer to text area name
        spinner1 = findViewById(R.id.spinner1);
        spinner2 = findViewById(R.id.spinner2);


        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                state = parent.getItemAtPosition(position).toString();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                action = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /*
        countDownTimer = new CountDownTimer(10*1000,1*1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d("time left",millisUntilFinished/1000+"s");
            }
            @Override
            public void onFinish() {
                stop_collect();
                textview.setText("End collecting successfully");
            }
        };
        */
        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //below is a method that use alertdialog to select experiment
//                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                // Set the dialog title
//                builder.setIcon(R.drawable.ic_launcher_foreground);
//                builder.setTitle(R.string.pick_toppings)
//                        // Specify the list array, the items to be selected by default (null for none),
//                        // and the listener through which to receive callbacks when items are selected
//                        .setSingleChoiceItems(choices, -1,null )//If actions are needed when click, use the listener below
//                        // Set the action buttons
//                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int id) {
//                                // User clicked OK, so save the selectedItems results somewhere or return them to the component that opened the dialog
//                            //    textview.setText("show id  "+id);
//                                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
//                                textview.setText(choices[selectedPosition]);
//                                timestamp1 = currentTimeMillis();
//                                time_management.start_s = timestamp1;
//                                collecting_data(choices[selectedPosition]);
//                                //countDownTimer.start();
//                            }
//                        })
//                        .setNegativeButton(R.string.cancel, null);
// /*                               new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int id) {
//                            }
//                        }); */
//                AlertDialog dia1 = builder.create();
//                dia1.show();
//////
                //below is a new method that uses spinner
                textview.setText(state+action);
                timestamp1 = currentTimeMillis();
                time_management.start_s = timestamp1;
                collecting_data(state+'/'+action);

            }
        });
        //end of start button definition
        endbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                stop_collect();
//                textview.setText("End collecting successfully");
//                currentTime = Calendar.getInstance().getTime();
//                String strDate = dateFormat.format(currentTime);
                time_management.end_s = currentTimeMillis();
                talkClick(Long.toString(time_management.end_s),"end","*");
            }
        });

        //create message handler//

        myHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });
        //Register to receive local broadcasts, which we'll be creating in the next step//
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
        /* initialize sensor */
        /*
        int permsRequestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, permsRequestCode);
        }
        */
    }

    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            textview.append("\n" + newinfo);
        }
    }

    /*check & request storage permission*/
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
            f = new File (ss+"mobile_log.txt");
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

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        //Upon receiving each message from the wearable, display the following text//

            String info = intent.getExtras().getString("message");
            String[] sub1 = info.split(" at ");
            String time = sub1[1];
            Log.d("watchtime",time);
            textview.append("\n"+info);
            Long flag;
            timestamp2 = currentTimeMillis();

            if (info.charAt(18)=='e'){
                time_management.end_r = timestamp2;
                Log.d("test","find end");
                stop_collect();
                textview.append("\nEnd successfully");
            }else{
                time_management.start_r = timestamp2;
                delay = (timestamp2-timestamp1)/2;
                bias = Long.parseLong(time) - delay -timestamp1;
                Log.d("Delay",Long.toString(delay));
                Log.d("T1",Long.toString(timestamp1));
                Log.d("T2",Long.toString(timestamp2));
                flag = timestamp2 - delay + bias ;
                Log.d("flag",Long.toString(flag));
                Log.d("bias",Long.toString(bias));
                sensorInfo.bias = bias;
                sensorInfo.delay = delay;
                Log.d("damn",sensorInfo.bias+"  "+sensorInfo.delay);
            }
//            else if(info.charAt(18)=='t'){
////                Long flag;
////                timestamp2 = currentTimeMillis();
////                delay = (timestamp2-timestamp1)/2;
////                bias = Long.parseLong(time) - delay -timestamp1;
////                Log.d("Delay",Long.toString(delay));
////                Log.d("T1",Long.toString(timestamp1));
////                Log.d("T2",Long.toString(timestamp2));
////                flag = timestamp2 - delay + bias ;
////                Log.d("flag",Long.toString(flag));
////                Log.d("bias",Long.toString(bias));
////                sensorInfo.bias = bias;
////                sensorInfo.delay = delay;
//
//
//            }
        }
    }

    public void talkClick(View v) {
        String message = "Sending message1.... ";
        textview.setText(message);
        //Sending a message can block the main UI thread, so use a new thread//
        timestamp1 = currentTimeMillis();
        message = "test";

        new NewThread("/my_path", message).start();
        //the other way is to declare a class that implements the Runnable interface
    }

    public void talkClick(String time, String state, String actname) {
        String message = "mobile " + state +" recording "+ actname + " at "+ time;
        textview.setText(message);
        //Sending a message can block the main UI thread, so use a new thread//
        new NewThread("/my_path", message).start();
        //the other way is to declare a class that implements the Runnable interface
    }


    public void collecting_data(String action){
        String phone_foldername;
//        currentTime = Calendar.getInstance().getTime();
//        String strDate = dateFormat.format(currentTime);
        Long ctm = currentTimeMillis();
        String strDate = dateFormat.format(ctm);
        phone_foldername = folder_name[0].toString() +"/" + action + "/" + strDate + "/";
        time_management.set_file(phone_foldername);
        Log.d("path",phone_foldername+"\n");
        talkClick(Long.toString(ctm),"start",action);
        sensorInfo.set_filename(phone_foldername);
        sensorInfo.register_listener(sensorManager,SensorManager.SENSOR_DELAY_GAME);//Begin sensor data collection
        long[] pattern = {0, 500};
        Log.d("watch", "event triggered");
        run_vibration(pattern);
    }

    public void stop_collect(){
        Log.d("watch", "try to end sensor service");
        long[] pattern = {200, 200};
        run_vibration(pattern);
        sensorInfo.unregister_listener(sensorManager);
        sensorInfo.close_files();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (countDownTimer != null) {
//            countDownTimer.cancel();
//            countDownTimer = null;
//        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }
/*
//Use a Bundle to encapsulate our message//

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);
    }
*/


}


