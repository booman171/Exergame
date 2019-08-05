package com.mbientlab.metawear.tutorial.exergame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.tutorial.exergame.model.Bullet;
import com.mbientlab.metawear.tutorial.exergame.model.Field;
import com.mbientlab.metawear.tutorial.exergame.model.LevelManager;
import com.mbientlab.metawear.tutorial.exergame.model.Vec2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** View which draws the sprites for the player and bullets, the goal areas, and the background image.
 * Extends SurfaceView for maximum performance and runs in a separate thread. The thread updates the Field
 * model objet by calling the tick method, and then draws to the view.
 * @author brian
 *
 */

public class FieldView extends SurfaceView implements SurfaceHolder.Callback {

	SurfaceHolder surfaceHolder;
	Handler messageHandler;

	boolean running = false;
	boolean canDraw = false;
	boolean setupField = false;
	Thread gameThread;
	Field field;
	
	Vec2 dodgerDeathPosition;
	int dodgerDeathFrame;
	static int DODGER_DEATH_FRAMES = 15;
	
	Paint blackPaint;
	Paint startAreaPaint;
	Paint endAreaPaint;
	Paint dodgerPaint;
	RectF bounds;
	
	Bitmap backgroundBitmap;
	Bitmap unscaledBackgroundBitmap;
	Bitmap current;
	Bitmap scaledBitmap;
	Bitmap enemy, scaledEnemy;

	boolean flashingBullets = false;
	boolean tiltControlEnabled = true;
	boolean showFPS = false;
	
	OrientationListener orientationListener;

	// when user moves by tilting the device, disable screen sleep so it doesn't go off while playing
	boolean displaySleepDisabled = false;
	long displaySleepDisableTime;
	boolean enableDisplaySleepScheduled = false; // set to true when draw thread has sent a message to re-enable sleep, so we only do it once
	static long TILT_DISPLAY_ON_MILLIS = 30000; // disable display sleep for this long after move via tilting
	
	FrameRateManager frameRateManager = new FrameRateManager(new double[] {60, 50, 40, 30}, new double[] {58, 48, 38});
	String debugText = null;

	
	// 10 and 5 pixels on Droid/Nexus with width=480, scaled down as needed for smaller displays
	static float DODGER_SCALE = 10f/480;
	static float BULLET_SCALE = 5f/480;
	public AlertDialog dialog;

	int imageSize = 35;
	int index = 0;
	String playerShip = "";
	String[] ship = {"playership1_blue_3ring", "playership1_blue_2ring", "playership1_blue_1ring", "playership1_blue_0ring"};
	public static int screenWidth = 0;

	public static String currentExercise = "rest";

	public  ArrayList<Float> x = new ArrayList<Float>();
	public  ArrayList<Float> y = new ArrayList<Float>();
	public  ArrayList<Float> z = new ArrayList<Float>();
	public int max;

	public FieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		surfaceHolder = this.getHolder();
		surfaceHolder.addCallback(this);
		
		blackPaint = new Paint();
		blackPaint.setARGB(255, 0, 0, 0);
		startAreaPaint = new Paint();
		startAreaPaint.setARGB(128, 255, 0, 0);
		endAreaPaint = new Paint();
		endAreaPaint.setARGB(128, 0, 255, 0);
		dodgerPaint = new Paint();
		dodgerPaint.setARGB(255, 0, 0, 255);
		dodgerPaint.setAntiAlias(true);

		enemy = BitmapFactory.decodeResource(getResources(),R.drawable.meteorbrown_med1);

		//current = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_3ring);
        scaledEnemy = scaleDown(enemy, 100, true);

		setFocusable(true);
	}

	public void setField(Field value) {
		field = value;
		setupField = false;
	}



	public void setMessageHandler(Handler value) {
		messageHandler = value;
	}
	
	/** Sets the background image of the game field. The given Bitmap will be scaled and cropped as needed so
	 * that it exactly fits the view size. Equal portions of the top and bottom of the image will be cropped
	 * if the bitmap's aspect ratio is taller than the view's, and the left and right will be cropped if the 
	 * aspect ratio is wider than the view's. A null argument will remove any existing background image.
	 */
	public synchronized void setBackgroundBitmap(Bitmap value) {
		if (value==null) {
			backgroundBitmap = null;
		}
		else {
			// scale to width/height of this view
			int vw = this.getWidth();
			int vh = this.getHeight();
			if (vw<=0 || vh<=0) {
				// view isn't set up yet, this can happen on startup.
				// store the original bitmap and call this method again from the surfaceCreated callback
				unscaledBackgroundBitmap = value;
				return;
			}
			
			unscaledBackgroundBitmap = null;
			int bw = value.getWidth();
			int bh = value.getHeight();
			if (vw<=0 || vh<=0 || bw<=0 || bh<=0) {
				backgroundBitmap = null; // shouldn't happen
				return;
			}
			double imageRatio = 1.0*bw/bh;
			double viewRatio  = 1.0*vw/vh;
			
			Rect srcRect = new Rect(0, 0, bw, bh); // x,y,width,height
			Rect dstRect = new Rect(0, 0, vw, vh);
			if (imageRatio>viewRatio) {
				// image is too wide, crop from sides
				int croppedWidth = (int)(bh * viewRatio);
				srcRect.left = bw/2 - croppedWidth/2;
				srcRect.right = srcRect.left + croppedWidth;
			}
			else if (imageRatio<viewRatio) {
				// image is too tall, crop from top/bottom
				int croppedHeight = (int)(bw / viewRatio);
				srcRect.top = bw/2 - croppedHeight/2;
				srcRect.bottom = srcRect.top + croppedHeight;
			}
			Bitmap newBitmap = Bitmap.createBitmap(vw, vh, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(newBitmap);
			c.drawBitmap(value, srcRect, dstRect, blackPaint);
			backgroundBitmap = newBitmap;
		}
	}
	
	public void setFlashingBullets(boolean value) {
		flashingBullets = value;
	}
	public void setShowFPS(boolean value) {
		showFPS = value;
	}
	
	public void setTiltControlEnabled(boolean value) {
		if (tiltControlEnabled!=value) {
			tiltControlEnabled = value;
			if (tiltControlEnabled) {
				if (running) {
					startOrientationListener();
				}
				else {
					stopOrientationListener();
				}
			}
		}
	}
	
	/** Starts the game thread which continuously updates the state of the objects and draws to the view.
	 */
	public void start() {
		running = true;
		gameThread = new Thread() {
			public void run() {
				threadMain();
			}
		};
		gameThread.start();
		
		if (tiltControlEnabled) {
			startOrientationListener();
		}
	}
	
	/** Stops the game thread, which will pause updates to the game state and view redraws.
	 */
	public void stop() {
		running = false;
		stopOrientationListener();
		try {
			gameThread.join();
		}
		catch(InterruptedException ex) {}
		frameRateManager.clearTimestamps();
	}
	
	void startOrientationListener() {
		setTiltLocked(false);
		if (orientationListener==null) {
			orientationListener = new OrientationListener(this.getContext(), SensorManager.SENSOR_DELAY_GAME, new OrientationListener.Delegate() {
				public void receivedOrientationValues(float azimuth, float pitch, float roll) {
					handleOrientationUpdate(azimuth, pitch, roll);
				}
			});
			orientationListener.setDeviceRotation(AndroidUtils.getDeviceRotation(this.getContext()));
			orientationListener.start();
		}
	}
	
	void stopOrientationListener() {
		if (orientationListener!=null) {
			orientationListener.stop();
			orientationListener = null;
		}
	}
	
	
	/** Called to start the death animation when the player is   by a bullet.
	 */
	public void startDeathAnimation(Vec2 position) {
		dodgerDeathPosition = position;
		dodgerDeathFrame = 0;
	}
	
	public boolean isDeathAnimationRunning() {
		return dodgerDeathPosition!=null;
	}
	
	/** Main loop for the game thread. Calls Field.tick to update the states of all game objects, then calls
	 * drawField to redraw the view.
	 */
	void threadMain() {
		frameRateManager.setAllowLockingFrameRate(false);
		while(running) {
			double dt = 1/frameRateManager.targetFramesPerSecond();
			
			frameRateManager.frameStarted();
			if (frameRateManager.getTotalFrames() % 50 == 0) {
				debugText = (showFPS) ? frameRateManager.fpsDebugInfo() : null; 
			}
				
			if (field!=null && canDraw) {
				if (!setupField) {
					field.setAspectRatio(this.getHeight()*1.0 / this.getWidth());
					field.start();
					setupField = true;
				}
				try {
					synchronized(field) {
						field.tick(dt);
					}
					drawField();
				}
				catch(Exception ex) {
					//ex.fillInStackTrace();
					ex.printStackTrace();
				}
			}
			frameRateManager.sleepUntilNextFrame();
		}
	}
	
	void drawCircleAtPosition(Canvas c, Vec2 position, float radius, Paint paint) {
		// scaling factor is width of view
		int x = (int)(position.x * this.getWidth());
		int y = (int)(position.y * this.getWidth());
		c.drawCircle(x, y, radius, paint);
	}
	
	Paint tempPaint = new Paint(); {tempPaint.setAntiAlias(true);}
	Random random = new Random();
	
	/** Draws background image, goal zones, player, and bullets.
	 */
	void drawField() {
		//Log.e("serious", "Current Level: " + Integer.toString(LevelManager.currentLevel));

		if(LevelManager.currentLevel == 3)
			enemy = BitmapFactory.decodeResource(getResources(),R.drawable.enemyblue1);

        if(DodgeMain.startGame){
            x.add(DodgeMain.xAccel);
            y.add(DodgeMain.yAccel);
            z.add(DodgeMain.zAccel);
        }

		if(x.size() == 200){
			List<Float> accel = new ArrayList<>();
			accel.addAll(x);
			accel.addAll(y);
			accel.addAll(z);
			DodgeMain.data_array = toFloatArray(accel);
			float[] data_array = toFloatArray(accel);
			x.clear();
			y.clear();
			z.clear();
			DodgeMain.results = DodgeMain.classifier.predictProbabilities(data_array);
			max = indexOfLargest(DodgeMain.results);

			if(max == 0) {
				Log.i("serious", "dumbbell curl");
				currentExercise = "dumbbell curl";
			}
			else if(max == 1) {
				Log.i("serious", "front lateral raise");
				currentExercise = "front lateral raise";
			}
			else if(max == 2) {
				Log.i("serious", "overhead triceps extension");
				currentExercise = "overhead triceps extension";
			}
			else if(max == 3) {
				Log.i("serious", "rest");
				currentExercise = "rest";
			}
			else if(max == 4) {
				Log.i("serious", "side lateral raise");
				currentExercise = "side lateral raise";
			}

			//Log.e("serious", Arrays.toString(DodgeMain.results));

			Context context = getContext();
			CharSequence text = "Hello toast!";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}

		// check for removing flag preventing display sleep, if no recent tilt input
		if (displaySleepDisabled && !enableDisplaySleepScheduled && System.currentTimeMillis() - displaySleepDisableTime > TILT_DISPLAY_ON_MILLIS) {
			// clearDisableSleepFlag will execute on main thread
			enableDisplaySleepScheduled = true;
			messageHandler.post(new Runnable() {
				public void run() {
					clearDisableSleepFlag();
				}
			});
		}
		
		Canvas c = surfaceHolder.lockCanvas(null);
		
		if (unscaledBackgroundBitmap!=null) {
			// ok to call this from a secondary thread?
			this.setBackgroundBitmap(unscaledBackgroundBitmap);
		}
		
		if (backgroundBitmap!=null) {
			c.drawBitmap(backgroundBitmap, 0, 0, blackPaint);
		}
		else {
			c.drawRect(bounds, blackPaint);
		}

		screenWidth = this.getWidth()/8;
		float goalHeight = (float)(field.goalHeight()*this.getWidth());
		//c.drawRect(new RectF(0, 0, this.getWidth(), goalHeight), field.getMovingUp() ? startAreaPaint : endAreaPaint);
		c.drawRect(new RectF(0, this.getHeight()-goalHeight, this.getWidth()/5, this.getHeight()), endAreaPaint);
		//c.drawRect(new RectF(0, this.getHeight()-goalHeight, this.getWidth(), this.getHeight()), field.getMovingUp() ? endAreaPaint);

		//Log.i("serious", "Hits: " + Integer.toString(DodgeMain.numHits));
        if(Field.inNeutralZone && DodgeMain.med >= DodgeMain.medBase && DodgeMain.numHits != 0){
            DeviceSetupActivityFragment.mwBoard.getModule(Haptic.class).startBuzzer((short) 1000);
			DodgeMain.numHits=0;
        }


		if(DodgeMain.numHits == 0){
			imageSize = 66;
			playerShip = "playership1_blue_3ring";
			current = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_3ring);
			current = scaleDown(current, 100, true);
		}
		else if(DodgeMain.numHits == 1){
			imageSize = 66;
			current = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_2ring);
			current = scaleDown(current, 100, true);
		}
		else if(DodgeMain.numHits == 2){
			imageSize = 66;
			current = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_1ring);
			current = scaleDown(current, 100, true);
		}
		else if(DodgeMain.numHits >= 3){
			imageSize = 44;
			current = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_0ring);
			current = scaleDown(current, 100, true);

		}

		/*if(DodgeMain.numHits > 3){
			stop();
		}*/


		if(field.getMovingUp())
			scaledBitmap = flip(scaleDown(current, imageSize, true), Direction.VERTICAL);
		else
			scaledBitmap = scaleDown(current, 130, true);

		for(Bullet bullet : field.getBullets()) {
			int[] color = bullet.getColor();
			if (flashingBullets) {
				tempPaint.setARGB(255, 100+random.nextInt(155), 100+random.nextInt(155), 100+random.nextInt(155));
			}
			else {
				tempPaint.setARGB(255, color[0], color[1], color[2]);
			}
			//drawCircleAtPosition(c, bullet.getPosition(), BULLET_SCALE*this.getWidth(), tempPaint);
            c.drawBitmap(scaledEnemy, (int)(bullet.getPosition().x * this.getWidth()), (int)(bullet.getPosition().y * this.getWidth()), null);
        }
		
		if (isDeathAnimationRunning()) {
			// use a path with two circles to show the player's circle disappearing from the inside out
			android.graphics.Path path = new android.graphics.Path();
			int cx = (int)(this.getWidth() * dodgerDeathPosition.x);
			int cy = (int)(this.getWidth() * dodgerDeathPosition.y);
			float fullRadius = DODGER_SCALE*this.getWidth();
			float deathRadius = fullRadius * (1.0f*dodgerDeathFrame/DODGER_DEATH_FRAMES);
			path.addCircle(cx, cy, fullRadius, android.graphics.Path.Direction.CW);
			path.addCircle(cx, cy, deathRadius, android.graphics.Path.Direction.CCW);
			c.drawPath(path, dodgerPaint);

			dodgerDeathFrame++;
			if (dodgerDeathFrame>=DODGER_DEATH_FRAMES) {
				dodgerDeathPosition = null;
			}
		}
		else if (field.getDodger()!=null) {
			//drawCircleAtPosition(c, field.getDodger().getPosition(), DODGER_SCALE*this.getWidth(), dodgerPaint);
			c.drawBitmap(scaledBitmap, (int)(field.getDodger().getPosition().x * this.getWidth()), (int)(field.getDodger().getPosition().y * this.getWidth()), null);

		}
		
		if (debugText!=null) {
			c.drawText(debugText, 10, 20, dodgerPaint);
		}

		surfaceHolder.unlockCanvasAndPost(c);
		surfaceHolder.unlockCanvasAndPost(c);
	}
	
	//boolean stopFlag; // for taking screenshots, uncomment this and first onTouchEvent section
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/*// uncomment to make the game pause on touch events, for screenshots
		if (stopFlag) this.start();
		else this.stop();
		stopFlag = !stopFlag;
		if (true) return true;
		 */
		if (!isDeathAnimationRunning() && field.getDodger()!=null) {
			Vec2 target = new Vec2(event.getX() / this.getWidth(), event.getY() / this.getWidth());
			setTiltLocked(true);
			field.getDodger().setTargetPosition(target);
		}
		return true;
		
	}
	
	/** Updates the player's velocity direction (the magnitude is always full speed). If x and y are both 0 the player will stop moving. 
	 */
	void changeSpeed(float x, float y) {
		if (field.getDodger()!=null) field.getDodger().setVelocityDirection(new Vec2(x, y));
	}
	
	@Override
	/** Changes the player's speed based on the direction of trackball movement.
	 */
	public boolean onTrackballEvent(MotionEvent event) {
		float trackballScale = 1.0f;
		if (!isDeathAnimationRunning() && field.getDodger()!=null) {
			setTiltLocked(true);
			changeSpeed(trackballScale*event.getX(), trackballScale*event.getY());
			return true;
		}
		return super.onTrackballEvent(event);
	}
	
	List controlKeys = Arrays.asList(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
			KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT);
	
	Set keysDown = new HashSet();
	
	/** Updates the player's speed based on which directional pad controls are pressed. More than one
	 * control can be down at once, e.g. up and right. If the center is pressed, the speed will be set
	 * to zero.
	 */
	void updateSpeedFromKeys() {
		float dpadScale = 0.1f;
		float x=0, y=0;
		if (keysDown.contains(KeyEvent.KEYCODE_DPAD_UP)) y -= dpadScale;
		if (keysDown.contains(KeyEvent.KEYCODE_DPAD_DOWN)) y += dpadScale;
		if (keysDown.contains(KeyEvent.KEYCODE_DPAD_LEFT)) x -= dpadScale;
		if (keysDown.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) x += dpadScale;
		setTiltLocked(true);
		changeSpeed(x, y);
	}
	
	@Override
	/** Changes the player's speed when the directional pad is pressed. A push in the center will make
	 * the player stop.
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (controlKeys.contains(keyCode)) {
			keysDown.add(keyCode);
			updateSpeedFromKeys();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (controlKeys.contains(keyCode)) {
			keysDown.remove(keyCode);
			updateSpeedFromKeys();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	// tilt control support
	float initialPitch;
	float lastPitch, lastRoll;
	boolean hasInitialPitch = false;
	float TILT_MOTION_SENSITIVITY = 0.05f; // change in radians required to to move
	float TILT_SLEEP_SENSITIVITY = 0.1f;  // change in radians required to prevent sleep
	
	// when user moves using the screen, trackball, or dpad, "lock" tilt motion until the device is tilted significantly
	boolean tiltLocked = false;
	float tiltLockPitch, tiltLockRoll;
	boolean hasTiltLockValues = false;
	float TILT_LOCK_SENSITIVITY = 0.15f; // change in radians required to resume tilt control
	
	/** Called when orientation event is received. Updates the player's direction based on pitch and roll values.
	 */
	void handleOrientationUpdate(float azimuth, float pitch, float roll) {
		if (!tiltControlEnabled || isDeathAnimationRunning() || field.getDodger()==null) return;
		
		if (!hasInitialPitch) {
			initialPitch = lastPitch = pitch;
			lastRoll = roll;
			hasInitialPitch = true;
			//tiltLocked = false;
		}
		else {
			float pitchdiff = pitch - initialPitch;
			// don't move unless it's at least 0.05 radians, about 3 degrees
			if (Math.abs(pitchdiff) < TILT_MOTION_SENSITIVITY) pitchdiff = 0;
			if (Math.abs(roll) < TILT_MOTION_SENSITIVITY) roll = 0;
			// Prevent sleeping if user is tilting to steer
			if (Math.abs(pitch-lastPitch) > TILT_SLEEP_SENSITIVITY || Math.abs(roll-lastRoll) > TILT_SLEEP_SENSITIVITY) {
				noteUserActivity();
				lastPitch = pitch;
				lastRoll = roll;
			}
			// if "tilt-locked" because the user moved using the screen, trackball, or d-pad, require a larger change
			if (tiltLocked) {
				if (!hasTiltLockValues) {
					tiltLockPitch = pitch;
					tiltLockRoll = roll;
					hasTiltLockValues = true;
				}
				else if (Math.abs(pitch-tiltLockPitch) > TILT_LOCK_SENSITIVITY || Math.abs(roll-tiltLockRoll) > TILT_LOCK_SENSITIVITY) {
					tiltLocked = false;
				}
			}
			// negative pitch is up, positive is down
			if (!tiltLocked) changeSpeed(roll, -pitchdiff);
		}
	}
	
	void setTiltLocked(boolean value) {
		if (tiltLocked!=value) {
			tiltLocked = value;
			hasTiltLockValues = false;
		}
		if (tiltLocked) {
			hasInitialPitch = false;
		}
	}

	// Called when the user moves by tilting the device, to prevent the display from sleeping. 
	void noteUserActivity() {
		displaySleepDisableTime = System.currentTimeMillis();
		if (!displaySleepDisabled) {
			Window window = ((Activity)this.getContext()).getWindow();
			window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
			displaySleepDisabled = true;
		}
	}
	
	// Called after no recent tilt movement, to allow the display to sleep
	void clearDisableSleepFlag() {
		Window window = ((Activity)this.getContext()).getWindow();
		window.clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		displaySleepDisabled = false;
		enableDisplaySleepScheduled = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		bounds = new RectF(0, 0, getWidth(), getHeight());
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		canDraw = true;		
		bounds = new RectF(0, 0, getWidth(), getHeight());
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		canDraw = false;
	}

	public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
		float ratio = Math.min(
				(float) maxImageSize / realImage.getWidth(),
				(float) maxImageSize / realImage.getHeight());
		int width = Math.round((float) ratio * realImage.getWidth());
		int height = Math.round((float) ratio * realImage.getHeight());

		Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
				height, filter);
		return newBitmap;
	}

	public enum Direction { VERTICAL, HORIZONTAL };

	public static Bitmap flip(Bitmap src, Direction type) {
		Matrix matrix = new Matrix();

		if(type == Direction.VERTICAL) {
			matrix.preScale(1.0f, -1.0f);
		}
		else if(type == Direction.HORIZONTAL) {
			matrix.preScale(-1.0f, 1.0f);
		} else {
			return src;
		}

		return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
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
