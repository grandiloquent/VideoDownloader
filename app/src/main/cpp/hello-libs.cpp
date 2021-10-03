#include <jni.h>
#include <string>
#include "Logger.h"
#include "Porn91.h"
#include "Ck57.h"
#include "KuaiShou.h"
#include "XVideos.h"
#include "DouYin.h"
#include "MangoTV.h"

using namespace std;




extern "C" JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetch91Porn(JNIEnv *env, jclass klass, jstring url) {

    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = porn91::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}

string parseXVideosVideo(string uri) {


//     httplib::SSLClient client("www.xvideos.com", 443);
//    client.set_connection_timeout(3);
//    httplib::Headers headers = {
//            {"Accept",                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
//    {"Accept-Encoding",           "br"},
//    {"Accept-Language",           "zh-CN,zh;q=0.9,en;q=0.8"},
//    {"Cache-Control",             "no-cache"},
//    {"Connection",                "keep-alive"},
//    {"Host",                      "www.xvideos.com"},
//    {"Pragma",                    "no-cache"},
//    {"sec-ch-ua",                 R"("Chromium";v="94", "Google Chrome";v="94", ";Not A Brand";v="99")"},
//    {"sec-ch-ua-mobile",          "?0"},
//    {"sec-ch-ua-platform",        "\"Windows\""},
//    {"Sec-Fetch-Dest",            "document"},
//    {"Sec-Fetch-Mode",            "navigate"},
//    {"Sec-Fetch-Site",            "none"},
//    {"Sec-Fetch-User",            "?1"},
//    {"Upgrade-Insecure-Requests", "1"},
//    {"User-Agent",                "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36"}
//};
//client.enable_server_certificate_verification(false);
//
//auto res = client.Get("/", headers);
//
//if (res == nullptr) {
//LOGE("%s", "Failed");
//} else
//LOGE("%d %s %s", res->status, res->get_header_value("Set-Cookie").c_str(),
//     res->body.c_str());


}

extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetch57Ck(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = ck57::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetchKuaiShou(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = KuaiShou::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetchXVideos(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = XVideos::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetchDouYin(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = DouYin::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetchMangoTV(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = MangoTV::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}