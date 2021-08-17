#include <jni.h>
#include <string>

extern "C" {
#include "libavutil/mem.h"
#include "android_log.h"
}

extern "C" int ffmpeg_execute(int argc, char **argv);

extern "C"
JNIEXPORT jint JNICALL
Java_com_jeffmony_ffmpeglib_FFmpegCmdUtils_ffmpegExecute(JNIEnv *env, jclass clazz,
                                                         jobjectArray cmds) {
    if (use_log_report) {
        av_log_set_callback(ffp_log_callback_report);
    } else {
        av_log_set_callback(ffp_log_callback_brief);
    }
    jstring *tempArray = NULL;
    int argc = 1;
    char **argv = NULL;
    if (cmds != NULL) {
        int programArgumentCount = (*env).GetArrayLength(cmds);
        argc = programArgumentCount;
        tempArray = (jstring *) av_malloc(sizeof(jstring) * programArgumentCount);
    }
    argv = (char **) av_malloc(sizeof(char *) * (argc));
    if (cmds != NULL) {
        for (int i = 0; i < argc; i++) {
            tempArray[i] = (jstring) (*env).GetObjectArrayElement(cmds, i);
            if (tempArray[i] != NULL) {
                argv[i] = (char *) (*env).GetStringUTFChars(tempArray[i], 0);
                LOGE("execute argv=%s", argv[i]);
            }
        }
    }

    int retCode = ffmpeg_execute(argc, argv);

    if (tempArray != NULL) {
        for (int i = 0; i < argc; i++) {
            (*env).ReleaseStringUTFChars(tempArray[i], argv[i]);
        }
        av_free(tempArray);
    }
    av_free(argv);
    return retCode;
}