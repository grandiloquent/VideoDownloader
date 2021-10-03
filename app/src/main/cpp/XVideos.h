#ifndef XVIDEOS_H
#define XVIDEOS_H
// #include "XVideos.h"
#include <string>

using namespace std;
namespace XVideos {
    string FetchVideo(const char *uri, int timeout = 3);
}
#endif
