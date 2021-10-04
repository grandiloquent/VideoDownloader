#ifndef IQIYI_H
#define IQIYI_H
// #include "Iqiyi.h"
#include <string>

using namespace std;
namespace Iqiyi {
    vector<std::string>  FetchVideo(const char *uri, int timeout = 3);
}
#endif
