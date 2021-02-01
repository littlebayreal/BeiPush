package com.example.beipush;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.wanglei.cameralibrary.CameraView;

import java.io.IOException;
import java.util.Arrays;

import static com.example.beipush.LiveActivity.convertByteToColor;
import static com.example.beipush.LiveActivity.saveBitmap;

class BeiPush implements AudioLive.OnAudioCaptureListener{
   private static final String TAG = "BeiPush";
   static {
       System.loadLibrary("native-lib");
   }
   private AudioLive audioLive;
   private Activity activity;
   private int mBitrate;
   private int mFps;
   private boolean isLiving;
   private int mWidth;
   private int mHeight;
   private DecodeListener mDecodeListener;
   public BeiPush(Activity activity, int bitrate,
                  int fps,String url) {
      this.activity = activity;
      this.mBitrate = bitrate;
      this.mFps = fps;
      beiPushInit(url);
      //初始化录音
      audioLive = new AudioLive(this);
      audioLive.setOnAudioCaptureListener(this);
   }
   //开始直播：推送数据
   public void startLive() {
      Log.i(TAG,"开始推送");
      beiPushStart();
   }

   //NDK回调java
   public void onPrepare(int isSuccess){
      if(isSuccess == 1){
         isLiving = true;
         Log.i(TAG,"rtmp链接成功");
         audioLive.startLive();
      }else {
         activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               Toast.makeText(activity,"rtmp创建或者链接失败",Toast.LENGTH_LONG).show();
            }
         });
      }
   }

   //停止直播
   public void stopLive(){
      isLiving = false;
      audioLive.stopLive();
      beiPushStop();
   }

   public void release(){
      audioLive.release();
      beiPushRelease();
   }

   public void onPreviewSizeConfirm(int w, int h,CameraView mCameraView) {
      mWidth = w;
      mHeight = h;
      //初始化编码器
      int mRotation = mCameraView.getCameraOrientation();//角度
      Log.i(TAG,"镜头角度:"+ mRotation);
      int mCameraId = mCameraView.getFacing();//前置还是后置摄像头
      switch (mRotation) {
         //暂时只支持竖屏
         case Surface.ROTATION_0:
            //rotation90(data);
            if (mCameraId == CameraView.FACING_BACK) {
               //后置摄像头顺时针旋转90度
               needRotate = true;
               degree = 90;
            } else {
               //前置摄像头
               //逆时针旋转90度,相当于顺时针旋转270
               needRotate = true;
               degree = 270;
            }
            break;
      }
      if (needRotate){
         mWidth = h;
         mHeight = w;
      }
      beiPushSetVideoEncoderInfo(mWidth, mHeight, mFps, mBitrate);
   }

   private boolean needRotate = false;
   private int degree = 0;
   public void onPreviewFrame(byte[] nv21, CameraView mCameraView) {
      if (isLiving) {
         int mRotation = mCameraView.getCameraOrientation();//角度
         Log.i(TAG,"镜头角度:"+ mRotation);
         int mCameraId = mCameraView.getFacing();//前置还是后置摄像头
         switch (mRotation) {
            case Surface.ROTATION_0:
               //rotation90(data);
               if (mCameraId == CameraView.FACING_BACK) {
                  //后置摄像头顺时针旋转90度
                  needRotate = true;
                  degree = 90;
               } else {
                  //逆时针旋转90度,相当于顺时针旋转270
                  needRotate = true;
                  degree = 270;
               }
               break;
         }
         Log.i(TAG,"mHeight:"+ mCameraView.getHeight() + "mWidth:"+mCameraView.getWidth() + "needRotate:" + needRotate+"degree" + degree);
         //将相机预览的数据进行编码
         beiPushSendVideo(nv21,mCameraView.getWidth(),mCameraView.getHeight(),needRotate,degree);
      }
   }

   @Override
   public void onAudioFrameCaptured(byte[] bytes) {
      beiPushSendAudio(bytes);
   }
public void onDecode(byte[] yuvFrame,String type){
//   int[] colors = convertByteToColor(yuvFrame);
//   Bitmap videoBitmap = Bitmap.createBitmap(colors,1440,1080, Bitmap.Config.ARGB_8888);
   Log.i(TAG,"保存图片 Bitmap 大小:"+ yuvFrame.length);
   if (mDecodeListener != null)
        mDecodeListener.onDecode(yuvFrame,type);
//        videoBitmap.copyPixelsFromBuffer(buffer);
//   try {
//      Log.i("保存图片"+ Environment.getExternalStorageDirectory()+"/DCIM","test.png");
//      saveBitmap(videoBitmap, Environment.getExternalStorageDirectory()+"/DCIM","test.png");
//   } catch (IOException e) {
//      e.printStackTrace();
//   }
}
   public native void beiPushInit(String path);
   public native void beiPushStart();
   public native void beiPushStop();
   public native void beiPushRelease();
   public native void beiPushSendVideo(byte[] data,int width,int height,boolean needRotate,int degree);
   public native void beiPushSendAudio(byte[] data);
   public native void beiPushSetVideoEncoderInfo(int width, int height, int fps, int bitrate);
   public native void beiPushSetAudioEncoderInfo(int sampleRateInHz, int channels);
   public native int getInputSamples();

   public void setDecodeListener(DecodeListener decodeListener){
      this.mDecodeListener = decodeListener;
   }
   public interface DecodeListener{
      void onDecode(byte[] yuvFrame,String type);
   }
}
