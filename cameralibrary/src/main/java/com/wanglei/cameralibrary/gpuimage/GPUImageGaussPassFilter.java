package com.wanglei.cameralibrary.gpuimage;

import android.content.Context;
import android.opengl.GLES20;

import com.wanglei.cameralibrary.R;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;

public class GPUImageGaussPassFilter extends GPUImageFilter{
    protected float mBlurSize = 1f;

    private int mTexelWidthOffsetHandle;
    private int mTexelHeightOffsetHandle;

    private float mTexelWidth;
    private float mTexelHeight;

    public GPUImageGaussPassFilter(MagicFilterType type) {
        this(type, R.raw.vertex_gaussian_pass,
                R.raw.fragment_gaussian_pass);
    }

    public GPUImageGaussPassFilter(MagicFilterType type, int vertexShader, int fragmentShader) {
        super(type, vertexShader, fragmentShader);
    }

    @Override
    public void onInit() {
        super.onInit();
        mTexelWidthOffsetHandle = GLES20.glGetUniformLocation(getProgram(), "texelWidthOffset");
        mTexelHeightOffsetHandle = GLES20.glGetUniformLocation(getProgram(), "texelHeightOffset");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }
    /**
     * 设置模糊半径大小，默认为1.0f
     * @param blurSize
     */
    public void setBlurSize(float blurSize) {
        mBlurSize = blurSize;
    }
    /**
     * 设置高斯模糊的宽高
     * @param width
     * @param height
     */
    public void setTexelOffsetSize(float width, float height) {
        mTexelWidth = width;
        mTexelHeight = height;
        if (mTexelWidth != 0) {
            setFloat(mTexelWidthOffsetHandle, mBlurSize / mTexelWidth);
        } else {
            setFloat(mTexelWidthOffsetHandle, 0.0f);
        }
        if (mTexelHeight != 0) {
            setFloat(mTexelHeightOffsetHandle, mBlurSize / mTexelHeight);
        } else {
            setFloat(mTexelHeightOffsetHandle, 0.0f);
        }
    }
}
