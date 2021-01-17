package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;



public class MainActivity extends AppCompatActivity {
    Button talkbutton,startbutton,endbutton;
    TextView textview;
    protected Handler myHandler;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;
    Date currentTime ;
    DateFormat dateFormat  = new SimpleDateFormat("yyyyMMddhhmmss");

    final String choices[] =  { "walking", "walking without swing", "swing", "swipe left","swipe right" };  // Where we track the selected items
    final String[] perms = {"Manifest.permission.WRITE_EXTERNAL_STORAGE","Manifest.permission.VIBRATE","Manifest.permission.READ_EXTERNAL_STORAGE"};
    File[] folder_name;
    SensorInfo sensorInfo;
    SensorDescriptor[] dsc_lst;
    SensorManager sensorManager;



    boolean is_collecting_data=false;
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


        talkbutton = findViewById(R.id.talkButton);
        startbutton = findViewById(R.id.startbutton);
        endbutton = findViewById(R.id.endbutton);
        textview = findViewById(R.id.textView); //refer to text area name

        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // Set the dialog title
                builder.setIcon(R.drawable.ic_launcher_foreground);
                builder.setTitle(R.string.pick_toppings)
                        // Specify the list array, the items to be selected by default (null for none),
                        // and the listener through which to receive callbacks when items are selected
                        .setSingleChoiceItems(choices, -1,null )//If actions are needed when click, use the listener below
                        // Set the action buttons
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK, so save the selectedItems results somewhere or return them to the component that opened the dialog
                            //    textview.setText("show id  "+id);
                                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                textview.setText(choices[selectedPosition]);
                                collecting_data(choices[selectedPosition]);

                            }
                        })
                        .setNegativeButton(R.string.cancel, null);
 /*                               new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        }); */
                AlertDialog dia1 = builder.create();
                dia1.show();
            }
        });
        //end of start button definition
        endbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop_collect();
                textview.setText("End collecting successfully");
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
        Receiver messageReceiver = new Receiver();
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

            //String message = "I just received a message from the wearable ";
            //textview.append(message);
            String info = intent.getExtras().getString("message");
            String[] sub1 = info.split(" at ");
            String time = sub1[1];
            textview.append("\n"+info);

        }
    }

    public void talkClick(View v) {
        String message = "Sending message1.... ";
        textview.setText(message);
        //Sending a message can block the main UI thread, so use a new thread//
        currentTime = Calendar.getInstance().getTime();

        String strDate = dateFormat.format(currentTime);

        new NewThread("/my_path", strDate).start();
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
        currentTime = Calendar.getInstance().getTime();

        String strDate = dateFormat.format(currentTime);
        phone_foldername = folder_name[0].toString() +"/" + action + "/" + strDate + "/";
        Log.d("path",phone_foldername+"\n");

        talkClick(strDate,"start",action);
        sensorInfo.set_filename(phone_foldername);
        sensorInfo.register_listener(sensorManager,SensorManager.SENSOR_DELAY_GAME);//Begin sensor data collection
        long[] pattern = {0, 1000};
        Log.d("watch", "event triggered");
        run_vibration(pattern);
    }

    public void stop_collect(){
        Log.d("watch", "try to end sensor service");
        textview.setText("try to end sensor service");
        long[] pattern = {400, 200};
        run_vibration(pattern);

        currentTime = Calendar.getInstance().getTime();
        String strDate = dateFormat.format(currentTime);
        talkClick(strDate,"end","*");

        sensorInfo.unregister_listener(sensorManager);
        sensorInfo.close_files();

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

                    try {

                        //Block on a task and get the result synchronously//
                        Integer result = Tasks.await(sendMessageTask);
                        /*String messagetext = "start sending message 1234";
                        Task<Integer> sendMessageTask2 =

                                //Send the message//
                                Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, messagetext.getBytes());
                        Integer result2 = Tasks.await(sendMessageTask);
/                       sendmessage("start sending message 1234"); //+ dateFormat.format(currentTime) */

                    //if the Task fails, then…..//
                    } catch (ExecutionException exception) {
                        //TO DO: Handle the exception//
                        Log.d("error1","shit happens");

                    } catch (InterruptedException exception) {
                        Log.d("error2","shit happens");

                        //TO DO: Handle the exception//
                    }
                }
            } catch (ExecutionException exception) {

                //TO DO: Handle the exception//

            } catch (InterruptedException exception) {

                //TO DO: Handle the exception//
            }
        }
    }
}


