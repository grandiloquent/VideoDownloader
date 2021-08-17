#ifndef DEF_H
#define DEF_H
// #include "def.h"

#define READ_URL() int urlBytesSize = (*env)->GetArrayLength(env, urlBytes); \
jbyte *url = (*env)->GetByteArrayElements(env, urlBytes, NULL);\
url[urlBytesSize] = 0;

#define PORN_HEADER() char headerBuffer[288]; \
srand((unsigned) time(0)); \
snprintf(headerBuffer, 288, \
"GET /view_video.php?viewkey=%s HTTP/1.1\r\n" \
"Host: 91porn.com\r\n"                        \
"User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1\r\n" \
"X-Forwarded-For: %d.%d.%d.%d\r\n" \
"\r\n", \
url, \
randomIP(1, 255), randomIP(1, 255), randomIP(1, 255), \
randomIP(1, 255));

#endif
