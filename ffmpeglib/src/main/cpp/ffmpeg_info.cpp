#include <jni.h>
#include <string>

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavfilter/avfilter.h"
#include "libavutil/avutil.h"
#include "android_log.h"
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_jeffmony_ffmpeglib_FFmpegInfoUtils_stringFromJNI(JNIEnv *env, jclass clazz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jeffmony_ffmpeglib_FFmpegInfoUtils_avcodecInfo(JNIEnv *env, jclass clazz) {
    char info[40000] = {0};

    av_register_all();

    AVCodec *temp = av_codec_next(NULL);

    while (temp != NULL) {
        if (temp->decode != NULL) {
            sprintf(info, "%sdecode:", info);
        } else {
            sprintf(info, "%sencode:", info);
        }
        switch (temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(info, "%s(video):", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(info, "%s(audio):", info);
                break;
            default:
                sprintf(info, "%s(other):", info);
                break;
        }
        sprintf(info, "%s[%10s]\n", info, temp->name);
        temp = temp->next;
    }

    return env->NewStringUTF(info);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jeffmony_ffmpeglib_FFmpegInfoUtils_avfilterInfo(JNIEnv *env, jclass clazz) {
    char info[40000] = {0};
    avfilter_register_all();

    AVFilter *temp = (AVFilter *) avfilter_next(NULL);
    while (temp != NULL) {
        sprintf(info, "%s%s\n", info, temp->name);
        temp = temp->next;
    }
    return env->NewStringUTF(info);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jeffmony_ffmpeglib_FFmpegInfoUtils_avformatInfo(JNIEnv *env, jclass clazz) {
    char info[40000] = {0};

    av_register_all();

    AVInputFormat *i_temp = av_iformat_next(NULL);
    AVOutputFormat *o_temp = av_oformat_next(NULL);
    while (i_temp != NULL) {
        sprintf(info, "%sInput: %s\n", info, i_temp->name);
        i_temp = i_temp->next;
    }
    while (o_temp != NULL) {
        sprintf(info, "%sOutput: %s\n", info, o_temp->name);
        o_temp = o_temp->next;
    }
    return env->NewStringUTF(info);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_jeffmony_ffmpeglib_FFmpegInfoUtils_protocolInfo(JNIEnv *env, jclass clazz) {
    char info[40000] = {0};
    av_register_all();

    struct URLProtocol *pup = NULL;

    struct URLProtocol **p_temp = &pup;
    avio_enum_protocols((void **) p_temp, 0);

    while ((*p_temp) != NULL) {
        sprintf(info, "%sInput: %s\n", info, avio_enum_protocols((void **) p_temp, 0));
    }
    pup = NULL;
    avio_enum_protocols((void **) p_temp, 1);
    while ((*p_temp) != NULL) {
        sprintf(info, "%sInput: %s\n", info, avio_enum_protocols((void **) p_temp, 1));
    }
    return env->NewStringUTF(info);
}