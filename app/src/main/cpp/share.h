#ifndef SHARE_H
#define  SHARE_H

#include <android/log.h>

#include <string.h>
#include <ctype.h>

#define LOG_TAG "TAG/Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

void urldecode(char *dst, const char *src) {
    char a, b;
    while (*src) {
        if ((*src == '%') &&
            ((a = src[1]) && (b = src[2])) &&
            (isxdigit(a) && isxdigit(b))) {
            if (a >= 'a')
                a -= 'a' - 'A';
            if (a >= 'A')
                a -= ('A' - 10);
            else
                a -= '0';
            if (b >= 'a')
                b -= 'a' - 'A';
            if (b >= 'A')
                b -= ('A' - 10);
            else
                b -= '0';
            *dst++ = 16 * a + b;
            src += 3;
        } else if (*src == '+') {
            *dst++ = ' ';
            src++;
        } else {
            *dst++ = *src++;
        }
    }
    *dst++ = '\0';
}

int substring(const char *s, const char *first, const char *second,
              char *buffer) {
    const char *save;
    size_t lFirst = strlen(first);
    size_t lSecond = strlen(second);
    int nomatch = 0;
    int found = 0;
    while (*s) {
        if (!found && *s == *first) {
            nomatch = 0;
            for (int i = 1; i < lFirst; i++) {
                if (*(s + i) == 0) return -1;
                if (*(s + i) != *(first + i)) {
                    s++;
                    nomatch = 1;
                    break;
                }
            }
            if (nomatch) continue;
            s += lFirst;
            save = s;
            found = 1;

            continue;
        }

        if (found && *s == *second) {

            nomatch = 0;

            for (int i = 1; i < lSecond; i++) {
                if (*(s + i) == 0) return -1;
                if (*(s + i) != *(second + i)) {
                    s++;
                    nomatch = 1;
                    break;
                }
            }
            if (nomatch) continue;
            size_t sz = s - save;
            strncat(buffer, save, sz);
            buffer[sz] = 0;
            return sz;
        }
        s++;
    }
    return -1;
}

static inline int randomIP(int begin, int end) {
    int gap = end - begin + 1;
    int ret = 0;
    //srand((unsigned) time(0));
    ret = rand() % gap + begin;
//in++;
    return ret;
}

#endif