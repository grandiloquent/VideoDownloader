#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

/*
 * Type of network interface
 */

typedef enum {
    NETWORK_TCP = 0, NETWORK_UDP = 1, NETWORK_TLS = 2, NETWORK_DTLS = 3
} NETWORK_TYPE;

/**
 * @brief Define structure for network stack
 */
typedef struct Network Network;

/**
 * @brief Define structure for network stack
 *
 * Network init/connect/read/write/disconnect/state
 */
struct Network {
    int (*init)(Network *);

    int (*connect)(Network *);

    int (*read)(Network *, unsigned char *, size_t, uint32_t, size_t *);

    int (*write)(Network *, unsigned char *, size_t, uint32_t, size_t *);

    void (*disconnect)(Network *);

    int (*is_connected)(Network *);

    // connetion handle:
    // for non-AT: 0 = not connected, non-zero = connected
    // for AT: 0 = valid connection, MAX_UNSINGED_INT = invalid
    uintptr_t handle;

#ifndef AUTH_WITH_NOTLS
    SSLConnectParams ssl_connect_params;
#endif

    const char *host;  // server address
    int port;  // server port
    NETWORK_TYPE type;
};

typedef struct {
    int remote_port;
    int response_code;
    char *header;
    char *auth_user;
    char *auth_password;
    Network network_stack;
} HTTPClient;

typedef struct {
    bool is_more;               // if more data to check
    bool is_chunked;            // if response in chunked data
    int retrieve_len;          // length of retrieve
    int response_content_len;  // length of resposne content
    int post_buf_len;          // post data length
    int response_buf_len;      // length of response data buffer
    char *post_content_type;     // type of post content
    char *post_buf;              // post data buffer
    char *response_buf;          // response data buffer
} HTTPClientData;

int is_network_connected(Network *pNetwork) {
    return pNetwork->handle;
}

#define AT_NO_CONNECTED_FD 0xffffffff

int is_network_at_connected(Network *pNetwork) {
    return pNetwork->handle == AT_NO_CONNECTED_FD ? 0 : pNetwork->handle == AT_NO_CONNECTED_FD;
}

static uint64_t _linux_get_time_ms(void) {
    struct timeval tv = {0};
    uint64_t time_ms;

    gettimeofday(&tv, NULL);

    time_ms = tv.tv_sec * 1000 + tv.tv_usec / 1000;

    return time_ms;
}

static uint64_t _linux_time_left(uint64_t t_end, uint64_t t_now) {
    uint64_t t_left;

    if (t_end > t_now) {
        t_left = t_end - t_now;
    } else {
        t_left = 0;
    }

    return t_left;
}

uintptr_t HAL_TCP_Connect(const char *host, uint16_t port) {
    int ret;
    struct addrinfo hints, *addr_list, *cur;
    int fd = 0;

    char port_str[6];
    snprintf(port_str, 6, "%d", port);

    memset(&hints, 0x00, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    ret = getaddrinfo(host, port_str, &hints, &addr_list);
    if (ret) {
        if (ret == EAI_SYSTEM)
            LOGE("getaddrinfo(%s:%s) error: %s", host, port_str, strerror(errno));
        else
            LOGE("getaddrinfo(%s:%s) error: %s", host, port_str, gai_strerror(ret));
        return 0;
    }

    for (cur = addr_list; cur != NULL; cur = cur->ai_next) {
        fd = (int) socket(cur->ai_family, cur->ai_socktype, cur->ai_protocol);
        if (fd < 0) {
            ret = 0;
            continue;
        }

        if (connect(fd, cur->ai_addr, cur->ai_addrlen) == 0) {
            ret = fd;
            break;
        }

        close(fd);
        ret = 0;
    }

    if (0 == ret) {
        LOGE("fail to connect with TCP server: %s:%s", host, port_str);
    } else {
        LOGI("connected with TCP server: %s:%s", host, port_str);
    }

    freeaddrinfo(addr_list);

    return (uintptr_t) ret;
}

int HAL_TCP_Disconnect(uintptr_t fd) {
    int rc;

    /* Shutdown both send and receive operations. */
    rc = shutdown((int) fd, 2);
    if (0 != rc) {
        LOGE("shutdown error: %s", strerror(errno));
        return -1;
    }

    rc = close((int) fd);
    if (0 != rc) {
        LOGE("closesocket error: %s", strerror(errno));
        return -1;
    }

    return 0;
}

int HAL_TCP_Write(uintptr_t fd, const unsigned char *buf, uint32_t len, uint32_t timeout_ms,
                  size_t *written_len) {
    int ret;
    uint32_t len_sent;
    uint64_t t_end, t_left;
    fd_set sets;

    t_end = _linux_get_time_ms() + timeout_ms;
    len_sent = 0;

    /* send one time if timeout_ms is value 0 */
    do {
        t_left = _linux_time_left(t_end, _linux_get_time_ms());

        if (0 != t_left) {
            struct timeval timeout;

            FD_ZERO(&sets);
            FD_SET(fd, &sets);

            timeout.tv_sec = t_left / 1000;
            timeout.tv_usec = (t_left % 1000) * 1000;

            ret = select(fd + 1, NULL, &sets, NULL, &timeout);
            if (ret > 0) {
                if (0 == FD_ISSET(fd, &sets)) {
                    LOGE("Should NOT arrive");
                    /* If timeout in next loop, it will not sent any data */
                    ret = 0;
                    continue;
                }
            } else if (0 == ret) {
                ret = QCLOUD_ERR_TCP_WRITE_TIMEOUT;
                LOGE("select-write timeout %d", (int) fd);
                break;
            } else {
                if (EINTR == errno) {
                    LOGE("EINTR be caught");
                    continue;
                }

                ret = QCLOUD_ERR_TCP_WRITE_FAIL;
                LOGE("select-write fail: %s", strerror(errno));
                break;
            }
        } else {
            ret = QCLOUD_ERR_TCP_WRITE_TIMEOUT;
        }

        if (ret > 0) {
            ret = send(fd, buf + len_sent, len - len_sent, 0);
            if (ret > 0) {
                len_sent += ret;
            } else if (0 == ret) {
                LOGE("No data be sent. Should NOT arrive");
            } else {
                if (EINTR == errno) {
                    LOGE("EINTR be caught");
                    continue;
                }

                ret = QCLOUD_ERR_TCP_WRITE_FAIL;
                LOGE("send fail: %s", strerror(errno));
                break;
            }
        }
    } while ((len_sent < len) && (_linux_time_left(t_end, _linux_get_time_ms()) > 0));

    *written_len = (size_t) len_sent;

    return len_sent > 0 ? QCLOUD_RET_SUCCESS : ret;
}

int HAL_TCP_Read(uintptr_t fd, unsigned char *buf, uint32_t len, uint32_t timeout_ms,
                 size_t *read_len) {
    int ret, err_code;
    uint32_t len_recv;
    uint64_t t_end, t_left;
    fd_set sets;
    struct timeval timeout;

    t_end = _linux_get_time_ms() + timeout_ms;
    len_recv = 0;
    err_code = 0;

    do {
        t_left = _linux_time_left(t_end, _linux_get_time_ms());
        if (0 == t_left) {
            err_code = QCLOUD_ERR_TCP_READ_TIMEOUT;
            break;
        }

        FD_ZERO(&sets);
        FD_SET(fd, &sets);

        timeout.tv_sec = t_left / 1000;
        timeout.tv_usec = (t_left % 1000) * 1000;

        ret = select(fd + 1, &sets, NULL, NULL, &timeout);
        if (ret > 0) {
            ret = recv(fd, buf + len_recv, len - len_recv, 0);
            if (ret > 0) {
                len_recv += ret;
            } else if (0 == ret) {
                struct sockaddr_in peer;
                socklen_t sLen = sizeof(peer);
                int peer_port = 0;
                getpeername(fd, (struct sockaddr *) &peer, &sLen);
                peer_port = ntohs(peer.sin_port);

                LOGE("connection is closed by server: %s:%d", inet_ntoa(peer.sin_addr),
                     peer_port);

                err_code = QCLOUD_ERR_TCP_PEER_SHUTDOWN;
                break;
            } else {
                if (EINTR == errno) {
                    LOGE("EINTR be caught");
                    continue;
                }
                LOGE("recv error: %s", strerror(errno));
                err_code = QCLOUD_ERR_TCP_READ_FAIL;
                break;
            }
        } else if (0 == ret) {
            err_code = QCLOUD_ERR_TCP_READ_TIMEOUT;
            break;
        } else {
            LOGE("select-recv error: %s", strerror(errno));
            err_code = QCLOUD_ERR_TCP_READ_FAIL;
            break;
        }
    } while ((len_recv < len));

    *read_len = (size_t) len_recv;

    if (err_code == QCLOUD_ERR_TCP_READ_TIMEOUT && len_recv == 0)
        err_code = QCLOUD_ERR_TCP_NOTHING_TO_READ;

    return (len == len_recv) ? QCLOUD_RET_SUCCESS : err_code;
}


int network_tcp_init(Network *pNetwork) {
    return QCLOUD_RET_SUCCESS;
}

int network_tcp_connect(Network *pNetwork) {

    pNetwork->handle = HAL_TCP_Connect(pNetwork->host, pNetwork->port);
    if (0 == pNetwork->handle) {
        return -1;
    }

    return 0;
}

int network_tcp_read(Network *pNetwork, unsigned char *data, size_t datalen, uint32_t timeout_ms,
                     size_t *read_len) {

    int rc = 0;

    rc = HAL_TCP_Read(pNetwork->handle, data, (uint32_t) datalen, timeout_ms, read_len);

    return rc;
}

int network_tcp_write(Network *pNetwork, unsigned char *data, size_t datalen, uint32_t timeout_ms,
                      size_t *written_len) {

    int rc = 0;

    rc = HAL_TCP_Write(pNetwork->handle, data, datalen, timeout_ms, written_len);

    return rc;
}

void network_tcp_disconnect(Network *pNetwork) {

    if (0 == pNetwork->handle) {
        return;
    }

    HAL_TCP_Disconnect(pNetwork->handle);
    pNetwork->handle = 0;
    return;
}

int network_init(Network *pNetwork) {

    // to avoid process crash when writing to a broken socket
#if defined(__linux__)
    signal(SIGPIPE, SIG_IGN);
#endif

    pNetwork->init = network_tcp_init;
    pNetwork->connect = network_tcp_connect;
    pNetwork->read = network_tcp_read;
    pNetwork->write = network_tcp_write;
    pNetwork->disconnect = network_tcp_disconnect;
    pNetwork->is_connected = is_network_connected;
    pNetwork->handle = 0;

    return pNetwork->init(pNetwork);
}

static void _http_client_base64enc(char *out, const char *in) {
    const char code[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    int i = 0, x = 0, l = 0;

    for (; *in; in++) {
        x = x << 8 | *in;
        for (l += 8; l >= 6; l -= 6) {
            out[i++] = code[(x >> (l - 6)) & 0x3f];
        }
    }
    if (l > 0) {
        x <<= 6 - l;
        out[i++] = code[x & 0x3f];
    }
    for (; i % 4;) {
        out[i++] = '=';
    }
    out[i] = '\0';
}

static int
_http_client_parse_url(const char *url, char *scheme, uint32_t max_scheme_len, char *host,
                       uint32_t maxhost_len, int *port, char *path, uint32_t max_path_len) {
    char *scheme_ptr = (char *) url;
    char *host_ptr = (char *) strstr(url, "://");
    uint32_t host_len = 0;
    uint32_t path_len;

    char *path_ptr;
    char *fragment_ptr;

    if (host_ptr == NULL) {
        LOGE("Could not find host");
        return QCLOUD_ERR_HTTP_PARSE;
    }

    if (max_scheme_len < host_ptr - scheme_ptr + 1) {
        LOGE("Scheme str is too small (%u >= %u)", max_scheme_len,
             (uint32_t) (host_ptr - scheme_ptr + 1));
        return QCLOUD_ERR_HTTP_PARSE;
    }
    memcpy(scheme, scheme_ptr, host_ptr - scheme_ptr);
    scheme[host_ptr - scheme_ptr] = '\0';

    host_ptr += 3;

    *port = 0;

    path_ptr = strchr(host_ptr, '/');
    if (NULL == path_ptr) {
        path_ptr = scheme_ptr + (int) strlen(url);
        host_len = path_ptr - host_ptr;
        memcpy(host, host_ptr, host_len);
        host[host_len] = '\0';

        memcpy(path, "/", 1);
        path[1] = '\0';

        return QCLOUD_RET_SUCCESS;
    }

    if (host_len == 0) {
        host_len = path_ptr - host_ptr;
    }

    if (maxhost_len < host_len + 1) {
        LOGE("Host str is too long (host_len(%d) >= max_len(%d))", host_len + 1, maxhost_len);
        return QCLOUD_ERR_HTTP_PARSE;
    }
    memcpy(host, host_ptr, host_len);
    host[host_len] = '\0';

    fragment_ptr = strchr(host_ptr, '#');
    if (fragment_ptr != NULL) {
        path_len = fragment_ptr - path_ptr;
    } else {
        path_len = strlen(path_ptr);
    }

    if (max_path_len < path_len + 1) {
        LOGE("Path str is too small (%d >= %d)", max_path_len, path_len + 1);
        return QCLOUD_ERR_HTTP_PARSE;
    }

    memcpy(path, path_ptr, path_len);

    path[path_len] = '\0';

    return QCLOUD_RET_SUCCESS;
}

static int _http_client_parse_host(const char *url, char *host, uint32_t host_max_len) {
    const char *host_ptr = (const char *) strstr(url, "://");
    uint32_t host_len = 0;
    char *path_ptr;

    if (host_ptr == NULL) {
        LOGE("Could not find host");
        return QCLOUD_ERR_HTTP_PARSE;
    }
    host_ptr += 3;

    uint32_t pro_len = 0;
    pro_len = host_ptr - url;

    path_ptr = strchr(host_ptr, '/');
    if (path_ptr != NULL)
        host_len = path_ptr - host_ptr;
    else
        host_len = strlen(url) - pro_len;

    if (host_max_len < host_len + 1) {
        LOGE("Host str is too small (%d >= %d)", host_max_len, host_len + 1);
        return QCLOUD_ERR_HTTP_PARSE;
    }
    memcpy(host, host_ptr, host_len);
    host[host_len] = '\0';

    return QCLOUD_RET_SUCCESS;
}

#define HTTP_CLIENT_MIN(x, y) (((x) < (y)) ? (x) : (y))
#define HTTP_CLIENT_MAX(x, y) (((x) > (y)) ? (x) : (y))

#define HTTP_CLIENT_AUTHB_SIZE 128

#define HTTP_CLIENT_CHUNK_SIZE    1024
#define HTTP_CLIENT_SEND_BUF_SIZE 1024

#define HTTP_CLIENT_MAX_HOST_LEN 64
#define HTTP_CLIENT_MAX_URL_LEN  1024

#define HTTP_RETRIEVE_MORE_DATA (1)
typedef enum {
    HTTP_GET, HTTP_POST, HTTP_PUT, HTTP_DELETE, HTTP_HEAD
} HttpMethod;
#define IOT_FUNC_EXIT_RC(x) \
    {                       \
        return x;           \
    }
#define IOT_FUNC_ENTRY
#define IOT_TRUE            (1)     /* indicate boolean value true */
#define IOT_FALSE           (0)     /* indicate boolean value false */
int left_ms(Timer *timer)
{
    return HAL_Timer_remain(timer);
}

static int
_http_client_get_info(HTTPClient *client, unsigned char *send_buf, int *send_idx, char *buf,
                      uint32_t len) {
    int rc = QCLOUD_RET_SUCCESS;
    int cp_len;
    int idx = *send_idx;

    if (len == 0) {
        len = strlen(buf);
    }

    do {
        if ((HTTP_CLIENT_SEND_BUF_SIZE - idx) >= len) {
            cp_len = len;
        } else {
            cp_len = HTTP_CLIENT_SEND_BUF_SIZE - idx;
        }

        memcpy(send_buf + idx, buf, cp_len);
        idx += cp_len;
        len -= cp_len;

        if (idx == HTTP_CLIENT_SEND_BUF_SIZE) {
            size_t byte_written_len = 0;
            rc = client->network_stack.write(&(client->network_stack), send_buf,
                                             HTTP_CLIENT_SEND_BUF_SIZE, 5000,
                                             &byte_written_len);
            if (byte_written_len) {
                return (byte_written_len);
            }
        }
    } while (len);

    *send_idx = idx;
    return rc;
}

static int _http_client_send_auth(HTTPClient *client, unsigned char *send_buf, int *send_idx) {
    char b_auth[(int) ((HTTP_CLIENT_AUTHB_SIZE + 3) * 4 / 3 + 1)];
    char base64buff[HTTP_CLIENT_AUTHB_SIZE + 3];

    _http_client_get_info(client, send_buf, send_idx, "Authorization: Basic ", 0);
    snprintf(base64buff, sizeof(base64buff), "%s:%s", client->auth_user, client->auth_password);

    _http_client_base64enc(b_auth, base64buff);
    b_auth[strlen(b_auth) + 1] = '\0';
    b_auth[strlen(b_auth)] = '\n';

    _http_client_get_info(client, send_buf, send_idx, b_auth, 0);

    return QCLOUD_RET_SUCCESS;
}

static int _http_client_send_header(HTTPClient *client, const char *url, HttpMethod method,
                                    HTTPClientData *client_data) {
    char scheme[8] = {0};
    char host[HTTP_CLIENT_MAX_HOST_LEN] = {0};
    char path[HTTP_CLIENT_MAX_URL_LEN] = {0};
    int len;
    unsigned char send_buf[HTTP_CLIENT_SEND_BUF_SIZE] = {0};
    char buf[HTTP_CLIENT_SEND_BUF_SIZE] = {0};
    char *meth = (method == HTTP_GET)
                 ? "GET"
                 : (method == HTTP_POST)
                   ? "POST"
                   : (method == HTTP_PUT)
                     ? "PUT"
                     : (method == HTTP_DELETE) ? "DELETE" : (method == HTTP_HEAD) ? "HEAD" : "";
    int rc;
    int port;

    int res = _http_client_parse_url(url, scheme, sizeof(scheme), host, sizeof(host), &port, path,
                                     sizeof(path));
    if (res != QCLOUD_RET_SUCCESS) {
        LOGE("httpclient_parse_url returned %d", res);
        return res;
    }

    if (strcmp(scheme, "http") == 0) {
    } else if (strcmp(scheme, "https") == 0) {
    }

    memset(send_buf, 0, HTTP_CLIENT_SEND_BUF_SIZE);
    len = 0;

    snprintf(buf, sizeof(buf), "%s %s HTTP/1.1\r\nHost: %s\r\n", meth, path, host);
    rc = _http_client_get_info(client, send_buf, &len, buf, strlen(buf));
    if (rc) {
        LOGE("Could not write request");
        return QCLOUD_ERR_HTTP_CONN;
    }

    if (client->auth_user) {
        _http_client_send_auth(client, send_buf, &len);
    }

    if (client->header) {
        _http_client_get_info(client, send_buf, &len, (char *) client->header,
                              strlen(client->header));
    }

    if (client_data->post_buf != NULL) {
        snprintf(buf, sizeof(buf), "Content-Length: %d\r\n", client_data->post_buf_len);
        _http_client_get_info(client, send_buf, &len, buf, strlen(buf));

        if (client_data->post_content_type != NULL) {
            snprintf(buf, sizeof(buf), "Content-Type: %s\r\n", client_data->post_content_type);
            _http_client_get_info(client, send_buf, &len, buf, strlen(buf));
        }
    }

    _http_client_get_info(client, send_buf, &len, "\r\n", 0);

    // LOGD("REQUEST:\n%s", send_buf);

    size_t written_len = 0;
    rc = client->network_stack.write(&client->network_stack, send_buf, len, 5000, &written_len);
    if (written_len > 0) {
        // LOGD("Written %lu bytes", written_len);
    } else if (written_len == 0) {
        LOGE("written_len == 0,Connection was closed by server");
        return QCLOUD_ERR_HTTP_CLOSED; /* Connection was closed by server */
    } else {
        LOGE("Connection error (send returned %d)", rc);
        return QCLOUD_ERR_HTTP_CONN;
    }

    return QCLOUD_RET_SUCCESS;
}

static int _http_client_send_userdata(HTTPClient *client, HTTPClientData *client_data) {
    if (client_data->post_buf && client_data->post_buf_len) {
        // LOGD("client_data->post_buf: %s", client_data->post_buf);
        {
            size_t written_len = 0;
            int rc = client->network_stack.write(&client->network_stack,
                                                 (unsigned char *) client_data->post_buf,
                                                 client_data->post_buf_len, 5000, &written_len);
            if (written_len > 0) {
                // LOGD("Written %d bytes", written_len);
            } else if (written_len == 0) {
                LOGE("written_len == 0,Connection was closed by server");
                return QCLOUD_ERR_HTTP_CLOSED;
            } else {
                LOGE("Connection error (send returned %d)", rc);
                return QCLOUD_ERR_HTTP_CONN;
            }
        }
    }

    return QCLOUD_RET_SUCCESS;
}

static int
_http_client_recv(HTTPClient *client, char *buf, int min_len, int max_len, int *p_read_len,
                  uint32_t timeout_ms, HTTPClientData *client_data) {
    IOT_FUNC_ENTRY;

    int rc = 0;
    Timer timer;
    size_t recv_size = 0;

    timer_init(&timer);
    countdown_ms(&timer, (unsigned int) timeout_ms);

    *p_read_len = 0;

    rc = client->network_stack.read(&client->network_stack, (unsigned char *) buf, max_len,
                                    (uint32_t) left_ms(&timer),
                                    &recv_size);
    *p_read_len = (int) recv_size;
    if (rc == QCLOUD_ERR_SSL_NOTHING_TO_READ || rc == QCLOUD_ERR_TCP_NOTHING_TO_READ) {
        LOGD("HTTP read nothing and timeout");
        rc = QCLOUD_RET_SUCCESS;
    } else if (rc == QCLOUD_ERR_SSL_READ_TIMEOUT || rc == QCLOUD_ERR_TCP_READ_TIMEOUT) {
        if (*p_read_len == client_data->retrieve_len || client_data->retrieve_len == 0)
            rc = QCLOUD_RET_SUCCESS;
        else
            LOGE("network_stack read timeout");
    } else if (rc == QCLOUD_ERR_TCP_PEER_SHUTDOWN && *p_read_len > 0) {
        /* HTTP server give response and close this connection */
        client->network_stack.disconnect(&client->network_stack);
        rc = QCLOUD_RET_SUCCESS;
    } else if (rc != QCLOUD_RET_SUCCESS) {
        LOGE("Connection error rc = %d (recv returned %d)", rc, *p_read_len);
        IOT_FUNC_EXIT_RC(rc);
    }

    // IOT_FUNC_EXIT_RC(rc);
    IOT_FUNC_EXIT_RC(QCLOUD_RET_SUCCESS);
}

static int
_http_client_retrieve_content(HTTPClient *client, char *data, int len, uint32_t timeout_ms,
                              HTTPClientData *client_data) {
    IOT_FUNC_ENTRY;

    int count = 0;
    int templen = 0;
    int crlf_pos;
    Timer timer;

    timer_init(&timer);
    countdown_ms(&timer, (unsigned int) timeout_ms);

    client_data->is_more = IOT_TRUE;

    if (client_data->response_content_len == -1 && client_data->is_chunked == IOT_FALSE) {
        while (1) {
            int rc, max_len;
            if (count + len < client_data->response_buf_len - 1) {
                memcpy(client_data->response_buf + count, data, len);
                count += len;
                client_data->response_buf[count] = '\0';
            } else {
                memcpy(client_data->response_buf + count, data,
                       client_data->response_buf_len - 1 - count);
                client_data->response_buf[client_data->response_buf_len - 1] = '\0';
                return HTTP_RETRIEVE_MORE_DATA;
            }

            max_len = HTTP_CLIENT_MIN(HTTP_CLIENT_CHUNK_SIZE - 1,
                                      client_data->response_buf_len - 1 - count);
            rc = _http_client_recv(client, data, 1, max_len, &len, (uint32_t) left_ms(&timer),
                                   client_data);

            /* Receive data */
            // LOGD("data len: %d %d", len, count);

            if (rc != QCLOUD_RET_SUCCESS) {
                IOT_FUNC_EXIT_RC(rc);
            }
            if (0 == left_ms(&timer)) {
                LOGE("HTTP read timeout!");
                IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_TIMEOUT);
            }

            if (len == 0) {
                /* read no more data */
                LOGD("no more data, len == 0");
                client_data->is_more = IOT_FALSE;
                IOT_FUNC_EXIT_RC(QCLOUD_RET_SUCCESS);
            }
        }
    }

    while (1) {
        uint32_t readLen = 0;
        if (client_data->is_chunked && client_data->retrieve_len <= 0) {
            /* Read chunk header */
            bool foundCrlf;
            int n;
            do {
                foundCrlf = IOT_FALSE;
                crlf_pos = 0;
                data[len] = 0;
                if (len >= 2) {
                    for (; crlf_pos < len - 2; crlf_pos++) {
                        if (data[crlf_pos] == '\r' && data[crlf_pos + 1] == '\n') {
                            foundCrlf = IOT_TRUE;
                            break;
                        }
                    }
                }
                if (!foundCrlf) {
                    /* Try to read more */
                    if (len < HTTP_CLIENT_CHUNK_SIZE) {
                        int new_trf_len, rc;
                        rc = _http_client_recv(client, data + len, 0,
                                               HTTP_CLIENT_CHUNK_SIZE - len - 1, &new_trf_len,
                                               left_ms(&timer), client_data);
                        len += new_trf_len;
                        if (rc != QCLOUD_RET_SUCCESS) {
                            IOT_FUNC_EXIT_RC(rc);
                        } else {
                            continue;
                        }
                    } else {
                        IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP);
                    }
                }
            } while (!foundCrlf);
            data[crlf_pos] = '\0';

            // n = sscanf(data, "%x", &readLen);/* chunk length */
            readLen = strtoul(data, NULL, 16);
            n = (0 == readLen) ? 0 : 1;
            client_data->retrieve_len = readLen;
            client_data->response_content_len += client_data->retrieve_len;
            if (readLen == 0) {
                client_data->is_more = IOT_FALSE;
                LOGD("no more (last chunk)");
            }

            if (n != 1) {
                LOGE("Could not read chunk length");
                return QCLOUD_ERR_HTTP_UNRESOLVED_DNS;
            }

            memmove(data, &data[crlf_pos + 2], len - (crlf_pos + 2));
            len -= (crlf_pos + 2);

        } else {
            readLen = client_data->retrieve_len;
        }

        do {
            templen = HTTP_CLIENT_MIN(len, readLen);
            if (count + templen < client_data->response_buf_len - 1) {
                memcpy(client_data->response_buf + count, data, templen);
                count += templen;
                client_data->response_buf[count] = '\0';
                client_data->retrieve_len -= templen;
            } else {
                memcpy(client_data->response_buf + count, data,
                       client_data->response_buf_len - 1 - count);
                client_data->response_buf[client_data->response_buf_len - 1] = '\0';
                client_data->retrieve_len -= (client_data->response_buf_len - 1 - count);
                IOT_FUNC_EXIT_RC(HTTP_RETRIEVE_MORE_DATA);
            }

            if (len > readLen) {
                LOGD("memmove %d %d %d\n", readLen, len, client_data->retrieve_len);
                memmove(data, &data[readLen],
                        len - readLen); /* chunk case, read between two chunks */
                len -= readLen;
                readLen = 0;
                client_data->retrieve_len = 0;
            } else {
                readLen -= len;
            }

            if (readLen) {
                int rc;
                int max_len = HTTP_CLIENT_MIN(HTTP_CLIENT_CHUNK_SIZE - 1,
                                              client_data->response_buf_len - 1 - count);
                max_len = HTTP_CLIENT_MIN(max_len, readLen);
                rc = _http_client_recv(client, data, 1, max_len, &len, left_ms(&timer),
                                       client_data);
                if (rc != QCLOUD_RET_SUCCESS) {
                    IOT_FUNC_EXIT_RC(rc);
                }
                if (left_ms(&timer) == 0) {
                    LOGE("HTTP read timeout!");
                    IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_TIMEOUT);
                }
            }
        } while (readLen);

        if (client_data->is_chunked) {
            if (len < 2) {
                int new_trf_len, rc;
                /* Read missing chars to find end of chunk */
                rc = _http_client_recv(client, data + len, 2 - len,
                                       HTTP_CLIENT_CHUNK_SIZE - len - 1, &new_trf_len,
                                       left_ms(&timer), client_data);
                if ((rc != QCLOUD_RET_SUCCESS) || (0 == left_ms(&timer))) {
                    IOT_FUNC_EXIT_RC(rc);
                }
                len += new_trf_len;
            }

            if ((data[0] != '\r') || (data[1] != '\n')) {
                LOGE("Format error, %s", data); /* after memmove, the beginning of next chunk */
                IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_UNRESOLVED_DNS);
            }
            memmove(data, &data[2], len - 2); /* remove the \r\n */
            len -= 2;
        } else {
            // LOGD("no more (content-length)");
            client_data->is_more = IOT_FALSE;
            break;
        }
    }

    IOT_FUNC_EXIT_RC(QCLOUD_RET_SUCCESS);
}

static int _http_client_response_parse(HTTPClient *client, char *data, int len, uint32_t timeout_ms,
                                       HTTPClientData *client_data) {
    IOT_FUNC_ENTRY;

    int crlf_pos;
    Timer timer;
    char *tmp_ptr, *ptr_body_end;

    timer_init(&timer);
    countdown_ms(&timer, timeout_ms);

    client_data->response_content_len = -1;

    char *crlf_ptr = strstr(data, "\r\n");
    if (crlf_ptr == NULL) {
        LOGE("\\r\\n not found");
        IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_UNRESOLVED_DNS);
    }

    crlf_pos = crlf_ptr - data;
    data[crlf_pos] = '\0';

#if 0
    if (sscanf(data, "HTTP/%*d.%*d %d %*[^\r\n]", &(client->response_code)) != 1) {
        LOGE("Not a correct HTTP answer : %s\n", data);
        return QCLOUD_ERR_HTTP_UNRESOLVED_DNS;
    }
#endif

    client->response_code = atoi(data + 9);

    if ((client->response_code < 200) || (client->response_code >= 400)) {
        LOGE("Response code %d", client->response_code);

        if (client->response_code == 403) IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_AUTH);

        if (client->response_code == 404) IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_NOT_FOUND);
    }

    // LOGD("Reading headers : %s", data);

    // remove null character
    memmove(data, &data[crlf_pos + 2], len - (crlf_pos + 2) + 1);
    len -= (crlf_pos + 2);

    client_data->is_chunked = IOT_FALSE;

    if (NULL == (ptr_body_end = strstr(data, "\r\n\r\n"))) {
        int new_trf_len, rc;
        rc = _http_client_recv(client, data + len, 1, HTTP_CLIENT_CHUNK_SIZE - len - 1,
                               &new_trf_len, left_ms(&timer),
                               client_data);
        if (rc != QCLOUD_RET_SUCCESS) {
            IOT_FUNC_EXIT_RC(rc);
        }
        len += new_trf_len;
        data[len] = '\0';
        if (NULL == (ptr_body_end = strstr(data, "\r\n\r\n"))) {
            LOGE("parse error: no end of the request body");
            IOT_FUNC_EXIT_RC(QCLOUD_ERR_FAILURE);
        }
    }

    if (NULL != (tmp_ptr = strstr(data, "Content-Length"))) {
        client_data->response_content_len = atoi(tmp_ptr + strlen("Content-Length: "));
        client_data->retrieve_len = client_data->response_content_len;
    } else if (NULL != (tmp_ptr = strstr(data, "Transfer-Encoding"))) {
        int len_chunk = strlen("Chunked");
        char *chunk_value = data + strlen("Transfer-Encoding: ");

        if ((!memcmp(chunk_value, "Chunked", len_chunk)) ||
            (!memcmp(chunk_value, "chunked", len_chunk))) {
            client_data->is_chunked = IOT_TRUE;
            client_data->response_content_len = 0;
            client_data->retrieve_len = 0;
        }
    } else {
        LOGE("Could not parse header");
        IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP);
    }

    len = len - (ptr_body_end + 4 - data);
    memmove(data, ptr_body_end + 4, len + 1);
    int rc = _http_client_retrieve_content(client, data, len, left_ms(&timer), client_data);
    IOT_FUNC_EXIT_RC(rc);
}

static int _http_client_connect(HTTPClient *client) {
    if (QCLOUD_RET_SUCCESS != client->network_stack.connect(&client->network_stack)) {
        return QCLOUD_ERR_HTTP_CONN;
    }

    return QCLOUD_RET_SUCCESS;
}

static int _http_client_send_request(HTTPClient *client, const char *url, HttpMethod method,
                                     HTTPClientData *client_data) {
    int rc;

    rc = _http_client_send_header(client, url, method, client_data);
    if (rc != 0) {
        LOGE("httpclient_send_header is error, rc = %d", rc);
        return rc;
    }

    if (method == HTTP_POST || method == HTTP_PUT) {
        rc = _http_client_send_userdata(client, client_data);
    }

    return rc;
}

static int
_http_client_recv_response(HTTPClient *client, uint32_t timeout_ms, HTTPClientData *client_data) {
    IOT_FUNC_ENTRY;

    int reclen = 0, rc = QCLOUD_ERR_HTTP_CONN;
    char buf[HTTP_CLIENT_CHUNK_SIZE] = {0};
    Timer timer;

    timer_init(&timer);
    countdown_ms(&timer, timeout_ms);

    if (0 == client->network_stack.handle) {
        LOGE("Connection has not been established");
        IOT_FUNC_EXIT_RC(rc);
    }

    if (client_data->is_more) {
        client_data->response_buf[0] = '\0';
        rc = _http_client_retrieve_content(client, buf, reclen, left_ms(&timer), client_data);
    } else {
        client_data->is_more = IOT_TRUE;
        rc = _http_client_recv(client, buf, 1, HTTP_CLIENT_CHUNK_SIZE - 1, &reclen, left_ms(&timer),
                               client_data);

        if (rc != QCLOUD_RET_SUCCESS) {
            IOT_FUNC_EXIT_RC(rc);
        }
        // else if(0 == left_ms(&timer)){
        //  IOT_FUNC_EXIT_RC(QCLOUD_ERR_HTTP_TIMEOUT);
        //}

        buf[reclen] = '\0';

        if (reclen) {
            // HAL_Printf("RESPONSE:\n%s", buf);
            rc = _http_client_response_parse(client, buf, reclen, left_ms(&timer), client_data);
        }
    }

    IOT_FUNC_EXIT_RC(rc);
}

static int
_http_network_init(Network *pNetwork, const char *host, int port) {
    int rc = QCLOUD_RET_SUCCESS;
    if (pNetwork == NULL) {
        return QCLOUD_ERR_INVAL;
    }
    pNetwork->type = NETWORK_TCP;
    pNetwork->host = host;
    pNetwork->port = port;

    rc = network_init(pNetwork);

    return rc;
}

int qcloud_http_client_connect(HTTPClient *client, const char *url, int port) {
    if (client->network_stack.handle != 0) {
        LOGE("http client has connected to host!");
        return QCLOUD_ERR_HTTP_CONN;
    }

    int rc;
    char host[HTTP_CLIENT_MAX_HOST_LEN] = {0};
    rc = _http_client_parse_host(url, host, sizeof(host));
    if (rc != QCLOUD_RET_SUCCESS)
        return rc;

    rc = _http_network_init(&client->network_stack, host, port);
    if (rc != QCLOUD_RET_SUCCESS)
        return rc;

    rc = _http_client_connect(client);
    if (rc != QCLOUD_RET_SUCCESS) {
        LOGE("http_client_connect is error,rc = %d", rc);

    } else {

        LOGD("http client connect success");
    }
    return rc;
}

void qcloud_http_client_close(HTTPClient *client) {
    if (client->network_stack.handle != 0) {
        client->network_stack.disconnect(&client->network_stack);
    }
}

int qcloud_http_client_common(HTTPClient *client, const char *url, int port,
                              HttpMethod method,
                              HTTPClientData *client_data) {
    int rc;

    if (client->network_stack.handle == 0) {
        rc = qcloud_http_client_connect(client, url, port);
        if (rc != QCLOUD_RET_SUCCESS)
            return rc;
    }

    rc = _http_client_send_request(client, url, method, client_data);
    if (rc != QCLOUD_RET_SUCCESS) {
        LOGE("http_client_send_request is error,rc = %d", rc);
        qcloud_http_client_close(client);
        return rc;
    }

    return QCLOUD_RET_SUCCESS;
}

int qcloud_http_recv_data(HTTPClient *client, uint32_t timeout_ms, HTTPClientData *client_data) {
    IOT_FUNC_ENTRY;

    int rc = QCLOUD_RET_SUCCESS;
    Timer timer;

    timer_init(&timer);
    countdown_ms(&timer, (unsigned int) timeout_ms);

    if ((NULL != client_data->response_buf) && (0 != client_data->response_buf_len)) {
        rc = _http_client_recv_response(client, left_ms(&timer), client_data);
        if (rc < 0) {
            LOGE("http_client_recv_response is error,rc = %d", rc);
            qcloud_http_client_close(client);
            IOT_FUNC_EXIT_RC(rc);
        }
    }
    IOT_FUNC_EXIT_RC(QCLOUD_RET_SUCCESS);
}
