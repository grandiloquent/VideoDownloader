#include <jni.h>
#include <string.h>
#include <stdlib.h>

#include "mbedtls/net_sockets.h"
#include "mbedtls/ssl.h"
#include "mbedtls/entropy.h"
#include "mbedtls/ctr_drbg.h"

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include "share.h"
#include "tls.h"

const char *pers = "mini_client";
#define GET_REQUEST "GET / HTTP/1.0\r\n\r\n\r\n"

JNIEXPORT jint JNICALL Java_euphoria_psycho_explorer_NativeShare_get91Porn(
        JNIEnv *env, jclass thisObj, jbyteArray urlBytes,
        jbyteArray buffer, jint bufferLength) {

    int ret = 0;
    mbedtls_net_context server_fd;
    struct sockaddr_in addr;
    mbedtls_x509_crt ca;

    mbedtls_entropy_context entropy;
    mbedtls_ctr_drbg_context ctr_drbg;
    mbedtls_ssl_context ssl;
    mbedtls_ssl_config conf;
    mbedtls_ctr_drbg_init(&ctr_drbg);

    /*
    * 0. Initialize and setup stuff
    */
    mbedtls_net_init(&server_fd);
    mbedtls_ssl_init(&ssl);
    mbedtls_ssl_config_init(&conf);

    mbedtls_x509_crt_init(&ca);

    mbedtls_entropy_init(&entropy);
    if (mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func, &entropy,
                              NULL, 0) != 0) {
        ret = -1;
        LOGE("%s", "mbedtls_ctr_drbg_seed");
        goto exit;
    }

    if (mbedtls_ssl_config_defaults(&conf,
                                    MBEDTLS_SSL_IS_CLIENT,
                                    MBEDTLS_SSL_TRANSPORT_STREAM,
                                    MBEDTLS_SSL_PRESET_DEFAULT) != 0) {
        LOGE("%s", "mbedtls_ssl_config_defaults");
        ret = -1;
        goto exit;
    }

    mbedtls_ssl_conf_rng(&conf, mbedtls_ctr_drbg_random, &ctr_drbg);
    crt_rsa[crt_rsa_size - 1] = 0;
    if (mbedtls_x509_crt_parse(&ca, crt_rsa, crt_rsa_size) != 0) {
        LOGE("%s", "mbedtls_x509_crt_parse_der");
        ret = -1;
        goto exit;
    }

    mbedtls_ssl_conf_ca_chain(&conf, &ca, NULL);
    mbedtls_ssl_conf_authmode(&conf, MBEDTLS_SSL_VERIFY_REQUIRED);

    if (mbedtls_ssl_setup(&ssl, &conf) != 0) {
        LOGE("%s", "mbedtls_ssl_setup");
        ret = -1;
        goto exit;
    }
    if (mbedtls_ssl_set_hostname(&ssl, "bing.com") != 0) {
        LOGE("%s", "mbedtls_ssl_set_hostname");
        ret = -1;
        goto exit;
    }
    /*
        * 1. Start the connection
        */
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;

    ret = 1; /* for endianness detection */
    addr.sin_port = htons(443);

    struct hostent *he;
    if ((he = gethostbyname("bing.com")) == NULL) {
        LOGE("%s", "gethostbyname");
        ret = -1;
        goto exit;
    }
    memcpy(&addr.sin_addr, he->h_addr_list[0], he->h_length);
    // addr.sin_addr.s_addr = "108.160.165.62";
    ret = 0;

    if ((server_fd.MBEDTLS_PRIVATE(fd) = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        LOGE("%s", "server_fd.MBEDTLS_PRIVATE(fd)");
        ret = -1;
        goto exit;
    }

    if (connect(server_fd.MBEDTLS_PRIVATE(fd),
                (const struct sockaddr *) &addr, sizeof(addr)) < 0) {
        LOGE("%s", "connect");
        ret = -1;
        goto exit;
    }

    mbedtls_ssl_set_bio(&ssl, &server_fd, mbedtls_net_send, mbedtls_net_recv, NULL);

    if (mbedtls_ssl_handshake(&ssl) != 0) {
        LOGE("%s", "mbedtls_ssl_handshake");
        ret = -1;
        goto exit;
    }

    /*
     * 2. Write the GET request and close the connection
     */
    int writeLen;
    HAL_TLS_Write(&ssl, GET_REQUEST, strlen(GET_REQUEST), 500, &writeLen);

    LOGE("HAL_TLS_Write %d %d", strlen(GET_REQUEST), writeLen);
    char buf[1024];
    memset(buf,0,1024);
    int readlen;
    if (HAL_TLS_Read(&ssl, buf, 1023, 5000, &readlen) != QCLOUD_RET_SUCCESS) {
        ret = -1;
       // goto exit;
    }
    LOGE("%d %s", readlen, buf);
    HAL_TLS_Disconnect(&ssl, &server_fd, &entropy, &ctr_drbg, &ca);
    return 0;
    exit:
    HAL_TLS_Disconnect(&ssl, &server_fd, &entropy, &ctr_drbg, &ca);
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