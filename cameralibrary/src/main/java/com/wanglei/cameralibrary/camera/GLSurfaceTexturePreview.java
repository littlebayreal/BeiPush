package com.wanglei.cameralibrary.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLSurfaceTexturePreview extends GLSurfaceView implements GLSurfaceView.Renderer {
    //用来接收摄像头的数据 通过它传输给surfaceview
    private SurfaceTexture surfaceTexture;
//    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    public GLSurfaceTexturePreview(Context context) {
        super(context);
    }

    public GLSurfaceTexturePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

    @Override
    public void onDrawFrame(GL10 gl10) {

    }
    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }
    public interface Callback {
        void onSurfaceCreated();

        void onSurfaceChanged();

        void onDrawFrame();
    }
}
