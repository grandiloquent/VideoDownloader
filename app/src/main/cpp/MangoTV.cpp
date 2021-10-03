#include <string>
#include "httplib/httplib.h"
#include "MangoTV.h"
#include "Shared.h"
#include "Logger.h"
#include "base64.h"
#include "rapidjson/document.h"

using namespace std;
using namespace rapidjson;

string EncodeTk(const string &s) {
    auto encoded = base64_encode(s, false);
    std::vector<char> v(encoded.begin(), encoded.end());
    for (char &i : v) {
        switch (i) {
            case 43:
                i = 95;
                break;
            case 47:
                i = 126;
                break;
            case 61:
                i = 45;
                break;
        }
    }
    std::string str(v.begin(), v.end());
    reverse(str.begin(), str.end());
    return str;
}

string CreateClit() {
    string clit = "clit=";
    clit += std::to_string(GetUnixTimestamp());
    return clit;
}

string CreatePm2QueryString(const string &clit, const string &videoId) {
    string pm2 = "did=f11dee65-4e0d-4d25-bfce-719ad9dc991d|pno=1030|ver=5.5.1|";
    pm2 += clit;
    auto tk = EncodeTk(pm2);

    stringstream ss;
    ss << "/player/video?video_id=" << videoId
       << "&tk2=" << EncodeTk(pm2);
    return ss.str();
}

string ExtractVideoId(const char *uri) {
    regex t(R"(mgtv\.com/[a-z]/\d+/(\d+)\.html)");
    smatch m;

    string text = uri;
    if (regex_search(text, m, t)) {
        return m[1].str();
    }
    return string();
}

string GetPm2(const string &clit, const string &videoId, int timeout) {

    auto hostName = "pcweb.api.mgtv.com";

    httplib::SSLClient client(hostName, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       hostName},
            {"Cookie",     "PM_CHKID=1"},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);
    auto res = client.Get(CreatePm2QueryString(clit, videoId).c_str(), headers);
    if (!res) {
        return string();
    }

    Document d;
    d.Parse(res->body.c_str());
    return d["data"]["atc"]["pm2"].GetString();
}

string GetSource(const string &clit, const string &videoId, const string &pm2, int timeout) {

    auto hostName = "pcweb.api.mgtv.com";

    httplib::SSLClient client(hostName, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       hostName},
            {"Cookie",     "PM_CHKID=1"},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);
    stringstream ss;
    ss << "/player/getSource?video_id=" << videoId
       << "&tk2=" << EncodeTk(clit)
       << "&pm2=" << pm2;
    auto res = client.Get(ss.str().c_str(), headers);
    if (!res) {
        return string();
    }

    Document d;
    d.Parse(res->body.c_str());

    auto streamDomain = d["data"]["stream_domain"].GetArray();
    auto stream = d["data"]["stream"].GetArray();

    string url;
    for (auto &it : stream) {
        string tmp = it["url"].GetString();
        if (tmp.empty())
            continue;
        url = tmp;
    }


    return streamDomain[0].GetString() + url;
}

string MangoTV::FetchVideo(const char *uri, int timeout) {

    auto clit = CreateClit();
    auto videoId = ExtractVideoId(uri);
    auto pm2 = GetPm2(clit, videoId, timeout);
    auto videoUri = GetSource(clit, videoId, pm2, timeout);

    auto uriParts = ParseUrl(videoUri.c_str());

    httplib::SSLClient client(uriParts.first, 443);
    client.set_connection_timeout(timeout);
    httplib::Headers headers = {
            {"Host",       uriParts.first},
            {"Cookie",     "PM_CHKID=1"},
            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
    };
    client.enable_server_certificate_verification(false);

    auto res = client.Get(uriParts.second.c_str(), headers);
    if (!res) {
        return string();
    }

    Document d;
    d.Parse(res->body.c_str());

    return d["info"].GetString();
}
