#ifndef STR_H
#define STR_H

#include <string.h>

// #include "str.h"
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

#endif
