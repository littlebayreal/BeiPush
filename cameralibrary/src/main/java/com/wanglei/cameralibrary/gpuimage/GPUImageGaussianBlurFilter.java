package com.wanglei.cameralibrary.gpuimage;

import android.content.Context;

import com.wanglei.cameralibrary.R;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;
import com.wanglei.cameralibrary.gpuimage.utils.OpenGLUtils;

public class GPUImageGaussianBlurFilter extends GPUImageFilter{
    private GPUImageGaussPassFilter mVerticalPassFilter;
    private GPUImageGaussPassFilter mHorizontalPassFilter;
    private int mCurrentTexture;
    public GPUImageGaussianBlurFilter(MagicFilterType type) {
        super(type, R.raw.vertex_beauty_blur, R.raw.fragment_beauty_blur);
        initFilters(type);
    }

    public GPUImageGaussianBlurFilter(MagicFilterType type, int vertexShader, int fragmentShader) {
        super(type, vertexShader, fragmentShader);
        initFilters(type,vertexShader, fragmentShader);
    }
    private void initFilters(MagicFilterType type) {
        mVerticalPassFilter = new GPUImageGaussPassFilter(type);
        mHorizontalPassFilter = new GPUImageGaussPassFilter(type);
    }

    private void initFilters(MagicFilterType type,int vertexShader, int fragmentShader) {
        mVerticalPassFilter = new GPUImageGaussPassFilter(type, vertexShader, fragmentShader);
        mHorizontalPassFilter = new GPUImageGaussPassFilter(type, vertexShader, fragmentShader);
    }

    @Override
    protected void onInit() {
        super.onInit();
        if (mVerticalPassFilter != null) {
            mVerticalPassFilter.init(getContext());
        }
        if (mHorizontalPassFilter != null) {
            mHorizontalPassFilter.init(getContext());
        }
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        if (mVerticalPassFilter != null) {
            mVerticalPassFilter.onInputSizeChanged(width, height);
            mVerticalPassFilter.setTexelOffsetSize(0, height);
        }
        if (mHorizontalPassFilter != null) {
            mHorizontalPassFilter.onInputSizeChanged(width, height);
            mHorizontalPassFilter.setTexelOffsetSize(width, 0);
        }
    }

    @Override
    public int onDrawFrameBuffer(int textureId) {
        mCurrentTexture = textureId;
        if (mCurrentTexture == OpenGLUtils.NO_TEXTURE) {
            return mCurrentTexture;
        }
        //绘制高斯模糊到texture
        if (mVerticalPassFilter != null) {
            mCurrentTexture = mVerticalPassFilter.onDrawFrameBuffer(mCurrentTexture);
        }
        if (mHorizontalPassFilter != null) {
            mCurrentTexture = mHorizontalPassFilter.onDrawFrameBuffer(mCurrentTexture);
        }
        return mCurrentTexture;
    }

    @Override
    public int onDrawFrame(int cameraTextureId) {
        if (cameraTextureId == OpenGLUtils.NO_TEXTURE) {
            return -1;
        }
        mCurrentTexture = cameraTextureId;
        if (mVerticalPassFilter != null) {
            mCurrentTexture = mVerticalPassFilter.onDrawFrameBuffer(mCurrentTexture);
        }
        if (mHorizontalPassFilter != null) {
            return mHorizontalPassFilter.onDrawFrame(mCurrentTexture);
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVerticalPassFilter != null) {
            mVerticalPassFilter.destroy();
        }
        if (mHorizontalPassFilter != null) {
            mHorizontalPassFilter.destroy();
        }
    }
    /**
     * 设置模糊半径大小，默认为1.0f
     * @param blurSize
     */
    public void setBlurSize(float blurSize) {
        if (mVerticalPassFilter != null) {
            mVerticalPassFilter.setBlurSize(blurSize);
        }
        if (mHorizontalPassFilter != null) {
            mHorizontalPassFilter.setBlurSize(blurSize);
        }
    }
}
