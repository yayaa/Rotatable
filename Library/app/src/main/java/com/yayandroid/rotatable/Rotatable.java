package com.yayandroid.rotatable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.DisplayMetrics;
import android.util.Property;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.CycleInterpolator;

import java.util.ArrayList;

/**
 * Created by Yahya Bayramoglu on 01/12/15.
 */
public class Rotatable implements View.OnTouchListener {

    private static final int NULL_INT = -1;

    public static final int DEFAULT_ROTATE_ANIM_TIME = 500;
    public static final int ROTATE_BOTH = 0;
    public static final int ROTATE_X = 1;
    public static final int ROTATE_Y = 2;

    private final int FIT_ANIM_TIME = 300;
    private final int FRONT_VIEW = 3;
    private final int BACK_VIEW = 4;

    private RotationListener rotationListener;
    private View rootView, frontView, backView;

    private boolean touchEnable = true;
    private boolean shouldSwapViews = false;

    private int rotation;
    private int screenWidth = NULL_INT, screenHeight = NULL_INT;
    private int currentVisibleView = FRONT_VIEW;

    private float rotationCount;
    private float rotationDistance;
    private float oldX, oldY, currentX, currentY;
    private float currentXRotation = 0, currentYRotation = 0;
    private float maxDistanceX = NULL_INT, maxDistanceY = NULL_INT;
    private float defaultPivotX = NULL_INT, defaultPivotY = NULL_INT;

    private Rotatable(Builder builder) {
        this.rootView = builder.root;
        this.defaultPivotX = rootView.getPivotX();
        this.defaultPivotY = rootView.getPivotY();
        this.rotationListener = builder.listener;

        if (builder.pivotX != NULL_INT) {
            this.rootView.setPivotX(builder.pivotX);
        }

        if (builder.pivotY != NULL_INT) {
            this.rootView.setPivotY(builder.pivotY);
        }

        if (builder.frontId != NULL_INT) {
            this.frontView = rootView.findViewById(builder.frontId);
        }

        if (builder.backId != NULL_INT) {
            this.backView = rootView.findViewById(builder.backId);
        }

        this.rotation = builder.rotation;
        this.rotationCount = builder.rotationCount;
        this.rotationDistance = builder.rotationDistance;
        this.shouldSwapViews = frontView != null && backView != null;

        rootView.setOnTouchListener(this);
    }

    /**
     * This method needs to be call, if only you need to reset and
     * rebuild a view as rotatable with different configurations
     */
    public void drop() {
        rootView.setPivotX(defaultPivotX);
        rootView.setPivotY(defaultPivotY);
        rootView.setOnTouchListener(null);
        rootView = null;
        frontView = null;
        backView = null;
    }

    /**
     * You can specify rotation direction as axis X, Y or BOTH
     */
    public void setDirection(int direction) {
        if (!isRotationValid(direction)) {
            throw new IllegalArgumentException("Cannot specify given value as rotation direction!");
        }
        this.rotation = direction;
    }

    /**
     * You may need to enable / disable touch interaction at some point,
     * so it is possible to do it so anytime by rotatable object
     */
    public void setTouchEnable(boolean enable) {
        this.touchEnable = enable;
    }

    /**
     * To determine rotatable object is currently touchable or not
     */
    public boolean isTouchEnable() {
        return touchEnable;
    }

    /**
     * If your application can be used multi orientated, then you have to declare
     * orientation changes to rotatable object, so it can recalculate its maxDistances.
     * <p/>
     * You only need to inform rotatable object about orientation changes, when you specified
     * {@link Builder#rotationCount(float)} or {@link Builder#rotationDistance(float)}
     */
    public void orientationChanged(int newOrientation) {
        if (screenWidth == NULL_INT) {
            calculateScreenDimensions();
        }
        measureScreenUpToOrientation(newOrientation);

        // reset maxDistances values to recalculate them
        maxDistanceX = NULL_INT;
        maxDistanceY = NULL_INT;
    }

    /**
     * Call this method to reveal rotatable view's existence
     */
    public void takeAttention() {
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(rootView, View.ROTATION_X, 10);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(rootView, View.ROTATION_Y, -10);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(DEFAULT_ROTATE_ANIM_TIME);
        set.setInterpolator(new CycleInterpolator(0.8f));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                rootView.animate().rotationX(0).rotationY(0).setDuration(FIT_ANIM_TIME)
                        .setInterpolator(new FastOutSlowInInterpolator()).start();
            }
        });
        set.playTogether(animatorX, animatorY);
        set.start();
    }

    /**
     * Animate rotatable object with given direction and degree also possible
     * to set duration and a listener with other derivation of this method
     */
    public void rotate(int direction, float degree) {
        rotate(direction, degree, DEFAULT_ROTATE_ANIM_TIME);
    }

    public void rotate(int direction, float degree, int duration) {
        rotate(direction, degree, duration, null);
    }

    public void rotate(final int direction, float degree, int duration, Animator.AnimatorListener listener) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new FastOutSlowInInterpolator());

        ArrayList<Animator> animators = new ArrayList<>();

        if (direction == ROTATE_X || direction == ROTATE_BOTH) {
            animators.add(getAnimatorForProperty(View.ROTATION_X, direction, degree));
        }

        if (direction == ROTATE_Y || direction == ROTATE_BOTH) {
            animators.add(getAnimatorForProperty(View.ROTATION_Y, direction, degree));
        }

        if (listener != null) {
            animatorSet.addListener(listener);
        }

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                updateRotationValues(true);
            }
        });

        animatorSet.playTogether(animators);
        animatorSet.start();
    }

    private Animator getAnimatorForProperty(Property property, final int direction, float degree) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, property, degree);

        if (shouldSwapViews) {
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    updateRotationValues(false);
                    swapViews(direction);
                }
            });
        }
        return animator;
    }

    private void updateRotationValues(boolean notifyListener) {
        currentXRotation = rootView.getRotationX();
        currentYRotation = rootView.getRotationY();

        if (notifyListener) {
            notifyListenerRotationChanged();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (touchEnable) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    restoreOldPositions(event);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    restoreNewPositions(event);
                    handleRotation();

                    if (shouldSwapViews) {
                        swapViews(rotation);
                    }
                    notifyListenerRotationChanged();
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    fitRotation();
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void restoreOldPositions(MotionEvent event) {
        if (shouldRotateX()) {
            oldY = getYValue(event.getRawY());
        }

        if (shouldRotateY()) {
            oldX = getXValue(event.getRawX());
        }
    }

    private float getXValue(float rawX) {
        if (rotationCount != NULL_INT && maxDistanceX != NULL_INT) {
            return rawX * rotationCount * 180 / maxDistanceX;
        }

        if (rotationDistance != NULL_INT) {
            return rawX * 180 / rotationDistance;
        }

        return rawX;
    }

    private float getYValue(float rawY) {
        if (rotationCount != NULL_INT && maxDistanceY != NULL_INT) {
            return rawY * rotationCount * 180 / maxDistanceY;
        }

        if (rotationDistance != NULL_INT) {
            return rawY * 180 / rotationDistance;
        }

        return rawY;
    }

    private void restoreNewPositions(MotionEvent event) {
        if (shouldRotateX()) {
            if (rotationCount != NULL_INT && maxDistanceY == NULL_INT) {
                maxDistanceY = (event.getRawY() - oldY) > 0 ? (getScreenHeight() - oldY) : oldY;
                oldY = getYValue(oldY);
            }
            currentY = getYValue(event.getRawY());
        }

        if (shouldRotateY()) {
            if (rotationCount != NULL_INT && maxDistanceX == NULL_INT) {
                maxDistanceX = (event.getRawX() - oldX) > 0 ? (getScreenWidth() - oldX) : oldX;
                oldX = getXValue(oldX);
            }
            currentX = getXValue(event.getRawX());
        }
    }

    private int getScreenWidth() {
        if (screenWidth == NULL_INT) {
            calculateScreenDimensions();
        }
        return screenWidth;
    }

    private int getScreenHeight() {
        if (screenHeight == NULL_INT) {
            calculateScreenDimensions();
        }
        return screenHeight;
    }

    private void calculateScreenDimensions() {
        Display display = ((WindowManager) rootView.getContext()
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    private void measureScreenUpToOrientation(int screenOrientation) {
        int tempWidth = screenWidth, tempHeight = screenHeight;
        if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            /**
             * If screenOrientation is landscape, then width will have a larger value than height
             */
            screenWidth = Math.max(tempWidth, tempHeight);
            screenHeight = Math.min(tempWidth, tempHeight);
        } else {
            /**
             * If screenOrientation:
             * is portrait, then width will have a smaller value than height
             * is square, then width and height will be same
             * is unknown, then unknown to rotatable as well
             * so either way...
             */
            screenWidth = Math.min(tempWidth, tempHeight);
            screenHeight = Math.max(tempWidth, tempHeight);
        }
    }

    private boolean shouldRotateX() {
        return rotation == ROTATE_X || rotation == ROTATE_BOTH;
    }

    private boolean shouldRotateY() {
        return rotation == ROTATE_Y || rotation == ROTATE_BOTH;
    }

    private void handleRotation() {
        if (shouldRotateX()) {
            float newXRotation = (rootView.getRotationX() + (oldY - currentY)) % 360;
            rootView.setRotationX(newXRotation);
            currentXRotation = newXRotation;
            oldY = currentY;
        }

        if (shouldRotateY()) {
            float newYRotation;
            if (isInFrontArea(currentXRotation)) {
                newYRotation = (rootView.getRotationY() + (currentX - oldX)) % 360;
            } else {
                newYRotation = (rootView.getRotationY() - (currentX - oldX)) % 360;
            }

            rootView.setRotationY(newYRotation);
            currentYRotation = newYRotation;
            oldX = currentX;
        }
    }

    private boolean isInFrontArea(float value) {
        return (-270 >= value && value >= -360)
                || (-90 <= value && value <= 90)
                || (270 <= value && value <= 360);
    }

    private void swapViews(int rotation) {
        boolean isFront = false;
        if (rotation == ROTATE_Y) {
            isFront = isInFrontArea(currentYRotation);

            if (!isInFrontArea(currentXRotation)) {
                isFront = !isFront;
            }
        }

        if (rotation == ROTATE_X) {
            isFront = isInFrontArea(currentXRotation);

            if (!isInFrontArea(currentYRotation)) {
                isFront = !isFront;
            }
        }

        if (rotation == ROTATE_BOTH) {
            isFront = (currentXRotation > -90 && currentXRotation < 90) && (currentYRotation > -90 && currentYRotation < 90)

                    || (currentXRotation > -90 && currentXRotation < 90) && (currentYRotation > -360 && currentYRotation < -270)
                    || (currentXRotation > -360 && currentXRotation < -270) && (currentYRotation > -90 && currentYRotation < 90)

                    || (currentXRotation > -90 && currentXRotation < 90) && (currentYRotation > 270 && currentYRotation < 360)
                    || (currentXRotation > 270 && currentXRotation < 360) && (currentYRotation > -90 && currentYRotation < 90)

                    || (currentXRotation > 90 && currentXRotation < 270) && (currentYRotation > -270 && currentYRotation < -90)
                    || (currentXRotation > -270 && currentXRotation < -90) && (currentYRotation > 90 && currentYRotation < 270)

                    || (currentXRotation > 90 && currentXRotation < 270) && (currentYRotation > 90 && currentYRotation < 270)
                    || (currentXRotation > -270 && currentXRotation < -90) && (currentYRotation > -270 && currentYRotation < -90);
        }

        boolean shouldSwap = (isFront && currentVisibleView == BACK_VIEW) || (!isFront && currentVisibleView == FRONT_VIEW);
        if (shouldSwap) {
            frontView.setVisibility(isFront ? View.VISIBLE : View.GONE);
            backView.setVisibility(isFront ? View.GONE : View.VISIBLE);
            currentVisibleView = isFront ? FRONT_VIEW : BACK_VIEW;
        }
    }

    private void notifyListenerRotationChanged() {
        if (rotationListener != null) {
            rotationListener.onRotationChanged(currentXRotation, currentYRotation);
        }
    }

    private void fitRotation() {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(FIT_ANIM_TIME);
        animatorSet.setInterpolator(new FastOutSlowInInterpolator());

        ArrayList<Animator> animators = new ArrayList<>();

        if (shouldRotateY()) {
            animators.add(ObjectAnimator.ofFloat(rootView, View.ROTATION_Y, getRequiredRotation(rootView.getRotationY())));
        }

        if (shouldRotateX()) {
            animators.add(ObjectAnimator.ofFloat(rootView, View.ROTATION_X, getRequiredRotation(rootView.getRotationX())));
        }

        animatorSet.playTogether(animators);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                updateRotationValues(true);
            }
        });
        animatorSet.start();

        // Reset max values to calculate again on touch down
        maxDistanceX = NULL_INT;
        maxDistanceY = NULL_INT;
    }

    private float getRequiredRotation(float currentRotation) {
        float requiredRotation;
        if (currentRotation < -270) {
            requiredRotation = -360;
        } else if (currentRotation < -90 && currentRotation > -270) {
            requiredRotation = -180;
        } else if (currentRotation > -90 && currentRotation < 90) {
            requiredRotation = 0;
        } else if (currentRotation > 90 && currentRotation < 270) {
            requiredRotation = 180;
        } else {
            requiredRotation = 360;
        }
        return requiredRotation;
    }

    /**
     * Listener to get notified whenever view's rotation is changed
     */
    public interface RotationListener {
        void onRotationChanged(float newRotationX, float newRotationY);
    }

    public static class Builder {

        private View root;
        private RotationListener listener;
        private int rotation = NULL_INT;
        private int frontId = NULL_INT;
        private int backId = NULL_INT;
        private int pivotX = NULL_INT;
        private int pivotY = NULL_INT;
        private float rotationCount = NULL_INT;
        private float rotationDistance = NULL_INT;

        public Builder(View viewToRotate) {
            this.root = viewToRotate;
        }

        /**
         * This listener will receive current rotation values of given view
         */
        public Builder listener(RotationListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Declaring sides will provide swapping between them when necessary,
         * if not declared, then rootView will be rotating by itself without any other effect
         */
        public Builder sides(int frontViewId, int backViewId) {
            this.frontId = frontViewId;
            this.backId = backViewId;
            return this;
        }

        /**
         * Specify an axis or both axises to rotate around
         */
        public Builder direction(int rotation) {
            this.rotation = rotation;
            return this;
        }

        /**
         * This method provides view to rotate only as given rotation count,
         * irrelevant to its position or touch distance
         */
        public Builder rotationCount(float count) {
            if (rotationDistance != NULL_INT) {
                throw new IllegalArgumentException("You cannot specify both distance and count for rotation limitation.");
            }

            this.rotationCount = count;
            return this;
        }

        /**
         * This method provides view to rotate once in given distance,
         * note that it won't rotate full if touch distance is not enough
         * but it may still fit the rotation. If you want to ensure rotation gets completed
         * see {@link #rotationCount(float}
         */
        public Builder rotationDistance(float distance) {
            if (rotationCount != NULL_INT) {
                throw new IllegalArgumentException("You cannot specify both distance and count for rotation limitation.");
            }

            this.rotationDistance = distance;
            return this;
        }

        /**
         * Consider not to change pivot values because view may out of its bounders and get invisible.
         */
        public Builder pivot(int pivotX, int pivotY) {
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            return this;
        }

        /**
         * Consider not to change pivot values because view may out of its bounders and get invisible.
         */
        public Builder pivotX(int pivotX) {
            this.pivotX = pivotX;
            return this;
        }

        /**
         * Consider not to change pivot values because view may out of its bounders and get invisible.
         */
        public Builder pivotY(int pivotY) {
            this.pivotY = pivotY;
            return this;
        }

        public Rotatable build() {
            if (rotation == NULL_INT || !isRotationValid(rotation)) {
                throw new IllegalArgumentException("You must specify a direction!");
            }
            return new Rotatable(this);
        }

    }

    private static boolean isRotationValid(int value) {
        return value == ROTATE_X || value == ROTATE_Y || value == ROTATE_BOTH;
    }

}