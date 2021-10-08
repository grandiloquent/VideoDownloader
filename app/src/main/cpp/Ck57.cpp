#include "Ck57.h"

#include "httplib/httplib.h"
#include "Logger.h"
#include "rapidjson/document.h"
#include "HttpUtils.h"
#include "Configuration.h"

using namespace std;
using namespace rapidjson;

static httplib::Headers BuildHeaders() {
    return {
            {"Accept",          "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
            {"Accept-Encoding", "gzip, deflate"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
    };
}

string ck57::FetchVideo(const char *uri, int timeout) {
    auto res = HttpUtils::GetString(uri, timeout, USER_AGENT_PC, BuildHeaders());
    if (!res.empty()) {
        auto raw = Substring(res, "var player_", "</script>");
        if (raw.empty())return raw;
        string json = SubstringAfter(raw, "=");

        Document d;
        d.Parse(json.c_str());
        if (d.HasParseError()) {
            return {};
        }
        return d["url"].GetString();
    }
    return string();
}

