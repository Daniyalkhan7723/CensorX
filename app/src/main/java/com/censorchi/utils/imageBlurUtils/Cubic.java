package com.censorchi.utils.imageBlurUtils;


public class Cubic {
    public static float easeOut(float time, float start, float end, float duration) {
        time = (time / duration) - PinchImageView.MIN_SCALE;
        return ((((time * time) * time) + PinchImageView.MIN_SCALE) * end) + start;
    }
}
