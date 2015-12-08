package com.yayandroid.rotatable.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

/**
 * Created by Yahya Bayramoglu on 04/12/15.
 */
public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    public void touchSampleClick(View v) {
        startActivity(new Intent(this, TouchActivity.class));
    }

    public void animateSampleClick(View v) {
        startActivity(new Intent(this, AnimateActivity.class));
    }

}