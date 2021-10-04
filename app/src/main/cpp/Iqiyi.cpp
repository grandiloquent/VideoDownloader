#include <string>
#include <future>
#include "httplib/httplib.h"
#include "Iqiyi.h"
#include "Shared.h"
#include "Logger.h"
#include "rapidjson/document.h"

using namespace std;
using namespace rapidjson;

static string GetParametersString(const char *uri, int timeout) {
    auto uriParts = ParseUrl(uri);
    httplib::SSLClient client(uriParts.first, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       uriParts.first},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);
    auto res = client.Get(uriParts.second.c_str(), headers);
    if (!res) {
        return string();
    }

    auto tvid = Substring(res->body, "\"tvId\":", ",");
    if (tvid.empty()) {
        return tvid;
    }
    auto vid = Substring(res->body, R"("vid":")", "\"");
    if (vid.empty()) {
        return vid;
    }

    stringstream ss;
    ss << "/vps?tvid=" << tvid
       << "&vid=" << vid
       << "&v=0&qypid=" << tvid
       << "_12&src=01012001010000000000&t=" << GetUnixTimestamp()
       << "&k_tag=1&k_uid=" << GetRandomString()
       << "&rs=1";
    return ss.str();
}


string Iqiyi::FetchVideo(const char *uri, int timeout) {
    auto params = GetParametersString(uri, timeout);
    auto hash = Md5Encoded(params + "1j2k2k3l3l4m4m5n5n6o6o7p7p8q8q9r");
    auto hostName = "cache.video.qiyi.com";
    httplib::SSLClient client(hostName, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       hostName},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);
    auto res = client.Get((params + "&vf=" + hash).c_str(), headers);
    if (!res) {
        return string();
    }

    WriteFile("1.json", res->body);

    Document d;
    d.Parse(res->body.c_str());

    auto videos = d["data"]["vp"]["tkl"][0]["vs"].GetArray();

    sort(videos.begin(), videos.end(),
         [](auto &lhs,
            auto &rhs) {
             return atoi(lhs["scrsz"].GetString()) > atoi(rhs["scrsz"].GetString());
         });




//    auto uriParts = ParseUrl(uri);
//
//    httplib::SSLClient client(uriParts.first, 443);
//    client.set_connection_timeout(timeout);
//    httplib::Headers headers = {
//            {"Host",       uriParts.first},
//            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
//    };
//    client.enable_server_certificate_verification(false);
//    auto res = client.Get(uriParts.second.c_str(), headers);
//    if (!res) {
//        return string();
//    }
    return string();
}

//string GetLocation(const char *uri, int timeout) {
//    auto hostName = "";
//    httplib::SSLClient client(hostName, 443);
//    client.set_connection_timeout(timeout);
//    httplib::Headers headers = {
//            {"Host",       hostName},
//            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
//    };
//    client.enable_server_certificate_verification(false);
//    auto res = client.Get(uri, headers);
//    if (!res) {
//        return string();
//    }
//    return res->headers.find("Location")->first;
//}