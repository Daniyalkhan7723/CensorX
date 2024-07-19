package com.censorchi.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.censorchi.utils.imageBlurUtils.ImageViewTouch;

public class ImageViewTouchBrush extends ImageViewTouch {
    private long mBrushDuration = 400;
    private float mBrushEndSize = 10.0f;
    private BrushHighlight mBrushHighlight;
    private OnSingleTapConfirmedListener mSingleTapConfirmedListener;

    public ImageViewTouchBrush(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void init() {
        super.init();
        this.mBrushHighlight = new BrushHighlight(this);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mBrushHighlight.draw(canvas);
    }

    protected OnGestureListener getGestureListener() {
        return new GestureListenerNoDoubleTap();
    }

    protected void onDetachedFromWindow() {
        this.mBrushHighlight.clear();
        this.mBrushHighlight = null;
        super.onDetachedFromWindow();
    }

    public void setOnSingleTapConfirmedListener(OnSingleTapConfirmedListener listener) {
        this.mSingleTapConfirmedListener = listener;
    }

    private void doSomeStuff(float x, float y) {
        if (this.mSingleTapConfirmedListener != null) {
            this.mSingleTapConfirmedListener.onSingleTap(x, y);
        }
        playSoundEffect(0);
        this.mBrushHighlight.addTouch(x, y, this.mBrushDuration, this.mBrushEndSize / 2.0f, this.mBrushEndSize);
    }

    public void setTapRadius(float f) {
        this.mBrushEndSize = f;
    }

    public void setBrushDuration(long duration) {
        this.mBrushDuration = duration;
    }

    public interface OnSingleTapConfirmedListener {
        void onSingleTap(float f, float f2);
    }

    class GestureListenerNoDoubleTap extends GestureListener {
        GestureListenerNoDoubleTap() {
            super();
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            ImageViewTouchBrush.this.doSomeStuff(e.getX(), e.getY());
            return super.onSingleTapConfirmed(e);
        }

        public boolean onDoubleTap(MotionEvent e) {
            ImageViewTouchBrush.this.doSomeStuff(e.getX(), e.getY());
            return false;
        }
    }
}
