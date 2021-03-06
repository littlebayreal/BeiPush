package com.wanglei.cameralibrary.gpuimage;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.wanglei.cameralibrary.R;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;
import com.wanglei.cameralibrary.gpuimage.utils.OpenGLUtils;

import java.nio.FloatBuffer;

public class GPUImageMultiBlurFilter extends GPUImageFilter{
    private int mBlurTextureHandle;
    private int mBlurOffsetYHandle;
    private int mScaleHandle;
    private float blurOffsetY;

    // 高斯模糊滤镜
    private GPUImageGaussianBlurFilter mGaussianBlurFilter;
    // 高斯模糊图像缩放半径
    private float mBlurScale = 0.5f;
    private int mBlurTexture;
    private float mScale = 1.2f;

    public GPUImageMultiBlurFilter(MagicFilterType type) {
        this(type, R.raw.vertex, R.raw.fragment_effect_multi_blur);
    }

    public GPUImageMultiBlurFilter(MagicFilterType type, int vertexShader, int fragmentShader) {
        super(type, vertexShader, fragmentShader);
        mGaussianBlurFilter = new GPUImageGaussianBlurFilter(type);
        mGaussianBlurFilter.setBlurSize(1.0f);
        mBlurTexture = OpenGLUtils.NO_TEXTURE;
    }

    @Override
    public void onInit() {
        super.onInit();
        if (mGaussianBlurFilter != null)
            mGaussianBlurFilter.init(getContext());
        if (getProgram() != OpenGLUtils.NOT_INIT) {
            mBlurTextureHandle = GLES30.glGetUniformLocation(getProgram(), "blurTexture");
            Log.i("zzz","blurTexture:"+ mBlurTextureHandle);
            mBlurOffsetYHandle = GLES30.glGetUniformLocation(getProgram(), "blurOffsetY");
            mScaleHandle = GLES30.glGetUniformLocation(getProgram(), "scale");
            setBlurOffset(0.33f);
        }
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
    }

    @Override
    public void onDrawArraysPre() {
        super.onDrawArraysPre();
        Log.i("zxb","mBlurTexture:"+ mBlurTexture);
        if (mBlurTexture != OpenGLUtils.NO_TEXTURE) {
            Log.i("zxb","onDrawArraysPre 激活纹理单元1");
            //激活纹理单元1 并将高斯模糊纹理绑定到上面
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBlurTexture);
            //设置采样器到正确的纹理单元
            GLES20.glUniform1i(mBlurTextureHandle, 1);
        }
        GLES20.glUniform1f(mScaleHandle, mScale);
    }
    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        if (mGaussianBlurFilter != null) {
            mGaussianBlurFilter.onInputSizeChanged((int) (width * mBlurScale), (int) (height * mBlurScale));
        }
    }

    @Override
    public void onDisplaySizeChanged(int width, int height) {
        super.onDisplaySizeChanged(width, height);
        if (mGaussianBlurFilter != null) {
            mGaussianBlurFilter.onDisplaySizeChanged(width, height);
        }
    }

    @Override
    public int onDrawFrame(int textureId) {
        if (mGaussianBlurFilter != null) {
            //先生成模糊纹理
            Log.i("vvv","GPUImageMultiBlurFilter textureId:"+ textureId);
            mBlurTexture = mGaussianBlurFilter.onDrawFrameBuffer(textureId);
            Log.i("vvv","GPUImageMultiBlurFilter mBlurTexture:"+ mBlurTexture);
        }
        //生成想要的纹理并绘制到屏幕
        return super.onDrawFrame(textureId);
    }

    @Override
    public int onDrawFrameBuffer(int textureId) {
        if (mGaussianBlurFilter != null) {
            mBlurTexture = mGaussianBlurFilter.onDrawFrameBuffer(textureId);
        }
        return super.onDrawFrameBuffer(textureId);
    }
    /**
     * 模糊的偏移值
     * @param offsetY 偏移值 0.0 ~ 1.0f
     */
    public void setBlurOffset(float offsetY) {
        if (offsetY < 0.0f) {
            offsetY = 0.0f;
        } else if (offsetY > 1.0f) {
            offsetY = 1.0f;
        }
        this.blurOffsetY = offsetY;
        setFloat(mBlurOffsetYHandle, blurOffsetY);
    }
}
