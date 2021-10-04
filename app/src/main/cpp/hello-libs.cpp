#include <jni.h>
#include <string>
#include "Logger.h"
#include "Porn91.h"
#include "Ck57.h"
#include "KuaiShou.h"
#include "XVideos.h"
#include "DouYin.h"
#include "MangoTV.h"
#include "Iqiyi.h"

using namespace std;

extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetch57Ck(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = ck57::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetch91Porn(JNIEnv *env, jclass klass, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = porn91::FetchVideo(
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
Java_euphoria_psycho_explorer_Native_fetchIqiyi(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = Iqiyi::FetchVideo(
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
Java_euphoria_psycho_explorer_Native_fetchMangoTV(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = MangoTV::FetchVideo(
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