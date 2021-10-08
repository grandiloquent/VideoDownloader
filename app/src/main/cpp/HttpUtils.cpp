#include "HttpUtils.h"
#include "httplib/httplib.h"
#include "Shared.h"
#include "Logger.h"
#include <string>

using namespace std;
namespace HttpUtils {
    string
    GetStrings(const char *host, const char *path,
               int timeout,
               const string &userAgent,
               const httplib::Headers &requestHeaders
    ) {
        httplib::SSLClient client(host, 443);
        client.set_connection_timeout(timeout);
        httplib::Headers headers = {
                {"Host",       host},
                {"User-Agent", userAgent}
        };
        if (!requestHeaders.empty()) {
            for (auto &it :requestHeaders) {
                headers.insert(it);
            }
        }
        client.enable_server_certificate_verification(false);
        auto res = client.Get(path, headers);
        if (!res) {
            return {};
        }
        return res->body;
    }

    string
    GetStrings(const char *uri, int timeout,
               const string &userAgent, const httplib::Headers &requestHeaders
    ) {
        auto uriParts = ParseUrl(uri);
        return GetStrings(uriParts.first.c_str(),
                          uriParts.second.c_str(), timeout,
                          userAgent, requestHeaders);
    }


    string
    GetString(const char *host, const char *path,
              int timeout,
              const string &userAgent,
              const httplib::Headers &requestHeaders,
              const function<void(const httplib::Headers &)> &f
    ) {
        httplib::Client client(host, 80);
        client.set_connection_timeout(timeout);
        httplib::Headers headers = {
                {"Host",       host},
                {"User-Agent", userAgent}
        };
        if (!requestHeaders.empty()) {
            for (auto &it :requestHeaders) {
                headers.insert(it);
            }
        }
        auto res = client.Get(path, headers);
        if (f) {
            f(res->headers);
        }
        if (!res) {
            return string();
        }
        return res->body;
    }

    string
    GetString(const char *uri, int timeout,
              const string &userAgent, const httplib::Headers &requestHeaders,
              const function<void(const httplib::Headers &)> &f
    ) {
        auto uriParts = ParseUrl(uri);
        return GetString(uriParts.first.c_str(),
                         uriParts.second.c_str(), timeout,
                         userAgent, requestHeaders, f);
    }
}
