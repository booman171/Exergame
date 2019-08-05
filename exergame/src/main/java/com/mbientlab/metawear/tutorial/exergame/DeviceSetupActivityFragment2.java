package com.mbientlab.metawear.tutorial.exergame;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.MagnetometerBmm150.Preset;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Timer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Calendar;


import bolts.Continuation;
import bolts.Task;

import com.neurosky.connection.*;
import com.neurosky.thinkgear.*;
//import android.widget.Switch;

/**
 * A placeholder fragment containing a simple view.
 */
public class DeviceSetupActivityFragment2 extends Fragment implements ServiceConnection {
    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard metawear1;
    private static final String LOG_TAG = "serious";
    public static MetaWearBoard mwBoard2;
    private Accelerometer accelerometer, accelerometer2, accelerometer3, accelerometer4, accelerometer5;
    private GyroBmi160 gyroscope;
    private MagnetometerBmm150 magnetometer;
    private SensorFusionBosch sensorFusion;
    private Gpio gpio;
    private ForcedDataProducer adc;
    private Timer timer;
    private Timer.ScheduledTask readGpio;
    private Debug debug;
    private Logging logging;
    private MetaWearBoard metawear = null;
    private FragmentSettings settings;
    private Timer.ScheduledTask scheduledTask;
    private Button start1, start2, stop1, stop2;
    Intent shareIntent;
    OutputStream outAccel, outGyro, outOrient, outGpio;
    private int STORAGE_PERMISSION_REQUEST = 1;
    private String CSV_HEADER = String.format("name,weight,activity,reps,sets,weight, time,accel x-axis,accel y-axis,accel z-axis, gyro x-axis,gyro y-axis,gyro z-axis, mag x-axis,mag y-axis,mag z-axis,poor,blink,med,att");
    private String filename = "MetaMotion_Data_" + System.currentTimeMillis() + ".csv";
    private File filepath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private TensorflowClassifier classifier;
    private float[] results = new float[19];
    private static float[] data_array;
    private static List<Float> x, y, z, counter, test;
    private static ArrayList<Float> listA, listB;
    Set<Double> set = new HashSet<>();
    Set<Double> set2 = new HashSet<>();
    private static final int N_SAMPLES = 200;
    private TextView activity, pushUp, benchPress, triExt, latRaise,curl, repView, setsView, weightView, timeView;
    private int switchRouteId = -1;
    private Switch switchModule;
    private Led ledModule;
    View view1;
    private Boolean oldButtonState = Boolean.FALSE, newButtonState, latRaiseDone;
    private int n = 0, f = 0;
    List<Float> accel;
    private static final String[] labels = new String[]{"Bench Press", "Curl", "Dumbbell Raise", "Pushups", "Triceps Extension"};
    float[] newArray;
    private DataProcessor dataProc;
    int delay;
    Activity thisActivity;
    float averageData, baseLineStart = 0, mag;
    int[] activities;
    private String[][] thisActivityData;
    private String[] activityData;
    private ArrayList<String> thisActivityNames;
    Button button, start, stop, pauseButton;
    private boolean running, pause = false, wasRunning, newSet = false;
    private int seconds = 0;
    int reps = 0, sets = 0;
    private float gXData, gYData, gZData, mXData, mYData, mZData;

    /* The objects related Mindwave */
    private TGDevice tgDevice;
    private TgStreamHandler tgStreamHandler = null;
    private TgStreamReader tgStreamReader = null;
    private BluetoothAdapter bluetoothAdapter = null;
    TGRawMulti tgRaw;
    double att = 0, med = 0, blink = 0, poor = 0;
    Calendar t = Calendar.getInstance();
    TextView signalView, blinkView, attView, medView;
    public static String mac2;
    public DeviceSetupActivityFragment2() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
        Toast.makeText(getActivity().getApplicationContext(), "Permission already Granted.", Toast.LENGTH_SHORT).show();}
        else{
            requestStoragePermission();
        }
        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        classifier = new TensorflowClassifier(getActivity().getApplicationContext());
        settings= (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        listA = new ArrayList<>();
        counter = new ArrayList<>();
        test = new ArrayList<>();
        listB = new ArrayList<>();
        thisActivity = new Activity();
        activities = new int[] {0,0,0,0,0};
        activityData = new String[5];
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            tgDevice = new TGDevice(bluetoothAdapter, handler);
//        tgDevice.connect(true);
//        tgDevice.start();
        }
        tgRaw = new TGRawMulti();


    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TGDevice.MSG_STATE_CHANGE:

                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            Log.i(LOG_TAG, "Connecting...\n");
                            break;
                        case TGDevice.STATE_CONNECTED:
                            Log.i(LOG_TAG, "Connected.\n");
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            Log.i(LOG_TAG, "Can't find\n");
                            break;
                        case TGDevice.STATE_NOT_PAIRED:
                            Log.i(LOG_TAG, "not paired\n");
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            Log.i(LOG_TAG, "Disconnected\n");
                    }

                    break;
                case TGDevice.MSG_POOR_SIGNAL:
                    poor = msg.arg1;
                    //Log.i(LOG_TAG, "PoorSignal: " + poor + "\n");
                    break;
                case TGDevice.MSG_RAW_DATA:
                    //raw1 = msg.arg1;
                    //tv.append("Got raw: " + msg.arg1 + "\n");

                    break;
                case TGDevice.MSG_HEART_RATE:
                    Log.i(LOG_TAG, "Heart rate: " + msg.arg1 + "\n");
                    break;
                case TGDevice.MSG_ATTENTION:
                    att = msg.arg1;// * 2.55;
                    //Log.i(LOG_TAG, "Attention: " + att + "\n");
                    //tv.append("Attention Color: " + att + "\n");
                    t = Calendar.getInstance();
                    //writeExtFile("Attention: " + msg.arg1 + " @" + t.get(Calendar.HOUR) + ":" + t.get(Calendar.MINUTE) + ":" + t.get(Calendar.SECOND) + t.get(Calendar.AM_PM) + " " + "\n");
                    //Log.v("HelloA", "Attention: " + att + "\n");
                    //tv.setBackgroundColor(Color.argb(255, (int)att, 0, 0));
                    break;
                case TGDevice.MSG_MEDITATION:
                    med = msg.arg1;// * 2.55;
                    //Log.i(LOG_TAG, "Meditation: " + med + "\n");
                    t = Calendar.getInstance();
                    //writeExtFile("Meditation: " + msg.arg1 + " @" + t.get(Calendar.HOUR) + ":" + t.get(Calendar.MINUTE) + ":" + t.get(Calendar.SECOND) + t.get(Calendar.AM_PM) + " " + "\n");
                    break;
                case TGDevice.MSG_BLINK:
                    blink = msg.arg1;
                    //Log.i(LOG_TAG, "Blink: " + blink + "\n");
                    break;
                case TGDevice.MSG_RAW_COUNT:
                    break;
                case TGDevice.MSG_LOW_BATTERY:
                    //Toast.makeText(DeviceSetupActivityFragment2.this, "Low battery!", Toast.LENGTH_SHORT).show();
                    break;
                case TGDevice.MSG_RAW_MULTI:
                    //TGRawMulti rawM = (TGRawMulti)msg.obj;
                    //tv.append("Raw1: " + rawM.ch1 + "\nRaw2: " + rawM.ch2);
                    tgRaw = (TGRawMulti)msg.obj;

                    Log.i(LOG_TAG, "raw: " +
                            tgRaw.ch1 + ", " +
                            tgRaw.ch2 + ", " +
                            tgRaw.ch3 + ", " +
                            tgRaw.ch4 + ", " +
                            tgRaw.ch5 + ", " +
                            tgRaw.ch6 + ", " +
                            tgRaw.ch7 + ", " +
                            tgRaw.ch8 + ", " +
                            "\n");
                default:
                    break;
            }
            //tv.setBackgroundColor(Color.argb(255, (int)att, 0, (int)med));
        }
    };

    private void requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)){

            new AlertDialog.Builder(this.getActivity().getApplicationContext())
                    .setTitle("Permission Needed")
                    .setMessage("Permission need for thsi and that")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getActivity().getApplicationContext(), "Permission Granted.", Toast.LENGTH_SHORT).show();}
            else {
                Toast.makeText(getActivity().getApplicationContext(), "Permission Denied.", Toast.LENGTH_SHORT).show();}
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getActivity().getApplicationContext().unbindService(this);
    }

    // Method for getting the maximum value
    public static float getMax(float[] inputArray){
        float maxValue = inputArray[0];
        for(int i=1;i < inputArray.length;i++){
            if(inputArray[i] > maxValue){
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pushUp = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.pushUpView);
        benchPress = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.benchPressView);
        latRaise = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.latRaiseView);
        curl = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.curlView);
        triExt = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.triExtView);
        activity = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.activity);
        start1 = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);
        repView =  view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.repView);
        setsView =  view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.setsView);
        weightView =  view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.weightView);
        start = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);
        stop = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.stopButton);
        pauseButton = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.resetButton);
        button = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);

        //MindWave views
        signalView = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.signalView);
        blinkView = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.blinkView);
        attView = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.attView);
        medView = view.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.medView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(com.mbientlab.metawear.tutorial.exergame.R.layout.fragment_device_setup2, container, false);
    }

    private void configureChannel(Led.PatternEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.highIntensity((byte) 31).lowIntensity((byte) 31)
                .highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
                .repeatCount((byte) -1).commit();
    }

    public void save4Late(){
        if (n == 0 && SelectActivity.activityData != null && SelectActivity.activityNames != null){
            thisActivityData = SelectActivity.activityData;
            Arrays.fill(thisActivityData, 0);
        }
        if (tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED) {
            tgDevice.connect(true);
        }
        accelerometer= mwBoard2.getModule(Accelerometer.class);
        Log.i(LOG_TAG, "Print : " + thisActivityData);
        dataProc = mwBoard2.getModule(DataProcessor.class);
        gyroscope = mwBoard2.getModule(GyroBmi160.class);
        magnetometer = mwBoard2.getModule(MagnetometerBmm150.class);
        sensorFusion = mwBoard2.getModule(SensorFusionBosch.class);
        gpio = mwBoard2.getModule(Gpio.class);
        adc = gpio.pin((byte) 0).analogAdc();
        timer = mwBoard2.getModule(Timer.class);
        ledModule = mwBoard2.getModule(Led.class);
        switchModule = mwBoard2.getModule(Switch.class);
        boolean done = false;
        //float base = getBaseLineStart(mwBoard2);
        //Log.i(LOG_TAG, "Baseline: " + Float.toString(baseLineStart));
        //



        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                running = true;
                Log.i(LOG_TAG, "Start");
                if (n == 0 && SelectActivity.activityData != null){

                    //thisActivityData = new String[9][3];
                    thisActivityData = SelectActivity.activityData;

                    //thisActivityNames = new ArrayList<>();
                    thisActivityNames = SelectActivity.activityNames;
                    for(int i = 0; i < thisActivityData.length; i++){
                        activityData[i] = thisActivityData[i][0];
                        Log.i(LOG_TAG, "fgnfgn" + activityData[i]);
                    }
                    Log.i(LOG_TAG, thisActivityNames.get(0) + ": " + thisActivityData[1][0] + " reps.");
                    configureChannel(ledModule.editPattern(Led.Color.GREEN));
                    ledModule.play();
                    int delay = 0;
                    //mwBoard2.getModule(Haptic.class).startBuzzer((short) 3000);
                    Log.i(LOG_TAG, "Pressed");
                    //Log.i(LOG_TAG, "Button State: " + data.value(Boolean.class));
                    n = 1;
                    //float start = getBaseLineStart(mwBoard2);
                    //Log.i(LOG_TAG, "Base is: " + Float.toString(start));
                    while(delay != 4)
                        delay++;
                    mwBoard2.getModule(Haptic.class).startBuzzer((short) 1000);
                    //Log.i(LOG_TAG, "grsgtgvr" + thisActivityData[0][0]);
                    OutputStream out;
                    try{
                        out = new BufferedOutputStream(new FileOutputStream(filepath, true));
                        out.write(CSV_HEADER.getBytes());
                        out.write("\n".getBytes());
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    nextActivity(activities);
                                /*latRaiseDone = false;
                                if(latRaiseDone == false)
                                    latRaise(mwBoard2);
                                else
                                    Log.i(LOG_TAG, "DONE");*/

                    /*// GPIO Stream
                    adc.addRouteAsync(new RouteBuilder() {
                        @Override
                        public void configure(RouteComponent source) {
                            source.stream(new Subscriber() {
                                @Override
                                public void apply(Data data, Object ... env) {

                                    //Log.i(LOG_TAG, "adc = " + data.value(Short.class));
                                    //CSV CODE
                                    String gpio_entry = String.format("Gpio, %s", data.value(Short.class).toString().replaceAll("\\(","").replaceAll("\\)",""));
                                    String csv_gpio_entry = "Gpio," + String.valueOf(data.value(Short.class));
                                    csv_gpio_entry.replaceAll("\\(","").replaceAll("\\)","");
                                    OutputStream out;
                                    try {
                                        outGpio = new BufferedOutputStream(new FileOutputStream(pathGpio, true));
                                        //Log.e(LOG_TAG, "CSV Created");
                                        //out.write(value.x(),);
                                        outGpio.write(csv_gpio_entry.getBytes());
                                        outGpio.write("\n".getBytes());
                                        outGpio.close();
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "CSV creation error", e);
                                    }
                                }
                            });
                        }
                    }).continueWithTask(task -> {
                        return timer.scheduleAsync(100, false, adc::read);
                    }).continueWith(task -> {
                        scheduledTask = task.getResult();
                        scheduledTask.start();
                        return null;
                    });*/
                }
                else {
                    Log.i(LOG_TAG, "Chose your activities!");
                    Toast.makeText(getActivity().getApplicationContext(), "Chose your activities!", Toast.LENGTH_LONG).show();

                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                running = false;
                //n = 0;
                Log.i(LOG_TAG, "Stop");
                Log.i(LOG_TAG, "Activities not Selected.");
                Toast.makeText(getActivity().getApplicationContext(), "Select Activities", Toast.LENGTH_LONG).show();
                Log.i(LOG_TAG, "Stopped");
                accelerometer.packedAcceleration().stop();
                accelerometer.stop();
                gyroscope.angularVelocity().stop();
                gyroscope.stop();
                magnetometer.magneticField().stop();
                magnetometer.stop();
                ///scheduledTask.stop();
                ledModule.stop(true);
                mwBoard2.getModule(Haptic.class).startBuzzer((short) 1000);
                x.clear();
                y.clear();
                z.clear();
                delay = 0;
                //results = classifier.predictProbabilities(data_array);
                n = 0;
                //SelectActivity.activityData != null && SelectActivity.activityNames != null)
                Arrays.fill(SelectActivity.activityData, null);
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //running = false;
                //Log.i(LOG_TAG, "Reset");
                //seconds = 0;
                //set.clear();
                //sensorMsg(Double.toString(0.0), "punch");
                if(pause == false){
                    accelerometer.packedAcceleration().stop();
                    accelerometer.stop();
                    gyroscope.angularVelocity().stop();
                    gyroscope.stop();
                    magnetometer.magneticField().stop();
                    magnetometer.stop();
                    pause = true;
                    Log.i(LOG_TAG, "Paused");
                    Toast.makeText(getActivity().getApplicationContext(), "Paused", Toast.LENGTH_SHORT).show();
                }
                else if(pause == true){
                    accelerometer.packedAcceleration().start();
                    accelerometer.start();
                    gyroscope.angularVelocity().start();
                    gyroscope.start();
                    magnetometer.magneticField().start();
                    magnetometer.start();
                    pause = false;
                    if(newSet == true){
                        sets++;
                        reps = 0;
                    }
                    Log.i(LOG_TAG, "Resumed");
                    Toast.makeText(getActivity().getApplicationContext(), "Resumed", Toast.LENGTH_SHORT).show();
                }

            }
        });


        if ((switchModule= mwBoard2.getModule(Switch.class)) != null) {
            Route oldSwitchRoute;
            if ((oldSwitchRoute = mwBoard2.lookupRoute(switchRouteId)) != null) {
                oldSwitchRoute.remove();
            }

            switchModule.state().addRouteAsync(source ->
                    source.stream((data, env) -> thisActivity.runOnUiThread(() -> {
                        newButtonState = data.value(Boolean.class);
                        if (newButtonState == Boolean.TRUE && oldButtonState == Boolean.FALSE){
                            if(pause == false){
                                accelerometer.packedAcceleration().stop();
                                accelerometer.stop();
                                pause = true;
                                Log.i(LOG_TAG, "Paused");
                            }
                            else if(pause == true){
                                accelerometer.packedAcceleration().start();
                                accelerometer.start();
                                pause = false;
                                Log.i(LOG_TAG, "Resumed");
                            }

                        }
                    }))
            ).continueWith(task -> switchRouteId = task.getResult().id());
        }
        // Set Magnetometer Configuration
        magnetometer.usePreset(Preset.REGULAR);
        gyroscope.configure()
                .odr(GyroBmi160.OutputDataRate.ODR_50_HZ)
                .range(GyroBmi160.Range.FSR_2000)
                .commit();
        accelerometer.configure()
                .odr(50f)       // Set sampling frequency to 25Hz, or closest valid ODR
                .range(4f)      // Set data range to +/-4g, or closet valid range
                .commit();
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard2 = ((BtleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice());
        mac2 = mwBoard2.getMacAddress();

        Intent intent = new Intent(getActivity(), DodgeMain.class);
        startActivity(intent);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }

    private float[] toFloatArray(@NonNull List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public void sensorMsg(String msg, final String sensor) {
        final String reading = msg;
        thisActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sensor == "pushUp") {
                    pushUp.setText("Push Up: " + reading);
                } else if (sensor.equals("curl"))
                    curl.setText("Curl: " + reading);
                else if (sensor.equals("benchPress"))
                    benchPress.setText("Bench Press: " + reading);
                else if (sensor.equals("latRaise"))
                    latRaise.setText("Lateral Raise: " + reading);
                else if (sensor.equals("triExt"))
                    triExt.setText("Tri Ext: " + reading);
                else if (sensor.equals("base"))
                    pushUp.setText("Base: " + reading);
                else if (sensor.equals("sensor"))
                    benchPress.setText("Pos: " + reading);
                else if (sensor.equals("rep"))
                    curl.setText("Rep: " + reading);
                else if (sensor.equals("press"))
                    latRaise.setText("Press: " + reading);
                else if (sensor.equals("set"))
                    triExt.setText("Set: " + reading);
                else if (sensor.equals("activity"))
                    activity.setText(reading);
                else if (sensor.equals("signal"))
                    signalView.setText("Signal: " + reading);
                else if (sensor.equals("blink"))
                    blinkView.setText("Blink: " + reading);
                else if (sensor.equals("att"))
                    attView.setText("Att: " + reading);
                else if (sensor.equals("med"))
                    medView.setText("Med: " + reading);
            }
        });
    }

    public float getBaseLineStart(MetaWearBoard mwBoard2){
        Toast.makeText(getActivity().getApplicationContext(), "On Vibrate perform test rep at normal speed.", Toast.LENGTH_SHORT).show();
        if(listB.size() < 32 && f == 0){
            accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {

                            x.add(data.value(Acceleration.class).x());
                            //y.add(data.value(Acceleration.class).y());
                            //z.add(data.value(Acceleration.class).z());
                            float xData = data.value(Acceleration.class).x();
                            float yData = data.value(Acceleration.class).y();
                            float zData = data.value(Acceleration.class).z();
                            averageData = (xData + yData + zData) / 3;
                            //List<Float> lis = new ArrayList<>();
                            //List<Float> test = new ArrayList<>();
                            counter.add(averageData);
                            listA.add(averageData);
                            int buffer = 0;
                            float base_line = 0;
                            //
                            Log.i(LOG_TAG, "x: " + Integer.toString(x.size()));

                            //if( x.size() == N_SAMPLES)
                            //mwBoard2.getModule(Haptic.class).startBuzzer((short) 1000);
                            //if (base_line == 0 && listA.size() < 600) {

                            if(listA.size() == 32){
                                mwBoard2.getModule(Haptic.class).startBuzzer((short) 1000);
                                while(listB.size() < 200){
                                    listB.add(movingAverageFilter(listA));
                                    Log.i(LOG_TAG, "             ListB size: " + Integer.toString(listB.size()));
                                    //Log.i(LOG_TAG, "Counter size: " + Integer.toString(counter.size()));
                                    //int index = test.indexOf(Collections.min(test));
                                    //baseLineStart = test.get(test.indexOf(Collections.min(test)));
                                    //}
                                }
                            }

                            if(listB.size() == 200){
                                baseLineStart = listB.get(listB.indexOf(Collections.min(listB))) + (Collections.max(listB) - Collections.min(listB) / 3);
                                listB.clear();
                                listB.trimToSize();
                                Log.i(LOG_TAG, "BBBBBBBBBBBBB" + Integer.toString(listB.size()));

                                accelerometer.stop();
                                f = 1;

                            }
                            Log.i(LOG_TAG, "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRREEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
                        }
                    });
                }
            }).continueWith(new Continuation<Route, Void>() {
                @Override
                public Void then(Task<Route> task) throws Exception {
                    accelerometer.packedAcceleration().start();
                    accelerometer.start();
                    return null;
                }
            });
        }


        if(listB.size() == 200){
            Log.i(LOG_TAG, "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRREEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
            return baseLineStart;
        }else
            return baseLineStart;
    }

    public void exercise(MetaWearBoard mwBoard2, char axis, int space, String name, int id, int filterSize){
        if(activities[id] == 0){
            magnetometer.magneticField().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {
                            mXData = data.value(MagneticField.class).x();
                            mYData = data.value(MagneticField.class).y();
                            mZData = data.value(MagneticField.class).z();
                        }
                    });
                }
            }).continueWith(new Continuation<Route, Void>() {
                @Override
                public Void then(Task<Route> task) throws Exception {
                    magnetometer.magneticField().start();
                    magnetometer.start();
                    return null;
                }
            });
            gyroscope.angularVelocity().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        List<Float> counter = new ArrayList<>();
                        List<Float> test = new ArrayList<>();
                        ArrayList<Float> v = new ArrayList<>();
                        ArrayList<Float> repCount = new ArrayList<>();
                        Set<Float> set = new HashSet<>();
                        float highestTotAcc = 0;
                        int p = 0, o = 0;
                        float window = 0, base_line = 0;
                        //reps = 0, sets = 0
                        boolean condition = true;
                        @Override
                        public void apply(Data data, Object... env) {
                            gXData = data.value(AngularVelocity.class).x();
                            gYData = data.value(AngularVelocity.class).y();
                            gZData = data.value(AngularVelocity.class).z();
                        }
                    });
                }
            }).continueWith(new Continuation<Route, Void>() {
                @Override
                public Void then(Task<Route> task) throws Exception {
                    gyroscope.angularVelocity().start();
                    gyroscope.start();
                    return null;
                }
            });
            accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        List<Float> counter = new ArrayList<>();
                        List<Float> test = new ArrayList<>();
                        ArrayList<Float> v = new ArrayList<>();
                        ArrayList<Float> repCount = new ArrayList<>();
                        Set<Float> set = new HashSet<>();
                        float highestTotAcc = 0;
                        int p = 0, o = 0;
                        float window = 0, base_line = 0;
                        //reps = 0, sets = 0
                        boolean cond = true;
                        int maxReps = Integer.parseInt(thisActivityData[id][0]),
                                maxSets = Integer.parseInt(thisActivityData[id][1]),
                                weight = Integer.parseInt(thisActivityData[id][2]);
                        boolean condition = true;
                        @Override
                        public void apply(Data data, Object... env) {
                            if(activities[id] == 0){
                                float xData = data.value(Acceleration.class).x();
                                float yData = data.value(Acceleration.class).y();
                                float zData = data.value(Acceleration.class).z();
                                if(axis == 'x')
                                    test.add(xData);
                                else if(axis == 'y')
                                    test.add(yData);
                                else if(axis == 'z')
                                    test.add(zData);

                                //Log.i(LOG_TAG, "Poor: " + poor + "\n");
                                //Log.i(LOG_TAG, "Blink: " + blink + "\n");
                                //Log.i(LOG_TAG, "Meditation: " + med + "\n");
                                //Log.i(LOG_TAG, "Attention: " + att + "\n");

                                sensorMsg(Double.toString(poor), "signal");
                                sensorMsg(Double.toString(blink), "blink");
                                sensorMsg(Double.toString(med), "med");
                                sensorMsg(Double.toString(att), "att");

                                sensorMsg(name, "activity");
                                int buffer = 0;
                                String accel_entry = String.format("Accel, %s", data.value(Acceleration.class).toString().replaceAll("\\(","").replaceAll("\\)",""));
                                String csv_accel_entry = "Jenario" + ", 175" + ", " + name + ", " + reps + ", " + sets + ", " + weight + ", " + System.currentTimeMillis() + ", "
                                        + String.valueOf(xData) + ","
                                        + String.valueOf(yData) + ","
                                        + String.valueOf(zData) + ", "
                                        + String.valueOf(gXData) + ", "
                                        + String.valueOf(gYData) + ", "
                                        + String.valueOf(gZData) + ", "
                                        + String.valueOf(mXData) + ", "
                                        + String.valueOf(mYData) + ", "
                                        + String.valueOf(mZData) + ", "
                                        + poor + ", "
                                        + blink + ", "
                                        + med + ", "
                                        + att;
                                csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                                OutputStream out;
                                try {
                                    out = new BufferedOutputStream(new FileOutputStream(filepath, true));
                                    //Log.e(LOG_TAG, "CSV Created");
                                    //out.write(value.x(),);
                                    out.write(csv_accel_entry.getBytes());
                                    out.write("\n".getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "CSV creation error", e);
                                }
                                //ArrayList<Float> repCount = new ArrayList<>();
                                //Set<Float> set = new HashSet<>();
                                float highestTotAcc = 0;
                                int p = 0, o = 0;
                                //float window = 0, base_line = 0;
                                //int reps = 0, sets = 0, maxReps = Integer.parseInt(thisActivityData[0][0]), maxSets = Integer.parseInt(thisActivityData[0][1]);
                                x.add(data.value(Acceleration.class).x());
                                y.add(data.value(Acceleration.class).y());
                                z.add(data.value(Acceleration.class).z());

                                averageData = (xData + yData + zData) / 3;
                                //counter.add(averageData);

                                //float[] testArray = toFloatArray(test);
                                //Log.i(LOG_TAG, "Test Size: " +  testArray);

                                if(test.size() == filterSize){
                                    window = movingAverageFilter(test);
                                    test.remove(test.size() - 1);
                                    //Log.i(LOG_TAG, "Window: " +  Float.toString(window));
                                }
                                counter.add(window);

                                if(counter.size() == 200){
                                    base_line = counter.get(199);
                                    //sensorMsg(Integer.toString(Math.round(base_line*1000)), "base");
                                    Log.i(LOG_TAG, "Base line: " + Float.toString(base_line*1000));
                                    mwBoard2.getModule(Haptic.class).startBuzzer((short) 1000);
                                    sensorMsg(Integer.toString(space), "base");
                                }
                                if(name == "Push Ups")
                                    cond = Math.round((window - base_line)*1000) < space;
                                else
                                    cond = Math.round((window - base_line)*1000) > space;
                                //Log.i(LOG_TAG, "Diff: " +  Math.round((window)*1000) + ", " + Math.round((base_line)*1000) + ", " + Math.round((window - base_line)*1000));
                                if(counter.size() > 200 && cond){
                                    while(condition == true){
                                        reps++;
                                        //mwBoard2.getModule(Haptic.class).startBuzzer((short) 500);
                                        Log.i(LOG_TAG, "In");
                                        if(reps == maxReps){
                                            pause = true;
                                            newSet = true;
                                            accelerometer.packedAcceleration().stop();
                                            accelerometer.stop();
                                            gyroscope.angularVelocity().stop();
                                            gyroscope.stop();
                                            magnetometer.magneticField().stop();
                                            magnetometer.stop();
                                            counter.clear();
                                        }
                                        Log.i(LOG_TAG, "Reps: " + Integer.toString(reps) + ", Sets: " + Integer.toString(sets + 1));
                                        sensorMsg(Integer.toString(reps), "rep");
                                        sensorMsg(Integer.toString(sets + 1), "set");
                                        condition = false;
                                        if(sets == maxSets){
                                            latRaiseDone = true;
                                            activities[id] = 1;
                                            accelerometer.packedAcceleration().stop();
                                            accelerometer.stop();
                                            gyroscope.angularVelocity().stop();
                                            gyroscope.stop();
                                            magnetometer.magneticField().stop();
                                            magnetometer.stop();
                                            pause = true;
                                            newSet = true;
                                            nextActivity(activities);
                                            return;
                                        }
                                    }
                                    //Log.i(LOG_TAG, "UP");
                                }
                                if(Math.abs(Math.round((window - base_line)*1000)) <= 3){
                                    Log.i(LOG_TAG, "DOWN");
                                    condition = true;
                                }
                                sensorMsg(Double.toString(Math.round((window - base_line)*1000)*10/10.0), "sensor");
                            }
                        }
                    });
                }
            }).continueWith(new Continuation<Route, Void>() {
                @Override
                public Void then(Task<Route> task) throws Exception {
                    accelerometer.packedAcceleration().start();
                    accelerometer.start();
                    return null;
                }
            });
        }

    }

    public void nextActivity(int[] activityList){
        if(!(activityData[0].isEmpty()) && activities[0] == 0){
            Log.i(LOG_TAG, "Reps: " + activityData[0] + ", Sets: " + thisActivityData[0][1]);
            Log.i(LOG_TAG, "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG");
            exercise(mwBoard2, 'x', 12, "Lateral Raises", 0, 32);
        }
        else if(!(activityData[1].isEmpty()) && activities[1] == 0){
            Log.i(LOG_TAG, "New Activity");
            exercise(mwBoard2, 'y', 9, "Triceps Extension", 1, 32);
        }
        else if(!(activityData[2].isEmpty()) && activities[2] == 0){
            Log.i(LOG_TAG, "New Activity");
            exercise(mwBoard2, 'y', 12, "Curls", 2, 32);
        }
        else if(!(activityData[3].isEmpty()) && activities[3] == 0){
            Log.i(LOG_TAG, "New Activity");
            exercise(mwBoard2, 'y', 12, "Press", 3, 32);
        }
        else if(!(activityData[4].isEmpty()) && activities[4] == 0){
            Log.i(LOG_TAG, "New Activity");
            exercise(mwBoard2, 'y', -3 , "Push Ups", 4, 64);
        }
        else
            Log.i(LOG_TAG, "All activities done!");
    }

    public static float movingAverageFilter(List<Float> givenArray){
        float sum = 0;
        if(!givenArray.isEmpty()){
            for(float givenElement: givenArray){
                sum += givenElement;
            }
            return sum / givenArray.size();
        }
        return sum;
    }
}
