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
#include "AcFun.h"
#include "PornOne.h"

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
JNIEXPORT jobjectArray JNICALL
Java_euphoria_psycho_explorer_Native_fetchIqiyi(JNIEnv *env, jclass clazz, jstring url) {
    jobjectArray ret{};
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = Iqiyi::FetchVideo(
            uri);
    if (result.empty())return ret;
    ret = (jobjectArray) env->NewObjectArray(result.size(), env->FindClass("java/lang/String"),
                                             env->NewStringUTF(""));
    for (int i = 0; i < result.size(); i++)
        env->SetObjectArrayElement(ret, i, env->NewStringUTF(
                result[i].c_str()));

    env->ReleaseStringUTFChars(url, uri);
    return ret;
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
    try {
        const char *uri = env->GetStringUTFChars(url, nullptr);
        auto result = XVideos::FetchVideo(
                uri);
        env->ReleaseStringUTFChars(url, uri);
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception &ex) {
        return nullptr;
    }
}
extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetchAcFun(JNIEnv *env, jclass clazz, jstring url) {
    try {
        const char *uri = env->GetStringUTFChars(url, nullptr);
        auto result = AcFun::FetchVideo(
                uri);
        env->ReleaseStringUTFChars(url, uri);
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception &ex) {
        return nullptr;
    }
}
extern "C"
JNIEXPORT jstring JNICALL
Java_euphoria_psycho_explorer_Native_fetchPornOne(JNIEnv *env, jclass clazz, jstring url) {
    const char *uri = env->GetStringUTFChars(url, nullptr);
    auto result = PornOne::FetchVideo(
            uri);
    env->ReleaseStringUTFChars(url, uri);
    return env->NewStringUTF(result.c_str());
}