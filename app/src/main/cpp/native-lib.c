#include <jni.h>
#include <string.h>

#include "https.h"
#include "share.h"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer, jint bufferLength) {
    // int len = (*env)->GetArrayLength(env, urlBytes);
    jbyte *url = (*env)->GetByteArrayElements(env, urlBytes, NULL);
    HTTP_INFO hi;
    // 32768 = 32 KB
    unsigned char arr[32768];
    char headers[128];
    srand((unsigned)time(0));
    snprintf(headers, 128,
                       "X-Forwarded-For: %d.%d.%d.%d\r\n",
                       randomIP(1, 255), randomIP(1, 255), randomIP(1, 255),
                       randomIP(1, 255));
    
    LOGE("%d", strlen(headers));

    int ret = http_get(&hi, (char *) url, (char *) arr, 32768, headers);
    if (ret == -1) {
        goto error;
    }
    char buf[512];
    char decodebuf[512];
    int patternSize = substring((const char *) arr, "document.write(strencode2(\"", "\"", buf);

    if (patternSize == -1) {
        goto error;
    }
    urldecode(decodebuf, buf);
    buf[0] = 0;
    patternSize = substring(decodebuf, "src='", "'", buf);

    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) bufferLength,
                               (jbyte *) (buf));
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return patternSize;
    error:
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return 0;
}

JNIEXPORT jboolean JNICALL Java_euphoria_psycho_explorer_NativeShare_getString(
        JNIEnv *env, jobject thisObj, jbyteArray data, jint length,
        jbyteArray buffer) {

    return JNI_TRUE;
}