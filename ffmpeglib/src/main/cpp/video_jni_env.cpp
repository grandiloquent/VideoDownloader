//
// Created by jeffli on 2021/7/27.
//

#include "video_jni_env.h"
#include <pthread.h>

static JavaVM* java_vm_;
static pthread_mutex_t lock_ = PTHREAD_MUTEX_INITIALIZER;

int video_jni_set_java_vm(void* vm) {
    JLOGE("video_jni_set_java_vm");
    int ret = 0;
    pthread_mutex_lock(&lock_);
    if (java_vm_ == NULL) {
        java_vm_ = static_cast<JavaVM *>(vm);
    } else if (java_vm_ != vm) {
        ret = -1;
    }
    pthread_mutex_unlock(&lock_);
    return ret;
}

JavaVM* video_jni_get_java_vm() {
    void* vm;
    pthread_mutex_lock(&lock_);
    vm = java_vm_;
    pthread_mutex_unlock(&lock_);
    return static_cast<JavaVM *>(vm);
}

int jni_get_env(JNIEnv** env) {
    JavaVM* vm = video_jni_get_java_vm();
    int ret = vm->GetEnv((void **) env, JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(env, nullptr) != JNI_OK) {
            JLOGE("%s Failed to attach the JNI Env to the current thread", __func__ );
            *env = nullptr;
        }
    }
    return ret;
}

void jni_detach_thread_env() {
    JavaVM* vm = video_jni_get_java_vm();
    vm->DetachCurrentThread();
}
