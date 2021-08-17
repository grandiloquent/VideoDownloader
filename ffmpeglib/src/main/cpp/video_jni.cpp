//
// Created by jeffli on 2021/7/27.
//

#include <jni.h>
#include "video_jni_env.h"

//JNI 挂载的时候执行的第一个方法
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reversed) {

    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    video_jni_set_java_vm(vm);
    //可以手动注册jni 方法了
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reversed) {
    JNIEnv *env = nullptr;
    if ((vm)->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }


}