package com.censorchi.utils.imageBlurUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;


public class ImageViewTouchBase extends androidx.appcompat.widget.AppCompatImageView {
    public static final String LOG_TAG = "image";
    protected final float MAX_ZOOM;
    protected final RotateBitmap mBitmapDisplayed;
    protected final Matrix mDisplayMatrix;
    protected final float[] mMatrixValues;
    protected Matrix mBaseMatrix;
    protected Handler mHandler;
    protected float mMaxZoom;
    protected Runnable mOnLayoutRunnable;
    protected Matrix mSuppMatrix;
    protected int mThisHeight;
    protected int mThisWidth;
    private OnBitmapChangedListener mListener;

    public ImageViewTouchBase(Context context) {
        super(context);
        this.mBaseMatrix = new Matrix();
        this.mSuppMatrix = new Matrix();
        this.mHandler = new Handler();
        this.mOnLayoutRunnable = null;
        this.mDisplayMatrix = new Matrix();
        this.mMatrixValues = new float[9];
        this.mThisWidth = -1;
        this.mThisHeight = -1;
        this.mBitmapDisplayed = new RotateBitmap(null, 0);
        this.MAX_ZOOM = 2.0f;
        init();
    }

    public ImageViewTouchBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBaseMatrix = new Matrix();
        this.mSuppMatrix = new Matrix();
        this.mHandler = new Handler();
        this.mOnLayoutRunnable = null;
        this.mDisplayMatrix = new Matrix();
        this.mMatrixValues = new float[9];
        this.mThisWidth = -1;
        this.mThisHeight = -1;
        this.mBitmapDisplayed = new RotateBitmap(null, 0);
        this.MAX_ZOOM = 2.0f;
        init();
    }

    public void setOnBitmapChangedListener(OnBitmapChangedListener listener) {
        this.mListener = listener;
    }

    protected void init() {
        setScaleType(ScaleType.MATRIX);
    }

    public void clear() {
        setImageBitmapReset(null, true);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mThisWidth = right - left;
        this.mThisHeight = bottom - top;
        Runnable r = this.mOnLayoutRunnable;
        if (r != null) {
            this.mOnLayoutRunnable = null;
            r.run();
        }
        if (this.mBitmapDisplayed.getBitmap() != null) {
            getProperBaseMatrix(this.mBitmapDisplayed, this.mBaseMatrix);
            setImageMatrix(getImageViewMatrix());
        }
    }

    public void setImageBitmapReset(Bitmap bitmap, boolean reset) {
        setImageRotateBitmapReset(new RotateBitmap(bitmap, 0), reset, null);
    }

    public void setImageBitmapReset(Bitmap bitmap, boolean reset, Matrix matrix) {
        setImageRotateBitmapReset(new RotateBitmap(bitmap, 0), reset, matrix);
    }

    public void setImageBitmapReset(Bitmap bitmap, int rotation, boolean reset, Matrix matrix) {
        setImageRotateBitmapReset(new RotateBitmap(bitmap, rotation), reset, matrix);
    }

    public void setImageRotateBitmapReset(RotateBitmap bitmap, boolean reset) {
        setImageRotateBitmapReset(bitmap, reset, null);
    }

    public void setImageRotateBitmapReset(RotateBitmap bitmap, boolean reset, Matrix initial_matrix) {
        Log.d(LOG_TAG, "setImageRotateBitmapReset");
        if (getWidth() <= 0) {
            this.mOnLayoutRunnable = new C00481(bitmap, reset, initial_matrix);
            return;
        }
        if (bitmap.getBitmap() != null) {
            getProperBaseMatrix(bitmap, this.mBaseMatrix);
            setImageBitmap(bitmap.getBitmap(), bitmap.getRotation());
        } else {
            this.mBaseMatrix.reset();
            setImageBitmap(null);
        }
        if (reset) {
            this.mSuppMatrix.reset();
            if (initial_matrix != null) {
                this.mSuppMatrix = new Matrix(initial_matrix);
            }
        }
        setImageMatrix(getImageViewMatrix());
        this.mMaxZoom = maxZoom();
        onBitmapChanged(bitmap);
    }

    protected void onBitmapChanged(RotateBitmap bitmap) {
        if (this.mListener != null) {
            this.mListener.onBitmapChanged(bitmap.getBitmap());
        }
    }

    protected float maxZoom() {
        if (this.mBitmapDisplayed.getBitmap() == null) {
            return PinchImageView.MIN_SCALE;
        }
        return Math.max(((float) this.mBitmapDisplayed.getWidth()) / ((float) this.mThisWidth), ((float) this.mBitmapDisplayed.getHeight()) / ((float) this.mThisHeight)) * PinchImageView.MAX_SCALE;
    }

    public RotateBitmap getDisplayBitmap() {
        return this.mBitmapDisplayed;
    }

    public float getMaxZoom() {
        return this.mMaxZoom;
    }

    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap, 0);
    }

    protected void setImageBitmap(Bitmap bitmap, int rotation) {
        super.setImageBitmap(bitmap);
        Drawable d = getDrawable();
        if (d != null) {
            d.setDither(true);
        }
        this.mBitmapDisplayed.setBitmap(bitmap);
        this.mBitmapDisplayed.setRotation(rotation);
    }

    public Matrix getImageViewMatrix() {
        this.mDisplayMatrix.set(this.mBaseMatrix);
        this.mDisplayMatrix.postConcat(this.mSuppMatrix);
        return this.mDisplayMatrix;
    }

    public Matrix getDisplayMatrix() {
        return new Matrix(this.mSuppMatrix);
    }

    protected void getProperBaseMatrix(RotateBitmap bitmap, Matrix matrix) {
        float viewWidth = (float) getWidth();
        float viewHeight = (float) getHeight();
        float w = (float) bitmap.getWidth();
        float h = (float) bitmap.getHeight();
        matrix.reset();
        float scale = Math.min(Math.min(viewWidth / w, 2.0f), Math.min(viewHeight / h, 2.0f));
        matrix.postConcat(bitmap.getRotateMatrix());
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewWidth - (w * scale)) / 2.0f, (viewHeight - (h * scale)) / 2.0f);
    }

    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(this.mMatrixValues);
        return this.mMatrixValues[whichValue];
    }

    protected RectF getBitmapRect() {
        if (this.mBitmapDisplayed.getBitmap() == null) {
            return null;
        }

        Matrix m = getImageViewMatrix();
        RectF rect = new RectF(0.0f, 0.0f, (float) this.mBitmapDisplayed.getBitmap().getWidth(), (float) this.mBitmapDisplayed.getBitmap().getHeight());
        m.mapRect(rect);
        return rect;
    }

    protected float getScale(Matrix matrix) {
        return getValue(matrix, 0);
    }

    public float getRotation() {
        return (float) this.mBitmapDisplayed.getRotation();
    }

    public float getScale() {
        return getScale(this.mSuppMatrix);
    }

    protected void center(boolean horizontal, boolean vertical) {
        if (this.mBitmapDisplayed.getBitmap() != null) {
            RectF rect = getCenter(horizontal, vertical);
            if (rect.left != 0.0f || rect.top != 0.0f) {
                postTranslate(rect.left, rect.top);
            }
        }
    }

    protected RectF getCenter(boolean horizontal, boolean vertical) {
        if (this.mBitmapDisplayed.getBitmap() == null) {
            return new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        }
        RectF rect = getBitmapRect();
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
        return new RectF(deltaX, deltaY, 0.0f, 0.0f);
    }

    protected void postTranslate(float deltaX, float deltaY) {
        this.mSuppMatrix.postTranslate(deltaX, deltaY);
        setImageMatrix(getImageViewMatrix());
    }

    protected void postScale(float scale, float centerX, float centerY) {
        this.mSuppMatrix.postScale(scale, scale, centerX, centerY);
        setImageMatrix(getImageViewMatrix());
    }

    protected void zoomTo(float scale) {
        zoomTo(scale, ((float) getWidth()) / 2.0f, ((float) getHeight()) / 2.0f);
    }

    public void zoomTo(float scale, float durationMs) {
        zoomTo(scale, ((float) getWidth()) / 2.0f, ((float) getHeight()) / 2.0f, durationMs);
    }

    protected void zoomTo(float scale, float centerX, float centerY) {
        if (scale > this.mMaxZoom) {
            scale = this.mMaxZoom;
        }
        postScale(scale / getScale(), centerX, centerY);
        onZoom(getScale());
        center(true, true);
    }

    protected void onZoom(float scale) {
    }

    public void scrollBy(float x, float y) {
        panBy(x, y);
    }

    protected void panBy(float dx, float dy) {
        RectF rect = getBitmapRect();
        RectF srect = new RectF(dx, dy, 0.0f, 0.0f);
        updateRect(rect, srect);
        postTranslate(srect.left, srect.top);
        center(true, true);
    }

    protected void updateRect(RectF bitmapRect, RectF scrollRect) {
        float width = (float) getWidth();
        float height = (float) getHeight();
        if (bitmapRect.top >= 0.0f && bitmapRect.bottom <= height) {
            scrollRect.top = 0.0f;
        }
        if (bitmapRect.left >= 0.0f && bitmapRect.right <= width) {
            scrollRect.left = 0.0f;
        }
        if (bitmapRect.top + scrollRect.top >= 0.0f && bitmapRect.bottom > height) {
            scrollRect.top = (float) ((int) (0.0f - bitmapRect.top));
        }
        if (bitmapRect.bottom + scrollRect.top <= height - 0.0f && bitmapRect.top < 0.0f) {
            scrollRect.top = (float) ((int) ((height - 0.0f) - bitmapRect.bottom));
        }
        if (bitmapRect.left + scrollRect.left >= 0.0f) {
            scrollRect.left = (float) ((int) (0.0f - bitmapRect.left));
        }
        if (bitmapRect.right + scrollRect.left <= width - 0.0f) {
            scrollRect.left = (float) ((int) ((width - 0.0f) - bitmapRect.right));
        }
    }

    protected void scrollBy(float distanceX, float distanceY, float durationMs) {
        float f = durationMs;
        this.mHandler.post(new C00492(f, System.currentTimeMillis(), distanceX, distanceY));
    }

    protected void zoomTo(float scale, float centerX, float centerY, float durationMs) {
        Log.d(LOG_TAG, "zoomTo: " + scale + ", " + centerX + ": " + centerY);
        float f = durationMs;
        this.mHandler.post(new C00503(f, System.currentTimeMillis(), getScale(), (scale - getScale()) / durationMs, centerX, centerY));
    }

    protected enum Command {
        Center,
        Move,
        Zoom,
        Layout,
        Reset
    }

    public interface OnBitmapChangedListener {
        void onBitmapChanged(Bitmap bitmap);
    }

    class C00481 implements Runnable {
        private final RotateBitmap val$bitmap;
        private final Matrix val$initial_matrix;
        private final boolean val$reset;

        C00481(RotateBitmap rotateBitmap, boolean z, Matrix matrix) {
            this.val$bitmap = rotateBitmap;
            this.val$reset = z;
            this.val$initial_matrix = matrix;
        }

        public void run() {
            ImageViewTouchBase.this.setImageBitmapReset(this.val$bitmap.getBitmap(), this.val$bitmap.getRotation(), this.val$reset, this.val$initial_matrix);
        }
    }

    class C00492 implements Runnable {
        private final float val$durationMs;
        private final float val$dx;
        private final float val$dy;
        private final long val$startTime;
        float old_x = 0.0f;
        float old_y = 0.0f;

        C00492(float f, long j, float f2, float f3) {
            this.val$durationMs = f;
            this.val$startTime = j;
            this.val$dx = f2;
            this.val$dy = f3;
        }

        public void run() {
            float currentMs = Math.min(this.val$durationMs, (float) (System.currentTimeMillis() - this.val$startTime));
            float x = Cubic.easeOut(currentMs, 0.0f, this.val$dx, this.val$durationMs);
            float y = Cubic.easeOut(currentMs, 0.0f, this.val$dy, this.val$durationMs);
            ImageViewTouchBase.this.panBy(x - this.old_x, y - this.old_y);
            this.old_x = x;
            this.old_y = y;
            if (currentMs < this.val$durationMs) {
                ImageViewTouchBase.this.mHandler.post(this);
                return;
            }
            RectF centerRect = ImageViewTouchBase.this.getCenter(true, true);
            if (centerRect.left != 0.0f || centerRect.top != 0.0f) {
                ImageViewTouchBase.this.scrollBy(centerRect.left, centerRect.top);
            }
        }
    }

    class C00503 implements Runnable {
        private final float val$centerX;
        private final float val$centerY;
        private final float val$durationMs;
        private final float val$incrementPerMs;
        private final float val$oldScale;
        private final long val$startTime;

        C00503(float f, long j, float f2, float f3, float f4, float f5) {
            this.val$durationMs = f;
            this.val$startTime = j;
            this.val$oldScale = f2;
            this.val$incrementPerMs = f3;
            this.val$centerX = f4;
            this.val$centerY = f5;
        }

        public void run() {
            float currentMs = Math.min(this.val$durationMs, (float) (System.currentTimeMillis() - this.val$startTime));
            ImageViewTouchBase.this.zoomTo(this.val$oldScale + (this.val$incrementPerMs * currentMs), this.val$centerX, this.val$centerY);
            if (currentMs < this.val$durationMs) {
                ImageViewTouchBase.this.mHandler.post(this);
            }
        }
    }
}
