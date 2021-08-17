#include <jni.h>
#include <string.h>
#include <stdlib.h>

#include "share.h"
#include "def.h"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {

    READ_URL();
    PORN_HEADER();

    crt_rsa[crt_rsa_size - 1] = 0;
    TLSConnectParams tlsConnectParams = {.ca=crt_rsa, .ca_len=(crt_rsa_size - 1)};
    uintptr_t handle = HAL_TLS_Connect(&tlsConnectParams, "91porn.com", 443);
    size_t written_len;
    int ret = HAL_TLS_Write(handle, headerBuffer, strlen(headerBuffer), 3000,
                            &written_len);

    if (ret != 0) {
        (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
        HAL_TLS_Disconnect(handle);
        return 0;
    }
    // 32768 = 32 KB
    int responseBufferSize = 32768;
    size_t read_len;
    unsigned char responseBuffer[responseBufferSize];

    ret = HAL_TLS_Read(handle, responseBuffer, 32768, 10000, &read_len);

    if (read_len == 0) {
        goto error;
    }
    char encodeBuffer[512];
    encodeBuffer[0] = 0;
    int encodeSize = substring((const char *) responseBuffer, "document.write(strencode2(\"", "\"",
                               encodeBuffer);
    if (encodeSize == -1) {
        goto error;
    }


//    FILE *f = fopen("/storage/emulated/0/Download/1.txt", "w");
//    fputs(responseBuffer, f);
//    fclose(f);

    char decodeBuffer[512];

    const char *r = strrchr(encodeBuffer, '\n');
    if (r) {
        const char *l = strchr(encodeBuffer, '\r');
        int len = (l - encodeBuffer) + (strlen(r) - 1);
        memmove((char *) l, r + 1, strlen(r) - 1);
        encodeBuffer[len] = 0;
    }

    urldecode(decodeBuffer, encodeBuffer);
    encodeBuffer[0] = 0;

    encodeSize = substring(decodeBuffer, "src='", "'", encodeBuffer);
    (*env)->SetByteArrayRegion(env, buffer, 0, (jsize) bufferLength,
                               (jbyte *) (encodeBuffer));
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    HAL_TLS_Disconnect(handle);
    return encodeSize;
    error:
    (*env)->ReleaseByteArrayElements(env, urlBytes, url, 0);
    HAL_TLS_Disconnect(handle);
    return 0;
}

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_getDouYin(
        JNIEnv *env, jobject thisObj, jbyteArray urlBytes, jint length,
        jbyteArray buffer) {

    return 0;
}