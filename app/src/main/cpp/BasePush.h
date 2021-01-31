//
// Created by bei on 2021/1/10.
//

#ifndef FFMPEGTEST_BASEPUSH_H
#define FFMPEGTEST_BASEPUSH_H
#include <android/log.h>
#include "SafeQueue.h"
#include "JavaCallHelper.h"
#include "macro.h"
extern "C"{
#include <libavutil/rational.h>
//封装格式，总上下文
#include "libavformat/avformat.h"
//解码器.
#include "libavcodec/avcodec.h"
//#缩放
#include "libswscale/swscale.h"
// 重采样
#include "libswresample/swresample.h"
//时间工具
#include "libavutil/time.h"
//编码转换工具yuv->rgb888
#include "libavutil/imgutils.h"
}

//#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "BeiPlayer", format, ##__VA_ARGS__)
//#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "BeiPlayer", format, ##__VA_ARGS__)

class BasePush {
    typedef void (*codecCallBack)(AVPacket * packet);
public:
    BasePush(AVFormatContext* _avFormatContext):ic(_avFormatContext){
    }
    ~BasePush(){

    };
    static void releaseAvPacket(AVPacket*& packet){
        if(packet){
            av_packet_free(&packet);
            packet = nullptr;
        }
    }

    static void releaseAvFrame(AVFrame*& frame){
        if(frame){
            av_frame_free(&frame);
            frame = nullptr;
        }
    }
    static void syncHandle(queue<AVPacket*>& queue){

    }
    static void syncFrameHandle(queue<AVFrame*>& queue){

    }

    //关闭资源
    virtual void close() = 0;
    //初始化音/视频编码器
//    virtual bool InitCodec() = 0;
//    //编码
//    virtual void encode(AVFrame *frame) = 0;
    //添加轨道
    virtual void addStream() = 0;

public:
    codecCallBack callBack;
    //音频/视频编码器上下文
    AVCodecContext *vc = 0;
    AVFormatContext * ic = 0;
};

#endif //FFMPEGTEST_BASEPUSH_H
