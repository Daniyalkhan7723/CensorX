package com.censorchi.utils;

import com.sherazkhilji.videffects.Constants;
import com.sherazkhilji.videffects.interfaces.Filter;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class BlurEffect2 implements Filter {

    private int mMaskSize;
    private String shaderString;

    public BlurEffect2(int radius, int width, int height) {
        float hStep = 1.0f / width;
        float vStep = 1.0f / height;
        this.mMaskSize = radius;


        shaderString = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                //"in" attributes from our vertex shader
                "varying vec2 vTextureCoord;\n" +

                //declare uniforms 
                "uniform samplerExternalOES sTexture;\n" +

                "float normpdf(in float x, in float sigma) {\n" +
                "    return 0.39894 * exp(-0.5 * x * x / (sigma * sigma)) / sigma;\n" +
                "}\n" +


                "void main() {\n" +
                "    vec3 c = texture2D(sTexture, vTextureCoord).rgb;\n" +

                //declare stuff
                "    const int mSize = " + mMaskSize + ";\n" +
                "    const int kSize = (mSize - 1) / 2;\n" +
                "    float kernel[ mSize];\n" +
                "    vec3 final_colour = vec3(0.0);\n" +

                //create the 1-D kernel
                "    float sigma = 7.0;\n" +
                "    float Z = 0.0;\n" +
                "    for (int j = 0; j <= kSize; ++j) {\n" +
                "        kernel[kSize + j] = kernel[kSize - j] = normpdf(float(j), sigma);\n" +
                "    }\n" +

                //get the normalization factor (as the gaussian has been clamped)
                "    for (int j = 0; j < mSize; ++j) {\n" +
                "        Z += kernel[j];\n" +
                "    }\n" +

                //read out the texels
                "    for (int i = -kSize; i <= kSize; ++i) {\n" +
                "        for (int j = -kSize; j <= kSize; ++j) {\n" +
                "            final_colour += kernel[kSize + j] * kernel[kSize + i] * texture2D(sTexture, (vTextureCoord.xy + vec2(float(i)*" + hStep + ", float(j)*" + vStep + "))).rgb;\n" +
                "        }\n" +
                "    }\n" +

                "    gl_FragColor = vec4(final_colour / (Z * Z), 1.0);\n" +
                "}";

    }


    @Override
    public String getVertexShader() {
        return Constants.DEFAULT_VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return String.format(Locale.ENGLISH, shaderString,
                ThreadLocalRandom.current().nextFloat(),
                ThreadLocalRandom.current().nextFloat(),
                mMaskSize);
    }

    @Override
    public void setIntensity(float v) {
        this.mMaskSize = Math.round(v);
    }
}