#ifndef DOUYIN_H
#define DOUYIN_H
// #include "DouYin.h"
#include <string>

using namespace std;
namespace DouYin {
    string FetchVideo(const char *uri, int timeout = 3);
}
#endif
