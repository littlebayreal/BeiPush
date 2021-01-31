//
// Created by bei on 2021/1/23.
//

#ifndef FFMPEGTEST_VIDEOCHANNEL_H
#define FFMPEGTEST_VIDEOPUSH_H
#include <jni.h>
#include <pthread.h>
#include "macro.h"
#include "BasePush.h"
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
class VideoPush : public BasePush{
    typedef void (*videoCallBack)(AVPacket* packet,AVFrame* yuvFrame,int index);
public:
    VideoPush(AVFormatContext* _avFormatContext);

    ~VideoPush();
    void setVideoCallBack(videoCallBack callBack);
    void openVideoEncodec(int width, int height, int fps, int bitrate);
    void encodeData(int8_t *data,int src_length,int width, int height, bool needRotate,
                    int degree);
    void addStream();
    void setUrl(char* url);
    void close();
private:
    pthread_mutex_t mutex;
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
    int index = 0;
    char* url;

    AVCodec *x264Codec = nullptr;
    AVFrame * pic_in = nullptr;
    videoCallBack callBack;
    AVPacket * enc_pkt = nullptr;
//    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);
//    void sendFrame(int type, uint8_t *payload, int i_payload);

public:
    AVCodecContext *x264CodecContext = nullptr;//编码器
    AVStream * avStream = nullptr;
    int video_stream_index = 0;
};
#endif //FFMPEGTEST_VIDEOCHANNEL_H
