package com.mbientlab.metawear.tutorial.exergame;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.MagnetometerBmm150.Preset;

import bolts.Continuation;
import bolts.Task;


/** Class which listens for orientation change events and delivers callback events with orientation values.
 * Getting orientation values requires reading gravitational and magnetic field values and calling a bunch of SensorManager methods
 * with rotation matrices; this class handles all of that and just provides a callback method with azimuth, pitch, and roll values.
 */

public class OrientationListener implements SensorEventListener, ServiceConnection {
	public String LOG_TAG = "serious";
	private Accelerometer accelerometer, accelerometer2;
	private GyroBmi160 gyroscope, gyroscope2;
	private MagnetometerBmm150 magnetometer, magnetometer2;
	private Activity thisActivity;
	private Led ledModule, ledModule2;
	private SensorFusionBosch sensorFusion;
	private Switch switchModule;
	private Settings boardSettings;
	private Settings boardSettings2;
	private BtleService.LocalBinder serviceBinder;
	//private TensorflowClassifier classifier;// = new TensorflowClassifier(OrientationListener.this);




	private void configureChannel(Led.PatternEditor editor) {
		final short PULSE_WIDTH= 1000;
		editor.highIntensity((byte) 31).lowIntensity((byte) 31)
				.highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
				.repeatCount((byte) -1).commit();
	}

	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {

	}

	public static interface Delegate {
		/** Callback method for orientation updates. All values are in radians.
		 * @param azimuth rotation around the Z axis. 0=north, pi/2=east.
		 * @param pitch rotation around the X axis. 0=flat, negative=titled up, positive=tilted down.
		 * @param roll rotation around the Y axis. 0=flat, negative=tilted left, positive=tilted right.
		 */
		public void receivedOrientationValues(float azimuth, float pitch, float roll);
	}
	
	Context context;
	int rate;
	Delegate delegate;
	SensorManager sensorManager;
	int deviceRotation = Surface.ROTATION_0;
	
	/** Creates an OrientationListener with the given rate and callback delegate. Does not start listening for sensor events until start() is called.
	 * @param context Context object, typically either an Activity or getContext() from a View 
	 * @param rate constant from SensorManager, e.g. SensorManager.SENSOR_DELAY_GAME
	 * @param delegate callback object implementing the Delegate interface method.
	 */
	public OrientationListener(Context context, int rate, Delegate delegate) {
		this.context = context;
		this.rate = rate;
		this.delegate = delegate;
		sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	/** Sets the rotation of the screen from its natural orientation, so that pitch and roll values can be properly 
	 *  adjusted. For example, if the natural orientation of a tablet is landscape but it's being used in portrait mode,
	 *  the rotation value should be Surface.ROTATION_90 or Surface.ROTATION_270.
	 *  @see //http://android-developers.blogspot.com/2010/09/one-screen-turn-deserves-another.html
	 *  @param rotation one of Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, or Surface.ROTATION_270.
	 */
	public void setDeviceRotation(int rotation) {
		this.deviceRotation = rotation;
	}
	
	/** Starts listening for sensor events and making callbacks to the delegate.
	 */
	public void start() {
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), rate);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), rate);
	}

	/** Stops listening for sensor events and making callbacks to the delegate.
	 */
	public void stop() {
		sensorManager.unregisterListener(this);
	}


	// values used to compute orientation based on gravitational and magnetic fields
	float[] R = new float[16];
	float[] I = new float[16];
	// initialize magnetic field with dummy values so we can get pitch and roll even when there's no compass
	float[] mags = new float[] {1, 0, 0};
	float[] accels = null;

	//float[] orientationValues = {0f, 0f, 0f};
	float[] orientationValues = DodgeMain.orientationValues;



	/** SensorEventListener method called when sensor values are updated. Reads gravitational and magnetic field information, and when both are available
	 * computes the orientation values and calls the delegate with them.
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		//mags = DodgeMain.mags;
		accels = DodgeMain.accels;



		switch(event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			mags = event.values.clone();
			//mags = DodgeMain.mags;
			break;
		case Sensor.TYPE_ACCELEROMETER:
			//accels = event.values.clone();
			//accels = DodgeMain.accels;
			//Log.i("serious", "Phone: " + Arrays.toString(accels));
			//Log.i("serious", "Wearable: " + Float.toString(xData) + ", " + Float.toString(yData) + ", " + Float.toString(zData));
			break;
		}

		if(DeviceSetupActivityFragment.mac != null && DeviceSetupActivityFragment2.mac2 != null)
			Log.i("serious", "Wearable Accel: " + Float.toString(Math.round(accels[0])) + ", " + Float.toString(Math.round(accels[1])) + ", " + Float.toString(Math.round(accels[2])));

		//Log.i(LOG_TAG,"xData: " + Float.toString(DodgeMain.xData));
		//Log.i("serious", "Wearable Accel: " + Float.toString(Math.round(DodgeMain.xData2)) + ", " + Float.toString(Math.round(DodgeMain.yData2)) + ", " + Float.toString(Math.round(DodgeMain.zData2)));
		//Log.i(LOG_TAG, "poor: " + Double.toString(DodgeMain.poor) + ", blink: " + Double.toString(DodgeMain.blink) + "med: " + Double.toString(DodgeMain.med) + "att: " + Double.toString(DodgeMain.med));
        //Log.i("serious", "Wearable Mag: " + Float.toString(Math.round(mags[0])) + ", " + Float.toString(Math.round(mags[1])) + ", " + Float.toString(Math.round(mags[2])));
		//Log.i("serious", "Wearable Gyro: " + Float.toString(DodgeMain.gXData) + ", " + Float.toString(DodgeMain.gYData) + ", " + Float.toString(DodgeMain.gZData));
		//Log.i("serious", "Wearable Magneto: " + Float.toString(DodgeMain.mXData) + ", " + Float.toString(DodgeMain.mYData) + ", " + Float.toString(DodgeMain.mZData));
		//Log.i("serious", "Phone accels: " + Arrays.toString(accels));
		//Log.i("serious", "Wearable accels: " + Arrays.toString(DodgeMain.accels));
		//Log.i("serious", "Left: " + Float.toString(Math.round(DodgeMain.xData2-10)) + ", Right: " + Float.toString(Math.round(DodgeMain.zData)));
		//Log.i("serious", "Phone mags: " + Arrays.toString(mags));
		//Log.i("serious", "Wearable mags: " + Arrays.toString(DodgeMain.mags));

		if (mags!=null && accels!=null) {
			SensorManager.getRotationMatrix(R, I, accels, mags);
			SensorManager.getOrientation(R, orientationValues);
			
			// adjust for device rotation if needed
			float pitch = orientationValues[1];
			float roll = orientationValues[2];
			switch (this.deviceRotation) {
				case Surface.ROTATION_90:
					float tmp1 = pitch;
					pitch = roll;
					roll = -tmp1;
					break;
				case Surface.ROTATION_180:
					pitch = -pitch;
					roll = -roll;
					break;
				case Surface.ROTATION_270:
					float tmp2 = pitch;
					pitch = -roll;
					roll = tmp2;
					break;
			}
					
			delegate.receivedOrientationValues(orientationValues[0], pitch, roll);
		}
	}

	// ignored SensorListener method
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private float[] toFloatArray(@NonNull List<Float> list) {
		int i = 0;
		float[] array = new float[list.size()];

		for (Float f : list) {
			array[i++] = (f != null ? f : Float.NaN);
		}
		return array;
	}
}
