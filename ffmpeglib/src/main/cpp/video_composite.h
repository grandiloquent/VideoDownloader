//
// Created by jeffli on 2021/7/27.
//

#ifndef JEFFFFMPEGDEMO_VIDEO_COMPOSITE_H
#define JEFFFFMPEGDEMO_VIDEO_COMPOSITE_H

#include <jni.h>
#include <pthread.h>

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
};

class VideoComposite {
public:
    VideoComposite();
    ~VideoComposite();

    int StartComposite(const char *output_video_path,
                       char** video_paths,
                       int size,
                       jobject composite_listener);

private:

    static void* CompositeWorkThreadCallback(void* context);
    void CompositeAction();
    int OpenInputVideo(int index);
    void OnError(int err_code);

    AVFormatContext *output_context_;
    AVFormatContext *input_context_;

    char** input_paths_;
    int input_size_;
    jobject composite_listener_;
    pthread_t composite_thread_;
    int current_video_index_;
    int64_t current_video_time_;
    int64_t duration_;

    int64_t last_video_pts_;
    int64_t last_video_dts_;
    int64_t last_audio_pts_;
    int64_t last_audio_dts_;

    int64_t current_video_pts_;
    int64_t current_video_dts_;
    int64_t current_audio_pts_;
    int64_t current_audio_dts_;

    AVRational video_time_base_;
    AVRational audio_time_base_;

};


#endif //JEFFFFMPEGDEMO_VIDEO_COMPOSITE_H
