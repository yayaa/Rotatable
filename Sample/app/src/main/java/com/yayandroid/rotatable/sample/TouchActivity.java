package com.yayandroid.rotatable.sample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.yayandroid.rotatable.Rotatable;

import java.util.Random;

/**
 * Created by Yahya Bayramoglu on 24/11/15.
 */
public class TouchActivity extends AppCompatActivity {

    private Rotatable rotatable;

    private final int DEFAULT_ROTATION_ID = R.id.action_rotate_y;
    private int selectedRotationId = DEFAULT_ROTATION_ID;

    private EditText rotationCount, rotationDistance, pivotX, pivotY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rotationCount = (EditText) findViewById(R.id.rotationCount);
        rotationDistance = (EditText) findViewById(R.id.rotationDistance);
        pivotX = (EditText) findViewById(R.id.rotationPivotX);
        pivotY = (EditText) findViewById(R.id.rotationPivotY);
    }

    public void buildClick(View v) {
        boolean countIsEmpty = TextUtils.isEmpty(rotationCount.getText());
        boolean distanceIsEmpty = TextUtils.isEmpty(rotationDistance.getText());

        if (!countIsEmpty && !distanceIsEmpty) {
            Toast.makeText(getApplicationContext(), R.string.toast_count_distance, Toast.LENGTH_SHORT).show();
            return;
        }

        Rotatable.Builder builder = new Rotatable.Builder(findViewById(R.id.targetView))
                .sides(R.id.frontView, R.id.backView)
                .direction(getRotationDirectionById(selectedRotationId))
                .listener(rotationListener);

        if (!countIsEmpty) {
            builder.rotationCount(Float.parseFloat(rotationCount.getText().toString()));
        }

        if (!distanceIsEmpty) {
            builder.rotationDistance(Float.parseFloat(rotationDistance.getText().toString()));
        }

        String pivotXValue = pivotX.getText().toString();
        if (!TextUtils.isEmpty(pivotXValue)) {
            builder.pivotX(Integer.parseInt(pivotXValue));
        }

        String pivotYValue = pivotY.getText().toString();
        if (!TextUtils.isEmpty(pivotYValue)) {
            builder.pivotY(Integer.parseInt(pivotYValue));
        }

        if (rotatable != null) {
            rotatable.drop();
        }
        rotatable = builder.build();
    }

    public void resetClick(View v) {
        rotationCount.setText("");
        rotationDistance.setText("");
        pivotX.setText("");
        pivotY.setText("");
        rotatable.drop();
        rotatable = null;
    }

    public void randomAnimateClick(View v) {
        if (rotatable != null) {
            rotatable.rotate(getRotationDirectionById(selectedRotationId), new Random().nextInt(360));
        }
    }

    private Rotatable.RotationListener rotationListener = new Rotatable.RotationListener() {
        @Override
        public void onRotationChanged(float newRotationX, float newRotationY) {
            setTitle("X: " + newRotationX + ", Y: " + newRotationY);
        }
    };

    private int getRotationDirectionById(int actionId) {
        switch (actionId) {
            case R.id.action_rotate_x:
                return Rotatable.ROTATE_X;
            case R.id.action_rotate_y:
                return Rotatable.ROTATE_Y;
            case R.id.action_rotate_both:
                return Rotatable.ROTATE_BOTH;
            default:
                // will crash!
                return -1;
        }
    }

    private boolean isTouchEnabled() {
        if (rotatable != null) {
            return rotatable.isTouchEnable();
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (rotatable != null) {
            rotatable.orientationChanged(newConfig.orientation);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rotation, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isCheckable()) {
                if (item.getItemId() == R.id.action_touch) {
                    item.setTitle(isTouchEnabled() ? R.string.action_touch_enabled : R.string.action_touch_disabled);
                    item.setChecked(isTouchEnabled());
                } else {
                    item.setChecked(item.getItemId() == selectedRotationId);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemId = item.getItemId();
        switch (menuItemId) {
            case android.R.id.home: {
                finish();
                return true;
            }
            case R.id.action_rotate_x:
            case R.id.action_rotate_y:
            case R.id.action_rotate_both: {
                selectedRotationId = menuItemId;
                if (rotatable != null) {
                    rotatable.setDirection(getRotationDirectionById(selectedRotationId));
                }
                return true;
            }
            case R.id.action_touch: {
                if (rotatable != null) {
                    rotatable.setTouchEnable(!rotatable.isTouchEnable());
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}