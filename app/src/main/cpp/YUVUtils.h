//
// Created by bei on 2021/1/23.
//

#ifndef FFMPEGTEST_YUVUTILS_H
#define FFMPEGTEST_YUVUTILS_H

#include "libyuv/include/libyuv.h"

class YUVUtils {
public:
    static void nv21ToI420(signed char *src_nv21_data, int width, int height,signed char *dst_i420_data);
    static void i420ToNV21(signed char *src_i420_data, int width, int height,signed char *dst_nv21_data);
    static void rotateI420(signed char *src_i420_data, int width, int height,
                           signed char *dst_i420_data, int degree);
    static void scaleI420(signed char *src_i420_data, int width, int height,
                          signed char *dst_i420_data, int dst_width,
                          int dst_height, int mode);
    static void mirrorI420(signed char *src_i420_data, int width, int height, signed char *dst_i420_data);

    /**
     * 裁剪操作：输出I420格式数据
     * libyuv做视频裁剪时，cropXY只能是偶数，否则会出现颜色出错现象
     * @param src_data
     * @param width
     * @param height
     * @param dst_i420_data
     * @param dst_width
     * @param dst_height
     * @param left
     * @param top
     */
    static void cropYUV(signed char *src_data, int src_length,int width, int height,
                        signed char *dst_i420_data,int dst_width, int dst_height,int left,int top,int degree);
};

#endif //FFMPEGTEST_YUVUTILS_H
