#include "Porn91.h"

#include "httplib/httplib.h"
#include "Logger.h"
#include "Shared.h"

using namespace std;

static httplib::Headers BuildHeaders() {
    return {
            {"Accept",          "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
            {"Accept-Encoding", "gzip, deflate"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
            {"Host",            "91porn.com"},
            {"User-Agent",      "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36"}
    };
}

static string FetchCookie(int timeout = 3) {
    httplib::Client client("91porn.com", 80);
    client.set_connection_timeout(timeout);
    auto headers = BuildHeaders();
    auto res = client.Get("/index.php", headers);
    if (res) {
        std::stringstream ss;
        for (auto &header : res->headers) {
            if (header.first == "Set-Cookie") {
                ss << SubstringBeforeLast(header.second, "; ") << "; ";
            }
        }
        return ss.str();
    }
    return string();
}


string porn91::FetchVideo(const char *uri, int timeout) {

    auto cookie = FetchCookie(timeout);
    if (cookie.empty())return cookie;

    httplib::Client client("91porn.com", 80);
    client.set_connection_timeout(timeout);
    auto headers = BuildHeaders();
    headers.insert(std::make_pair("Cookie", cookie));
    headers.insert(std::make_pair("X-Forwarded-For", RandomIp()));
    auto res = client.Get(uri, headers);

    if (res) {

        auto htm = Substring(res->body, "document.write(strencode2(\"", "\"));");
        auto decoded = UrlDecode(htm);
        return Substring(decoded, "src='", "'");
    }
    return string();
}











