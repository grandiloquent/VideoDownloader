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

static string GetSource(const string &params, const string &hash, int timeout) {
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
    return res->body;
}

static string GetVideoUri(const char *uri, int timeout) {
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
    Document d;
    d.Parse(res->body.c_str());
    return d["l"].GetString();
}

template<typename T>
bool isReady(const std::future<T> &f) {
    if (f.valid()) { // otherwise you might get an exception (std::future_error: No associated state)
        return f.wait_for(std::chrono::seconds(0)) == std::future_status::ready;
    } else {
        return false;
    }
}


vector<std::string> Iqiyi::FetchVideo(const char *uri, int timeout) {
    std::vector<std::string> readyFutures;

    auto params = GetParametersString(uri, timeout);
    auto hash = Md5Encoded(params + "1j2k2k3l3l4m4m5n5n6o6o7p7p8q8q9r");
    auto source = GetSource(params, hash, timeout);
    if (source.empty()) {
        return readyFutures;
    }

    Document d;
    d.Parse(source.c_str());

    auto videos = d["data"]["vp"]["tkl"][0]["vs"].GetArray();
    auto du = d["data"]["vp"]["du"].GetString();
    sort(videos.begin(), videos.end(),
         [](auto &lhs,
            auto &rhs) {
             return atoi(lhs["scrsz"].GetString()) > atoi(rhs["scrsz"].GetString());
         });

    auto fs = videos[0]["fs"].GetArray();
    vector<string> v{};
    for (auto &f : fs) {
        auto l = string(du) + f["l"].GetString();
        v.emplace_back(l);
    }

    std::vector<std::future<std::string>> futures;

    futures.reserve(v.size());
    for (auto &i : v) {
        futures.push_back(
                std::async(std::launch::async,
                           [&](const std::string &uri) {
                               return GetVideoUri(uri.c_str(), timeout);
                           }, i)
        );
    }
    do {
        for (auto &future : futures) {
            if (isReady(future)) {
                readyFutures.push_back(future.get());
            }
        }
    } while (readyFutures.size() < futures.size());

    return readyFutures;
}
