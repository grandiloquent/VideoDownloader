#ifndef MANGOTV_H
#define MANGOTV_H
// #include "MangoTV.h"
#include <string>

using namespace std;
namespace MangoTV {
    string FetchVideo(const char *uri, int timeout = 3);
}
#endif
