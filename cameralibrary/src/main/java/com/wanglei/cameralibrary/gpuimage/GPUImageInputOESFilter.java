package com.wanglei.cameralibrary.gpuimage;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;

import com.wanglei.cameralibrary.R;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;

public class GPUImageInputOESFilter extends GPUImageFilter{
    private int mTransformMatrixHandle;
    private float[] mTransformMatrix;

    public GPUImageInputOESFilter(MagicFilterType type) {
        this(type, R.raw.vertex_oes_input,
                R.raw.fragment_oes_input);
    }

    public GPUImageInputOESFilter(MagicFilterType type, int vertexShader, int fragmentShader) {
        super(type, vertexShader, fragmentShader);
    }

    @Override
    public void onInit() {
        super.onInit();
        mTransformMatrixHandle = GLES20.glGetUniformLocation(getProgram(), "transformMatrix");
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
    }

    @Override
    public void onDrawArraysPre() {
        super.onDrawArraysPre();
        GLES20.glUniformMatrix4fv(mTransformMatrixHandle, 1, false, mTransformMatrix, 0);
    }

    @Override
    public int getTextureType() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    /**
     * 设置SurfaceTexture的变换矩阵
     * @param transformMatrix
     */
    public void setTextureTransformMatrix(float[] transformMatrix) {
        super.setTextureTransformMatrix(transformMatrix);
        mTransformMatrix = transformMatrix;
    }
}
