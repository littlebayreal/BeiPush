package com.wanglei.cameralibrary.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.wanglei.cameralibrary.R;
import com.wanglei.cameralibrary.gpuimage.GPUImageFilter;
import com.wanglei.cameralibrary.gpuimage.GPUImageGaussPassFilter;
import com.wanglei.cameralibrary.gpuimage.GPUImageGaussianBlurFilter;
import com.wanglei.cameralibrary.gpuimage.GPUImageHueFilter;
import com.wanglei.cameralibrary.gpuimage.GPUImageInputOESFilter;
import com.wanglei.cameralibrary.gpuimage.GPUImageMultiBlurFilter;
import com.wanglei.cameralibrary.gpuimage.advance.MagicBeautyFilter;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterFactory;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;
import com.wanglei.cameralibrary.gpuimage.utils.OpenGLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

public class GLSurfaceTexturePreview implements GLSurfaceView.Renderer {
    private static final String TAG = "GLSurfaceTexturePreview";
    private GLSurfaceView mGLSurfaceView;
    private Context mContext;
    //用来接收摄像头的数据 通过它传输给surfaceview
//    private GPUImageHueFilter magicFilter;
    private GPUImageInputOESFilter magicFilter;
    private GPUImageMultiBlurFilter gpuImageMultiBlurFilter;
    private SurfaceTexture surfaceTexture;
    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    private int mWidth;
    private int mHeight;
    private Callback mCallback;
    private int mDisplayOrientation;
    //    private float[] mProjectionMatrix = new float[16];
    private float[] mSurfaceMatrix = new float[16];
    private float[] mTransformMatrix = new float[16];
//    private float[] mProjectionMatrix = {
//            1, 0, 0, 0,
//            0, 1, 0, 0,
//            0, 0, -1, 0,
//            0, 0, 0, 1
//    };
    //返回的是以列为主排序的矩阵
    private float[] mTransformMatrix_face = {
            0, 1, 0, 0,
            1, 0, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    private float[] mTransformMatrix_back = {
            0, -1, 0, 0,
            -1, 0, 0, 0,
            0, 0, 1, 0,
            1, 1, 0, 1
    };

    //    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    public GLSurfaceTexturePreview(Context context, ViewGroup viewParent) {
        mContext = context;
        final View view = View.inflate(context, R.layout.gl_surface_view, viewParent);
        mGLSurfaceView = view.findViewById(R.id.surface_view);
        final SurfaceHolder holder = mGLSurfaceView.getHolder();

        //noinspection deprecation
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mGLSurfaceView.setEGLContextClientVersion(2);//设置opgles版本
        mGLSurfaceView.setRenderer(this);//设置渲染回调方法
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//设置为手动模式
        Log.i(TAG, "GLSurfaceTexturePreview");
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
//        magicFilter = new GPUImageFilter(MagicFilterType.NONE);
        magicFilter = new GPUImageInputOESFilter(MagicFilterType.MULTI_BLUR);
        magicFilter.init(mContext.getApplicationContext());
//        magicFilter.setBlurSize(1.0f);
        gpuImageMultiBlurFilter = new GPUImageMultiBlurFilter(MagicFilterType.MULTI_BLUR);
        gpuImageMultiBlurFilter.init(mContext.getApplicationContext());

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mOESTextureId = textures[0];

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mOESTextureId);
        surfaceTexture = new SurfaceTexture(mOESTextureId);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                Log.i(TAG, "onFrameAvailable:图像纹理更新");
                mGLSurfaceView.requestRender();
            }
        });
    }

//    private float mOutputAspectRatio;
//    private float mInputAspectRatio;
    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        Log.i(TAG, "onSurfaceChanged");
        GLES20.glViewport(0, 0, i, i1);
        //设置surfaceview的宽高
        setSize(i, i1);
        // TODO: 2021/2/24  暂时设置成一样的 不够严谨
        magicFilter.onDisplaySizeChanged(i, i1);
        magicFilter.onInputSizeChanged(i, i1);

        gpuImageMultiBlurFilter.onDisplaySizeChanged(i,i1);
        gpuImageMultiBlurFilter.onInputSizeChanged(i,i1);
//        gpuImageMultiBlurFilter.setTexelOffsetSize(i,i1);
//        mOutputAspectRatio = i > i1 ? (float) i / i1 : (float) i1 / i;
//        float aspectRatio = mOutputAspectRatio / mInputAspectRatio;
//        if (i > i1) {
//            Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -aspectRatio, aspectRatio, -1.0f, 1.0f);
//        } else {
//            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, -1.0f, 1.0f);
//        }
        if (mCallback != null)
            mCallback.onSurfaceChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        //接收帧的刷新
        surfaceTexture.updateTexImage();
        //获得纹理的矩阵
        surfaceTexture.getTransformMatrix(mSurfaceMatrix);
        Log.i(TAG, "ff: --------------start----------------");
        Log.i(TAG, "ff:" + mSurfaceMatrix.length);
        for (int i = 0; i < mSurfaceMatrix.length; i++) {
            Log.i(TAG, "i position:" + i + "ff:" + mSurfaceMatrix[i]);
        }
        Log.i(TAG, "ff: --------------end----------------");
        Log.i(TAG, "mff: --------------start----------------");
//        Matrix.multiplyMM(mTransformMatrix, 0, mSurfaceMatrix, 0, mProjectionMatrix, 0);
        for (int i = 0; i < mTransformMatrix.length; i++) {
            Log.i(TAG, "mi position:" + i + "mff:" + mTransformMatrix[i]);
        }
        Log.i(TAG, "mff: --------------end----------------");
        magicFilter.setTextureTransformMatrix(mTransformMatrix);
        int textureID = magicFilter.onDrawFrameBuffer(mOESTextureId);
//        gpuImageMultiBlurFilter.setTextureTransformMatrix(mTransformMatrix);
        gpuImageMultiBlurFilter.onDrawFrame(textureID);
    }
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
//        mInputAspectRatio = width > height ?
//                (float) width / height : (float) width / height;
    }

    public int getSurfaceWidth() {
        return mWidth;
    }

    public int getSurfaceHeight() {
        return mHeight;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public View getView() {
        return mGLSurfaceView;
    }
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        switch (mDisplayOrientation){
            case 0:
                break;
            case 90:
                mTransformMatrix = mTransformMatrix_back;
                break;
            case 180:
                break;
            case 270:
                mTransformMatrix = mTransformMatrix_face;
                break;
        }
    }
    public interface Callback {
        void onSurfaceCreated();

        void onSurfaceChanged();

        void onDrawFrame();
    }
}
