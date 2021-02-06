package com.wanglei.cameralibrary.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.View;

import com.wanglei.cameralibrary.gpuimage.GPUImageFilter;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

public class GLSurfaceTexturePreview extends GLSurfaceView implements GLSurfaceView.Renderer {
    //用来接收摄像头的数据 通过它传输给surfaceview
    private GPUImageFilter magicFilter;
    private SurfaceTexture surfaceTexture;
    private int mOESTextureId;
    private int mWidth;
    private int mHeight;
    private Callback mCallback;
    private float[] mProjectionMatrix = new float[16];
    private float[] mSurfaceMatrix = new float[16];
    private float[] mTransformMatrix = new float[16];
//    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    public GLSurfaceTexturePreview(Context context) {
        super(context);
    }

    public GLSurfaceTexturePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);//设置opgles版本
        setRenderer(this);//设置渲染回调方法
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//设置为手动模式

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
//        surfaceTexture = new SurfaceTexture();
        magicFilter = new GPUImageFilter(MagicFilterType.NONE);
        magicFilter.init(getContext().getApplicationContext());


        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mOESTextureId = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mOESTextureId);
        surfaceTexture = new SurfaceTexture(mOESTextureId);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        GLES20.glViewport(0,0,i,i1);
        //设置surfaceview的宽高
        setSize(i,i1);
        magicFilter.onDisplaySizeChanged(i, i1);
        magicFilter.onInputSizeChanged(i, i1);

//        mOutputAspectRatio = width > height ? (float) width / height : (float) height / width;
//        float aspectRatio = mOutputAspectRatio / mInputAspectRatio;
//        if (width > height) {
//            Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -aspectRatio, aspectRatio, -1.0f, 1.0f);
//        } else {
//            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, -1.0f, 1.0f);
//        }
        mCallback.onSurfaceChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
       //接收帧的刷新
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        surfaceTexture.updateTexImage();

//        surfaceTexture.getTransformMatrix(mSurfaceMatrix);
//        Matrix.multiplyMM(mTransformMatrix, 0, mSurfaceMatrix, 0, mProjectionMatrix, 0);
//        magicFilter.setTextureTransformMatrix(mTransformMatrix);
        magicFilter.onDrawFrame(mOESTextureId);
    }
    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getSurfaceWidth() {
        return mWidth;
    }

    public int getSurfaceHeight() {
        return mHeight;
    }
    public void setCallback(Callback callback){
        mCallback = callback;
    }
    public View getView(){
        return this;
    }
    public interface Callback {
        void onSurfaceCreated();

        void onSurfaceChanged();

        void onDrawFrame();
    }
}
