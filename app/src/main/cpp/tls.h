#ifndef TLS_H
#define TLS_H
// #include "tls.h"

#include <errno.h>
#include <mbedtls/net_sockets.h>
#include <mbedtls/entropy.h>
#include <mbedtls/ctr_drbg.h>

#define QCLOUD_RET_MQTT_ALREADY_CONNECTED 4
#define QCLOUD_RET_MQTT_CONNACK_CONNECTION_ACCEPTED 3
#define QCLOUD_RET_MQTT_MANUALLY_DISCONNECTED 2
#define QCLOUD_RET_MQTT_RECONNECTED 1
#define QCLOUD_RET_SUCCESS 0
#define QCLOUD_ERR_FAILURE -1001
#define QCLOUD_ERR_INVAL -1002
#define QCLOUD_ERR_DEV_INFO -1003
#define QCLOUD_ERR_MALLOC -1004
#define QCLOUD_ERR_HTTP_CLOSED -3
#define QCLOUD_ERR_HTTP -4
#define QCLOUD_ERR_HTTP_PRTCL -5
#define QCLOUD_ERR_HTTP_UNRESOLVED_DNS -6
#define QCLOUD_ERR_HTTP_PARSE -7
#define QCLOUD_ERR_HTTP_CONN -8
#define QCLOUD_ERR_HTTP_AUTH -9
#define QCLOUD_ERR_HTTP_NOT_FOUND -10
#define QCLOUD_ERR_HTTP_TIMEOUT -11
#define QCLOUD_ERR_MQTT_PUSH_TO_LIST_FAILED -102
#define QCLOUD_ERR_MQTT_NO_CONN -103
#define QCLOUD_ERR_MQTT_UNKNOWN -104
#define QCLOUD_ERR_MQTT_ATTEMPTING_RECONNECT -105
#define QCLOUD_ERR_MQTT_RECONNECT_TIMEOUT -106
#define QCLOUD_ERR_MQTT_MAX_SUBSCRIPTIONS -107
#define QCLOUD_ERR_MQTT_SUB -108
#define QCLOUD_ERR_MQTT_NOTHING_TO_READ -109
#define QCLOUD_ERR_MQTT_PACKET_READ -110
#define QCLOUD_ERR_MQTT_REQUEST_TIMEOUT -111
#define QCLOUD_ERR_MQTT_CONNACK_UNKNOWN -112
#define QCLOUD_ERR_MQTT_CONNACK_UNACCEPTABLE_PROTOCOL_VERSION -113
#define QCLOUD_ERR_MQTT_CONNACK_IDENTIFIER_REJECTED -114
#define QCLOUD_ERR_MQTT_CONNACK_SERVER_UNAVAILABLE -115
#define QCLOUD_ERR_MQTT_CONNACK_BAD_USERDATA -116
#define QCLOUD_ERR_MQTT_CONNACK_NOT_AUTHORIZED -117
#define QCLOUD_ERR_RX_MESSAGE_INVAL -118
#define QCLOUD_ERR_BUF_TOO_SHORT -119
#define QCLOUD_ERR_MQTT_QOS_NOT_SUPPORT -120
#define QCLOUD_ERR_MQTT_UNSUB_FAIL -121
#define QCLOUD_ERR_JSON_PARSE -132
#define QCLOUD_ERR_JSON_BUFFER_TRUNCATED -133
#define QCLOUD_ERR_JSON_BUFFER_TOO_SMALL -134
#define QCLOUD_ERR_JSON -135
#define QCLOUD_ERR_MAX_JSON_TOKEN -136
#define QCLOUD_ERR_MAX_APPENDING_REQUEST -137
#define QCLOUD_ERR_MAX_TOPIC_LENGTH -138
#define QCLOUD_ERR_COAP_NULL -150
#define QCLOUD_ERR_COAP_DATA_SIZE -151
#define QCLOUD_ERR_COAP_INTERNAL -152
#define QCLOUD_ERR_COAP_BADMSG -153
#define QCLOUD_ERR_DTLS_PEER_CLOSE_NOTIFY -160
#define QCLOUD_ERR_PROPERTY_EXIST -201
#define QCLOUD_ERR_NOT_PROPERTY_EXIST -202
#define QCLOUD_ERR_REPORT_TIMEOUT -203
#define QCLOUD_ERR_REPORT_REJECTED -204
#define QCLOUD_ERR_GET_TIMEOUT -205
#define QCLOUD_ERR_GET_REJECTED -206
#define QCLOUD_ERR_ACTION_EXIST -210
#define QCLOUD_ERR_NOT_ACTION_EXIST -211
#define QCLOUD_ERR_GATEWAY_CREATE_SESSION_FAIL -221
#define QCLOUD_ERR_GATEWAY_SESSION_NO_EXIST -222
#define QCLOUD_ERR_GATEWAY_SESSION_TIMEOUT -223
#define QCLOUD_ERR_GATEWAY_SUBDEV_ONLINE -224
#define QCLOUD_ERR_GATEWAY_SUBDEV_OFFLINE -225
#define QCLOUD_ERR_TCP_SOCKET_FAILED -601
#define QCLOUD_ERR_TCP_UNKNOWN_HOST -602
#define QCLOUD_ERR_TCP_CONNECT -603
#define QCLOUD_ERR_TCP_READ_TIMEOUT -604
#define QCLOUD_ERR_TCP_WRITE_TIMEOUT -605
#define QCLOUD_ERR_TCP_READ_FAIL -606
#define QCLOUD_ERR_TCP_WRITE_FAIL -607
#define QCLOUD_ERR_TCP_PEER_SHUTDOWN -608
#define QCLOUD_ERR_TCP_NOTHING_TO_READ -609
#define QCLOUD_ERR_SSL_INIT -701
#define QCLOUD_ERR_SSL_CERT -702
#define QCLOUD_ERR_SSL_CONNECT -703
#define QCLOUD_ERR_SSL_CONNECT_TIMEOUT -704
#define QCLOUD_ERR_SSL_WRITE_TIMEOUT -705
#define QCLOUD_ERR_SSL_WRITE -706
#define QCLOUD_ERR_SSL_READ_TIMEOUT -707
#define QCLOUD_ERR_SSL_READ -708
#define QCLOUD_ERR_SSL_NOTHING_TO_READ -709

typedef struct {
    mbedtls_net_context socket_fd;
    mbedtls_entropy_context entropy;
    mbedtls_ctr_drbg_context ctr_drbg;
    mbedtls_ssl_context ssl;
    mbedtls_ssl_config ssl_conf;
#if defined(MBEDTLS_X509_CRT_PARSE_C)
    mbedtls_x509_crt ca_cert;
    mbedtls_x509_crt client_cert;
#endif
    mbedtls_pk_context private_key;
} TLSDataParams;

typedef struct {
    const char *ca;
    uint32_t ca_len;

#ifdef AUTH_MODE_CERT
    /**
     * Device with certificate
     */
    const char       *cert_file;            // public certificate file
    const char       *key_file;             // pravite certificate file
#else
    /**
     * Device with PSK
     */
    const char *psk;                  // PSK string
    const char *psk_id;               // PSK ID
#endif

    size_t psk_length;            // PSK length

    unsigned int timeout_ms;            // SSL handshake timeout in millisecond

} SSLConnectParams;


typedef SSLConnectParams TLSConnectParams;

#ifndef AUTH_MODE_CERT
static const int ciphersuites[] = {MBEDTLS_TLS_PSK_WITH_AES_128_CBC_SHA,
                                   MBEDTLS_TLS_PSK_WITH_AES_256_CBC_SHA, 0};
#endif

/**
 * @brief free memory/resources allocated by mbedtls
 */
static void _free_mebedtls(TLSDataParams *pParams) {
    mbedtls_net_free(&(pParams->socket_fd));
#if defined(MBEDTLS_X509_CRT_PARSE_C)
    mbedtls_x509_crt_free(&(pParams->client_cert));
    mbedtls_x509_crt_free(&(pParams->ca_cert));
    mbedtls_pk_free(&(pParams->private_key));
#endif
    mbedtls_ssl_free(&(pParams->ssl));
    mbedtls_ssl_config_free(&(pParams->ssl_conf));
    mbedtls_ctr_drbg_free(&(pParams->ctr_drbg));
    mbedtls_entropy_free(&(pParams->entropy));

    free(pParams);
}

/**
 * @brief mbedtls SSL client init
 *
 * 1. call a series of mbedtls init functions
 * 2. init and set seed for random functions
 * 3. load CA file, cert files or PSK
 *
 * @param pDataParams       mbedtls TLS parmaters
 * @param pConnectParams    device info for TLS connection
 * @return                  QCLOUD_RET_SUCCESS when success, or err code for failure
 */
static int _mbedtls_client_init(TLSDataParams *pDataParams, TLSConnectParams *pConnectParams) {
    int ret = QCLOUD_RET_SUCCESS;
    mbedtls_net_init(&(pDataParams->socket_fd));
    mbedtls_ssl_init(&(pDataParams->ssl));
    mbedtls_ssl_config_init(&(pDataParams->ssl_conf));
    mbedtls_ctr_drbg_init(&(pDataParams->ctr_drbg));
#if defined(MBEDTLS_X509_CRT_PARSE_C)
    mbedtls_x509_crt_init(&(pDataParams->ca_cert));
    mbedtls_x509_crt_init(&(pDataParams->client_cert));
    mbedtls_pk_init(&(pDataParams->private_key));
#endif

    mbedtls_entropy_init(&(pDataParams->entropy));
    // custom parameter is NULL for now
    if ((ret = mbedtls_ctr_drbg_seed(&(pDataParams->ctr_drbg), mbedtls_entropy_func,
                                     &(pDataParams->entropy), NULL,
                                     0)) != 0) {
        LOGE("mbedtls_ctr_drbg_seed failed returned 0x%04x", ret < 0 ? -ret : ret);
        return QCLOUD_ERR_SSL_INIT;
    }

#if defined(MBEDTLS_X509_CRT_PARSE_C)
    if (pConnectParams->ca != NULL) {
        if ((ret = mbedtls_x509_crt_parse(&(pDataParams->ca_cert),
                                          (const unsigned char *) pConnectParams->ca,
                                          (pConnectParams->ca_len + 1)))) {
            LOGE("parse ca failed returned 0x%04x", ret < 0 ? -ret : ret);
            return QCLOUD_ERR_SSL_CERT;
        }
    }
#endif

#ifdef AUTH_MODE_CERT
    if (pConnectParams->cert_file != NULL && pConnectParams->key_file != NULL) {
#if defined(MBEDTLS_X509_CRT_PARSE_C)
        if ((ret = mbedtls_x509_crt_parse_file(&(pDataParams->client_cert), pConnectParams->cert_file)) != 0) {
            LOGE("load client cert file failed returned 0x%x", ret < 0 ? -ret : ret);
            return QCLOUD_ERR_SSL_CERT;
        }
#endif

        if ((ret = mbedtls_pk_parse_keyfile(&(pDataParams->private_key), pConnectParams->key_file, "")) != 0) {
            LOGE("load client key file failed returned 0x%x", ret < 0 ? -ret : ret);
            return QCLOUD_ERR_SSL_CERT;
        }
    } else {
        LOGD("cert_file/key_file is empty!|cert_file=%s|key_file=%s", pConnectParams->cert_file,
              pConnectParams->key_file);
    }
#else
    if (pConnectParams->psk != NULL && pConnectParams->psk_id != NULL) {
        const char *psk_id = pConnectParams->psk_id;
        ret = mbedtls_ssl_conf_psk(&(pDataParams->ssl_conf), (unsigned char *) pConnectParams->psk,
                                   pConnectParams->psk_length, (const unsigned char *) psk_id,
                                   strlen(psk_id));
    } else {
        LOGD("psk/pskid is empty!|psk=%s|psd_id=%s", pConnectParams->psk, pConnectParams->psk_id);
    }

    if (0 != ret) {
        LOGE("mbedtls_ssl_conf_psk fail: 0x%x", ret < 0 ? -ret : ret);
        return ret;
    }
#endif

    return QCLOUD_RET_SUCCESS;
}

/**
 * @brief Setup TCP connection
 *
 * @param socket_fd  socket handle
 * @param host       server address
 * @param port       server port
 * @return QCLOUD_RET_SUCCESS when success, or err code for failure
 */
int _mbedtls_tcp_connect(mbedtls_net_context *socket_fd, const char *host, int port) {
    int ret = 0;
    char port_str[6];
    snprintf(port_str, 6, "%d", port);
    if ((ret = mbedtls_net_connect(socket_fd, host, port_str, MBEDTLS_NET_PROTO_TCP)) != 0) {
        LOGE("tcp connect failed returned 0x%04x errno: %d", ret < 0 ? -ret : ret, errno);

        switch (ret) {
            case MBEDTLS_ERR_NET_SOCKET_FAILED:
                return QCLOUD_ERR_TCP_SOCKET_FAILED;
            case MBEDTLS_ERR_NET_UNKNOWN_HOST:
                return QCLOUD_ERR_TCP_UNKNOWN_HOST;
            default:
                return QCLOUD_ERR_TCP_CONNECT;
        }
    }

#if 0
    if ((ret = mbedtls_net_set_block(socket_fd)) != 0) {
        LOGE("set block faliled returned 0x%04x", ret < 0 ? -ret : ret);
        return QCLOUD_ERR_TCP_CONNECT;
    }
#endif

    return QCLOUD_RET_SUCCESS;
}

/**
 * @brief verify server certificate
 *
 * mbedtls has provided similar function mbedtls_x509_crt_verify_with_profile
 *
 * @return
 */
#if defined(MBEDTLS_X509_CRT_PARSE_C)

int _qcloud_server_certificate_verify(void *hostname, mbedtls_x509_crt *crt, int depth,
                                      uint32_t *flags) {
    return *flags;
}

#endif

uintptr_t HAL_TLS_Connect(TLSConnectParams *pConnectParams, const char *host, int port) {
    int ret = 0;

    TLSDataParams *pDataParams = (TLSDataParams *) malloc(sizeof(TLSDataParams));

    if ((ret = _mbedtls_client_init(pDataParams, pConnectParams)) != QCLOUD_RET_SUCCESS) {
        goto error;
    }

    LOGD("Setting up the SSL/TLS structure...");
    if ((ret = mbedtls_ssl_config_defaults(&(pDataParams->ssl_conf), MBEDTLS_SSL_IS_CLIENT,
                                           MBEDTLS_SSL_TRANSPORT_STREAM,
                                           MBEDTLS_SSL_PRESET_DEFAULT)) != 0) {
        LOGE("mbedtls_ssl_config_defaults failed returned 0x%04x", ret < 0 ? -ret : ret);
        goto error;
    }

#if defined(MBEDTLS_X509_CRT_PARSE_C)
    mbedtls_ssl_conf_verify(&(pDataParams->ssl_conf), _qcloud_server_certificate_verify,
                            (void *) host);

    mbedtls_ssl_conf_authmode(&(pDataParams->ssl_conf), MBEDTLS_SSL_VERIFY_REQUIRED);
#endif

    mbedtls_ssl_conf_rng(&(pDataParams->ssl_conf), mbedtls_ctr_drbg_random,
                         &(pDataParams->ctr_drbg));

#if defined(MBEDTLS_X509_CRT_PARSE_C)
    mbedtls_ssl_conf_ca_chain(&(pDataParams->ssl_conf), &(pDataParams->ca_cert), NULL);
    if ((ret = mbedtls_ssl_conf_own_cert(&(pDataParams->ssl_conf), &(pDataParams->client_cert),
                                         &(pDataParams->private_key))) != 0) {
        LOGE("mbedtls_ssl_conf_own_cert failed returned 0x%04x", ret < 0 ? -ret : ret);
        goto error;
    }
#endif

    mbedtls_ssl_conf_read_timeout(&(pDataParams->ssl_conf), pConnectParams->timeout_ms);
    if ((ret = mbedtls_ssl_setup(&(pDataParams->ssl), &(pDataParams->ssl_conf))) != 0) {
        LOGE("mbedtls_ssl_setup failed returned 0x%04x", ret < 0 ? -ret : ret);
        goto error;
    }

#ifndef AUTH_MODE_CERT
    // ciphersuites selection for PSK device
    if (pConnectParams->psk != NULL) {
        mbedtls_ssl_conf_ciphersuites(&(pDataParams->ssl_conf), ciphersuites);
    }
#endif

#if defined(MBEDTLS_X509_CRT_PARSE_C)
    // Set the hostname to check against the received server certificate and sni
    if ((ret = mbedtls_ssl_set_hostname(&(pDataParams->ssl), host)) != 0) {
        LOGE("mbedtls_ssl_set_hostname failed returned 0x%04x", ret < 0 ? -ret : ret);
        goto error;
    }
#endif

    mbedtls_ssl_set_bio(&(pDataParams->ssl), &(pDataParams->socket_fd), mbedtls_net_send,
                        mbedtls_net_recv,
                        mbedtls_net_recv_timeout);

    LOGD("Performing the SSL/TLS handshake...");
    LOGD("Connecting to /%s/%d...", host, port);
    if ((ret = _mbedtls_tcp_connect(&(pDataParams->socket_fd), host, port)) != QCLOUD_RET_SUCCESS) {
        goto error;
    }

    while ((ret = mbedtls_ssl_handshake(&(pDataParams->ssl))) != 0) {
        if (ret != MBEDTLS_ERR_SSL_WANT_READ && ret != MBEDTLS_ERR_SSL_WANT_WRITE) {
            LOGE("mbedtls_ssl_handshake failed returned 0x%04x", ret < 0 ? -ret : ret);
#if defined(MBEDTLS_X509_CRT_PARSE_C)
            if (ret == MBEDTLS_ERR_X509_CERT_VERIFY_FAILED) {
                LOGE("Unable to verify the server's certificate");
            }
#endif
            goto error;
        }
    }

    if ((ret = mbedtls_ssl_get_verify_result(&(pDataParams->ssl))) != 0) {
        LOGE("mbedtls_ssl_get_verify_result failed returned 0x%04x", ret < 0 ? -ret : ret);
        goto error;
    }

    mbedtls_ssl_conf_read_timeout(&(pDataParams->ssl_conf), 100);

    LOGI("connected with /%s/%d...", host, port);

    return (uintptr_t) pDataParams;

    error:
    _free_mebedtls(pDataParams);
    return 0;
}

void HAL_TLS_Disconnect(uintptr_t handle) {
    if ((uintptr_t) NULL == handle) {
        LOGD("handle is NULL");
        return;
    }
    TLSDataParams *pParams = (TLSDataParams *) handle;
    int ret = 0;
    do {
        ret = mbedtls_ssl_close_notify(&(pParams->ssl));
    } while (ret == MBEDTLS_ERR_SSL_WANT_READ || ret == MBEDTLS_ERR_SSL_WANT_WRITE);

    mbedtls_net_free(&(pParams->socket_fd));
#if defined(MBEDTLS_X509_CRT_PARSE_C)
    mbedtls_x509_crt_free(&(pParams->client_cert));
    mbedtls_x509_crt_free(&(pParams->ca_cert));
    mbedtls_pk_free(&(pParams->private_key));
#endif
    mbedtls_ssl_free(&(pParams->ssl));
    mbedtls_ssl_config_free(&(pParams->ssl_conf));
    mbedtls_ctr_drbg_free(&(pParams->ctr_drbg));
    mbedtls_entropy_free(&(pParams->entropy));

    free((void *) handle);
}

int HAL_TLS_Write(uintptr_t handle, unsigned char *msg, size_t totalLen, uint32_t timeout_ms,
                  size_t *written_len) {
    Timer timer;
    timer_init(&timer);
    countdown_ms(&timer, (unsigned int) timeout_ms);
    size_t written_so_far;
    bool errorFlag = false;
    int write_rc = 0;

    TLSDataParams *pParams = (TLSDataParams *) handle;

    for (written_so_far = 0;
         written_so_far < totalLen && !expired(&timer); written_so_far += write_rc) {
        while (!expired(&timer) &&
               (write_rc = mbedtls_ssl_write(&(pParams->ssl), msg + written_so_far,
                                             totalLen - written_so_far)) <= 0) {
            if (write_rc != MBEDTLS_ERR_SSL_WANT_READ && write_rc != MBEDTLS_ERR_SSL_WANT_WRITE) {
                LOGE("HAL_TLS_write failed 0x%04x", write_rc < 0 ? -write_rc : write_rc);
                errorFlag = true;
                break;
            }
        }

        if (errorFlag) {
            break;
        }
    }

    *written_len = written_so_far;

    if (errorFlag) {
        return QCLOUD_ERR_SSL_WRITE;
    } else if (expired(&timer) && written_so_far != totalLen) {
        return QCLOUD_ERR_SSL_WRITE_TIMEOUT;
    }

    return QCLOUD_RET_SUCCESS;
}

int HAL_TLS_Read(uintptr_t handle, unsigned char *msg, size_t totalLen, uint32_t timeout_ms,
                 size_t *read_len) {
    // mbedtls_ssl_conf_read_timeout(&(pParams->ssl_conf), timeout_ms); TODO:this cause read blocking and no return even
    // timeout
    // use non-blocking read
    Timer timer;
    timer_init(&timer);
    countdown_ms(&timer, (unsigned int) timeout_ms);
    *read_len = 0;

    TLSDataParams *pParams = (TLSDataParams *) handle;

    do {
        int read_rc = 0;
        read_rc = mbedtls_ssl_read(&(pParams->ssl), msg + *read_len, totalLen - *read_len);

        if (read_rc > 0) {
            *read_len += read_rc;
        } else if (read_rc == 0 ||
                   (read_rc != MBEDTLS_ERR_SSL_WANT_WRITE && read_rc != MBEDTLS_ERR_SSL_WANT_READ &&
                    read_rc != MBEDTLS_ERR_SSL_TIMEOUT)) {
            LOGE("cloud_iot_network_tls_read failed: 0x%04x", read_rc < 0 ? -read_rc : read_rc);
            return QCLOUD_ERR_SSL_READ;
        }

        if (expired(&timer)) {
            break;
        }

    } while (*read_len < totalLen);

    if (totalLen == *read_len) {
        return QCLOUD_RET_SUCCESS;
    }

    if (*read_len == 0) {
        return QCLOUD_ERR_SSL_NOTHING_TO_READ;
    } else {
        return QCLOUD_ERR_SSL_READ_TIMEOUT;
    }
}

#endif
