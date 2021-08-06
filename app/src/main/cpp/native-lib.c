#include <jni.h>
#include "https.h"
#include <android/log.h>

#define LOG_TAG "TAG/Native"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

JNIEXPORT jboolean JNICALL
Java_euphoria_psycho_share_NativeShare_getString(JNIEnv *env, jobject thisObj,
                                                 jbyteArray data, jint length,
                                                 jbyteArray buffer) {
    // int len = (*env)->GetArrayLength(env, data);
    jbyte *dataToWrite = (*env)->GetByteArrayElements(env, data, NULL);


    HTTP_INFO hi1;
    http_init(&hi1, FALSE);


    if (http_open(&hi1, (char *) dataToWrite) < 0) {
        (*env)->ReleaseByteArrayElements(env, data, dataToWrite, 0);
        return FALSE;
    }
    unsigned char arr[length];
    http_get(&hi1, (char *) dataToWrite, (char *) arr, length);
    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) length, (jbyte *) arr);
    http_close(&hi1);
    return JNI_TRUE;
}