package com.censorchi.utils.imageBlurUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.google.android.exoplayer2.util.NalUnitUtil;

public class PinchImageView extends ImageView implements OnTouchListener {
    public static final int GROW = 0;
    public static final float MAX_SCALE = 4.0f;
    public static final float MIN_SCALE = 1.0f;
    public static final int SHRINK = 1;
    public static final int TOUCH_INTERVAL = 0;
    public static final float ZOOM = 1.5f;
    float distCur;
    float distDelta;
    float distPre;
    ImageView im = null;
    Matrix mBaseMatrix = new Matrix();
    Bitmap mBitmap = null;
    int mHeight;
    long mLastGestureTime;
    Matrix mResultMatrix = new Matrix();
    Matrix mSuppMatrix = new Matrix();
    int mTouchSlop;
    int mWidth;
    float xCur;
    float xPre;
    float xSec;
    float yCur;
    float yPre;
    float ySec;

    public PinchImageView(Context context, AttributeSet attr) {
        super(context, attr);
        _init();
    }

    public PinchImageView(Context context) {
        super(context);
        _init();
    }

    public PinchImageView(ImageView im) {
        super(im.getContext());
        _init();
        this.im = im;
        this.im.setScaleType(ScaleType.MATRIX);
        this.im.setOnTouchListener(this);
    }

    public float getScale() {
        return getScale(this.mSuppMatrix);
    }

    public float getScale(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[TOUCH_INTERVAL];
    }

    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        Drawable d = getDrawable();
        if (d != null) {
            d.setDither(true);
        }
        this.mBitmap = bm;
        center(true, true);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mWidth = right - left;
        this.mHeight = bottom - top;
        if (this.mBitmap != null) {
            getProperBaseMatrix(this.mBaseMatrix);
            setImageMatrix(getImageViewMatrix());
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & NalUnitUtil.EXTENDED_SAR;
        int p_count = event.getPointerCount();
        return false;
    }

    private void _init() {
        this.im = this;
        this.mTouchSlop = ViewConfiguration.getTouchSlop();
        this.im.setScaleType(ScaleType.MATRIX);
    }

    public boolean onTouch(View v, MotionEvent event) {
        return onTouchEvent(event);
    }

    public void zoomMax() {
        zoomTo(MAX_SCALE);
    }

    public void zoomMin() {
        zoomTo(MIN_SCALE);
    }

    public synchronized void postTranslate(float dx, float dy) {
        this.mSuppMatrix.postTranslate(dx, dy);
    }

    public void center(boolean horizontal, boolean vertical) {
        if (this.mBitmap != null) {
            Matrix m = getImageViewMatrix();
            RectF rect = new RectF(0.0f, 0.0f, (float) this.mBitmap.getWidth(), (float) this.mBitmap.getHeight());
            m.mapRect(rect);
            float height = rect.height();
            float width = rect.width();
            float deltaX = 0.0f;
            float deltaY = 0.0f;
            if (vertical) {
                int viewHeight = getHeight();
                if (height < ((float) viewHeight)) {
                    deltaY = ((((float) viewHeight) - height) / 2.0f) - rect.top;
                } else if (rect.top > 0.0f) {
                    deltaY = -rect.top;
                } else if (rect.bottom < ((float) viewHeight)) {
                    deltaY = ((float) getHeight()) - rect.bottom;
                }
            }
            if (horizontal) {
                int viewWidth = getWidth();
                if (width < ((float) viewWidth)) {
                    deltaX = ((((float) viewWidth) - width) / 2.0f) - rect.left;
                } else if (rect.left > 0.0f) {
                    deltaX = -rect.left;
                } else if (rect.right < ((float) viewWidth)) {
                    deltaX = ((float) viewWidth) - rect.right;
                }
            }
            postTranslate(deltaX, deltaY);
            setImageMatrix(getImageViewMatrix());
        }
    }

    protected Matrix getImageViewMatrix() {
        this.mResultMatrix.set(this.mBaseMatrix);
        this.mResultMatrix.postConcat(this.mSuppMatrix);
        return this.mResultMatrix;
    }

    protected void zoomTo(float scale) {
        this.mSuppMatrix.setScale(scale, scale, ((float) getWidth()) / 2.0f, ((float) getHeight()) / 2.0f);
        setImageMatrix(getImageViewMatrix());
        center(true, true);
    }

    protected void zoomIn(float scale) {
        if (scale <= MAX_SCALE) {
            this.mSuppMatrix.postScale(ZOOM, ZOOM, ((float) getWidth()) / 2.0f, ((float) getHeight()) / 2.0f);
            setImageMatrix(getImageViewMatrix());
        }
    }

    protected void zoomOut(float scale) {
        if (scale >= MIN_SCALE) {
            float cx = ((float) getWidth()) / 2.0f;
            float cy = ((float) getHeight()) / 2.0f;
            Matrix tmp = new Matrix(this.mSuppMatrix);
            tmp.postScale(0.6666667f, 0.6666667f, cx, cy);
            if (getScale(tmp) < MIN_SCALE) {
                this.mSuppMatrix.setScale(MIN_SCALE, MIN_SCALE, cx, cy);
            } else {
                this.mSuppMatrix.postScale(0.6666667f, 0.6666667f, cx, cy);
            }
            setImageMatrix(getImageViewMatrix());
            center(true, true);
        }
    }

    private void getProperBaseMatrix(Matrix matrix) {
        float viewWidth = (float) getWidth();
        float viewHeight = (float) getHeight();
        float w = (float) this.mBitmap.getWidth();
        float h = (float) this.mBitmap.getHeight();
        matrix.reset();
        float scale = Math.min(Math.min(viewWidth / w, MAX_SCALE), Math.min(viewHeight / h, MAX_SCALE));
        Matrix bitmapMatrix = new Matrix();
        bitmapMatrix.preTranslate((float) (-(this.mBitmap.getWidth() >> SHRINK)), (float) (-(this.mBitmap.getHeight() >> SHRINK)));
        bitmapMatrix.postTranslate((float) (getWidth() / 2), (float) (getHeight() / 2));
        matrix.postConcat(bitmapMatrix);
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewWidth - (w * scale)) / 2.0f, (viewHeight - (h * scale)) / 2.0f);
    }
}
