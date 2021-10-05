#include <string>
#include "httplib/httplib.h"
#include "AcFun.h"
#include "Shared.h"
#include "Logger.h"
#include "rapidjson/document.h"

using namespace std;
using namespace rapidjson;

string AcFun::FetchVideo(const char *uri, int timeout) {

    auto uriParts = ParseUrl(uri);

    httplib::SSLClient client(uriParts.first, 443);
    client.set_connection_timeout(timeout);
    client.enable_server_certificate_verification(false);

    httplib::Headers headers = {
            {"Host",       uriParts.first},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    auto res = client.Get(uriParts.second.c_str(), headers);

    if (!res) {
        LOGE("%s, %s", "!res", "");
        return string();
    }

    auto json = Substring(res->body, "window.pageInfo = window.videoInfo = ", ";");
    if (json.empty()) {
        return json;
    }

    Document d;

    d.Parse(json.c_str());
    d.Parse(d["currentVideoInfo"]["ksPlayJson"].GetString());

    return d["adaptationSet"][0]["representation"][0]["url"].GetString();

}
