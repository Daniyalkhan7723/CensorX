package com.censorchi.utils.imageBlurUtils;


import static com.censorchi.views.activities.ImageEditActivity.setScroll;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.ViewConfiguration;

import com.google.android.exoplayer2.util.NalUnitUtil;

import cz.msebera.android.httpclient.impl.client.cache.CacheConfig;


public class ImageViewTouch extends ImageViewTouchBase {
    static final float MIN_ZOOM = 0.9f;
    protected float mCurrentScaleFactor;
    protected int mDoubleTapDirection;
    protected GestureDetector mGestureDetector;
    protected GestureListener mGestureListener;
    protected ScaleGestureDetector mScaleDetector;
    protected float mScaleFactor;
    protected ScaleListener mScaleListener;
    protected int mTouchSlop;

    public ImageViewTouch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void init() {
        super.init();
        this.mTouchSlop = ViewConfiguration.getTouchSlop();
        this.mGestureListener = new GestureListener();
        this.mScaleListener = new ScaleListener();
        this.mScaleDetector = new ScaleGestureDetector(getContext(), this.mScaleListener);
        this.mGestureDetector = new GestureDetector(getContext(), this.mGestureListener, null, true);
        this.mCurrentScaleFactor = PinchImageView.MIN_SCALE;
        this.mDoubleTapDirection = 1;
    }

    public void setImageRotateBitmapReset(RotateBitmap bitmap, boolean reset) {
        super.setImageRotateBitmapReset(bitmap, reset);
        this.mScaleFactor = getMaxZoom() / 3.0f;
    }

    public boolean onTouchEvent(MotionEvent event) {
        this.mScaleDetector.onTouchEvent(event);
        if (!this.mScaleDetector.isInProgress()) {
            if (!setScroll){
                this.mGestureDetector.onTouchEvent(event);
            }
        }

        switch (event.getAction() & NalUnitUtil.EXTENDED_SAR) {
            case CacheConfig.DEFAULT_MAX_UPDATE_RETRIES /*1*/:
                if (getScale() < PinchImageView.MIN_SCALE) {
                    zoomTo(PinchImageView.MIN_SCALE, 50.0f);
                    break;
                }
                break;
        }
        return true;
    }

    protected void onZoom(float scale) {
        super.onZoom(scale);
        if (!this.mScaleDetector.isInProgress()) {
            this.mCurrentScaleFactor = scale;
        }
    }

    protected float onDoubleTapPost(float scale, float maxZoom) {
        if (this.mDoubleTapDirection != 1) {
            this.mDoubleTapDirection = 1;
            return PinchImageView.MIN_SCALE;
        } else if ((this.mScaleFactor * 2.0f) + scale <= maxZoom) {
            return scale + this.mScaleFactor;
        } else {
            this.mDoubleTapDirection = -1;
            return maxZoom;
        }
    }

    public class GestureListener extends SimpleOnGestureListener {
       public GestureListener() {
        }

        public boolean onDoubleTap(MotionEvent e) {
            float scale = ImageViewTouch.this.getScale();
            float targetScale = scale;
            targetScale = Math.min(ImageViewTouch.this.getMaxZoom(), Math.max(ImageViewTouch.this.onDoubleTapPost(scale, ImageViewTouch.this.getMaxZoom()), ImageViewTouch.MIN_ZOOM));
            ImageViewTouch.this.mCurrentScaleFactor = targetScale;
            ImageViewTouch.this.zoomTo(targetScale, e.getX(), e.getY(), 200.0f);
            ImageViewTouch.this.invalidate();
            return super.onDoubleTap(e);
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null || e1.getPointerCount() > 1 || e2.getPointerCount() > 1 || ImageViewTouch.this.mScaleDetector.isInProgress() || ImageViewTouch.this.getScale() == PinchImageView.MIN_SCALE) {
                return false;
            }

            ImageViewTouch.this.scrollBy(-distanceX, -distanceY);
            ImageViewTouch.this.invalidate();
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)  {
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1 || ImageViewTouch.this.mScaleDetector.isInProgress()) {
                return false;
            }
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(velocityX) > 800.0f || Math.abs(velocityY) > 800.0f) {
                ImageViewTouch.this.scrollBy(diffX / 2.0f, diffY / 2.0f, 300.0f);
                ImageViewTouch.this.invalidate();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    class ScaleListener extends SimpleOnScaleGestureListener {
        ScaleListener() {
        }

        public boolean onScale(ScaleGestureDetector detector) {
            float span = detector.getCurrentSpan() - detector.getPreviousSpan();
            float targetScale = Math.min(ImageViewTouch.this.getMaxZoom(), Math.max(ImageViewTouch.this.mCurrentScaleFactor * detector.getScaleFactor(), ImageViewTouch.MIN_ZOOM));
            ImageViewTouch.this.zoomTo(targetScale, detector.getFocusX(), detector.getFocusY());
            ImageViewTouch.this.mCurrentScaleFactor = Math.min(ImageViewTouch.this.getMaxZoom(), Math.max(targetScale, ImageViewTouch.MIN_ZOOM));
            ImageViewTouch.this.mDoubleTapDirection = 1;
            ImageViewTouch.this.invalidate();
            return true;
        }

    }
}
