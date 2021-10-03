#include "XVideos.h"

#include <string>
#include <algorithm>
#include <regex>
#include "httplib/httplib.h"
#include "Shared.h"
#include "Logger.h"

using namespace std;

string FetchHls(const char *uri, int timeout) {
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
    return Substring(res->body, "html5player.setVideoHLS('", "');");

}

string XVideos::FetchVideo(const char *uri, int timeout) {
    auto hls = FetchHls(uri, timeout);
    if (hls.empty()) {
        return hls;
    }
    auto uriParts = ParseUrl(hls.c_str());

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
    auto lines = Split(res->body, "\n");

    vector<pair<int, string>> list;

    auto baseAddress = SubstringBeforeLast(hls, "/");
    for (auto line = begin(lines); line != end(lines); ++line) {
        if (line->find("#EXT-X-STREAM-INF") == string::npos) {
            continue;
        }
        auto size = stoi(Substring(*line, "NAME=\"", "p\""));
        line++;
        auto src = baseAddress + "/" + (*line);
        list.emplace_back(size, src);
    }

    sort(list.begin(), list.end(), [](auto &p1, auto &p2) {
        return p1.first > p2.first;
    });
    if (list.empty()) {
        return string();
    }
    return  list[0].second;

}
