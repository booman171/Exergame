package com.mbientlab.metawear.tutorial.exergame;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGEegPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGRawMulti;

import java.util.Calendar;

public class MindWave extends AppCompatActivity {

    public String LOG_TAG = "serious";

    /* The objects related Mindwave */
    private TGDevice tgDevice;
    private TgStreamHandler tgStreamHandler = null;
    private TgStreamReader tgStreamReader = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private TGRawMulti tgRaw;
    public static double att = 0, med = 0, blink = 0, poor = 0;
    private Calendar t = Calendar.getInstance();
    private TextView alphaHigh, alphaLow, betaHigh, betaLow, gammaMid, gammaLow, theta, delta, signal;
    public static String mac2;
    public Boolean mindWaveConnected = false;


    private Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mind_wave);

        thisActivity = new Activity();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            tgDevice = new TGDevice(bluetoothAdapter, handler);
            //tgDevice.connect(true);
            //tgDevice.start();
        }
        connect();
        alphaHigh = findViewById(R.id.alphaHighView);
        alphaLow = findViewById(R.id.alphaLowView);
        betaHigh = findViewById(R.id.betaHighView);
        betaLow = findViewById(R.id.betaLowView);
        gammaMid = findViewById(R.id.gammaMidView);
        gammaLow = findViewById(R.id.gammaLowView);
        delta = findViewById(R.id.deltaView);
        theta = findViewById(R.id.thetaView);
        signal = findViewById(R.id.poorSignalView);

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
                    sensorMsg(Double.toString(poor), "signal");

                    break;
                case TGDevice.MSG_RAW_DATA:
                    //raw1 = msg.arg1;
                    //tv.append("Got raw: " + msg.arg1 + "\n");
                    //Log.i(LOG_TAG, "Raw: " + msg.arg1 + "\n");

                    break;
                case TGDevice.MSG_HEART_RATE:
                    Log.i(LOG_TAG, "Heart rate: " + msg.arg1 + "\n");
                    break;
                case TGDevice.MSG_EEG_POWER:
                    TGEegPower power = (TGEegPower)msg.obj;

                    sensorMsg(Integer.toString(power.highAlpha), "aHi");
                    sensorMsg(Integer.toString(power.lowAlpha), "aLo");
                    sensorMsg(Integer.toString(power.highBeta), "bHi");
                    sensorMsg(Integer.toString(power.lowBeta), "bLo");
                    sensorMsg(Integer.toString(power.delta), "delta");
                    sensorMsg(Integer.toString(power.theta), "theta");

                    //Log.i(LOG_TAG, "High Alpha: " + power.highAlpha + "\n");
                    //Log.i(LOG_TAG, "Low Alpha: " + power.lowAlpha + "\n");
                    //Log.i(LOG_TAG, "High Beta: " + power.highBeta + "\n");
                    //Log.i(LOG_TAG, "Low Beta: " + power.lowBeta + "\n");
                    //Log.i(LOG_TAG, "Mid Gamma: " + power.midGamma + "\n");
                    //Log.i(LOG_TAG, "Low Gamma: " + power.lowGamma + "\n");
                    //Log.i(LOG_TAG, "Theta: " + power.theta + "\n");
                    //Log.i(LOG_TAG, "Delta: " + power.delta + "\n");
                    break;
                case TGDevice.MSG_EKG_IDENTIFIED:
                    Log.i(LOG_TAG, "EKG: " + msg.arg1 + "\n");
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
                    Toast.makeText(MindWave.this, "Low battery!", Toast.LENGTH_SHORT).show();
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
            connect();
            //tv.setBackgroundColor(Color.argb(255, (int)att, 0, (int)med));
        }
    };


    boolean connect() {
        if (tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED) {
            tgDevice.connect(true);
            Toast.makeText(MindWave.this, "Connected!", Toast.LENGTH_SHORT).show();
            mindWaveConnected = true;
            return tgDevice.getState() != TGDevice.STATE_CONNECTED;
        }

        return false;
    }

    public void sensorMsg(String msg, final String sensor) {
        final String reading = msg;
        thisActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sensor.equals("aHi") && mindWaveConnected)
                    alphaHigh.setText("Alpha High: " + reading);
                else if (sensor.equals("aLo") && mindWaveConnected)
                    alphaLow.setText("Alpha Low: " + reading);
                else if (sensor.equals("bHi") && mindWaveConnected)
                    betaHigh.setText("Beta High: " + reading);
                else if (sensor.equals("bLo") && mindWaveConnected)
                    betaLow.setText("Beta Low: " + reading);
                else if (sensor.equals("gMid") && mindWaveConnected)
                    gammaMid.setText("Gamma Mid: " + reading);
                else if (sensor.equals("gLo") && mindWaveConnected)
                    gammaLow.setText("Gamma Low: " + reading);
                else if (sensor.equals("delta") && mindWaveConnected)
                    delta.setText("Delta: " + reading);
                else if (sensor.equals("theta") && mindWaveConnected)
                    theta.setText("Theta: " + reading);
                else if (sensor.equals("signal") && mindWaveConnected)
                    signal.setText("Signal: " + reading);
            }
        });
    }

}
