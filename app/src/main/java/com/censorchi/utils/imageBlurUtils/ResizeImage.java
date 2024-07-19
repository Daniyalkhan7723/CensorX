package com.censorchi.utils.imageBlurUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.Toast;

import com.censorchi.utils.imageBlurUtils.PinchImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ResizeImage {
    float Orientation;
    private Context context;
    private int imageHeight;
    private int imageWidth;

    public ResizeImage(Context context) {
        this.context = context;
    }

    private void getAspectRatio(String str, int i) {
        float f;
        float f2;
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(str, options);
        float f3 = ((float) options.outWidth) / ((float) options.outHeight);
        if (f3 > PinchImageView.MIN_SCALE) {
            f = (float) i;
            f2 = f / f3;
        } else {
            f2 = (float) i;
            f = f2 * f3;
        }
        this.imageWidth = (int) f;
        this.imageHeight = (int) f2;
    }

    private float getImageOrientation(String str) {
        try {
            int attributeInt = new ExifInterface(str).getAttributeInt("Orientation", 1);
            if (attributeInt == 6) {
                return 90.0f;
            }
            if (attributeInt == 3) {
                return 180.0f;
            }
            if (attributeInt == 8) {
                return 270.0f;
            }
            return 0.0f;
        } catch (IOException e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    private Bitmap getResizedOriginalBitmap(String str, int i, int i2) {
        int i3 = 1;
        try {
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(str), null, options);
            int i4 = options.outWidth;
            int i5 = options.outHeight;
            while (i4 / 2 > i) {
                i4 /= 2;
                i5 /= 2;
                i3 *= 2;
            }
            float f = ((float) i) / ((float) i4);
            float f2 = ((float) i2) / ((float) i5);
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inSampleSize = i3;
            options.inScaled = false;
            options.inPreferredConfig = Config.ARGB_8888;
            Bitmap decodeStream = BitmapFactory.decodeStream(new FileInputStream(str), null, options);
            if (decodeStream == null) {
                return null;
            }
            Matrix matrix = new Matrix();
            matrix.postScale(f, f2);
            matrix.postRotate(this.Orientation);
            return Bitmap.createBitmap(decodeStream, 0, 0, decodeStream.getWidth(), decodeStream.getHeight(), matrix, true);
        } catch (FileNotFoundException e) {
            Toast.makeText(this.context, "Image format not supported...please choose other image...", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public Bitmap getBitmap(String str, int i) {
        this.Orientation = getImageOrientation(str);
        getAspectRatio(str, i);
        return getResizedOriginalBitmap(str, this.imageWidth, this.imageHeight);
    }
}
