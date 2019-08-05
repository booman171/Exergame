package com.mbientlab.metawear.tutorial.exergame;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Timer;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Myo.VibrationType;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import ai.api.AIService;
import bolts.Continuation;
import bolts.Task;

public class MyoArmband extends AppCompatActivity {
    private Myo myo;
    private Vector3 accel;
    private Quaternion gyro;
    private Toast mToast;
    private static final String LOG_TAG = "serious";
    private Vector3 lastAccel;
    private long cooldownTimestamp;
    private MediaPlayer punchCrack;
    private TextView resultTextView;
    private AIService aiService;
    private String CLIENT_ACCESS_TOKEN = "d2f72c6df1c646b68accd642f1b64410";
    private String SUBSCRIPTION_KEY = "547af05e-1b49-471d-936b-829974d6f7f4";
    private MediaPlayer mikeTyson;
    private EditText punch;
    private int counter = 0;
    private ImageView image1;
    Button btnStop;
    TextView textViewTime;
    private TextView mLockStateView, mTextView;
    private Button startAccel, stopAccel;
    private static List<Double> x, y, z;
    private static List<Float> rollList, pitchList, yawList;
    private static float[] data_array;
    private String filenameAccel = "Myo_Accel_Data_" + System.currentTimeMillis() + ".csv";
    private File pathAccel = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filenameAccel);
    OutputStream outAccel;
    boolean stream = false;
    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard metawear1;
    private MetaWearBoard mwBoard;
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
    private DeviceSetupActivityFragment.FragmentSettings settings;
    private Timer.ScheduledTask scheduledTask;
    private Button start1, start2, stop1, stop2;
    Intent shareIntent;
    OutputStream outGyro, outOrient, outGpio;
    private int STORAGE_PERMISSION_REQUEST = 1;
    private String CSV_HEADER = String.format("sensor,x_axis,y_axis,z_axis");
    //private String filenameAccel = "MetaMotion_Accel_Data_" + System.currentTimeMillis() + ".csv";
    private String filenameGyro = "Gyro_Data_" + System.currentTimeMillis() + ".csv";
    private String filenameOrient = "Orient_Data_" + System.currentTimeMillis() + ".csv";
    private String filenameGpio = "Gpio_Data_" + System.currentTimeMillis() + ".csv";
    //private File pathAccel = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filenameAccel);
    private File pathGyro = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filenameGyro);
    private File pathOrient = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filenameOrient);
    private File pathGpio = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filenameGpio);
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private TensorflowClassifier classifier;
    private float[] results = new float[19];
    //private static float[] data_array;
    private static List<Float> test;
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
    //List<Float> accel;
    private static final String[] labels = new String[]{"Bench Press", "Curl", "Dumbbell Raise", "Pushups", "Triceps Extension"};
    float[] newArray;
    private DataProcessor dataProc;
    int delay;
    Activity thisActivity;
    double averageData, baseLineStart = 0, mag;
    int[] activities;
    private String[][] thisActivityData;
    private String[] activityData;
    private ArrayList<String> thisActivityNames;
    Button button, start, stop, pauseButton;
    private boolean running, pause = false;
    private boolean wasRunning;
    private int seconds = 0;

    private DeviceListener mListener = new AbstractDeviceListener() {

        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {

            showToast(myo.getName() + " Connected ");
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            showToast(myo.getName() + " Disconnected");
        }

        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            //mTextView.setText(myo.getArm() == Arm.LEFT ? R.string.arm_left : R.string.arm_right);
            //image1.setImageResource(R.drawable.left_boxing_glove);
        }

        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            //mTextView.setText(R.string.hello_world);
            //image1.setImageResource(R.drawable.left_boxing_glove);
        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    //mTextView.setText(getString(R.string.hello_world));
                    //image1.setImageResource(R.drawable.left_boxing_glove);
                    break;
                case REST:
                case DOUBLE_TAP:
                    //int restTextId = R.string.hello_world;
                    //image1.setImageResource(R.drawable.left_boxing_glove);
                    switch (myo.getArm()) {
                        case LEFT:
                            //restTextId = R.string.arm_left;
                            //image1.setImageResource(R.drawable.left_boxing_glove);
                            break;
                        case RIGHT:
                            //restTextId = R.string.arm_right;
                            //image1.setImageResource(R.drawable.left_boxing_glove);
                            break;
                    }
                    //mTextView.setText(getString(restTextId));
                    //image1.setImageResource(R.drawable.left_boxing_glove);
                    break;
            }

            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);

                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }

        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
            image1.setRotation(roll);
            image1.setRotationX(pitch);
            image1.setRotationY(yaw);
            //Log.i(LOG_TAG, "Yaw: " + yaw);

            startAccel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stream = true;
                    myo.vibrate(VibrationType.MEDIUM);
                    Toast.makeText(MyoArmband.this, "Stream started!", Toast.LENGTH_LONG).show();
                }
            });

            stopAccel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stream = false;
                    myo.vibrate(VibrationType.MEDIUM);
                    Toast.makeText(MyoArmband.this, "Stream ended!", Toast.LENGTH_LONG).show();
                }
            });
            if(stream == true) {
                Log.i(LOG_TAG, "Gyro: " + roll + ", " + pitch + ", " + yaw);
                String accel_entry = String.format("Accel: " + roll + ", " + pitch + ", " + yaw);
                String csv_accel_entry = "Gyro," + roll + "," + pitch + "," + yaw;
                //csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                rollList.add(roll);
                pitchList.add((float)pitch);
                yawList.add((float)yaw);
                List<Float> gyroscope = new ArrayList<>();

                //Log.i(LOG_TAG, Integer.toString(x.size()));
                gyroscope.addAll(rollList);
                gyroscope.addAll(pitchList);
                gyroscope.addAll(yawList);
                data_array = toFloatArray(gyroscope);

                if(stream){
                    //OutputStream out;
                    try {
                        //Log.e(LOG_TAG, "Writing to CSV");
                        outAccel = new BufferedOutputStream(new FileOutputStream(pathAccel, true));
                        //out.write(value.x(),);
                        outAccel.write(csv_accel_entry.getBytes());
                        outAccel.write("\n".getBytes());
                        outAccel.close();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "CSV creation error", e);
                    }
                }
            }
        }

        @Override
        public void onAccelerometerData (Myo myo, long timestamp, Vector3 accel) {
            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    running = true;
                    Log.i(LOG_TAG, "Start");
                    if (n == 0 && SelectActivity.activityData != null && SelectActivity.activityNames != null){
                        thisActivityData = SelectActivity.activityData;
                        thisActivityNames = SelectActivity.activityNames;
                        for(int i = 0; i < thisActivityData.length; i++){
                            activityData[i] = thisActivityData[i][0];
                            Log.i(LOG_TAG, "fgnfgn" + activityData[i]);
                        }
                        Log.i(LOG_TAG, thisActivityNames.get(0) + ": " + thisActivityData[1][0] + " reps.");
                        int delay = 0;
                        Log.i(LOG_TAG, "Pressed");
                        n = 1;
                        while(delay != 4)
                            delay++;
                        nextActivity(activities);
                    }
                    else {
                        Log.i(LOG_TAG, "Chose your activities!");
                        Toast.makeText(MyoArmband.this, "Chose your activities!", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(MyoArmband.this, "Select Activities", Toast.LENGTH_LONG).show();
                    Log.i(LOG_TAG, "Stopped");
                    accelerometer.packedAcceleration().stop();
                    accelerometer.stop();
                    magnetometer.magneticField().stop();
                    magnetometer.stop();
                    ///scheduledTask.stop();
                    ledModule.stop(true);
                    mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
                    x.clear();
                    y.clear();
                    z.clear();
                    delay = 0;
                    //results = classifier.predictProbabilities(data_array);
                    n = 0;
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
            });
            startAccel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stream = true;
                    myo.vibrate(VibrationType.MEDIUM);
                    Toast.makeText(MyoArmband.this, "Stream started!", Toast.LENGTH_LONG).show();
                }
            });

            stopAccel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stream = false;
                    myo.vibrate(VibrationType.MEDIUM);
                    Toast.makeText(MyoArmband.this, "Stream ended!", Toast.LENGTH_LONG).show();
                }
            });
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.mbientlab.metawear.tutorial.exergame.R.layout.activity_myo_armband);

        Toolbar toolbar = (Toolbar) findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.toolbar);
        setSupportActionBar(toolbar);

        //image1 = (ImageView) findViewById(R.id.imageLeftGlove);

        mLockStateView = (TextView) findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.lock_state);
        mTextView = (TextView) findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.text);

        startAccel = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);
        stopAccel = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.stopButton);
        pushUp = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.pushUpView);
        benchPress = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.benchPressView);
        latRaise = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.latRaiseView);
        curl = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.curlView);
        triExt = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.triExtView);
        activity = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.activity);
        start1 = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);
        repView =  findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.repView);
        setsView =  findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.setsView);
        weightView =  findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.weightView);
        start = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);
        stop = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.stopButton);
        pauseButton = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.resetButton);
        button = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startButton);
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();

        cooldownTimestamp = 0;
        lastAccel = new Vector3();

        //onAllData(myo, accel, gyro);

        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

    }

    public void onAllData (Myo myo, Vector3 accel, Quaternion gyro){
        Log.i(LOG_TAG, "Accel: " + Double.toString(accel.x()));
    }

    private void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(com.mbientlab.metawear.tutorial.exergame.R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (com.mbientlab.metawear.tutorial.exergame.R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    private float[] toFloatArray(@NonNull List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public void latRaise(Vector3 accel){
        if(activities[0] == 0){
            List<Double> counter = new ArrayList<>();
            List<Double> test = new ArrayList<>();
            ArrayList<Double> v = new ArrayList<>();
            ArrayList<Double> repCount = new ArrayList<>();
            Set<Double> set = new HashSet<>();
            double highestTotAcc = 0;
            int p = 0, o = 0;
            double window = 0, base_line = 0;
            int reps = 0, sets = 0, maxReps = Integer.parseInt(thisActivityData[0][0]), maxSets = Integer.parseInt(thisActivityData[0][1]);
            boolean condition = true;
            if(activities[0] == 0){
                sensorMsg("Lateral Raise", "activity");
                int buffer = 0;
                String accel_entry = String.format("Accel, %s", accel);
                String csv_accel_entry = "Accel,"
                        + String.valueOf(accel.x()) + ","
                        + String.valueOf(accel.y()) + ","
                        + String.valueOf(accel.z());
                csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                x.add(accel.x());
                y.add(accel.y());
                z.add(accel.z());
                double xData = accel.x();
                double yData = accel.y();
                double zData = accel.z();
                averageData = (xData + yData + zData) / 3;
                //counter.add(averageData);
                test.add(xData);
                //float[] testArray = toFloatArray(test);
                //Log.i(LOG_TAG, "Test Size: " +  testArray);

                if(test.size() == 32){
                    window = movingAverageFilter(test, 256);
                    test.remove(test.size() - 1);
                    //Log.i(LOG_TAG, "Window: " +  Float.toString(window));
                }
                counter.add(window);
                //Log.i(LOG_TAG, "Counter: " +  Float.toString(counter.size()));

                if(counter.size() == 200){
                    base_line = counter.get(199);
                    sensorMsg(Double.toString(Math.round(base_line*1000)), "base");
                    Log.i(LOG_TAG, "Base line: " + Double.toString(base_line));
                    mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
                }
                if(counter.size() > 200 && Math.round((window - base_line)*1000) > 12){
                    while(condition == true){
                        if(reps == maxReps){
                            sets++;
                            reps = 0;
                        }
                        reps++;
                        sensorMsg(Integer.toString(reps), "rep");
                        sensorMsg(Integer.toString(sets), "set");
                        condition = false;
                        if(sets == maxSets + 1){
                            latRaiseDone = true;
                            activities[0] = 1;
                            accelerometer.packedAcceleration().stop();
                            accelerometer.stop();
                            nextActivity(activities);
                            return;
                        }
                    }
                    //Log.i(LOG_TAG, "UP");
                }
                if(Math.round((window - base_line)*1000) <= 3){
                    Log.i(LOG_TAG, "DOWN");
                    condition = true;
                }

                sensorMsg(Double.toString(Math.round((window - base_line)*1000)), "sensor");
            }
        }

    }

    public void triExt(Vector3 accel){
        if(activities[1] == 0){
            accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        List<Double> counter = new ArrayList<>();
                        List<Double> test = new ArrayList<>();
                        ArrayList<Double> v = new ArrayList<>();
                        ArrayList<Double> repCount = new ArrayList<>();
                        Set<Double> set = new HashSet<>();
                        double highestTotAcc = 0;
                        int p = 0, o = 0;
                        double window = 0, base_line = 0;
                        int reps = 0, sets = 0, maxReps = Integer.parseInt(thisActivityData[1][0]), maxSets = Integer.parseInt(thisActivityData[1][1]);
                        boolean condition = true;
                        @Override
                        public void apply(Data data, Object... env) {
                            if(activities[1] == 0){
                                sensorMsg("Triceps Extension", "activity");
                                int buffer = 0;
                                String accel_entry = String.format("Accel, %s", data.value(Acceleration.class).toString().replaceAll("\\(","").replaceAll("\\)",""));
                                String csv_accel_entry = "Accel,"
                                        + String.valueOf(data.value(Acceleration.class).x()) + ","
                                        + String.valueOf(data.value(Acceleration.class).y()) + ","
                                        + String.valueOf(data.value(Acceleration.class).z());
                                csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                                x.add(accel.x());
                                y.add(accel.y());
                                z.add(accel.z());
                                double xData = data.value(Acceleration.class).x();
                                double yData = data.value(Acceleration.class).y();
                                double zData = data.value(Acceleration.class).z();
                                averageData = (xData + yData + zData) / 3;
                                //counter.add(averageData);
                                test.add(yData);
                                //float[] testArray = toDoubleArray(test);
                                //Log.i(LOG_TAG, "Test Size: " +  testArray);

                                if(test.size() == 32){
                                    window = movingAverageFilter(test, 256);
                                    test.remove(test.size() - 1);
                                    //Log.i(LOG_TAG, "Window: " +  Double.toString(window));
                                }
                                counter.add(window);
                                //Log.i(LOG_TAG, "Counter: " +  Double.toString(counter.size()));

                                if(counter.size() == 200){
                                    base_line = counter.get(199);
                                    sensorMsg(Double.toString(Math.round(base_line*1000)), "base");
                                    Log.i(LOG_TAG, "Base line: " + Double.toString(base_line));
                                    mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
                                }
                                if(counter.size() > 200 && Math.round((window - base_line)*1000) > 9){
                                    while(condition == true){
                                        if(reps == maxReps){
                                            sets++;
                                            reps = 0;
                                        }
                                        reps++;
                                        sensorMsg(Integer.toString(reps), "rep");
                                        sensorMsg(Integer.toString(sets), "set");
                                        condition = false;
                                        if(sets == maxSets + 1){
                                            latRaiseDone = true;
                                            activities[1] = 1;
                                            accelerometer.packedAcceleration().stop();
                                            accelerometer.stop();
                                            nextActivity(activities);
                                            return;
                                        }
                                    }
                                    //Log.i(LOG_TAG, "UP");
                                }
                                if(Math.round((window - base_line)*1000) <= 3){
                                    Log.i(LOG_TAG, "DOWN");
                                    condition = true;
                                }

                                sensorMsg(Double.toString(Math.round((window - base_line)*1000)), "sensor");
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

    public void curl(Vector3 accel){
        if(activities[2] == 0){
            accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        List<Double> counter = new ArrayList<>();
                        List<Double> test = new ArrayList<>();
                        ArrayList<Double> v = new ArrayList<>();
                        ArrayList<Double> repCount = new ArrayList<>();
                        Set<Double> set = new HashSet<>();
                        double highestTotAcc = 0;
                        int p = 0, o = 0;
                        double window = 0, base_line = 0;
                        int reps = 0, sets = 0, maxReps = Integer.parseInt(thisActivityData[2][0]), maxSets = Integer.parseInt(thisActivityData[2][1]);
                        boolean condition = true;
                        @Override
                        public void apply(Data data, Object... env) {
                            if(activities[2] == 0){
                                sensorMsg("Curl", "activity");
                                int buffer = 0;
                                String accel_entry = String.format("Accel, %s", data.value(Acceleration.class).toString().replaceAll("\\(","").replaceAll("\\)",""));
                                String csv_accel_entry = "Accel,"
                                        + String.valueOf(data.value(Acceleration.class).x()) + ","
                                        + String.valueOf(data.value(Acceleration.class).y()) + ","
                                        + String.valueOf(data.value(Acceleration.class).z());
                                csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                                x.add(accel.x());
                                y.add(accel.y());
                                z.add(accel.z());
                                double xData = data.value(Acceleration.class).x();
                                double yData = data.value(Acceleration.class).y();
                                double zData = data.value(Acceleration.class).z();
                                averageData = (xData + yData + zData) / 3;
                                //counter.add(averageData);
                                test.add(yData);
                                //double[] testArray = toDoubleArray(test);
                                //Log.i(LOG_TAG, "Test Size: " +  testArray);

                                if(test.size() == 32){
                                    window = movingAverageFilter(test, 256);
                                    test.remove(test.size() - 1);
                                    //Log.i(LOG_TAG, "Window: " +  Double.toString(window));
                                }
                                counter.add(window);
                                //Log.i(LOG_TAG, "Counter: " +  Double.toString(counter.size()));

                                if(counter.size() == 200){
                                    base_line = counter.get(199);
                                    sensorMsg(Double.toString(Math.round(base_line*1000)), "base");
                                    Log.i(LOG_TAG, "Base line: " + Double.toString(base_line));
                                    mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
                                }
                                if(counter.size() > 200 && Math.round((window - base_line)*1000) > 12){
                                    while(condition == true){
                                        if(reps == maxReps){
                                            sets++;
                                            reps = 0;
                                        }
                                        reps++;
                                        sensorMsg(Integer.toString(reps), "rep");
                                        sensorMsg(Integer.toString(sets), "set");
                                        condition = false;
                                        if(sets == maxSets + 1){
                                            latRaiseDone = true;
                                            activities[2] = 1;
                                            accelerometer.packedAcceleration().stop();
                                            accelerometer.stop();
                                            nextActivity(activities);
                                            return;
                                        }
                                    }
                                    //Log.i(LOG_TAG, "UP");
                                }
                                if(Math.round((window - base_line)*1000) <= 3){
                                    Log.i(LOG_TAG, "DOWN");
                                    condition = true;
                                }

                                sensorMsg(Double.toString(Math.round((window - base_line)*1000)), "sensor");
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

    public void press(Vector3 accel){
        if(activities[3] == 0){
            accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        List<Double> counter = new ArrayList<>();
                        List<Double> test = new ArrayList<>();
                        ArrayList<Double> v = new ArrayList<>();
                        ArrayList<Double> repCount = new ArrayList<>();
                        Set<Double> set = new HashSet<>();
                        double highestTotAcc = 0;
                        int p = 0, o = 0;
                        double window = 0, base_line = 0;
                        int reps = 0, sets = 0, maxReps = Integer.parseInt(thisActivityData[3][0]), maxSets = Integer.parseInt(thisActivityData[3][1]);
                        boolean condition = true;
                        @Override
                        public void apply(Data data, Object... env) {
                            if(activities[3] == 0){
                                sensorMsg("Press", "activity");
                                int buffer = 0;
                                String accel_entry = String.format("Accel, %s", data.value(Acceleration.class).toString().replaceAll("\\(","").replaceAll("\\)",""));
                                String csv_accel_entry = "Accel,"
                                        + String.valueOf(data.value(Acceleration.class).x()) + ","
                                        + String.valueOf(data.value(Acceleration.class).y()) + ","
                                        + String.valueOf(data.value(Acceleration.class).z());
                                csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                                x.add(accel.x());
                                y.add(accel.y());
                                z.add(accel.z());
                                double xData = data.value(Acceleration.class).x();
                                double yData = data.value(Acceleration.class).y();
                                double zData = data.value(Acceleration.class).z();
                                averageData = (xData + yData + zData) / 3;
                                //counter.add(averageData);
                                test.add(yData);
                                //double[] testArray = toDoubleArray(test);
                                //Log.i(LOG_TAG, "Test Size: " +  testArray);

                                if(test.size() == 32){
                                    window = movingAverageFilter(test, 256);
                                    test.remove(test.size() - 1);
                                    //Log.i(LOG_TAG, "Window: " +  Double.toString(window));
                                }
                                counter.add(window);
                                //Log.i(LOG_TAG, "Counter: " +  Double.toString(counter.size()));

                                if(counter.size() == 200){
                                    base_line = counter.get(199);
                                    sensorMsg(Double.toString(Math.round(base_line*1000)), "base");
                                    Log.i(LOG_TAG, "Base line: " + Double.toString(base_line));
                                    mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
                                }
                                if(counter.size() > 200 && Math.abs(Math.round((window - base_line)*1000)) > 12){
                                    while(condition == true){
                                        if(reps == maxReps){
                                            sets++;
                                            reps = 0;
                                        }
                                        reps++;
                                        sensorMsg(Integer.toString(reps), "rep");
                                        sensorMsg(Integer.toString(sets), "set");
                                        condition = false;
                                        if(sets == maxSets + 1){
                                            latRaiseDone = true;
                                            activities[3] = 1;
                                            accelerometer.packedAcceleration().stop();
                                            accelerometer.stop();
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

                                sensorMsg(Double.toString(Math.round((window - base_line)*1000)), "sensor");
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

    public void pushUp(Vector3 accel){
        if(activities[4] == 0){
            accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        List<Double> counter = new ArrayList<>();
                        List<Double> test = new ArrayList<>();
                        ArrayList<Double> v = new ArrayList<>();
                        ArrayList<Double> repCount = new ArrayList<>();
                        Set<Double> set = new HashSet<>();
                        double highestTotAcc = 0;
                        int p = 0, o = 0;
                        double window = 0, base_line = 0;
                        int reps = 0, sets = 0, maxReps = Integer.parseInt(thisActivityData[4][0]), maxSets = Integer.parseInt(thisActivityData[4][1]);
                        boolean condition = true;
                        @Override
                        public void apply(Data data, Object... env) {
                            if(activities[4] == 0){
                                sensorMsg("Push Ups", "activity");
                                int buffer = 0;
                                String accel_entry = String.format("Accel, %s", data.value(Acceleration.class).toString().replaceAll("\\(","").replaceAll("\\)",""));
                                String csv_accel_entry = "Accel,"
                                        + String.valueOf(data.value(Acceleration.class).x()) + ","
                                        + String.valueOf(data.value(Acceleration.class).y()) + ","
                                        + String.valueOf(data.value(Acceleration.class).z());
                                csv_accel_entry.replaceAll("\\(","").replaceAll("\\)","");
                                x.add(accel.x());
                                y.add(accel.y());
                                z.add(accel.z());
                                double xData = data.value(Acceleration.class).x();
                                double yData = data.value(Acceleration.class).y();
                                double zData = data.value(Acceleration.class).z();
                                averageData = (xData + yData + zData) / 3;
                                //counter.add(averageData);
                                test.add(yData);
                                //double[] testArray = toDoubleArray(test);
                                //Log.i(LOG_TAG, "Test Size: " +  testArray);

                                if(test.size() == 32){
                                    window = movingAverageFilter(test, 256);
                                    test.remove(test.size() - 1);
                                    //Log.i(LOG_TAG, "Window: " +  Double.toString(window));
                                }
                                counter.add(window);
                                //Log.i(LOG_TAG, "Counter: " +  Double.toString(counter.size()));

                                if(counter.size() == 200){
                                    base_line = counter.get(199);
                                    sensorMsg(Double.toString(Math.round(base_line*1000)), "base");
                                    Log.i(LOG_TAG, "Base line: " + Double.toString(base_line));
                                    mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
                                }
                                if(counter.size() > 200 && Math.abs(Math.round((window - base_line)*1000)) > 12){
                                    while(condition == true){
                                        if(reps == maxReps){
                                            sets++;
                                            reps = 0;
                                        }
                                        reps++;
                                        sensorMsg(Integer.toString(reps), "rep");
                                        sensorMsg(Integer.toString(sets), "set");
                                        condition = false;
                                        if(sets == maxSets + 1){
                                            latRaiseDone = true;
                                            activities[4] = 1;
                                            accelerometer.packedAcceleration().stop();
                                            accelerometer.stop();
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

                                sensorMsg(Double.toString(Math.round((window - base_line)*1000)), "sensor");
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
            latRaise(accel);
        }
        else if(!(activityData[1].isEmpty()) && activities[1] == 0){
            Log.i(LOG_TAG, "New Activity");
            triExt(accel);
        }
        else if(!(activityData[2].isEmpty()) && activities[2] == 0){
            Log.i(LOG_TAG, "New Activity");
            curl(accel);
        }
        else if(!(activityData[3].isEmpty()) && activities[3] == 0){
            Log.i(LOG_TAG, "New Activity");
            press(accel);
        }
        else if(!(activityData[4].isEmpty()) && activities[4] == 0){
            Log.i(LOG_TAG, "New Activity");
            pushUp(accel);
        }
        else
            Log.i(LOG_TAG, "All activities done!");
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
                    pushUp.setText("Base Line: " + reading);
                else if (sensor.equals("sensor"))
                    benchPress.setText("Sensor: " + reading);
                else if (sensor.equals("rep"))
                    curl.setText("Reps: " + reading);
                else if (sensor.equals("press"))
                    latRaise.setText("Press: " + reading);
                else if (sensor.equals("set"))
                    triExt.setText("Sets: " + reading);
                else if (sensor.equals("activity"))
                    activity.setText("Activity: " + reading);
            }
        });
    }

    public static float movingAverageFilter(List<Double> givenArray, int sample){
        float sum = 0;
        if(!givenArray.isEmpty()){
            for(double givenElement: givenArray){
                sum += givenElement;
            }
            return sum / givenArray.size();
        }
        return sum;
    }

    private static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}
