package com.wanglei.cameralibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wanglei.cameralibrary.base.CameraViewImpl;
import com.wanglei.cameralibrary.base.Constants;
import com.wanglei.cameralibrary.base.PreviewImpl;
import com.wanglei.cameralibrary.camera.GLSurfaceTexturePreview;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * 适配opengles
 */
class CameraGLView extends FrameLayout {
    /** The camera device faces the opposite direction as the device's screen. */
    public static final int FACING_BACK = Constants.FACING_BACK;
    /** The camera device faces the same direction as the device's screen. */
    public static final int FACING_FRONT = Constants.FACING_FRONT;
    private int mCameraOrientation;//屏幕旋转方向
    /** Direction the camera faces relative to device screen. */
    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }
    /** Flash will not be fired. */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /** Flash will always be fired during snapshot. */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /** Constant emission of light during preview, auto-focus and snapshot. */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /** Flash will be fired automatically when required. */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /** Flash will be fired in red-eye reduction mode. */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    /** The mode for for the camera device's flash control */
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash {
    }
    private GLSurfaceTexturePreview mGLSurfaceTexturePreview;
//    CameraViewImpl mImpl;
    private final CameraView.CallbackBridge mCallbacks;
//
//    private boolean mAdjustViewBounds;
//
//    private PreviewImpl preview;
//
//    private final DisplayOrientationDetector mDisplayOrientationDetector;
    public CameraGLView(@NonNull Context context) {
        super(context);
    }

    public CameraGLView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraGLView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    private class CallbackBridge implements CameraViewImpl.Callback {

        private final ArrayList<CameraView.Callback> mCallbacks = new ArrayList<>();

        private boolean mRequestLayoutOnOpen;

        CallbackBridge() {
        }

        public void add(CameraView.Callback callback) {
            mCallbacks.add(callback);
        }

        public void remove(CameraView.Callback callback) {
            mCallbacks.remove(callback);
        }

        @Override
        public void onCameraOpened() {
            if (mRequestLayoutOnOpen) {
                mRequestLayoutOnOpen = false;
                requestLayout();
            }
            for (CameraView.Callback callback : mCallbacks) {
                callback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            for (CameraView.Callback callback : mCallbacks) {
                callback.onCameraClosed(CameraView.this);
            }
        }

        @Override
        public void onPreviewSizeConfirm(int width, int height) {
            for (CameraView.Callback callback : mCallbacks) {
                callback.onPreviewSizeConfirm(width,height);
            }
        }

        @Override
        public void onPreviewFrame(byte[] data) {
            for (CameraView.Callback callback : mCallbacks) {
                callback.onPreviewFrame(data);
            }
        }

        @Override
        public void onPictureTaken(byte[] data) {
            for (CameraView.Callback callback : mCallbacks) {
                callback.onPictureTaken(CameraView.this, data);
            }
        }

        public void reserveRequestLayoutOnOpen() {
            mRequestLayoutOnOpen = true;
        }
    }
}
