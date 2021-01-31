//
// Created by bei on 2021/1/31.
//

#ifndef BEIPUSH_AUDIOPUSH_H
#define BEIPUSH_AUDIOPUSH_H
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
}
class AudioPush : public BasePush{
    typedef void (*AudioCallBack)(AVPacket * packet);
public:
    AudioPush(AVFormatContext* _avFormatContext);
    ~AudioPush();
    void setAudioCallBack(AudioCallBack callBack);
    u_long getInputSamples();
    AVPacket * getAudioTag();
    void encodeData(int8_t *data);
    void setAudioEncInfo(int samplesInHZ, int channels);
    void setUrl(char* url);
    void addStream();
    void close();
private:
    unsigned long inputSamples;
    unsigned long maxOutputBytes;
    int mChannels;//声道数量
    int mSampleInHz;
    AudioCallBack callBack;
    AVCodec *audioCodec = nullptr;
    u_char *outPutBuffer = 0;//编码后的数据存放buffer
    SwrContext *swrContext;//音频重采样上下文
    AVFrame * pcmAvFrame;
    AVPacket * aac_pkt = nullptr;
    int audioIndex;
    char* url;
public:
    AVCodecContext *audioCodecContext = nullptr;//编码器
    AVStream * avStream = nullptr;
    int audio_stream_index = 0;
};

#endif //BEIPUSH_AUDIOPUSH_H
