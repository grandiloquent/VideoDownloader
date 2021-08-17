#include <jni.h>
#include <string.h>
#include <stdlib.h>

#include "share.h"


JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {
    crt_rsa[crt_rsa_size - 1] = 0;
    TLSConnectParams tlsConnectParams = {.ca=crt_rsa, .ca_len=(crt_rsa_size - 1)};

    uintptr_t handle = HAL_TLS_Connect(&tlsConnectParams, "91porn.com", 443);
    size_t written_len;
    char *header =
            "GET / HTTP/1.1\r\n"
            "Accept: text/css,*/*;q=0.1\r\n"
            "Accept-Encoding: gzip, deflate, br\r\n"
            "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8\r\n"
            "Cache-Control: no-cache\r\n"
            "Connection: keep-alive\r\n"
            "Host: 91porn.com\r\n"
            "Pragma: no-cache\r\n"
            "sec-ch-ua: Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";v=\"92\r\n"
            "sec-ch-ua-mobile: ?0\r\n"
            "Sec-Fetch-Dest: style\r\n"
            "Sec-Fetch-Mode: cors\r\n"
            "Sec-Fetch-Site: cross-site\r\n"
            "User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36\r\n"
            "\r\n";
    HAL_TLS_Write(handle, header, strlen(header), 5000,
                  &written_len);
    LOGE("%d", written_len);
    char buf[1024];
    size_t read_len;
    HAL_TLS_Read(handle, buf, 1023, 5000, &read_len);
    LOGE("%s", buf);
    return -1;
/*
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
        LOGE("get91Porn: statusCode = %d", statusCode);
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
    */

    return 0;
}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getDouYin(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer) {

    return 0;
}