#include <jni.h>
#include <string.h>

#include "https.h"
#include "share.h"
#include "cJSON.h"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer, jint bufferLength) {

    // ------------------
    int urlBytesSize = (*env)->GetArrayLength(env, urlBytes);
    jbyte *url = (*env)->GetByteArrayElements(env, urlBytes, NULL);
    url[urlBytesSize] = 0;

    // ------------------
    char headerBuffer[64];
    srand((unsigned) time(0));
    snprintf(headerBuffer, 64,
             "X-Forwarded-For: %d.%d.%d.%d\r\n",
             randomIP(1, 255), randomIP(1, 255), randomIP(1, 255),
             randomIP(1, 255));

    // 32768 = 32 KB
    int responseBufferSize = 32768;
    unsigned char responseBuffer[responseBufferSize];

    // ------------------
    HTTP_INFO hi;
    int statusCode = http_get(&hi, (char *) url, (char *) responseBuffer, responseBufferSize,
                              headerBuffer);
    if (statusCode >= 400 || statusCode < 200) {
        LOGE("get91Porn: %d", statusCode);
        goto error;
    }

    // ------------------
    char encodeBuffer[512];
    int encodeSize = substring((const char *) responseBuffer, "document.write(strencode2(\"", "\"",
                               encodeBuffer);

    if (encodeSize == -1) {
        LOGE(
                "get91Porn: can't find the encoded code which contains the real "
                "video uri '%s' ",
                url);
        goto error;
    }

    // ------------------
    char decodeBuffer[512];
    urldecode(decodeBuffer, encodeBuffer);
    encodeBuffer[0] = 0;

    // ------------------
    encodeSize = substring(decodeBuffer, "src='", "'", encodeBuffer);

    // ------------------
    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) bufferLength,
                               (jbyte *) (encodeBuffer));
    // ------------------
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return encodeSize;

    // ------------------
    error:
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return 0;
}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getDouYin(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer) {
    // ------------------
    int urlBytesSize = (*env)->GetArrayLength(env, urlBytes);
    jbyte *url = (*env)->GetByteArrayElements(env, urlBytes, NULL);
    url[urlBytesSize] = 0;

    HTTP_INFO hi;
    int responseBufferSize = 8192;
    unsigned char responseBuffer[responseBufferSize];

    int statusCode = http_get(&hi, (char *) url, (char *) responseBuffer, responseBufferSize, "");
    if (statusCode >= 400 || statusCode < 200) {
        goto error;
    }
    char shareIdBuffer[64];
    substring(hi.response.location, "video/", "/", shareIdBuffer);
    char bufUrl[512];
    sprintf(bufUrl, "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=%s",
            shareIdBuffer);
    HTTP_INFO hi1;
    statusCode = http_get(&hi1, bufUrl, (char *) responseBuffer, responseBufferSize, "");
    if (statusCode >= 400 || statusCode < 200) {
        goto error;
    }
    bufUrl[0] = 0;
    substring(responseBuffer, "\"play_addr\":", "}", bufUrl);
    responseBuffer[0] = 0;
    substring(bufUrl, "\"url_list\":[\"", "=", responseBuffer);

    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) length,
                               (jbyte *) (responseBuffer));
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return strlen(responseBuffer);
    error:
    //cJSON_Delete(js);
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return 0;
}