package com.mbientlab.metawear.tutorial.exergame;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.tutorial.exergame.model.Field;
import com.mbientlab.metawear.tutorial.exergame.model.LevelManager;
import com.mbientlab.metawear.module.MagnetometerBmm150.Preset;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.EEGPower;
import com.neurosky.thinkgear.TGEegPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGRawMulti;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

public class DodgeMain extends Activity implements Field.Delegate, ServiceConnection {
	
	Field field;
	LevelManager levelManager;
	
	FieldView fieldView;
	public static View menuView;
	TextView levelText;
	TextView livesText;
	TextView signalText;
	TextView medText;
	TextView statusText;
	TextView bestLevelText;
	TextView bestFreePlayLevelText;
	Button continueFreePlayButton;
	View bestFreePlayLevelView;
	View bestLevelView;
	MenuItem endGameMenuItem;
	MenuItem selectBackgroundImageMenuItem;
	MenuItem preferencesMenuItem;
	
	private static final int ACTIVITY_PREFERENCES = 1;
	
	Handler messageHandler = new Handler() {
        public void handleMessage(Message m) {
            processMessage(m);
        }


    };
		
	int lives = 9;

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
	public static float gXData;
	public static float gYData;
	public static float gZData;
	public static float xData;
	public static float yData;
	public static float zData;
	public static float xData2;
	public static float yData2;
	public static float zData2;
	public static float mXData;
	public static float mYData;
	public static float mZData;
	public static float mXData2;
	public static float mYData2;
	public static float mZData2;
	public static float[] mags = new float[] {1, 0, 0};
	public static float[] accels = new float[] {1, 0, 0};
	public static float[] orientationValues = new float[] {1, 0, 0};
	private List<Float> baseX; //pitch
	private List<Float> baseY; //pitch
	private List<Float> baseZ; //pitch
	private List<Float> baseX2; //pitch
	private List<Float> baseY2; //pitch
	private List<Float> baseZ2; //pitch
	
	private float base_line_X;
	private float base_line_Y;
	private float base_line_Z;
	private float base_line_X2;
	private float base_line_Y2;
	private float base_line_Z2;

	/* The objects related Mindwave */
	TGDevice tgDevice;
	private TgStreamHandler tgStreamHandler = null;
	private TgStreamReader tgStreamReader = null;
	private BluetoothAdapter bluetoothAdapter = null;
	TGRawMulti tgRaw;
	public static double att = 0, med = 0, blink = 0, poor = 0, alphaHi = 0, alphaLo = 0, betaLo = 0, betaHi = 0;
	Calendar t = Calendar.getInstance();
	TextView signalView, blinkView, attView, medView;
	public static String mac2;
	public Boolean mindWaveConnected = false;

	private BtleService.LocalBinder serviceBinder;
	boolean isHit = false;
	public static int numHits = 0;
	Button aboutButton;
	public static long startTime = System.currentTimeMillis();
    public static boolean startGame = false;

    private String CSV_HEADER = String.format("time,ctrl x,accel x,accel y,accel z,exercise,med,base");
    private String filename = "SessionData" + System.currentTimeMillis() + ".csv";
	private String filenameMetrics = "SessionMetrics" + System.currentTimeMillis() + ".csv";

	private File folderSessionData = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/exergaming/Session");
    File filepath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/exergaming/session", filename);
    File filepath2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/exergaming/session", filenameMetrics);

    public static long enterNeutral;
    public static boolean enteredNeutral;
    public static long timeInNeutral;
    public static long exitNeutral;
    public static double restTime;

    public static TensorflowClassifier classifier;
    public static float[] results = new float[19], results2 = new float[19];
    public static float[] data_array, data_array2;

	public static float xAccel, yAccel, zAccel;
	public static int medBase = 80;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission already Granted.", Toast.LENGTH_SHORT).show();}
        else{
            requestStoragePermission();
        }
        levelManager = new LevelManager();
        field = new Field();
        field.setDelegate(this);
        field.setLevelManager(levelManager);
        field.setMaxBullets(levelManager.numberOfBulletsForCurrentLevel());
        
        levelText = findViewById(R.id.levelText);
        livesText = findViewById(R.id.livesText);
        statusText = findViewById(R.id.statusText);
        signalText = findViewById(R.id.signalText);
        medText = findViewById(R.id.medText);
        // uncomment to clear high scores
        /*
        setBestLevel(true, 0);
        setBestLevel(false, 0);
        */
		classifier = new TensorflowClassifier(this);
        bestLevelText = findViewById(R.id.bestLevelText);
        bestFreePlayLevelText = findViewById(R.id.bestFreePlayLevelText);
        continueFreePlayButton = findViewById(R.id.continueFreePlayButton);
        bestLevelView = findViewById(R.id.bestLevelView);
        bestFreePlayLevelView = findViewById(R.id.bestFreePlayLevelView);

        Button newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(mindWaveConnected){
                    startGame = true;
                    doNewGame();
                }
            	else
					Toast.makeText(DodgeMain.this, "Connect to MindWave!", Toast.LENGTH_SHORT).show();
			}
        });
        
        Button freePlayButton = findViewById(R.id.freePlayButton);
        freePlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				if(mindWaveConnected){
                    doFreePlay(1);
                    startGame = true;
                }
				else
					Toast.makeText(DodgeMain.this, "Connect to MindWave!", Toast.LENGTH_SHORT).show();
            }
        });
        
        continueFreePlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				if(mindWaveConnected == true)
					doFreePlay(bestLevel(true));
				else
					Toast.makeText(DodgeMain.this, "Connect to MindWave!", Toast.LENGTH_SHORT).show();
            }
        });

        Button conectMindWave = findViewById(R.id.connectMindWave);
        conectMindWave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				connect();
			}
		});
        
        aboutButton = (Button)findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAbout();
            }
        });

        menuView = findViewById(R.id.menuView);
        updateBestLevelFields();
        menuView.requestFocus();
                
        fieldView = (FieldView)findViewById(R.id.fieldView);
        fieldView.setField(field);
        fieldView.setMessageHandler(messageHandler);
        
        updateFromPreferences();
        baseX = new ArrayList<Float>();
		baseY = new ArrayList<Float>();
		baseZ = new ArrayList<Float>();

		baseX2 = new ArrayList<Float>();
		baseY2 = new ArrayList<Float>();
		baseZ2 = new ArrayList<Float>();

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter != null) {
			tgDevice = new TGDevice(bluetoothAdapter, handler);
			//tgDevice.connect(true);
			//tgDevice.start();
		}
		connect();
		tgRaw = new TGRawMulti();
		startActivity();
    }

    private void requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){

            new AlertDialog.Builder(this.getApplicationContext())
                    .setTitle("Permission Needed")
                    .setMessage("Permission need for thsi and that")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    //@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted.", Toast.LENGTH_SHORT).show();}
            else {
                Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show();}
        }


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
							//Log.i(LOG_TAG, "Connecting...\n");
							break;
						case TGDevice.STATE_CONNECTED:
							Log.i(LOG_TAG, "Connected.\n");
							tgDevice.start();
							break;
						case TGDevice.STATE_NOT_FOUND:
							//Log.i(LOG_TAG, "Can't find\n");
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
					//Log.i(LOG_TAG, "Raw: " + msg.arg1 + "\n");

					break;
				case TGDevice.MSG_HEART_RATE:
					Log.i(LOG_TAG, "Heart rate: " + msg.arg1 + "\n");
					break;
				case TGDevice.MSG_EEG_POWER:
					TGEegPower power = (TGEegPower)msg.obj;

					alphaHi = power.highAlpha;
					alphaLo = power.lowAlpha;
					betaHi = power.highBeta;
					betaLo = power.lowBeta;

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
					Toast.makeText(DodgeMain.this, "Low battery!", Toast.LENGTH_SHORT).show();
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
    
    @Override
    public void onPause() {
    	super.onPause();
    	fieldView.stop();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
		if (tgDevice != null)
			checkForBluetooth();
    	fieldView.start();
    }

    @Override
	public void onDestroy(){
    	super.onDestroy();
		if (tgDevice != null)
			tgDevice.close();
	}

	boolean connect() {
		if (tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED) {
			tgDevice.connect(true);
			//Toast.makeText(DodgeMain.this, "Connected!", Toast.LENGTH_SHORT).show();
			mindWaveConnected = true;
			return tgDevice.getState() != TGDevice.STATE_CONNECTED;
		}

		return false;
	}

	private void checkForBluetooth() {
		if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, 100);
		}
	}

    /** Called when preferences activity completes, updates background image and bullet flash settings.
     */
    void updateFromPreferences() {

		Log.i(LOG_TAG,"this and that");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	boolean flash = prefs.getBoolean(DodgePreferences.FLASHING_COLORS_KEY, false);
    	fieldView.setFlashingBullets(flash);
    	
    	boolean tilt = prefs.getBoolean(DodgePreferences.TILT_CONTROL_KEY, true);
    	fieldView.setTiltControlEnabled(tilt);
    	
    	boolean showFPS = prefs.getBoolean(DodgePreferences.SHOW_FPS_KEY, false);
    	fieldView.setShowFPS(showFPS);

    	Bitmap backgroundBitmap = null;
    	String backgroundImage = prefs.getString(DodgePreferences.IMAGE_URI_KEY, null);
    	if (prefs.getBoolean("useBackgroundImage",false)==true) {
        	if (backgroundImage!=null && prefs.getBoolean(DodgePreferences.USE_BACKGROUND_KEY,false)==true) {
        		try {
        			Uri imageURI = Uri.parse(backgroundImage);
        	    	// Load image scaled to the screen size (the FieldView size would be better but it's not available in onCreate)
        			WindowManager windowManager = (WindowManager)this.getSystemService(Context.WINDOW_SERVICE);
        			Display display = windowManager.getDefaultDisplay();
        			
        			backgroundBitmap = AndroidUtils.scaledBitmapFromURIWithMinimumSize(this, imageURI, display.getWidth(), display.getHeight());
        			fieldView.setBackgroundBitmap(backgroundBitmap);
        		}
        		catch(Throwable ex) {
        			// we shouldn't get out of memory errors, but it's possible with weird images
        			backgroundBitmap = null;
        		}
        	}
    	}
    	else {
    		fieldView.setBackgroundBitmap(null);
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	endGameMenuItem = menu.add(R.string.end_game);
    	preferencesMenuItem = menu.add(R.string.preferences);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item==endGameMenuItem) {
    		doGameOver();
    	}
    	else if (item==preferencesMenuItem) {
    		Intent settingsActivity = new Intent(getBaseContext(), DodgePreferences.class);
    		startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
    	}
    	return true;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent); 

        switch(requestCode) { 
            case ACTIVITY_PREFERENCES:
            	updateFromPreferences();
            	break;
        }
    }
    
    boolean inFreePlay() {
    	return (lives<0);
    }
    
    // methods to store and retrieve the highest normal and free play levels reached, using SharedPreferences
    int bestLevel(boolean freePlay) {
    	String key = (freePlay) ? "BestFreePlayLevel" : "BestLevel";
    	return getPreferences(MODE_PRIVATE).getInt(key, 0);
    }

    void setBestLevel(boolean freePlay, int val) {
       	String key = (freePlay) ? "BestFreePlayLevel" : "BestLevel";
       	SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
       	editor.putInt(key, val);
       	editor.commit();
    }
    
    void recordCurrentLevel() {
    	if (levelManager.getCurrentLevel() > bestLevel(inFreePlay())) {
    		setBestLevel(inFreePlay(), levelManager.getCurrentLevel());
    	}
    }

    void updateBestLevelFields() {
    	int bestNormal = bestLevel(false);
    	bestLevelText.setText((bestNormal>1) ? String.valueOf(bestNormal) : getString(R.string.score_none));
    	
    	int bestFree = bestLevel(true);
    	bestFreePlayLevelText.setText((bestFree>1) ? String.valueOf(bestFree) : getString(R.string.score_none));
		continueFreePlayButton.setEnabled(bestFree>1);
    	menuView.forceLayout();
    }

    void processMessage(Message m) {
    	String action = m.getData().getString("event");
    	if ("goal".equals(action)) {
    		levelManager.setCurrentLevel(1 + levelManager.getCurrentLevel());
    		recordCurrentLevel();
        	synchronized(field) {
        		field.setMaxBullets(levelManager.numberOfBulletsForCurrentLevel());
        	}
        	updateScore();
    	}
    	else if ("death".equals(action)) {
    		if (lives>0) lives--;
    		synchronized(field) {
        		if (lives==0) {
        			field.removeDodger();
        			doGameOver();
        		}
        		else {
        			fieldView.startDeathAnimation(field.getDodger().getPosition());
        			field.createDodger();
        		}
    		}
    		updateScore();
    	}
    }
    
    void updateScore() {
		levelText.setText(getString(R.string.level_prefix) + levelManager.getCurrentLevel());
		livesText.setText(getString(R.string.lives_prefix)+((lives>=0) ? ""+lives : getString(R.string.free_play_lives)));
    }
    
    void doGameOver() {
    	field.removeDodger();
    	statusText.setText(getString(R.string.game_over_message));
    	updateBestLevelFields();
    	menuView.setVisibility(View.VISIBLE);
    }
    
    void startGameAtLevelWithLives(int startLevel, int numLives) {
    	levelManager.setCurrentLevel(startLevel);
    	field.setMaxBullets(levelManager.numberOfBulletsForCurrentLevel());
    	field.createDodger();
    	menuView.setVisibility(View.INVISIBLE);
    	lives = numLives;
    	updateScore();
    }
    
    void doNewGame() {
    	startGameAtLevelWithLives(1, 9);
    }

    void doFreePlay(int startLevel) {
    	startGameAtLevelWithLives(startLevel, -1);
		Log.i(LOG_TAG,"ghfhtgbfgbfgfgbfgbngnh");

	}
    
    void doAbout() {
    	Intent aboutIntent = new Intent(this, DodgeAbout.class);
    	this.startActivity(aboutIntent);
    }

    void sendMessage(Map params) {
    	Bundle b = new Bundle();
    	for(Object key : params.keySet()) {
    		b.putString((String)key, (String)params.get(key));
    	}
    	Message m = messageHandler.obtainMessage();
    	m.setData(b);
    	messageHandler.sendMessage(m);
    }
    
    // Field.Delegate methods
    // these occur in a separate thread, so use Handler
	public void dodgerHitByBullet(Field theField) {
		Map params = new HashMap();

		if (numHits <= 3) {
			DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startBuzzer((short) 100);
			//DeviceSetupActivityFragment2.mwBoard2.getModule(Haptic.class).startBuzzer((short) 100);
			numHits++;
		}
		else{
			params.put("event", "death");
			sendMessage(params);
			DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startBuzzer((short) 100);
			DeviceSetupActivityFragment2.mwBoard2.getModule(Haptic.class).startBuzzer((short) 100);
			Dodge.gameSurface.pause();
		}
		if (numHits == 3){
			DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
			//DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startMotor(50.f, (short) 100);
			//DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startBuzzer((short) 100);
			//DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startMotor(50.f, (short) 100);
			//DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startBuzzer((short) 100);

		}
	}

	public void dodgerReachedGoal(Field theField) {
		Map params = new HashMap();
		params.put("event", "goal");
		sendMessage(params);

	}

	public  void startActivity(){
    	Log.i(LOG_TAG, "Thisss");
		thisActivity = new Activity();

		//Create Folder
        folderSessionData.mkdirs();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		ledModule = DeviceSetupActivityFragment.mwBoard.getModule(Led.class);
		switchModule = DeviceSetupActivityFragment.mwBoard.getModule(Switch.class);
		accelerometer= DeviceSetupActivityFragment.mwBoard.getModule(Accelerometer.class);
		gyroscope = DeviceSetupActivityFragment.mwBoard.getModule(GyroBmi160.class);
		magnetometer = DeviceSetupActivityFragment.mwBoard.getModule(MagnetometerBmm150.class);
		boardSettings = DeviceSetupActivityFragment.mwBoard.getModule(Settings.class);
		accelerometer.configure()
				.odr(50f)       // Set sampling frequency to 25Hz, or closest valid ODR
				.range(1f)      // Set data range to +/-4g, or closet valid range
				.commit();
		magnetometer.usePreset(Preset.REGULAR);
		configureChannel(ledModule.editPattern(Led.Color.BLUE));
		ledModule.play();
        OutputStream out;
        try{
            out = new BufferedOutputStream(new FileOutputStream(filepath, true));
            out.write(CSV_HEADER.getBytes());
            out.write("\n".getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
		accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
			@Override
			public void configure(RouteComponent source) {
				source.stream(new Subscriber() {
                    protected LinkedList<Float> P = new LinkedList<Float>();
                    boolean gotBase = false;
                    float base_line;
                    int distance;
                    ; //pitch
					String metricsEntry;
                    @SuppressLint("SetTextI18n")
					@Override
					public void apply(Data data, Object... env) {
						if(startGame){
							xData = data.value(Acceleration.class).x()*10;
							yData = data.value(Acceleration.class).y()*10;
							zData = data.value(Acceleration.class).z()*10;

							xAccel = data.value(Acceleration.class).x();
							yAccel = data.value(Acceleration.class).y();
							zAccel = data.value(Acceleration.class).z();

							accels[0] = xData;
							accels[1] = 0;
							accels[2] = zData;

							if(mindWaveConnected){
								sensorMsg(Double.toString(poor), "signal");
								sensorMsg(Double.toString(med), "med");
							}

							if(Field.inNeutralZone){
								enterNeutral = System.currentTimeMillis();
								enteredNeutral = true;
							}

							if(Field.inNeutralZone == false && enteredNeutral){
								exitNeutral = System.currentTimeMillis();
								timeInNeutral = exitNeutral - enterNeutral;
								restTime = timeInNeutral / 1000.0;
								//Log.i(LOG_TAG, "Rest time: " + Double.toString(restTime));
								metricsEntry = Double.toString(restTime);
								enteredNeutral = false;
							}
							String csv_entry = System.currentTimeMillis() +
									", " + Math.round(xData) +
                                    ", " + xData +
									", " + yData +
									", " + zData +
									", " + FieldView.currentExercise +
									", " + med +
									", " + Integer.toString(medBase);

                            //Log.e(LOG_TAG, Integer.toString(x.size()));


							OutputStream out;
							try {
								out = new BufferedOutputStream(new FileOutputStream(filepath, true));
								out.write(csv_entry.getBytes());
								out.write("\n".getBytes());
								out.close();
							} catch (Exception e) {
								Log.e(LOG_TAG, "CSV creation error", e);
							}

							OutputStream out2;
							try {
								out2 = new BufferedOutputStream(new FileOutputStream(filepath2, true));
								out2.write(metricsEntry.getBytes());
								out2.write("\n".getBytes());
								out2.close();
							} catch (Exception e) {
								Log.e(LOG_TAG, "CSV creation error", e);
							}
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

	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
		serviceBinder = (BtleService.LocalBinder) iBinder;
		//signalText = findViewById(R.id.signalView);
		//attText = findViewById(R.id.attView);
		connect();
		Log.i(LOG_TAG,"TRU");
		startActivity();
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {

	}

	private void configureChannel(Led.PatternEditor editor) {
		final short PULSE_WIDTH= 1000;
		editor.highIntensity((byte) 31).lowIntensity((byte) 31)
				.highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
				.repeatCount((byte) -1).commit();
	}

	public void sensorMsg(String msg, final String sensor) {
		final String reading = msg;
		thisActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (sensor.equals("signal") && mindWaveConnected){
					//signalText = findViewById(R.id.signalView);
					signalText.setText("Signal: " + reading);
				}
                else if (sensor.equals("med") && mindWaveConnected){
                    //attText = findViewById(R.id.attView);
                   medText.setText("Med: " + reading);
                }
			}
		});
	}

	public void startExercise(){

	}

	public static void resetTime(){
    	startTime = System.currentTimeMillis();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		startGame = false;
		Intent intent = new Intent(DodgeMain.this, DodgeMain.class);
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

    public int indexOfLargest(float[] results){
		int maxAt = 0;
		for (int i = 0; i < results.length; i++) {
			maxAt = results[i] > results[maxAt] ? i : maxAt;
		}
		return maxAt;
	}
}