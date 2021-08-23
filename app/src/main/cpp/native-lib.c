#include <jni.h>
#include <string.h>
#include <stdlib.h>

#include "share.h"
#include "def.h"
#include "HAL_TCP_linux.h"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {

    READ_URL();
    PORN_HEADER();

    uintptr_t handle = HAL_TCP_Connect("91porn.com", 80);
    size_t written_len;
    int ret = HAL_TCP_Write(handle, headerBuffer, strlen(headerBuffer), 3000,
                            &written_len);

    if (ret != 0) {
        (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
        HAL_TCP_Disconnect(handle);
        return 0;
    }
    // 32768 = 32 KB
    int responseBufferSize = 32768;
    size_t read_len;
    unsigned char responseBuffer[responseBufferSize];

    ret = HAL_TCP_Read(handle, responseBuffer, 32768, 10000, &read_len);
    HAL_TCP_Disconnect(handle);

    LOGE("%d\n", ret);

    char *body = strstr(responseBuffer, "\r\n\r\n");
    if (body != NULL) {
        body += 4;
    }
    int value = 0;
    while (1) {
        char c = *body;
        if ((c >= '0') && (c <= '9')) value = (value << 4) + (c - '0');
        else if ((c >= 'a') && (c <= 'f')) value = (value << 4) + (c - 'a' + 10);
        else if ((c >= 'A') && (c <= 'F')) value = (value << 4) + (c - 'A' + 10);
        else
            switch (c) {
                case '\r':
                    LOGE("===========%d", value);
                    return 0;
//                case '\n':
//                    INC_BUFFER_POS_NO_FILL(parser);
//                    goto done;
//                default:
//                    goto bad_request;
            }
//        INC_BUFFER_POS(parser);
//        len++;
        body++;
//        if (len >= MAX_CHUNKED_ENCODING_CHUNK_SIZE_LENGTH) goto bad_request;
    }

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
//    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
//    HAL_TCP_Disconnect(handle);
    return 0;
}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getPornHub(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {
    READ_URL();
    PORNHUB_HEADER();

}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getDouYin(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer) {

    return 0;
}