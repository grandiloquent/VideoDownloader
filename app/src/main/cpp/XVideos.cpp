#include "XVideos.h"

#include <string>
#include <algorithm>
#include <regex>
#include "httplib/httplib.h"
#include "Shared.h"
#include "Logger.h"
#include "HttpUtils.h"

using namespace std;

string FetchHls(const char *uri, int timeout) {
    auto res = HttpUtils::GetStrings(uri, timeout);
    if (res.empty())return {};
    return Substring(res, "html5player.setVideoHLS('", "');");
}

string XVideos::FetchVideo(const char *uri, int timeout) {
    auto hls = FetchHls(uri, timeout);
    if (hls.empty()) {
        return hls;
    }
    auto res = HttpUtils::GetStrings(hls.c_str(), timeout);
    if (res.empty())return {};
    auto lines = Split(res, "\n");

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
    return list[0].second;

}
