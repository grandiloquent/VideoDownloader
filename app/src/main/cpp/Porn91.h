#ifndef PORN91_H
#define PORN91_H

#include <string>
#include "Shared.h"

namespace porn91 {
    std::string FetchVideo(const char *uri, int timeout = 3);
}

#endif
