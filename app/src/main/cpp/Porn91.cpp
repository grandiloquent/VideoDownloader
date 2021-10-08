#include "Porn91.h"

#include "httplib/httplib.h"
#include "Logger.h"
#include "Shared.h"
#include "HttpUtils.h"

using namespace std;

static string FetchCookie(int timeout = 3) {
    std::stringstream ss;

    HttpUtils::GetString("91porn.com", "/index.php", timeout, USER_AGENT_PC, {
            {"Accept",          "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
            {"Accept-Encoding", "gzip, deflate"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
    }, [&](const httplib::Headers &headers) {
        for (auto &h :headers) {
            if (h.first == "Set-Cookie") {
                ss << SubstringBeforeLast(h.second, "; ") << "; ";
            }
        }
    });

    return ss.str();
}


string porn91::FetchVideo(const char *uri, int timeout) {

    auto cookie = FetchCookie(timeout);
    if (cookie.empty())return cookie;

    auto res = HttpUtils::GetString("91porn.com", uri, timeout,
                                    USER_AGENT_PC, {
                                            {"Accept",          "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
                                            {"Accept-Encoding", "gzip, deflate"},
                                            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
                                            {"Cookie",          cookie},
                                            {"X-Forwarded-For", RandomIp()}
                                    });
    if (res.empty())return {};
    auto htm = Substring(res, "document.write(strencode2(\"", "\"));");
    auto decoded = UrlDecode(htm);
    return Substring(decoded, "src='", "'");
}











