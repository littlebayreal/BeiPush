/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wanglei.cameralibrary.gpuimage;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;


import com.wanglei.cameralibrary.R;
import com.wanglei.cameralibrary.gpuimage.utils.MagicFilterType;
import com.wanglei.cameralibrary.gpuimage.utils.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

public class GPUImageFilter {

    private boolean mIsInitialized;
    private Context mContext;
    private MagicFilterType mType = MagicFilterType.NONE;
    private final LinkedList<Runnable> mRunOnDraw;
    private final int mVertexShaderId;
    private final int mFragmentShaderId;

    private int mGLProgId;
    private int mGLPositionIndex;
    private int mGLInputImageTextureIndex;
    private int mGLTextureCoordinateIndex;
    private int mGLTextureTransformIndex;

    protected int mInputWidth;
    protected int mInputHeight;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;

    private int[] mGLCubeId;
    private int[] mGLTextureCoordinateId;
    private float[] mGLTextureTransformMatrix;

    private int[] mGLFboId;
    private int[] mGLFboTexId;
    private IntBuffer mGLFboBuffer;

    public GPUImageFilter() {
        this(MagicFilterType.NONE);
    }

    public GPUImageFilter(MagicFilterType type) {
        //设置顶点着色器和片元着色器
        this(type, R.raw.vertex, R.raw.splite_screen);
    }

    public GPUImageFilter(MagicFilterType type, int fragmentShaderId) {
        this(type, R.raw.vertex, fragmentShaderId);
    }

    public GPUImageFilter(MagicFilterType type, int vertexShaderId, int fragmentShaderId) {
        mType = type;
        mRunOnDraw = new LinkedList<>();
        mVertexShaderId = vertexShaderId;
        mFragmentShaderId = fragmentShaderId;
    }

    public void init(Context context) {
        mContext = context;
        onInit();
        onInitialized();
    }

    protected void onInit() {
        initVbo();
        loadSamplerShader();
    }

    protected void onInitialized() {
        mIsInitialized = true;
    }

    public final void destroy() {
        mIsInitialized = false;
        destroyFboTexture();
        destoryVbo();
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    protected void onDestroy() {
    }

    public void onInputSizeChanged(final int width, final int height) {
        mInputWidth = width;
        mInputHeight = height;
        initFboTexture(width, height);
    }

    public void onDisplaySizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    private void loadSamplerShader() {
        //创建glprogram 渲染小程序
        mGLProgId = OpenGLUtils.loadProgram(OpenGLUtils.readShaderFromRawResource(getContext(), mVertexShaderId),
                OpenGLUtils.readShaderFromRawResource(getContext(), mFragmentShaderId));
        //从glprogram中找到各个变量
        mGLPositionIndex = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLTextureCoordinateIndex = GLES20.glGetAttribLocation(mGLProgId, "inputTextureCoordinate");
//        mGLTextureTransformIndex = GLES20.glGetUniformLocation(mGLProgId, "textureTransform");
        mGLInputImageTextureIndex = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");

        Log.i("zxb","mGLPositionIndex:"+ mGLPositionIndex + "mGLTextureCoordinateIndex:"+ mGLInputImageTextureIndex
                + "mGLTextureTransformIndex:"+ mGLTextureTransformIndex + "mGLInputImageTextureIndex:"+ mGLInputImageTextureIndex);
    }

    //初始化顶点矩阵
    private void initVbo() {
        final float VEX_CUBE[] = {
                -1.0f, -1.0f, // Bottom left.
                1.0f, -1.0f, // Bottom right.
                -1.0f, 1.0f, // Top left.
                1.0f, 1.0f, // Top right.
        };
        //初始化片元矩阵
        final float TEX_COORD[] = {
//                0.0f, 0.0f, // Bottom left.
//                0.0f, 1.0f, // Bottom right.
//                1.0f, 0.0f, // Top left.
//                1.0f, 1.0f // Top right.
//                0.0f, 1.0f, // Bottom left.
//                0.0f, 0.0f, // Bottom right.
//                1.0f, 1.0f, // Top left.
//                1.0f, 0.0f // Top right.
            0.0f, 0.0f, // Bottom left.
            1.0f, 0.0f, // Bottom right.
            0.0f, 1.0f, // Top left.
            1.0f, 1.0f // Top right.
        };

        mGLCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(VEX_CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(TEX_COORD).position(0);

        mGLCubeId = new int[1];
        mGLTextureCoordinateId = new int[1];
        //初始化vbo
        GLES20.glGenBuffers(1, mGLCubeId, 0);
        //用创建好的属性数据填充VBO缓冲区
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);
        //glBufferData- 创建并初始化缓冲区对象的数据存储  float占4个字节
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mGLCubeBuffer.capacity() * 4, mGLCubeBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glGenBuffers(1, mGLTextureCoordinateId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mGLTextureBuffer.capacity() * 4, mGLTextureBuffer, GLES20.GL_STATIC_DRAW);
    }

    private void destoryVbo() {
        if (mGLCubeId != null) {
            GLES20.glDeleteBuffers(1, mGLCubeId, 0);
            mGLCubeId = null;
        }
        if (mGLTextureCoordinateId != null) {
            GLES20.glDeleteBuffers(1, mGLTextureCoordinateId, 0);
            mGLTextureCoordinateId = null;
        }
    }

    private void initFboTexture(int width, int height) {
        if (mGLFboId != null && (mInputWidth != width || mInputHeight != height)) {
            destroyFboTexture();
        }

        mGLFboId = new int[1];
        mGLFboTexId = new int[1];
        mGLFboBuffer = IntBuffer.allocate(width * height);

        GLES20.glGenFramebuffers(1, mGLFboId, 0);
        GLES20.glGenTextures(1, mGLFboTexId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGLFboTexId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mGLFboTexId[0], 0);

        //解绑 非必需操作
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        OpenGLUtils.checkGlError("createFrameBuffer");
    }

    private void destroyFboTexture() {
        if (mGLFboTexId != null) {
            GLES20.glDeleteTextures(1, mGLFboTexId, 0);
            mGLFboTexId = null;
        }
        if (mGLFboId != null) {
            GLES20.glDeleteFramebuffers(1, mGLFboId, 0);
            mGLFboId = null;
        }
    }

    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer, final FloatBuffer textureBuffer) {
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();

        GLES20.glEnableVertexAttribArray(mGLPositionIndex);
        GLES20.glVertexAttribPointer(mGLPositionIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, cubeBuffer);

        GLES20.glEnableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glVertexAttribPointer(mGLTextureCoordinateIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, textureBuffer);

        if (textureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLInputImageTextureIndex, 0);
        }

        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        onDrawArraysAfter();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisableVertexAttribArray(mGLPositionIndex);
        GLES20.glDisableVertexAttribArray(mGLTextureCoordinateIndex);

        return OpenGLUtils.ON_DRAWN;
    }

    public int onDrawFrame(int cameraTextureId) {
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        if (mGLFboId == null) {
            return OpenGLUtils.NO_TEXTURE;
        }
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        // 当你想配置某一块内存或者你想绘制这一块内存的内容时，
        // 你需要调用GLBindbuffer函数，并且该函数第二个参数要设置成你想操作内存的标识，
        // 然后接下来的操作都是针对这一块内存进行的，操作完过后记得再次调用GLBindBuffer(GL_ARRAY_BUFFER,0),
        // 将当前活动内存设置为空，非必需，但是这样做会是个好习惯.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);
        //启用顶点索引
        GLES20.glEnableVertexAttribArray(mGLPositionIndex);
        //步长为一组顶点2 * float所占字节4
        GLES20.glVertexAttribPointer(mGLPositionIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        //启用片元索引
        GLES20.glEnableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glVertexAttribPointer(mGLTextureCoordinateIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

//        GLES20.glUniformMatrix4fv(mGLTextureTransformIndex, 1, false, mGLTextureTransformMatrix, 0);
        //激活用来显示图片的窗口/画框  采样器的纹理单元要一致 texture0 对应 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glBindTexture(getTextureType(),cameraTextureId);
        GLES20.glUniform1i(mGLInputImageTextureIndex, 0);

        onDrawArraysPre();

//        GLES20.glViewport(0, 0, mInputWidth, mInputHeight);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);
//        //参数1：有三种取值
//        //1.GL_TRIANGLES：每三个顶之间绘制三角形，之间不连接
//        //2.GL_TRIANGLE_FAN：以V0V1V2,V0V2V3,V0V3V4，……的形式绘制三角形
//        //3.GL_TRIANGLE_STRIP：顺序在每三个顶点之间均绘制三角形。这个方法可以保证从相同的方向上
//        //参数2：从数组缓存中的哪一位开始绘制，一般都定义为0
//        //参数3：顶点的数量
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        GLES20.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLFboBuffer);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        onDrawArraysAfter();
        //将变量置空或关闭
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindTexture(getTextureType(),0);
        GLES20.glDisableVertexAttribArray(mGLPositionIndex);
        GLES20.glDisableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        return mGLFboTexId[0];
    }

    /**
     * 绘制到FBO
     * @return
     */
    public int onDrawFrameBuffer(int textureId){
        Log.i("vvv","GPUImageFilter onDrawFrameBuffer textureId:"+ textureId);
        if (!mIsInitialized) {
            Log.i("vvv","GPUImageFilter mIsInitialized:"+ mIsInitialized);
            return OpenGLUtils.NOT_INIT;
        }
        if (mGLFboId == null) {
            Log.i("vvv","GPUImageFilter mGLFboId:"+ mGLFboId[0]);
            return OpenGLUtils.NO_TEXTURE;
        }
        // 绑定FBO
        GLES20.glViewport(0, 0, mInputWidth, mInputHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);
        // 使用当前的program
        GLES20.glUseProgram(mGLProgId);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效 主要是设置变量值
        runPendingOnDrawTasks();

        // 当你想配置某一块内存或者你想绘制这一块内存的内容时，
        // 你需要调用GLBindbuffer函数，并且该函数第二个参数要设置成你想操作内存的标识，
        // 然后接下来的操作都是针对这一块内存进行的，操作完过后记得再次调用GLBindBuffer(GL_ARRAY_BUFFER,0),
        // 将当前活动内存设置为空，非必需，但是这样做会是个好习惯.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);
        //启用顶点索引
        GLES20.glEnableVertexAttribArray(mGLPositionIndex);
        //步长为一组顶点2 * float所占字节4
        GLES20.glVertexAttribPointer(mGLPositionIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        //启用片元索引
        GLES20.glEnableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glVertexAttribPointer(mGLTextureCoordinateIndex, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

//        GLES20.glUniformMatrix4fv(mGLTextureTransformIndex, 1, false, mGLTextureTransformMatrix, 0);
        //opengl渲染黑屏 大概率是参与运算的参数没有赋值
        onDrawArraysPre();
        //激活用来显示图片的窗口/画框  采样器的纹理单元要一致 texture0 对应 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(getTextureType(), textureId);
        GLES20.glUniform1i(mGLInputImageTextureIndex, 0);
        //参数1：有三种取值
        //1.GL_TRIANGLES：每三个顶之间绘制三角形，之间不连接
        //2.GL_TRIANGLE_FAN：以V0V1V2,V0V2V3,V0V3V4，……的形式绘制三角形
        //3.GL_TRIANGLE_STRIP：顺序在每三个顶点之间均绘制三角形。这个方法可以保证从相同的方向上
        //参数2：从数组缓存中的哪一位开始绘制，一般都定义为0
        //参数3：顶点的数量
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        GLES20.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLFboBuffer);
        onDrawArraysAfter();
        // 解绑
        GLES20.glUseProgram(0);
        GLES20.glDisableVertexAttribArray(mGLPositionIndex);
        GLES20.glDisableVertexAttribArray(mGLTextureCoordinateIndex);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(getTextureType(),0);
        Log.i("vvv","GPUImageFilter onDrawFrameBuffer FBOTexureId:"+ mGLFboTexId[0]);
        return mGLFboTexId[0];
    }
    protected void onDrawArraysPre() {
        Log.i("zxb","onDrawArraysPre");
    }

    protected void onDrawArraysAfter() {
        Log.i("zxb","onDrawArraysAfter");
    }

    private void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    public int getProgram() {
        return mGLProgId;
    }

    public IntBuffer getGLFboBuffer() {
        return mGLFboBuffer;
    }

    protected Context getContext() {
        return mContext;
    }

    protected MagicFilterType getFilterType() {
        return mType;
    }

    public void setTextureTransformMatrix(float[] mtx) {
        mGLTextureTransformMatrix = mtx;
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }
    /**
     * 获取Texture类型
     * GLES30.TEXTURE_2D / GLES11Ext.GL_TEXTURE_EXTERNAL_OES等
     */
    public int getTextureType() {
        return GLES20.GL_TEXTURE_2D;
    }
}

