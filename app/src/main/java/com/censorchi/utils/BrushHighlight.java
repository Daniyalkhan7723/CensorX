package com.censorchi.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;


import com.censorchi.utils.imageBlurUtils.Cubic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BrushHighlight {
    static final String LOG_TAG = "image";
    List<Brush> mBrushes = Collections.synchronizedList(new ArrayList());
    private View mContext;

    BrushHighlight(View context) {
        this.mContext = context;
    }

    public void addTouch(float x, float y, long duration, float startSize, float endSize) {
        if (this.mContext != null) {
            Brush brush = new Brush(x, y, duration, startSize, endSize);
            synchronized (this.mBrushes) {
                this.mBrushes.add(brush);
            }
            this.mContext.invalidate();
        }
    }

    public void clear() {
        this.mContext = null;
        this.mBrushes.clear();
    }

    protected void draw(Canvas canvas) {
        boolean shouldInvalidate = false;
        synchronized (this.mBrushes) {
            if (this.mBrushes.size() > 0) {
                shouldInvalidate = true;
                for (int i = this.mBrushes.size() - 1; i >= 0; i--) {
                    Brush brush = (Brush) this.mBrushes.get(i);
                    if (brush.mActive) {
                        brush.draw(canvas);
                    } else {
                        this.mBrushes.remove(i);
                    }
                }
            }
        }
        if (shouldInvalidate) {
            this.mContext.invalidate();
        }
    }

    class Brush {
        private boolean mActive = true;
        private long mDurationMs;
        private float mEndSize;
        private Paint mPaint = new Paint(1);
        private float mStartSize;
        private long mStartTime = System.currentTimeMillis();
        private float mX;
        private float mY;

        public Brush(float x, float y, long duration, float startSize, float endSize) {
            this.mX = x;
            this.mY = y;
            this.mDurationMs = duration;
            this.mStartSize = startSize;
            this.mEndSize = endSize;
            this.mPaint.setColor(-16777216);
        }

        protected void draw(Canvas canvas) {
            if (this.mActive) {
                long now = System.currentTimeMillis();
                float currentMs = (float) Math.min(this.mDurationMs, now - this.mStartTime);
                float radius = this.mStartSize + Cubic.easeOut(currentMs, 0.0f, this.mEndSize - this.mStartSize, (float) this.mDurationMs);
                float alpha = Cubic.easeOut(currentMs, 0.0f, 255.0f, (float) this.mDurationMs);
                if (now - this.mStartTime > this.mDurationMs) {
                    this.mActive = false;
                    return;
                }
                this.mPaint.setAlpha(255 - ((int) alpha));
                canvas.drawCircle(this.mX, this.mY, radius, this.mPaint);
            }
        }
    }
}
