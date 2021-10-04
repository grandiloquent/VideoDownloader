#ifndef SHARED_H
#define SHARED_H

#include <random>
#include <string>
#include "httplib/httplib.h"

// #include "Shared.h"

using namespace std;


void DumpHeaders(httplib::Headers &headers);

std::string GetCookie(httplib::Headers &headers);

uint32_t GetTick();

int64_t GetUnixTimestamp();

pair<string, string> ParseUrl(const char *url);

int RandomInt(int min, int max);

string RandomIp();

std::string RandomString(size_t length);

bool Replace(std::string &str, const std::string &from, const std::string &to);

vector<string> Split(const string &s, const string &delimiter);

std::string Substring(const std::string &value,
                      const std::string &left, const std::string &right);

std::string SubstringAfter(const std::string &value,
                           const std::string &str);

std::string SubstringAfterLast(const std::string &value,
                               const std::string &str);

std::string SubstringBefore(const std::string &value,
                            const std::string &str);

std::string SubstringBeforeLast(const std::string &value,
                                const std::string &str);

std::string UrlDecode(const std::string &s);

string Md5Encoded(const string &s);

string GetRandomString(int size = 32);

#endif
