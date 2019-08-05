/*
 * Code taken from http://www.edumobile.org/android/touch-rotate-example-in-android/
 */
package com.mbientlab.tutorial.sensorfusion;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SensorFusionBosch.*;
import com.mbientlab.metawear.module.Settings;

import bolts.Continuation;
import bolts.Task;

public class CubeActivity extends AppCompatActivity implements ServiceConnection {
    public final static String EXTRA_BT_DEVICE= "com.mbientlab.tutorial.sensorfusion.CubeActivity.EXTRA_BT_DEVICE";

    private BluetoothDevice btDevice;
    private MetaWearBoard board;

    @Override
    public void onBackPressed() {
        board.getModule(SensorFusionBosch.class).stop();
        board.getModule(Settings.class).editBleConnParams()
                .maxConnectionInterval(125f)
                .commit();
        board.getModule(Debug.class).disconnectAsync();

        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new CubeSurfaceView(this);
        setContentView(mGLSurfaceView);
        mGLSurfaceView.requestFocus();
        mGLSurfaceView.setFocusableInTouchMode(true);

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    private CubeSurfaceView mGLSurfaceView;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BtleService.LocalBinder binder = (BtleService.LocalBinder) service;
        board = binder.getMetaWearBoard(btDevice);

        final SensorFusionBosch sensorFusion = board.getModule(SensorFusionBosch.class);
        sensorFusion.configure()
                .mode(Mode.NDOF)
                .accRange(AccRange.AR_2G)
                .gyroRange(GyroRange.GR_250DPS)
                .commit();
        sensorFusion.quaternion().addRouteAsync(source -> source.limit(33).stream((data, env) -> {
            mGLSurfaceView.updateRotation(data.value(Quaternion.class));
        })).continueWith((Continuation<Route, Void>) ignored -> {
            sensorFusion.quaternion().start();
            sensorFusion.start();
            return null;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
