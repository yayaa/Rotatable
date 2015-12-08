package com.yayandroid.rotatable.sample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.yayandroid.rotatable.Rotatable;

/**
 * Created by Yahya Bayramoglu on 04/12/15.
 */
public class AnimateActivity extends AppCompatActivity {

    private Handler handler;
    private final int ANIM_DURATION = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animate);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        handler = new Handler();
        runAnimationOn(R.id.view1, Rotatable.ROTATE_Y, 3600, 300);
        runAnimationOn(R.id.view4, Rotatable.ROTATE_Y, 3960, 600);
        runAnimationOn(R.id.view2, Rotatable.ROTATE_BOTH, 1440, 1200);
        runAnimationOn(R.id.view3, Rotatable.ROTATE_X, 2880, 900);
    }

    private void runAnimationOn(final int resId, final int direction, final int degree, int delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Rotatable rotatable = new Rotatable.Builder(findViewById(resId))
                        .direction(Rotatable.ROTATE_BOTH)
                        .build();
                rotatable.rotate(direction, degree, ANIM_DURATION);
            }
        }, delay);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}