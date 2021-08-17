//
// Created by jeffli on 2021/7/27.
//

#ifndef JEFFFFMPEGDEMO_JEFFMONY_LOG_H
#define JEFFFFMPEGDEMO_JEFFMONY_LOG_H

#include <android/log.h>

#define FF_LOG_TAG     "JeffMony_AndroidLog"

#define JLOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, FF_LOG_TAG, format, ##__VA_ARGS__)
#define JLOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  FF_LOG_TAG, format, ##__VA_ARGS__)


#endif //JEFFFFMPEGDEMO_JEFFMONY_LOG_H
