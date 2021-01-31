//
// Created by GNNBEI on 2021/1/22.
//

#ifndef FFMPEGTEST_JAVACALLHELPER_H
#define FFMPEGTEST_JAVACALLHELPER_H

#include <jni.h>

class JavaCallHelper {
private:
    JavaVM *vm;
    JNIEnv *env;
    jobject instance;
    jmethodID onPrepareMethodID;
    jmethodID onDecodeID;
public:
    JavaCallHelper(JavaVM *vm,JNIEnv *env,jobject instance);
    ~JavaCallHelper();
    void onPrepare(int threadID,int isSuccess);//0失败，1成功
    void onDecode(int threadID, uint8_t *data, char* type);
};
#endif //FFMPEGTEST_JAVACALLHELPER_H
