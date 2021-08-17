//
// Created by jeffli on 2021/7/27.
//

#ifndef JEFFFFMPEGDEMO_VIDEO_JNI_ENV_H
#define JEFFFFMPEGDEMO_VIDEO_JNI_ENV_H

#include <jni.h>

#include "jeffmony_log.h"

int video_jni_set_java_vm(void* vm);

JavaVM* video_jni_get_java_vm();

int jni_get_env(JNIEnv** env);

void jni_detach_thread_env();

#endif //JEFFFFMPEGDEMO_VIDEO_JNI_ENV_H
