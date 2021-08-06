#include <jni.h>
#include "https.h"
#include <android/log.h>

#define LOG_TAG "TAG/Native"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

JNIEXPORT jstring JNICALL
Java_euphoria_psycho_share_NativeShare_getString(JNIEnv *env, jclass clazz,
                                                 jstring uri_) {

    char *url;
    char data[1024], response[4096];
    int i, ret, size;

    HTTP_INFO hi1;
    http_init(&hi1, FALSE);

    url = "https://lucidu.cn";

    if (http_open(&hi1, url) < 0) {
        http_strerror(data, 1024);
        LOGE("socket error: %s \n", data);
    }

    ret = http_get(&hi1, url, response, sizeof(response));

    LOGE("return code: %d \n", ret);
    LOGE("return body: %s \n", response);
    http_close(&hi1);

    return NULL;
}