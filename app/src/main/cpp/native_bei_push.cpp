//
// Created by GNNBEI on 2021/1/22.
//
#include <jni.h>
#include <string>
#include "SafeQueue.h"

#include <pthread.h>
#include "macro.h"
#include "AudioPush.h"
#include "VideoPush.h"
#include "BasePush.h"
#include "JavaCallHelper.h"
//#include "WYuvUtils.h"
extern "C" {
//封装格式，总上下文
#include "libavformat/avformat.h"
//解码器.
#include "libavcodec/avcodec.h"
//缩放
#include "libswscale/swscale.h"
// 重采样
#include "libswresample/swresample.h"
}

SafeQueue<AVPacket *> packets;//存储已经编码后的数据
VideoPush *videoPush = 0;
AudioPush *audioPush = 0;
int isStart = 0;
char *url = 0;
pthread_t pid_start;//从packets中取出stmp包发送

int readyPushing = 0;
uint32_t start_time;

JavaVM *javaVm = nullptr;
JavaCallHelper *javaCallHelper = 0;

//封装器
AVFormatContext *ic = NULL;

int JNI_OnLoad(JavaVM *vm, void *r) {
    javaVm = vm;
    return JNI_VERSION_1_6;
}

void releasePacketCallBack(AVPacket **packet) {
    if (packet) {
        BasePush::releaseAvPacket(*packet);
    }
}

void callBack(AVPacket *packet, AVFrame *yuvFrame, int index) {
    if (packet) {
        LOGE("callBack packets.push(packet) %p",&packet);
        //ffmpeg中没有这个操作
//        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
    LOGI("callBack packet pts:%lld", packet->pts);
//    if(index == 10){
//        AVFrame *pFrameYUV = av_frame_alloc();
//        uint8_t *out_buffer = (unsigned char *) av_malloc(
//                av_image_get_buffer_size(AV_PIX_FMT_RGB24,videoPush->x264CodecContext->width, videoPush->x264CodecContext->height, 1));
//
//        av_image_fill_arrays(pFrameYUV->data, pFrameYUV->linesize, out_buffer,
//                             AV_PIX_FMT_RGB24, videoPush->x264CodecContext->width, videoPush->x264CodecContext->height, 1);
//
//        SwsContext *img_convert_ctx = sws_getContext(540, 720, AV_PIX_FMT_YUV420P,
//                                                     540, 720, AV_PIX_FMT_RGB24,
//                                                     SWS_BICUBIC, NULL, NULL, NULL);
//        LOGI("yuvframe linesize:%d",yuvFrame->linesize[0]);
//        LOGI("x264CodecContext height:%d", videoPush->x264CodecContext->height);
//        sws_scale(img_convert_ctx, (const uint8_t *const *) yuvFrame->data, yuvFrame->linesize,
//                  0,
//                  videoPush->x264CodecContext->height,
//                  pFrameYUV->data, pFrameYUV->linesize);
//        char type[10] ="关键帧";
//        if(javaCallHelper)
//        javaCallHelper->onDecode(THREAD_CHILD,pFrameYUV->data[0],type);
//        av_frame_free(&yuvFrame);
//    }
}
void audioCallBack(AVPacket* packet){
    if (packet) {
        LOGE("callBack packets.push(packet) %p",&packet);
        //ffmpeg中没有这个操作
//        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}
void *start(void *args) {
    LOGI("正常链接服务器，可以推流了");
    //正常链接服务器，可以推流了
    readyPushing = 1;
    //通知java层
    if (javaCallHelper) {
        javaCallHelper->onPrepare(THREAD_CHILD, 1);
    }
    start_time = av_gettime();//记录开始推流时间
    packets.setWork(1);
    AVPacket *packet;
    //第一个数据是发送aac解码数据包
//    callBack(audioLive->getAudioTag());
    while (readyPushing) {//不断从队列取出数据进行发送
        LOGI("不断从队列取出数据进行发送");
        int ret = packets.pop(packet);
        if (!readyPushing) {
            break;
        }
        if (!ret) {
            LOGI("packet 已经没有了 继续循环");
            continue;
        }
        AVRational srcTimeBase;
        AVRational desTimeBase;

        //判断音视频
        if (packet->stream_index == videoPush->video_stream_index) {
            srcTimeBase = videoPush->vc->time_base;
            desTimeBase = videoPush->avStream->time_base;
        }else if (packet->stream_index == audioPush->audio_stream_index){
            srcTimeBase = videoPush->vc->time_base;
            desTimeBase = audioPush->avStream->time_base;
        }else {
            continue;
        }
        packet->pts = av_rescale_q(packet->pts, srcTimeBase, desTimeBase);
        LOGE("pts:%lld", packet->pts);
//        packet->dts = av_rescale_q(packet->dts, srcTimeBase, desTimeBase);
        packet->dts = packet->pts;
        LOGE("dts:%lld,pts:%lld", packet->dts,packet->pts);
        packet->duration = av_rescale_q(packet->duration, srcTimeBase, desTimeBase);
        packet->pos = -1;
        //FIX：No PTS (Example: Raw H.264)
        //Simple Write PTS
//        if(packet->pts==AV_NOPTS_VALUE){
//            //Write PTS
//            srcTimeBase=ic->streams[videoPush->video_stream_index]->time_base;
//            //Duration between 2 frames (us)
//            int64_t calc_duration=(double)AV_TIME_BASE/av_q2d(ic->streams[videoPush->video_stream_index]->r_frame_rate);
//            //Parameters
//            packet->pts=(double)(packet->pts*calc_duration)/(double)(av_q2d(srcTimeBase)*AV_TIME_BASE);
//            packet->dts=packet->pts;
//            packet->duration=(double)calc_duration/(double)(av_q2d(srcTimeBase)*AV_TIME_BASE);
//        }
//        //Important:Delay
//        if(packet->stream_index==videoPush->video_stream_index){
//            srcTimeBase=ic->streams[videoPush->video_stream_index]->time_base;
//            AVRational time_base_q={1,AV_TIME_BASE};
//            int64_t pts_time = av_rescale_q(packet->dts, srcTimeBase, time_base_q);
//            int64_t now_time = av_gettime() - start_time;
//            if (pts_time > now_time)
//                av_usleep(pts_time - now_time);
//
//        }
//
////        in_stream  = ifmt_ctx->streams[pkt.stream_index];
////        out_stream = ofmt_ctx->streams[pkt.stream_index];
//        /* copy packet */
//        //Convert PTS/DTS
//        packet->pts = av_rescale_q_rnd(packet->pts, srcTimeBase, desTimeBase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
//        packet->dts = av_rescale_q_rnd(packet->dts, srcTimeBase, desTimeBase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
//        packet->duration = av_rescale_q(packet->duration, srcTimeBase, desTimeBase);
//        LOGE("pts:%lld", packet->pts);
//        LOGE("dts:%lld", packet->dts);
//        packet->pos = -1;
        //发送数据
        int result = av_interleaved_write_frame(ic, packet);
        av_packet_free(&packet);//发送完及时释放内存
        if (result != 0) {
            LOGE("发送数据失败:%d", result);
            break;
        } else {
            LOGI("发送成功");
        }
    }
    LOGI("结束了推流线程");
    isStart = 0;
    readyPushing = 0;
    packets.setWork(0);
    packets.clear();
    if (packet) {
        releasePacketCallBack(&packet);
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushInit(JNIEnv *env, jobject thiz, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    url = new char[strlen(path) + 1];
    strcpy(url, path);
    LOGE("url地址 %s", url);
    javaCallHelper = new JavaCallHelper(javaVm, env, thiz);
    av_register_all();
    avformat_network_init();
    //在这个方法中进行构造 ffmpeg的输出参数
    //output initialize
    ///封装器设置
    int result = avformat_alloc_output_context2(&ic, 0, "flv", url);
    if (result < 0) {
        LOGE("封装器初始化失败");
        return;
    }
    videoPush = new VideoPush(ic);
    videoPush->setVideoCallBack(callBack);
    videoPush->setUrl(url);
    audioPush = new AudioPush(ic);
    audioPush->setAudioCallBack(audioCallBack);
    audioPush->setUrl(url);

    packets.setReleaseCallBack(releasePacketCallBack);
    env->ReleaseStringUTFChars(path_, path);
    LOGE("Init success");
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushStart(JNIEnv *env, jobject instance) {
    if (isStart) {
        return;
    }
    isStart = 1;

    //添加视频流
    videoPush->addStream();
    //添加音频流
    audioPush->addStream();
//    if (audioIndex < 0)
//    {
//        cout << inUrl << "添加音频流 failed" << endl;
//    }
//
//    cout << inUrl << "添加音频流 success" << endl;
    ///打开网络IO流通道
    int result = avio_open(&ic->pb, url, AVIO_FLAG_WRITE);
    if (result < 0) {
        LOGE("网络通道打开失败");
        return;
    }
    //写入封装头
    result = avformat_write_header(ic, NULL);
    if (result < 0) {
        LOGE("封装头写入失败 %d", result);
        return;
    }
    LOGI("avformat_write_header Success!");
    pthread_create(&pid_start, 0, start, nullptr);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushStop(JNIEnv *env, jobject instance) {
    LOGI("beiPushStop");
    avio_close(ic->pb);
    avformat_free_context(ic);
    readyPushing = false;
    packets.setWork(0);
    pthread_join(pid_start, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushRelease(JNIEnv *env, jobject instance) {
    LOGI("beiPushRelease");
    DELETE(videoPush);
    DELETE(audioPush);
}

extern "C" //设置视频编码信息
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushSetVideoEncoderInfo(JNIEnv *env, jobject instance,
                                                            jint width, jint height, jint fps,
                                                            jint bitrate) {
    if (videoPush) {
        videoPush->openVideoEncodec(width, height, fps, bitrate);
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_beipush_BeiPush_getInputSamples(JNIEnv *env, jobject instance) {

//    if(audioLive){
//        return audioLive->getInputSamples();
//    }
    return -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushSetAudioEncoderInfo(JNIEnv *env, jobject instance,
                                                            jint sampleRateInHz, jint channels) {

    if(audioPush){
        audioPush->setAudioEncInfo(sampleRateInHz,channels);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushSendAudio(JNIEnv *env, jobject instance,
                                                  jbyteArray data_) {
    if(!videoPush || !readyPushing){
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    audioPush->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_beipush_BeiPush_beiPushSendVideo(JNIEnv *env, jobject instance, jbyteArray nv21_,
                                                  jint width, jint height, jboolean needRotate,
                                                  jint degree) {
    if (!videoPush || !readyPushing) {
        return;
    }
    LOGI("PushSendVideo :height:%d width:%d", height, width);
    jbyte *data = env->GetByteArrayElements(nv21_, NULL);
    jint src_length = env->GetArrayLength(nv21_);
    videoPush->encodeData(data, src_length, width, height, needRotate, degree);
    env->ReleaseByteArrayElements(nv21_, data, 0);
}