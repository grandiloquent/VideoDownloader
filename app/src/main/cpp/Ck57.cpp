#include "Ck57.h"

#include "httplib/httplib.h"
#include "Logger.h"
#include "rapidjson/document.h"

using namespace std;
using namespace rapidjson;

static httplib::Headers BuildHeaders() {
    return {
            {"Accept",          "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
            {"Accept-Encoding", "gzip, deflate"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
            {"User-Agent",      "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36"}
    };
}

string GetBaseAddress(int timeout) {

    httplib::Client client("57ck.cc", 80);
    client.set_connection_timeout(timeout);
    auto headers = BuildHeaders();
    auto res = client.Get("/", headers);

    headers.insert(make_pair("Host", "57ck.cc"));

    if (res) {
        return Substring(res->body, "strU=\"", "\"");
    }
    return string();
}

string ck57::FetchVideo(const char *uri, int timeout) {
    auto uriParts = ParseUrl(uri);
    httplib::Client client(uriParts.first, 80);

    client.set_connection_timeout(timeout);
    auto headers = BuildHeaders();
    headers.insert(make_pair("Host", uriParts.first));

    auto res = client.Get(uriParts.second.c_str(), headers);
    if (res) {
        auto raw = Substring(res->body, "var player_", "</script>");
        if (raw.empty())return raw;
        string json = SubstringAfter(raw, "=");

        Document d;
        d.Parse(json.c_str());

        return d["url"].GetString();
    }
    return string();
}

