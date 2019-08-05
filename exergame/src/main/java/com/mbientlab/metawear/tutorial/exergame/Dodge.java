package com.mbientlab.metawear.tutorial.exergame;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class Dodge extends AppCompatActivity implements SensorEventListener{
    //Code from this program has been used from "Beginning Android Games" by Mario Zechner
    //Review SurfaceView, Canvas, continue

    public static GameSurface gameSurface;
    SensorManager manager;
    double cx, cy, cz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);
        setContentView(gameSurface);

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        manager.registerListener(this,manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        gameSurface.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        gameSurface.resume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            cx = sensorEvent.values[0];
            cy = sensorEvent.values[1];
            cz = sensorEvent.values[2];
            Log.d("TAG",cx+" "+cy+" "+cz);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    //----------------------------GameSurface Below This Line--------------------------
    public class GameSurface extends SurfaceView implements Runnable {

        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap myImage, background, enemy,damaged,current;
        Paint paintProperty;
        Canvas canvas;
        int screenWidth;
        int screenHeight;
        int enemyvalue1, enemyvalue2;
        int xpos;
        Enemy en1,en2;
        ConstraintLayout layout;
        Boolean clicked = false;
        Boolean hit=false;
        Boolean hitround1=false,hitround2=false;
        Boolean hit1=false,hit2=false;
        int score;
        int timer;
        Boolean stop = false,played=false;
        SoundPool soundPool;
        int soundID;
        MediaPlayer player,hitsound;


        public GameSurface(Context context) {
            super(context);

            holder=getHolder();

            myImage = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_3ring);
            enemy = BitmapFactory.decodeResource(getResources(),R.drawable.meteorbrown_big1);
            //background = BitmapFactory.decodeResource(getResources(),R.drawable.bg);
            damaged = BitmapFactory.decodeResource(getResources(),R.drawable.hitapple);
            layout = (ConstraintLayout) findViewById(R.id.layout);
            current = BitmapFactory.decodeResource(getResources(),R.drawable.playership1_blue_3ring);
            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            //screenDisplay.getSize(sizeOfScreen);
            screenWidth= Resources.getSystem().getDisplayMetrics().widthPixels;
            screenHeight=Resources.getSystem().getDisplayMetrics().heightPixels;
            Log.i("serious", "Width: " + Integer.toString(screenWidth));
            Log.i("serious", "Height: " + Integer.toString(screenHeight));
            paintProperty= new Paint();
            paintProperty.setTextSize(100);
            paintProperty.setColor(Color.WHITE);

            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
            soundID = soundPool.load(Dodge.this,R.raw.startup,1);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                    Log.d("sound","finished");
                }
            });
            player = MediaPlayer.create(Dodge.this,R.raw.background);
            player.start();
            new CountDownTimer(31000, 1000) {

                public void onTick(long millisUntilFinished) {
                    timer =(int) millisUntilFinished/1000;
                }

                public void onFinish() {
                    stop = true;
                }
            }.start();

        }

        @Override
        public void run() {
            int value = 325;
            en1 = new Enemy();
            en2 = new Enemy();
            enemyvalue1 = -128;
            enemyvalue2 = -128;
            en1.setFallSpeed(7);
            en2.setFallSpeed(7);
            gameSurface.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("Click", "clicked");
                    clicked = !clicked;
                    if (clicked) {
                        en1.setFallSpeed(16);
                        en2.setFallSpeed(16);
                    }
                    if (!clicked) {
                        en1.setFallSpeed(8);
                        en2.setFallSpeed(8);
                    }
                    if(stop){
                        stop=false;
                        score=0;
                        new CountDownTimer(31000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                timer =(int) millisUntilFinished/1000;
                            }

                            public void onFinish() {
                                stop = true;
                            }
                        }.start();
                        clicked=false;
                    }
                }
            });

            while (running == true) {
                double x = 0;
                if (holder.getSurface().isValid() == false)
                    continue;
                if (!stop) {
                    x = cx * (-1);
                    Log.d("abs",Math.abs(x)+"");
                    if(Math.abs(x)<Math.abs(0.12)){
                        x=0;
                    }
                    value += (x * 3);
                    if (enemyvalue1 == -128 && enemyvalue2 == -128) {
                        en1.setXpos(en1.randomXpos1());
                        en2.setXpos(en2.randomXpos2());
                    }

                    enemyvalue1 += en1.getFallSpeed();
                    Log.d("T2", enemyvalue1 + "");
                    enemyvalue2 += en2.getFallSpeed();
                    //-27 658
                    if (value < -27) {
                        value = -27;
                    }
                    if (value > screenWidth) {
                        value = screenWidth;
                    }
                    Log.d("TAG", value + "");
                    Rect apple = new Rect(value, screenHeight, value, screenHeight);
                    Rect andr1 = new Rect(en1.getXpos(), enemyvalue1, en1.getXpos() + 128, enemyvalue1 + 128);
                    Rect andr2 = new Rect(en2.getXpos(), enemyvalue2, en2.getXpos() + 128, enemyvalue2 + 128);
                    if (apple.intersect(andr1)) {
                        hit = true;
                        hit1 = true;
                        hitround1 = true;
                        Log.d("Click", "hit");
                    }
                    if (apple.intersect(andr2)) {
                        hit = true;
                        hit2 = true;
                        hitround2 = true;
                    }
                    if (apple.intersect(andr1) || apple.intersect(andr2)) {
                        hit = true;
                    } else {
                        hit = false;
                        hit1 = false;
                        hit2 = false;
                    }
                    canvas = holder.lockCanvas();
                    canvas.drawRGB(0, 0, 255);
                    //canvas.drawText("Hello World",50,200,paintProperty);
                    //canvas.drawBitmap(background, 0, 0, null);
                    if (hit) {
                        current = damaged;
                        if(!played){
                            Log.d("sound","soundplayed");
                            soundPool.play(soundID,1,1,0,0,1);
                            played=true;

                        }
                    }
                    canvas.drawText(timer - 1 + "", 550, 100, paintProperty);
                    canvas.drawBitmap(current, value, screenHeight-200, null);
                    canvas.drawBitmap(enemy, en1.getXpos(), enemyvalue1, null);
                    canvas.drawBitmap(enemy, en2.getXpos(), enemyvalue2, null);
                    canvas.drawText(score + "", 100, 100, paintProperty);
                    holder.unlockCanvasAndPost(canvas);
                    if (enemyvalue1 > 1300) {
                        enemyvalue1 = -128;
                        enemyvalue2 = -128;
                        score += 2;
                        if (hitround1) {
                            score -= 1;
                        }
                        if (hitround2) {
                            score -= 1;
                        }
                        hitround1 = false;
                        hitround2 = false;
                        current = myImage;
                        played=false;
                    }
                }
                if(stop){
                    canvas=holder.lockCanvas();
                    Log.d("STOP","stop");
                    canvas.drawRGB(0,0,0);
                    canvas.drawText("Score: "+score,205,600,paintProperty);
                    holder.unlockCanvasAndPost(canvas);

                }
            }
        }

        public void resume(){
            running=true;
            gameThread=new Thread(this);
            gameThread.start();
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                }
            }
        }
        public void damagedApple(){

        }


    }//GameSurface

}//Activity

