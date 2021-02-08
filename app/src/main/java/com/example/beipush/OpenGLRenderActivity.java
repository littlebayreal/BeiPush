package com.example.beipush;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wanglei.cameralibrary.CameraGLView;
import com.wanglei.cameralibrary.CameraView;
import com.wanglei.cameralibrary.camera.Camera1ForGL;

/**
 * Created by LiTtleBayReal.
 * Date: 2021/2/6
 * Time: 15:33
 * Explain:
 */
public class OpenGLRenderActivity extends AppCompatActivity implements SensorController.CameraFocusListener{
    private static final String TAG = "OpenGLRenderActivity";
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };
    private int mCurrentFlash;

    private CameraGLView mCameraView;

    private ImageView ivFoucView;

    //加速度传感器
    private SensorController sensorControler;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl_render);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        sensorControler = SensorController.getInstance(this);
        sensorControler.setCameraFocusListener(this);

        ivFoucView = findViewById(R.id.iv_focus);
        mCameraView = findViewById(R.id.camera);
        mCameraView.setFacing(CameraView.FACING_FRONT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_DOWN) {
                    startAutoFocus(event.getRawX(), event.getRawY());
                }
                return true;
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.stop();
        sensorControler.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        livePusher.stopLive();
//        livePusher.release();
//        if (mBackgroundHandler != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                mBackgroundHandler.getLooper().quitSafely();
//            } else {
//                mBackgroundHandler.getLooper().quit();
//            }
//            mBackgroundHandler = null;
//        }
    }
    private Camera1ForGL.Callback mCallback = new Camera1ForGL.Callback() {

        @Override
        public void onCameraOpened() {

        }

        @Override
        public void onCameraClosed() {

        }

        @Override
        public void onPreviewSizeConfirm(int width, int height) {

        }

//        @Override
//        public void onPreviewFrame(byte[] data) {
//            if (data!=null && livePusher!=null && null!=mCameraView){
//                livePusher.onPreviewFrame(data,mCameraView);
//            }
//        }
    };
    //传感器触发
    public void startAutoFocus(float x, float y) {
        //后置摄像头才有对焦功能
        if (mCameraView != null && mCameraView.getFacing()
                == CameraView.FACING_FRONT) {
            return;
        }
        if (x != -1 && y != -1) { //这里有一个对焦的动画
            //设置位置和初始状态
            ivFoucView.setTranslationX(x - (ivFoucView.getWidth()) / 2);
            ivFoucView.setTranslationY(y - (ivFoucView.getWidth()) / 2);
            ivFoucView.clearAnimation();
            //执行动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFoucView, "scaleX", 1.5f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFoucView, "scaleY", 1.5f, 1.0f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.play(scaleX).with(scaleY);
            animSet.setDuration(500);
            animSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ivFoucView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ivFoucView.setVisibility(View.GONE);
                }
            });
            animSet.start();
        }
        mCameraView.autoFocus();
    }

    @Override
    public void onFocus() {
        mCameraView.autoFocus();
    }
}
