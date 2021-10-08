#ifndef HTTPUTILS_H
#define HTTPUTILS_H
// #include "HttpUtils.h"
#include <string>
#include "Configuration.h"
#include "httplib/httplib.h"

using namespace std;
namespace HttpUtils {
    string
    GetStrings(const char *host, const char *path,
               int timeout = 3,
               const string &userAgent = USER_AGENT_PC,
               const httplib::Headers &requestHeaders = {}
    );

    string
    GetStrings(const char *uri, int timeout = 3,
               const string &userAgent = USER_AGENT_PC,
               const httplib::Headers &requestHeaders = {}
    );

    string
    GetString(const char *host, const char *path,
              int timeout = 3,
              const string &userAgent = USER_AGENT_PC,
              const httplib::Headers &requestHeaders = {}
    );

    string
    GetString(const char *uri, int timeout = 3,
              const string &userAgent = USER_AGENT_PC,
              const httplib::Headers &requestHeaders = {}
    );
}
#endif

