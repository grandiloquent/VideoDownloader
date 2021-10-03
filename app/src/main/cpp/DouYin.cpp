
#include <string>
#include "httplib/httplib.h"
#include "DouYin.h"
#include "Shared.h"
#include "Logger.h"
#include "rapidjson/document.h"

using namespace std;
using namespace rapidjson;

string GetDouYinLocation(const char *uri, int timeout) {
    auto hostName = "v.douyin.com";
    httplib::SSLClient client(hostName, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       hostName},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);
    auto res = client.Get(uri, headers);
    if (!res) {
        return string();
    }
    auto location = res->headers.find("Location");
    regex t("video/(\\d+)");
    std::smatch match;
    if (std::regex_search(location->second, match, t)) {
        return match[1].str();
    }
    return string();
}

string DouYin::FetchVideo(const char *uri, int timeout) {
    auto id = GetDouYinLocation(uri, timeout);
    auto hostName = "www.iesdouyin.com";
    httplib::SSLClient client(hostName, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       hostName},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);
    string v("/web/api/v2/aweme/iteminfo/?item_ids=");
    auto res = client.Get((v + id).c_str(), headers);
    if (!res) {
        return string();
    }
    Document d;
    d.Parse(res->body.c_str());

    string url = d["item_list"][0]["video"]["play_addr"]["url_list"][0].GetString();
    Replace(url, "playwm", "play");
    return url;
}
