package com.mbientlab.metawear.tutorial.exergame;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class Menu extends AppCompatActivity {
    private Button metawear, myo, neuroSky;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.mbientlab.metawear.tutorial.exergame.R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.toolbar);
        //setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        metawear = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startMetaWear);
        myo = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startMyo);
        neuroSky = findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.startNeuroSky);

        metawear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
        myo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DodgeMain.class);
                startActivity(intent);
            }
        });
        neuroSky.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MindWave.class);
                startActivity(intent);
            }
        });
    }

}
