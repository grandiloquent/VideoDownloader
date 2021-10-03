#include <string>
#include "KuaiShou.h"
#include "httplib/httplib.h"
#include "Shared.h"
#include "Logger.h"

std::pair<std::string, std::string> GetLocation(const char *uri, int timeout) {
    auto hostname = Substring(uri, "//", "/");
    if (hostname.empty()) {
        return make_pair(string(), string());
    }
    httplib::SSLClient client(hostname, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       hostname},
            {"User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1"}
    };
    client.enable_server_certificate_verification(false);
    auto path = SubstringAfter(uri, hostname);
    auto res = client.Get(path.c_str(), headers);
    return make_pair(res->headers.find("Location")->second, GetCookie(res->headers));;
}

std::string KuaiShou::FetchVideo(const char *uri, int timeout) {
    auto location = GetLocation(uri, timeout);
    if (location.first.empty() || location.second.empty()) {
        return string();
    }
    auto hostname = Substring(location.first, "//", "/");
    if (hostname.empty()) {
        return hostname;
    }
    httplib::SSLClient client(hostname, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers
            headers = {
            {"Host", hostname},
            {"User-Agent",
             "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1"},
            {"Cookie", location.second},
    };

    client.enable_server_certificate_verification(false);
    auto path = SubstringAfter(location.first, hostname);
    auto res = client.Get(path.c_str(), headers);
    return Substring(res->body, R"("srcNoMark":")", "\"");
}
