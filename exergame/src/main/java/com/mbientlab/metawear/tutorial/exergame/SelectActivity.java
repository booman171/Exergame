package com.mbientlab.metawear.tutorial.exergame;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class SelectActivity extends AppCompatActivity {
    private static final String TAG = "SelectActivity";

    private ArrayList<String> mNames = new ArrayList<>();
    private ArrayList<String> mImageUrls = new ArrayList<>();
    static String[][] activityData;
    static ArrayList<String> activityNames;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.mbientlab.metawear.tutorial.exergame.R.layout.activity_select);
        Toolbar toolbar = (Toolbar) findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.toolbar);
        setSupportActionBar(toolbar);


        Log.i(TAG, "onCreate: started.");
        initImageBitmaps();
    }

    private void initImageBitmaps(){
        Log.i(TAG, "initImageBitmaps: preparing bitmaps.");

        mImageUrls.add("https://cdn-ami-drupal.heartyhosting.com/sites/muscleandfitness.com/files/dumbell-lateral-raise-upper-body-weights-main.jpg");
        mNames.add("Lateral Rasies");

        mImageUrls.add("https://loseyoself.files.wordpress.com/2015/01/seated-triceps-extension1.jpg");
        mNames.add("Triceps Extension");

        mImageUrls.add("https://cdn-ami-drupal.heartyhosting.com/sites/muscleandfitness.com/files/_main_dbcurl.jpg");
        mNames.add("Curl");

        mImageUrls.add("http://cdn1.coachmag.co.uk/sites/coachmag/files/styles/insert_main_wide_image/public/2016/06/foam_roller_bench_press.jpg?itok=kTtA1fk_");
        mNames.add("Bench Press");

        mImageUrls.add("https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/articles/2015/08/main-0-1484243182.jpg?resize=768:*");
        mNames.add("Push Ups");

        //mImageUrls.add("https://img.aws.livestrongcdn.com/ls-article-image-673/ds-photo/getty/article/176/227/610686406_XS.jpg");
        //mNames.add("Sit Ups");

        //mImageUrls.add("http://s3.amazonaws.com/prod.skimble/assets/770211/image_iphone.jpg");
        //mNames.add("Squats");

        //mImageUrls.add("http://cdn1.coachmag.co.uk/sites/coachmag/files/styles/insert_main_wide_image/public/2016/07/4-2b-seated-dumbbell-shoulder-press.jpg?itok=Vr3aGAgT");
        //mNames.add("Shoulder Press");

        //mImageUrls.add("https://www.shape.com/sites/shape.com/files/1200-woman-running-outside_1.jpg");
        //mNames.add("Running");

        //mImageUrls.add("https://www.mensjournal.com/wp-content/uploads/mf/main-power-up-your-jump-squats-literally.jpg?w=1200&h=630&crop=1");
        //mNames.add("Jump Squats");

        //mImageUrls.add("https://thumbs.dreamstime.com/z/boxer-stylized-black-white-illustration-31870425.jpg");
        //mNames.add("Boxing");

        intitRecyclerView();
    }

    private void intitRecyclerView(){
        Log.i(TAG, "initRecylcerView: init recyclerview.");
        RecyclerView recyclerView = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.activityView);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter( mNames, mImageUrls, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        activityData = adapter.getData();
        activityNames = adapter.getmImageNames();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.mbientlab.metawear.tutorial.exergame.R.menu.menu_select_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Intent intent = new Intent(getApplicationContext(), DeviceSetupActivityFragment.class);
        //intent.putExtra("ACTIVITY_NAMES", newmNames);
        //intent.putExtra("ACTIVITY_DETAILS", data);
        //startActivity(intent);\
        finish();
        return super.onOptionsItemSelected(item);
    }
}