package com.censorchi.utils.imageBlurUtils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

public class RotateBitmap {
    private Bitmap mBitmap;
    private int mBitmapHeight;
    private int mBitmapWidth;
    private int mHeight;
    private int mRotation;
    private int mWidth;

    public RotateBitmap(Bitmap bitmap, int rotation) {
        this.mRotation = rotation % 360;
        setBitmap(bitmap);
    }

    public int getRotation() {
        return this.mRotation % 360;
    }

    public void setRotation(int rotation) {
        this.mRotation = rotation;
        invalidate();
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        if (this.mBitmap != null) {
            this.mBitmapWidth = bitmap.getWidth();
            this.mBitmapHeight = bitmap.getHeight();
            invalidate();
        }
    }

    private void invalidate() {
        Matrix matrix = new Matrix();
        int cx = this.mBitmapWidth / 2;
        matrix.preTranslate((float) (-cx), (float) (-(this.mBitmapHeight / 2)));
        matrix.postRotate((float) this.mRotation);
        matrix.postTranslate((float) cx, (float) cx);
        RectF rect = new RectF(0.0f, 0.0f, (float) this.mBitmapWidth, (float) this.mBitmapHeight);
        matrix.mapRect(rect);
        this.mWidth = (int) rect.width();
        this.mHeight = (int) rect.height();
    }

    public Matrix getRotateMatrix() {
        Matrix matrix = new Matrix();
        if (this.mRotation != 0) {
            matrix.preTranslate((float) (-(this.mBitmapWidth / 2)), (float) (-(this.mBitmapHeight / 2)));
            matrix.postRotate((float) this.mRotation);
            matrix.postTranslate((float) (this.mWidth / 2), (float) (this.mHeight / 2));
        }
        return matrix;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public void recycle() {
        if (this.mBitmap != null) {
            this.mBitmap.recycle();
            this.mBitmap = null;
        }
    }
}
