//
// Created by GNNBEI on 2021/1/22.
//
#include "JavaCallHelper.h"
#include "macro.h"

JavaCallHelper::JavaCallHelper(JavaVM *vm, JNIEnv *env, jobject instance) {
    this->vm = vm;
    this->env = env;
    this->instance = env->NewGlobalRef(instance);

    jclass jclazz = env->GetObjectClass(instance);
    onPrepareMethodID = env->GetMethodID(jclazz,"onPrepare","(I)V");
    onDecodeID = env->GetMethodID(jclazz,"onDecode","([BLjava/lang/String;)V");
}

JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(instance);
}

void JavaCallHelper::onPrepare(int threadID,int isSuccess) {
    if (threadID==THREAD_MAIN){
        env->CallVoidMethod(instance,onPrepareMethodID);
    } else{
        JNIEnv *env;
        if (vm->AttachCurrentThread(&env,0)!=JNI_OK){
            return;
        }
        env->CallVoidMethod(instance,onPrepareMethodID,isSuccess);
        vm->DetachCurrentThread();
    }
}
void JavaCallHelper::onDecode(int threadID,uint8_t *data, char* type) {
    LOGI("进入onDecode回调函数");
    if (threadID==THREAD_MAIN){
        env->CallVoidMethod(instance,onDecodeID);
    } else{
        JNIEnv *env;
        if (vm->AttachCurrentThread(&env,0)!=JNI_OK){
            return;
        }
        jbyteArray jbarray = env->NewByteArray(540*720*3);//建立jbarray数组
        env->SetByteArrayRegion(jbarray, 0, 540*720*3, (jbyte *) data);
        LOGI("simple decoder Frame size: %d",540*720*3);
        env->CallVoidMethod(instance,onDecodeID,jbarray,env->NewStringUTF(type));
        LOGI("回调执行成功 解绑当前线程");
//        vm->DetachCurrentThread();
    }
}
