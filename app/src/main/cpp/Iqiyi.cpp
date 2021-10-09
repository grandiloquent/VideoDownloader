#include <string>
#include <future>
#include "httplib/httplib.h"
#include "Iqiyi.h"
#include "Shared.h"
#include "Logger.h"
#include "rapidjson/document.h"
#include "Configuration.h"
#include "HttpUtils.h"

using namespace std;
using namespace rapidjson;

static string GetParametersString(const char *uri, int timeout) {
    auto res = HttpUtils::GetStrings(uri, timeout, USER_AGENT_ANDROID);
    auto tvid = Substring(res, "\"tvid\":", ",");
    if (tvid.empty()) {
        return tvid;
    }
    auto vid = Substring(res, R"("vid":")", "\"");
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
    auto host = "cache.video.qiyi.com";
    auto res = HttpUtils::GetStrings(host, (params + "&vf=" + hash).c_str(),
                                     timeout, USER_AGENT_ANDROID);
    return res;
}

static string GetVideoUri(const char *uri, int timeout) {
    auto res = HttpUtils::GetStrings(uri, timeout, USER_AGENT_ANDROID);
    PARSE_JSON(res.c_str());

    if (d.HasMember("l"))
        return d["l"].GetString();
    return {};
}

template<typename T>
bool isReady(const std::future<T> &f) {
    if (f.valid()) { // otherwise you might get an exception (std::future_error: No associated state)
        return f.wait_for(std::chrono::seconds(0)) == std::future_status::ready;
    } else {
        return false;
    }
}

vector<string> parseVideoClips(const string &source) {
    PARSE_JSON(source.c_str());

    if (!d.HasMember("data")) {
        return {};
    }
    Value &data = d["data"];
    if (!data.HasMember("vp")) {
        return {};
    }
    Value &vp = data["vp"];
    if (!vp.HasMember("tkl")) {
        return {};
    }
    Value &tkl = vp["tkl"];
    if (!tkl[0].HasMember("vs")) {
        return {};
    }
    auto videos = tkl[0]["vs"].GetArray();
    if (!videos[0].HasMember("fs")) {
        return {};
    }
    if (!vp.HasMember("du")) {
        return {};
    }
    auto du = vp["du"].GetString();
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
    return v;
}

vector<std::string> Iqiyi::FetchVideo(const char *uri, int timeout) {
    std::vector<std::string> readyFutures;

    auto params = GetParametersString(uri, timeout);
    if (params.empty()) {
        return readyFutures;
    }
    auto hash = Md5Encoded(params + "1j2k2k3l3l4m4m5n5n6o6o7p7p8q8q9r");
    auto source = GetSource(params, hash, timeout);
    if (source.empty()) {
        return {};
    }

    vector<string> v = parseVideoClips(source);

    if (v.empty()) {
        return {};
    }

//    std::vector<std::future<std::string>> futures;
//    futures.reserve(v.size());
//    for (auto &i : v) {
//        futures.push_back(
//                std::async(std::launch::async,
//                           [&](const std::string &uri) {
//                               return GetVideoUri(uri.c_str(), timeout);
//                           }, i)
//        );
//    }
//    do {
//        for (auto &future : futures) {
//            if (isReady(future)) {
//                readyFutures.push_back(future.get());
//            }
//        }
//    } while (readyFutures.size() < futures.size());
//
//    return readyFutures;
return v;
}
