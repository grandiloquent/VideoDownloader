#ifndef CK57_H
#define CK57_H
// #include "Ck57.h"

#include <string>
#include "Shared.h"

namespace ck57 {
    std::string FetchVideo(const char *uri, int timeout = 3);
}
#endif
