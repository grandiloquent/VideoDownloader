#include "Shared.h"
#include "Logger.h"
#include <chrono>

using namespace std::chrono;

void DumpHeaders(httplib::Headers &headers) {
    for (auto &header:headers) {
        LOGE("%s: %s", header.first.c_str(), header.second.c_str());
    }
}

std::string GetCookie(httplib::Headers &headers) {
    std::stringstream ss;
    for (auto &header :headers) {
        if (header.first == "Set-Cookie") {
            ss << SubstringBefore(header.second, "; ") << "; ";
        }
    }
    return ss.str();
}

uint32_t GetTick() {
    struct timespec ts;
    unsigned theTick = 0U;
    clock_gettime(CLOCK_REALTIME, &ts);
    theTick = ts.tv_nsec / 1000000;
    theTick += ts.tv_sec * 1000;
    return theTick;
}

pair<string, string> ParseUrl(const char *url) {
    auto hostname = Substring(url, "//", "/");
    auto path = SubstringAfter(url, hostname);
    return make_pair(hostname, path);
}

int RandomInt(int min, int max) {
    static std::mt19937 engine{};
    std::uniform_int_distribution<int> distribution(min, max);
    return distribution(engine);
}

string RandomIp() {
    stringstream ss;
    ss << std::to_string(RandomInt(0, 255))
       << "."
       << std::to_string(RandomInt(0, 255))
       << "."
       << std::to_string(RandomInt(0, 255))
       << "."
       << std::to_string(RandomInt(0, 255));
    return ss.str();
}

vector<string> Split(const string &s, const string &delimiter) {
    size_t pos_start = 0, pos_end, delim_len = delimiter.length();
    string token;
    vector<string> res;

    while ((pos_end = s.find(delimiter, pos_start)) != string::npos) {
        token = s.substr(pos_start, pos_end - pos_start);
        pos_start = pos_end + delim_len;
        res.push_back(token);
    }

    res.push_back(s.substr(pos_start));
    return res;
}


std::string Substring(const std::string &value,
                      const std::string &left, const std::string &right) {
    auto start = value.find(left);
    if (start != std::string::npos) {
        start += left.length();
        auto end = value.find(right, start);
        if (end != std::string::npos) {
            return value.substr(start, end - start);
        }
    }
    return std::string();
}

std::string SubstringAfter(const std::string &value,
                           const std::string &str) {
    auto index = value.find(str);
    if (index != std::string::npos)
        return value.substr(index + str.length());
    else
        return std::string();
}

std::string SubstringAfterLast(const std::string &value,
                               const std::string &str) {
    auto index = value.find_last_of(str);
    if (index != std::string::npos)
        return value.substr(index + str.length());
    else
        return std::string();
}

std::string SubstringBefore(const std::string &value,
                            const std::string &str) {
    auto index = value.find(str);
    if (index != std::string::npos)
        return value.substr(0, index);
    else
        return std::string();
}

std::string SubstringBeforeLast(const std::string &value,
                                const std::string &str) {
    auto index = value.find_last_of(str);
    if (index != std::string::npos)
        return value.substr(0, index);
    else
        return std::string();
}

std::string UrlDecode(const std::string &s) {
    int len = s.size();
    std::string res;
    int i;
    for (i = 0; i < len; ++i) {
        unsigned char c = s[i];
        if (c != '%') {
            res += c;
        } else {
            i += 2;
            if (i >= len)
                break;
            char t[3] = {s[i - 1], s[i], 0};
            unsigned char r = strtoul(t, 0, 0x10);
            if (r)
                res += r;
        }
    }
    return res;
}

bool Replace(std::string &str, const std::string &from, const std::string &to) {
    size_t start_pos = str.find(from);
    if (start_pos == std::string::npos)
        return false;
    str.replace(start_pos, from.length(), to);
    return true;
}

int64_t GetUnixTimestamp() {
    int64_t timestamp = duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
    return timestamp;
}