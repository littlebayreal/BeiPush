package com.wanglei.cameralibrary.camera;

import android.graphics.ImageFormat;
import android.hardware.Camera;

import androidx.collection.SparseArrayCompat;

import com.wanglei.cameralibrary.base.AspectRatio;
import com.wanglei.cameralibrary.base.CameraViewImpl;
import com.wanglei.cameralibrary.base.Constants;
import com.wanglei.cameralibrary.base.PreviewImpl;
import com.wanglei.cameralibrary.base.Size;
import com.wanglei.cameralibrary.base.SizeMap;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Carmera1ForGL {
    private static final int INVALID_CAMERA_ID = -1;

    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();

    static {
        FLASH_MODES.put(Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private int mCameraId;

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);

    Camera mCamera;
    private Callback mCallback;
    private GLSurfaceTexturePreview mGLSurfaceTexturePreview;
    private Camera.Parameters mCameraParameters;

    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private AspectRatio mAspectRatio;

    private boolean mShowingPreview;

    private boolean mAutoFocus;

    private int mFacing;

    private int mFlash;

    private int mDisplayOrientation;

    public Carmera1ForGL(Callback callback, GLSurfaceTexturePreview glSurfaceTexturePreview) throws IOException {
        this.mCallback = callback;
        this.mGLSurfaceTexturePreview = glSurfaceTexturePreview;
        adjustCameraParameters();
        if (mCamera != null)
            mCamera.setPreviewTexture(glSurfaceTexturePreview.getSurfaceTexture());
    }
    public boolean start() {
        chooseCamera();
        openCamera();
//        if (mPreview.isReady()) {
//            setUpPreview();
//        }
        if (mCamera != null)
            mCamera.setPreviewTexture(glSurfaceTexturePreview.getSurfaceTexture());
        adjustCameraParameters();
        mShowingPreview = true;
        mCamera.startPreview();
        return true;
    }

    /**
     * This rewrites {@link #mCameraId} and {@link #mCameraInfo}.
     */
    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraId = i;
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
    }

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }
        mCamera = Camera.open(mCameraId);
        mCameraParameters = mCamera.getParameters();
        // Supported preview sizes
        mPreviewSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
            mPreviewSizes.add(new Size(size.width, size.height));
        }
        // Supported picture sizes;
        mPictureSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
            mPictureSizes.add(new Size(size.width, size.height));
        }
        // AspectRatio
        if (mAspectRatio == null) {
            mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
        }
        //adjustCameraParameters();
        mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));
        mCallback.onCameraOpened();
    }
    private byte[] buffer;
    void adjustCameraParameters() {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
        if (sizes == null) { // Not supported
            mAspectRatio = choosePreviewAspectRatio();
            sizes = mPreviewSizes.sizes(mAspectRatio);
        }
        //预览尺寸选择符合比率的尺寸集合中刚好比预览控件宽高均大一些的
        Size size = chooseOptimalSize(sizes);
        // Always re-apply camera parameters
        // Largest picture size in this ratio
        //拍摄帧往往选择尺寸最大的，那样拍摄的图片更清楚
        SortedSet<Size> picSizes = mPictureSizes.sizes(mAspectRatio);
        if (picSizes == null){
            AspectRatio mPicAspectRatio = choosePicAspectRatio();
            picSizes = mPictureSizes.sizes(mPicAspectRatio);
        }
        final Size pictureSize =picSizes.last();
        if (mShowingPreview) {
            mCamera.stopPreview();
        }
        mCameraParameters.setPreviewSize(size.getWidth(), size.getHeight());
        //回调外部通知设置的预览宽高
        mCallback.onPreviewSizeConfirm(size.getWidth(), size.getHeight());
        //设置预览数据格式为nv21
        mCameraParameters.setPreviewFormat(ImageFormat.NV21);
        buffer = new byte[size.getWidth() * size.getWidth() * 3 / 2];
        //数据缓存区
        mCamera.addCallbackBuffer(buffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));
        setAutoFocusInternal(mAutoFocus);
        setFlashInternal(mFlash);
        mCamera.setParameters(mCameraParameters);
        if (mShowingPreview) {
            mCamera.startPreview();
        }
    }
    @SuppressWarnings("SuspiciousNameCombination")
    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (!mPreview.isReady()) { // Not yet laid out
            return sizes.first(); // Return the smallest size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (isLandscape(mDisplayOrientation)) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) { // Iterate from small to large
            if (desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()) {
                return size;
            }
            result = size;
        }
        return result;
    }
    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     *
     * This calculation is used for orienting the preview
     *
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mCallback.onCameraClosed();
        }
    }
    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == Constants.LANDSCAPE_90 ||
                orientationDegrees == Constants.LANDSCAPE_270);
    }
    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
        if (isCameraOpened()) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashInternal(int flash) {
        if (isCameraOpened()) {
            List<String> modes = mCameraParameters.getSupportedFlashModes();
            String mode = FLASH_MODES.get(flash);
            if (modes != null && modes.contains(mode)) {
                mCameraParameters.setFlashMode(mode);
                mFlash = flash;
                return true;
            }
            String currentMode = FLASH_MODES.get(mFlash);
            if (modes == null || !modes.contains(currentMode)) {
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mFlash = Constants.FLASH_OFF;
                return true;
            }
            return false;
        } else {
            mFlash = flash;
            return false;
        }
    }
    public boolean isCameraOpened() {
        return mCamera != null;
    }
    public interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPreviewSizeConfirm(int width, int height);
//        void onPreviewFrame(byte[] data);

//        void onPictureTaken(byte[] data);
    }
}
