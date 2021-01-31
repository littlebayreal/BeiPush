//
// Created by bei on 2021/1/23.
//
#include <malloc.h>
#include "VideoPush.h"
#include "YUVUtils.h"

void rotate90YUV420P(int8_t *src, int8_t *des, int width, int height);

VideoPush::VideoPush(AVFormatContext *_AvFormatContext) : BasePush(_AvFormatContext) {

    pthread_mutex_init(&mutex, 0);
}

VideoPush::~VideoPush() {
    pthread_mutex_destroy(&mutex);
    if (!x264Codec) {
        avcodec_close(x264CodecContext);
        x264Codec = nullptr;
    }
    if (!pic_in) {
        av_frame_free(&pic_in);
        pic_in = nullptr;
    }
    if(!enc_pkt){
        av_packet_free(&enc_pkt);
        enc_pkt = nullptr;
    }
}
/**
 * 这里同步是为了用户切换摄像头考虑
 * @param width
 * @param height
 * @param fps
 * @param bitrate
 */
//打开编码器
void VideoPush::openVideoEncodec(int width, int height, int fps, int bitrate) {
    pthread_mutex_lock(&mutex);
    LOGE("openVideoEncodec!\n");
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;
    ySize = width * height;
    LOGI("y像素大小:%d", ySize);
    uvSize = (width >> 1) * (height >> 1);
    if (x264CodecContext) {
        avcodec_close(x264CodecContext);
        x264CodecContext = nullptr;
    }
    if (pic_in) {
        av_frame_free(&pic_in);
        pic_in = nullptr;
    }
    if(enc_pkt){
        av_packet_free(&enc_pkt);
        enc_pkt = nullptr;
    }

    av_register_all();

    //output encoder initialize
    x264Codec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!x264Codec) {
        LOGE("Can not find x264Codec!\n");
        return;
    }
    x264CodecContext = avcodec_alloc_context3(x264Codec);
    if (!x264CodecContext) {
        LOGE("Can not create x264CodecContext!\n");
        return;
    }
    //编码器的ID号，这里为264编码器，可以根据video_st里的codecID 参数赋值
    x264CodecContext->codec_id = x264Codec->id;
    //像素的格式，也就是说采用什么样的色彩空间来表明一个像素点
    x264CodecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    //编码器编码的数据类型
    x264CodecContext->codec_type = AVMEDIA_TYPE_VIDEO;
    //编码目标的视频帧大小，以像素为单位 竖屏是需要调整宽高的
    LOGI("openVideoEncodec width:%d height:%d",width,height);
    x264CodecContext->width = width/2;
    x264CodecContext->height = height/2;
    x264CodecContext->framerate = (AVRational) {fps, 1};
    //帧率的基本单位，我们用分数来表示，
    x264CodecContext->time_base = (AVRational) {1, fps};
    //目标的码率，即采样的码率；显然，采样码率越大，视频大小越大
    x264CodecContext->bit_rate = bitrate;
    //固定允许的码率误差，数值越大，视频越小
//    pCodecCtx->bit_rate_tolerance = 4000000;
    x264CodecContext->gop_size = fps/2;
    /* Some formats want stream headers to be separate. */
    x264CodecContext->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

    //H264 codec param
//    pCodecCtx->me_range = 16;
    //pCodecCtx->max_qdiff = 4;
    x264CodecContext->qcompress = 0.6;
    //最大和最小量化系数
    x264CodecContext->qmin = 10;
    x264CodecContext->qmax = 51;
    //Optional Param
    //两个非B帧之间允许出现多少个B帧数
    //设置0表示不使用B帧
    //b 帧越多，图片越小
    x264CodecContext->max_b_frames = 0;
    x264CodecContext->thread_count = 4;
    // Set H264 preset and tune
    AVDictionary *param = 0;
    //H.264
    if (x264CodecContext->codec_id == AV_CODEC_ID_H264) {
//        av_dict_set(&param, "preset", "slow", 0);
        /**
         * 这个非常重要，如果不设置延时非常的大
         * ultrafast,superfast, veryfast, faster, fast, medium
         * slow, slower, veryslow, placebo.　这是x264编码速度的选项
       */
        av_dict_set(&param, "preset", "superfast", 0);
        av_dict_set(&param, "tune", "zerolatency", 0);
    }

    if (avcodec_open2(x264CodecContext, x264Codec, &param) < 0) {
        LOGE("Failed to open encoder!\n");
        return;
    }
    vc = x264CodecContext;
    pthread_mutex_unlock(&mutex);
    LOGI("open encoder success!\n");
}

/**
 * data是摄像头拍摄的图像数据为NV21格式，
 * X264需要i420格式，需要转换
 * @param data 相机原样采集的nv21格式数据，未经任何处理
 */
void VideoPush::encodeData(int8_t *data, int src_length, int width, int height,
                           bool needRotate, int degree) {
    //
    pthread_mutex_lock(&mutex);
    LOGE("encodeData 视频开始编码");
    int ret = 0;
    int8_t *dst_i420_data = (int8_t *) malloc(sizeof(int8_t) * width * height * 3 / 2);
    int8_t *dst_i420_data_rotate = (int8_t *) malloc(sizeof(int8_t) * width * height * 3 / 2);
    int8_t *dst_i420_data_scale= (int8_t *) malloc(sizeof(int8_t) * width * height * 3 / 2);
    if (!dst_i420_data || !dst_i420_data_rotate) {
        pthread_mutex_unlock(&mutex);
        return;
    }
    memset(dst_i420_data, 0, sizeof(int8_t) * width * height * 3 / 2);
    memset(dst_i420_data_rotate, 0, sizeof(int8_t) * width * height * 3 / 2);
    //NV21(I420SP)->I420P
    YUVUtils::nv21ToI420(data, height, width, dst_i420_data);
//    LOGI("视频开始编码 nv21->yuv420p success");
    //needRotate = false;
    if (needRotate) {
        LOGI("encodeData 视频开始编码 转换角度%d", degree);
        YUVUtils::rotateI420(dst_i420_data, height, width, dst_i420_data_rotate, degree);
        LOGI("encodeData 视频开始编码 缩放 before width:%d height:%d after width:%d height:%d", width,height,x264CodecContext->width,x264CodecContext->height);
        //旋转过后 宽高发生改变
        YUVUtils::scaleI420(dst_i420_data_rotate,width,height,dst_i420_data_scale,x264CodecContext->width,x264CodecContext->height,0);
    }
    LOGI("encodeData 视频开始编码 缩放成功 width:%d height:%d",x264CodecContext->width,x264CodecContext->height);
    pic_in = av_frame_alloc();
    int picture_size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, x264CodecContext->width,
                                                x264CodecContext->height, 1);
    uint8_t *buffers = (uint8_t *) av_malloc(picture_size);

    //将buffers的地址赋给AVFrame中的图像数据，根据像素格式判断有几个数据指针
    av_image_fill_arrays(pic_in->data, pic_in->linesize, buffers, AV_PIX_FMT_YUV420P,
                         x264CodecContext->width, x264CodecContext->height, 1);

    int ySize = x264CodecContext->width*x264CodecContext->height;
    int uvSize = (x264CodecContext->width>>1)*(x264CodecContext->height>>1);
    //将处理后的yuv数据复制到pic_in
    if (needRotate) {
        LOGI("缩放之后的ysize:%d",ySize);
//    rotate90YUV420P(dst_i420_data,dst_i420_data_rotate,width,height);
        memcpy(pic_in->data[0], dst_i420_data_scale, ySize);//Y
        memcpy(pic_in->data[1], dst_i420_data_scale + ySize, uvSize);//U
        memcpy(pic_in->data[2], dst_i420_data_scale + ySize + uvSize, uvSize);//V
    } else {
        memcpy(pic_in->data[0], dst_i420_data, ySize);//Y
        memcpy(pic_in->data[1], dst_i420_data + ySize, uvSize);//U
        memcpy(pic_in->data[2], dst_i420_data + ySize + uvSize, uvSize);//V
    }
    LOGI("encodeData 复制数据到frame成功");
    //释放内存
    free(dst_i420_data);
    free(dst_i420_data_rotate);
    free(dst_i420_data_scale);
//    memcpy(pic_in->img.plane[0], data, ySize);
//    for (int i = 0; i <uvSize ; ++i) {
//        *(pic_in->img.plane[1]+i) = *(data+ySize+i * 2+1);//U
//        *(pic_in->img.plane[2]+i) = *(data+ySize+i * 2);//v
//    }
    pic_in->pts = index++;
    pic_in->format = AV_PIX_FMT_YUV420P;
//    pic_in->height = x264CodecContext->height;
//    pic_in->width = x264CodecContext->width;
    //例如对于H.264来说。1个AVPacket的data通常对应一个NAL
    //初始化AVPacket
    enc_pkt = av_packet_alloc();
    int start_time = av_gettime();
    //开始编码YUV数据
    ret = avcodec_send_frame(x264CodecContext, pic_in);
    if (ret != 0) {
        LOGE("encodeData avcodec_send_frame error");
        AVERROR(ret);
        pthread_mutex_unlock(&mutex);
        return;
    }
    //获取编码后的数据
    ret = avcodec_receive_packet(x264CodecContext, enc_pkt);

    if (ret != 0 || enc_pkt->size <= 0) {
        LOGE("encodeData avcodec_receive_packet error");
        AVERROR(ret);
        pthread_mutex_unlock(&mutex);
        return;
    }
    //将编译好的数据回传
    if (callBack)
        callBack(enc_pkt, pic_in, index);
//    av_frame_free(&pic_in);
    av_free(buffers);
    //会将packet中的值置为空
//    av_packet_free(&enc_pkt);
    int cost = av_gettime() - start_time;
    LOGE("encodeData 视频编码结束 花费%d", cost);
    pthread_mutex_unlock(&mutex);
}

void VideoPush::setUrl(char *_url) {
    url = _url;
}

void VideoPush::addStream() {
    if (!ic) {
        LOGE("addStream AVFormatContext为空");
    }
    //添加视频流
    AVStream *vs = avformat_new_stream(ic, NULL);
    if (!vs) {
        LOGE("addStream 添加视频流失败");
    }
    vs->codecpar->codec_tag = 0;

    //从编码器复制参数
    avcodec_parameters_from_context(vs->codecpar, x264CodecContext);
    av_dump_format(ic, 0, url, 1);

    avStream = vs;
    video_stream_index = vs->index;
//    if (x264CodecContext->codec_type == AVMEDIA_TYPE_VIDEO)
//    {
//        this->vc = actx;
//        this->vs = vs;
//    }
//    else if (actx->codec_type == AVMEDIA_TYPE_AUDIO)
//    {
//        this->ac = actx;
//        this->as = vs;
//    }
}

void rotate90YUV420P(int8_t *src, int8_t *des, int width, int height) {
    int n = 0;
    int hw = width / 2;
    int hh = height / 2;
    //copy y
    for (int j = width; j > 0; j--) {
        for (int i = 0; i < height; i++) {
            des[n++] = src[width * i + j];
        }
    }

    //copy u
    int8_t *ptemp = src + width * height;
    for (int j = hw; j > 0; j--) {
        for (int i = 0; i < hh; i++) {
            des[n++] = ptemp[hw * i + j];
        }
    }

    //copy v
    ptemp += width * height / 4;
    for (int j = hw; j > 0; j--) {
        for (int i = 0; i < hh; i++) {
            des[n++] = ptemp[hw * i + j];
        }
    }
}

void VideoPush::setVideoCallBack(videoCallBack _callBack) {
    callBack = _callBack;
};

void VideoPush::close() {

}