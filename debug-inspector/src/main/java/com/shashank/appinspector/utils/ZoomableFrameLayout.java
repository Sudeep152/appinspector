package com.shashank.appinspector.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ZoomableFrameLayout extends FrameLayout {

    private static final float MIN_SCALE = 0.35f;
    private static final float MAX_SCALE = 2.5f;
    private static final float ZOOM_STEP = 1.3f;
    private static final float PAN_THRESHOLD_PX = 8f;

    private float scaleFactor = 1f;
    private float translateX = 0f;
    private float translateY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isCurrentlyPanning = false;

    private final ScaleGestureDetector scaleDetector;

    public ZoomableFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public ZoomableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor = Math.min(Math.max(scaleFactor * detector.getScaleFactor(), MIN_SCALE), MAX_SCALE);
                constrainTranslation();
                applyTransform();
                return true;
            }
        });
    }

    public void resetZoom() {
        scaleFactor = 1f;
        translateX = 0f;
        translateY = 0f;
        applyTransform();
    }

    public void zoomIn() {
        scaleFactor = Math.min(scaleFactor * ZOOM_STEP, MAX_SCALE);
        constrainTranslation();
        applyTransform();
    }

    public void zoomOut() {
        scaleFactor = Math.max(scaleFactor / ZOOM_STEP, MIN_SCALE);
        if (scaleFactor <= 1f) {
            translateX = 0f;
            translateY = 0f;
        } else {
            constrainTranslation();
        }
        applyTransform();
    }

    public void fitToScreen() {
        View child = getChildAt(0);
        if (child == null) return;
        float childWidth = child.getWidth();
        float containerWidth = getWidth();
        if (childWidth > 0f && containerWidth > 0f) {
            scaleFactor = Math.min(Math.max(containerWidth / childWidth, MIN_SCALE), 1f);
        } else {
            scaleFactor = 0.6f;
        }
        translateX = 0f;
        translateY = 0f;
        applyTransform();
    }

    public float getCurrentScale() {
        return scaleFactor;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1 || scaleDetector.isInProgress()) return true;

        if (scaleFactor > 1.05f) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = ev.getX();
                    lastTouchY = ev.getY();
                    isCurrentlyPanning = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(ev.getX() - lastTouchX);
                    float dy = Math.abs(ev.getY() - lastTouchY);
                    if (dx > PAN_THRESHOLD_PX || dy > PAN_THRESHOLD_PX) {
                        isCurrentlyPanning = true;
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scaleDetector.isInProgress()) return true;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (scaleFactor > 1.05f) {
                    translateX += event.getX() - lastTouchX;
                    translateY += event.getY() - lastTouchY;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    constrainTranslation();
                    applyTransform();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isCurrentlyPanning = false;
                break;
        }
        return true;
    }

    private void constrainTranslation() {
        View child = getChildAt(0);
        if (child == null) return;
        float scaledW = child.getWidth() * scaleFactor;
        float scaledH = child.getHeight() * scaleFactor;

        float minTransX = scaledW > getWidth() ? -(scaledW - getWidth()) : 0f;
        float minTransY = scaledH > getHeight() ? -(scaledH - getHeight()) : 0f;

        translateX = Math.min(Math.max(translateX, minTransX), 0f);
        translateY = Math.min(Math.max(translateY, minTransY), 0f);
    }

    private void applyTransform() {
        View child = getChildAt(0);
        if (child == null) return;
        child.setPivotX(0f);
        child.setPivotY(0f);
        child.setScaleX(scaleFactor);
        child.setScaleY(scaleFactor);
        child.setTranslationX(translateX);
        child.setTranslationY(translateY);
    }
}
