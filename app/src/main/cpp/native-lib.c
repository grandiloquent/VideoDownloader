#include <jni.h>
#include <string.h>
#include <stdlib.h>

#include "log.h"
#include "def.h"
#include "porn91.h"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {

    READ_URL();


//    if (read_len == 0) {
//        goto error;
//    }
//    char encodeBuffer[512];
//    encodeBuffer[0] = 0;
//    int encodeSize = substring((const char *) responseBuffer, "document.write(strencode2(\"", "\"",
//                               encodeBuffer);
//    if (encodeSize == -1) {
//        goto error;
//    }
//
//
//    FILE *f = fopen("/storage/emulated/0/Download/1.txt", "w");
//    fputs(responseBuffer, f);
//    fclose(f);
//
//    char decodeBuffer[512];
//
//    const char *r = strrchr(encodeBuffer, '\n');
//    if (r) {
//        const char *l = strchr(encodeBuffer, '\r');
//        int len = (l - encodeBuffer) + (strlen(r) - 1);
//        memmove((char *) l, r + 1, strlen(r) - 1);
//        encodeBuffer[len] = 0;
//    }
//
//    urldecode(decodeBuffer, encodeBuffer);
//    encodeBuffer[0] = 0;
//
//    encodeSize = substring(decodeBuffer, "src='", "'", encodeBuffer);
//    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) bufferLength,
//                               (jbyte *) (encodeBuffer));
//    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
//    HAL_TCP_Disconnect(handle);
//    return encodeSize;
//    error:
//    HAL_TCP_Disconnect(handle);
    PORN_HEADER();
    get91PornVideo(headerBuffer, 288);
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return 0;
}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getPornHub(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {
    READ_URL();

}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getDouYin(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer) {

    return 0;
}