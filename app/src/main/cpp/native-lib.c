#include <jni.h>
#include <string.h>

#include "https.h"
#include "share.h"
#include "cJSON.h"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer, jint bufferLength) {
    int len = (*env)->GetArrayLength(env, urlBytes);
    jbyte *url = (*env)->GetByteArrayElements(env, urlBytes, NULL);
    url[len] = 0;
    HTTP_INFO hi;
    // 32768 = 32 KB
    unsigned char arr[32768];
    char headers[64];
    srand((unsigned) time(0));
    snprintf(headers, 64,
             "X-Forwarded-For: %d.%d.%d.%d\r\n",
             randomIP(1, 255), randomIP(1, 255), randomIP(1, 255),
             randomIP(1, 255));
    int ret = http_get(&hi, (char *) url, (char *) arr, 32768, headers);
    if (ret >= 400 || ret < 200) {
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

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getDouYin(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer) {
    int len = (*env)->GetArrayLength(env, urlBytes);
    jbyte *url = (*env)->GetByteArrayElements(env, urlBytes, NULL);
    url[len] = 0;
    HTTP_INFO hi;
    unsigned char arr[8192];

    int ret = http_get(&hi, (char *) url, (char *) arr, 8192, "");
    if (ret >= 400 || ret < 200) {
        goto error;
    }
    char buf[64];
    substring(hi.response.location, "video/", "/", buf);
    char bufUrl[512];
    sprintf(bufUrl, "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=%s", buf);
    HTTP_INFO hi1;
    ret = http_get(&hi1, bufUrl, (char *) arr, 8192, "");
    if (ret >= 400 || ret < 200) {
        goto error;
    }
    bufUrl[0] = 0;
    substring(arr, "\"play_addr\":", "}", bufUrl);
    arr[0] = 0;
    substring(bufUrl, "\"url_list\":[\"", "=", arr);

    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) length,
                               (jbyte *) (arr));
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return strlen(arr);
    error:
    //cJSON_Delete(js);
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    return 0;
}