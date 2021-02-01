//
// Created by bei on 2021/1/31.
//
#include "AudioPush.h"
AudioPush::AudioPush(AVFormatContext* _avFormatContext):BasePush(_avFormatContext){

}
AudioPush::~AudioPush() {
    DELETE(outPutBuffer);
    //释放编码器
    if (audioCodec) {
        avcodec_close(audioCodecContext);
        audioCodec = nullptr;
    }
}
//打开faac编码器
void AudioPush::setAudioEncInfo(int samplesInHZ, int channels) {
    LOGE("打开faac编码器");
    int ret;
    mSampleInHz = samplesInHZ;
    mChannels = channels;
    //inputSamples、一次最大能输入编码器的样本数量 也编码的数据的个数 (一个样本是16位 2字节)
    //maxOutputBytes、最大可能的输出数据  编码后的最大字节数
    AVSampleFormat inSampleFmt = AV_SAMPLE_FMT_S16;
    AVSampleFormat outSampleFmt = AV_SAMPLE_FMT_S16;

    if (audioCodec) {
        avcodec_close(audioCodecContext);
        audioCodec = nullptr;
    }
    if (pcmAvFrame) {
        av_frame_free(&pcmAvFrame);
        pcmAvFrame = nullptr;
    }
    if(aac_pkt){
        av_packet_free(&aac_pkt);
        aac_pkt = nullptr;
    }
    //打开编码器
    av_register_all();
//    audioCodec = faacEncOpen(samplesInHZ,channels,&inputSamples,&maxOutputBytes);
    //寻找编码器
    audioCodec = avcodec_find_encoder_by_name("libfdk_aac");
    if (!audioCodec) {
        LOGI("没有找到合适的音频编码器！");
        return;
    }
    //音频编码器上下文
    audioCodecContext = avcodec_alloc_context3(audioCodec);
//    avformat_alloc_output_context2(&pFormatCtx, NULL, NULL, out_file);
//    fmt = pFormatCtx->oformat;
    audioCodecContext->codec_id = audioCodec->id;
    audioCodecContext->codec_type = AVMEDIA_TYPE_AUDIO;
    audioCodecContext->sample_fmt = outSampleFmt;
    audioCodecContext->sample_rate = samplesInHZ;
    audioCodecContext->channels = channels;
    audioCodecContext->channel_layout = AV_CH_LAYOUT_STEREO;
    audioCodecContext->channels = av_get_channel_layout_nb_channels(audioCodecContext->channel_layout);
    audioCodecContext->bit_rate = 64000;
//    audioCodecContext->frame_size = 1024;
//    if (pFormatCtx->oformat->flags & AVFMT_GLOBALHEADER) {
    audioCodecContext->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
//    }

    ///2 音频重采样 上下文初始化
    swrContext = swr_alloc_set_opts(swrContext,
                                    av_get_default_channel_layout(channels), (AVSampleFormat)outSampleFmt, samplesInHZ,//输出格式
                                    av_get_default_channel_layout(channels), (AVSampleFormat)inSampleFmt, samplesInHZ, 0, 0);//输入格式
    if (!swrContext)
    {
        LOGE("swrContext failed!");
        return;
    }
    ret = swr_init(swrContext);
    if (ret != 0)
    {
        LOGE("swr_init failed!");
        return;
    }

    ///3 音频重采样输出空间分配
    pcmAvFrame = av_frame_alloc();
    pcmAvFrame->format = outSampleFmt;
    pcmAvFrame->channels = channels;
    pcmAvFrame->channel_layout = av_get_default_channel_layout(channels);
    pcmAvFrame->nb_samples = 1024; //一帧音频一通道的采用数量
    ret = av_frame_get_buffer(pcmAvFrame, 0); // 给pcm分配存储空间
    if (ret != 0)
    {
        LOGE("av_frame_get_buffer failed!");
        return;
    }
    LOGE("音频重采样 上下文初始化成功!");
    //设置编码器参数
    //打开音频编码器
    ret = avcodec_open2(audioCodecContext, audioCodec, 0);
    if (ret != 0)
    {
        LOGE("avcodec_open2 failed!");
        return;
    }
    LOGE("打开faac编码器 success!");
}
void AudioPush::encodeData(int8_t *data) {
    const uint8_t *indata[AV_NUM_DATA_POINTERS] = { 0 };
    indata[0] = (uint8_t *)data;
    int len = swr_convert(swrContext, pcmAvFrame->data, pcmAvFrame->nb_samples, //输出参数，输出存储地址和样本数量
                          indata, pcmAvFrame->nb_samples
    );
    if (len <= 0)
    {
        LOGE("音频转换失败!");
        return;
    }
    pcmAvFrame->pts = audioIndex;
    audioIndex += av_rescale_q(pcmAvFrame->nb_samples, { 1,mSampleInHz}, audioCodecContext->time_base);
    int ret = avcodec_send_frame(audioCodecContext, pcmAvFrame);
    if (ret != 0) {
        LOGE("音频编码发送失败!");
        return;
    }
    ret = avcodec_receive_packet(audioCodecContext, aac_pkt);
    if (ret != 0) {
        LOGE("音频编码接收失败!");
        return;
    }
    if(callBack)
        callBack(aac_pkt);
}
//固定一帧可输入的最大样本为1024个
u_long AudioPush::getInputSamples() {
    return 1024;
}
void AudioPush::setUrl(char *_url) {
    url = _url;
}
void AudioPush::addStream() {
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
    avcodec_parameters_from_context(vs->codecpar, audioCodecContext);
    av_dump_format(ic, 0, url, 1);

    avStream = vs;
    audio_stream_index = vs->index;
}
void AudioPush::close() {

}

