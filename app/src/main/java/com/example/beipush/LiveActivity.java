package com.example.beipush;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wanglei.cameralibrary.CameraView;
import com.wanglei.cameralibrary.base.AspectRatio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

public class LiveActivity extends AppCompatActivity implements SensorController.CameraFocusListener,AspectRatioFragment.Listener, BeiPush.DecodeListener {
    private static final String TAG = "LiveActivity";
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

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                    break;
            }
        }
    };
    private BeiPush livePusher;
    private ImageView ivFoucView;
    //加速度传感器
    private SensorController sensorControler;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        mHandlerThread = new HandlerThread("decode thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 0:
                        onDraw(msg.getData().getByteArray("data"));
                        break;
                }
            }
        };

        ivFoucView = findViewById(R.id.iv_focus);
        mCameraView = findViewById(R.id.camera);
        mCameraView.setFacing(CameraView.FACING_FRONT);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
        FloatingActionButton fab = findViewById(R.id.take_picture);
        if (fab != null) {
            fab.setOnClickListener(mOnClickListener);
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        sensorControler = SensorController.getInstance(this);
        sensorControler.setCameraFocusListener(this);
//        livePusher = new BeiPush(this,1200*1024, 15,"rtmp://192.168.2.218/zxb/mylive");
        livePusher = new BeiPush(this,1200*1024, 25,
                "rtmp://sendtc3.douyu.com/live/9561631rB2YpOsSZ?wsSecret=549b837123af85e9bedeeccb7e3e2d79&wsTime=6018eda4&wsSeek=off&wm=0&tw=0&roirecognition=0&record=flv&origin=tct");
        livePusher.setDecodeListener(this);
    }

    public void startLive(View view) {
        Log.i(TAG,"startLive");
        livePusher.startLive();
    }
    public void stopLive(View view){
        Log.i(TAG,"stopLive");
        livePusher.stopLive();
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
    public void onFocus() {
        mCameraView.autoFocus();
    }
    @Override
    protected void onPause() {
        mCameraView.stop();
        sensorControler.onStop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        livePusher.stopLive();
        livePusher.release();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
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
    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.i(TAG,"onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.i(TAG,"onCameraClosed");
        }

        @Override
        public void onPreviewSizeConfirm(int width, int height) {
            if (livePusher!=null){
                livePusher.onPreviewSizeConfirm(width, height,mCameraView);
            }
            Log.i(TAG,"onPreviewSizeConfirm-> width:"+width+" height:"+height);
        }

        @Override
        public void onPreviewFrame(byte[] data) {
//            Log.i(TAG,"onPreviewFrame->"+data);
            if (data!=null && livePusher!=null && null!=mCameraView){
                livePusher.onPreviewFrame(data,mCameraView);
            }
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Toast.makeText(cameraView.getContext(), R.string.picture_taken, Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    savePic(data);
                }
            });
        }
    };

    private void savePic(byte[] data) {
        File pictureDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        @SuppressWarnings("static-access")
        final String picturePath = pictureDir
                + File.separator
                + new DateFormat().format("yyyyMMddHHmmss", new Date())
                .toString() + ".jpg";
        File file = new File(picturePath);
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                    data.length);
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aspect_ratio:
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (mCameraView != null
                        && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                    final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
                    final AspectRatio currentRatio = mCameraView.getAspectRatio();
                    AspectRatioFragment.newInstance(ratios, currentRatio)
                            .show(fragmentManager, FRAGMENT_DIALOG);
                }
                return true;
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (mCameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            mCameraView.setAspectRatio(ratio);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private HandlerThread mHandlerThread = null;
    private Handler mHandler;
//    public void onDecode(byte[] yuvFrame,String type){
//        byte[] temp = new byte[128];
//        System.arraycopy(yuvFrame,200000,temp,0,128);
//        Log.i(TAG,"rgba:"+ Arrays.toString(temp));
//        Log.i(TAG,"type:"+ type);
//
//        Message message = new Message();
//        message.what = 0;
//        Bundle bundle = new Bundle();
//        bundle.putByteArray("data",yuvFrame);
//        message.setData(bundle);
//        mHandler.sendMessage(message);
//    }
    public void onDraw(byte[] yuvFrame){
//        SurfaceHolder surfaceHolder = vv.getHolder();
//        Canvas canvas = surfaceHolder.lockCanvas();
//        ByteBuffer buffer = ByteBuffer.wrap(yuvFrame);
        Log.i(TAG,"生成图片");
        int[] colors = convertByteToColor(yuvFrame);
        Bitmap videoBitmap = Bitmap.createBitmap(colors,540,720, Bitmap.Config.ARGB_8888);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageView)findViewById(R.id.iv_show)).setImageBitmap(videoBitmap);
            }
        });
//        videoBitmap.copyPixelsFromBuffer(buffer);
//        canvas.drawBitmap(videoBitmap, 0, 0, null);
//        surfaceHolder.unlockCanvasAndPost(canvas);
    }
    // 将一个byte数转成int
    // 实现这个函数的目的是为了将byte数当成无符号的变量去转化成int
    public static int convertByteToInt(byte data) {
        int heightBit = (int) ((data >> 4) & 0x0F);
        int lowBit = (int) (0x0F & data);
        return heightBit * 16 + lowBit;
    }
    // 将纯RGB数据数组转化成int像素数组
    public static int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }
        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }
        // 一般RGB字节数组的长度应该是3的倍数，
        // 不排除有特殊情况，多余的RGB数据用黑色0XFF000000填充
        int[] color = new int[size / 3 + arg];
        int red, green, blue;
        int colorLen = color.length;
        if (arg == 0) {
            for (int i = 0; i < colorLen; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);

                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
        } else {
            for (int i = 0; i < colorLen - 1; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
            color[colorLen - 1] = 0xFF000000;
        }
        return color;
    }
    public static void saveBitmap(Bitmap bitmap,String path, String filename) throws IOException
    {
        File file = new File(path + filename);
        if(file.exists()){
            file.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
            {
                out.flush();
                out.close();
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDecode(byte[] yuvFrame, String type) {
        byte[] temp = new byte[128];
        System.arraycopy(yuvFrame,200000,temp,0,128);
        Log.i(TAG,"rgba:"+ Arrays.toString(temp));
        Log.i(TAG,"type:"+ type);

        Message message = new Message();
        message.what = 0;
        Bundle bundle = new Bundle();
        bundle.putByteArray("data",yuvFrame);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }
}
