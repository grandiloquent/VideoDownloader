//
// Created by jeffli on 2021/7/27.
//

#include "video_composite.h"

#include "video_jni_env.h"
#include "process_error.h"

VideoComposite::VideoComposite() :
output_context_(nullptr),
input_context_(nullptr),
input_paths_(nullptr),
input_size_(0),
composite_listener_(nullptr),
composite_thread_(0),
current_video_index_(0),
current_video_time_(0),
duration_(0),
last_video_pts_(0),
last_video_dts_(0),
last_audio_pts_(0),
last_audio_dts_(0),
current_video_pts_(0),
current_video_dts_(0),
current_audio_pts_(0),
current_audio_dts_(0),
video_time_base_(),
audio_time_base_() {
    //构造函数初始化操作
}

VideoComposite::~VideoComposite() {

}

int VideoComposite::StartComposite(const char *output_video_path, char **video_paths, int size,
                                   jobject composite_listener) {

    input_paths_ = video_paths;
    input_size_ = size;

    if (composite_listener != nullptr) {
        JNIEnv *env = nullptr;
        int ret = jni_get_env(&env);
        if (env == nullptr) {
            return ERR_COMPOSITE_NO_ENV;
        }
        composite_listener_ = env->NewGlobalRef(composite_listener);

        if (ret == JNI_EDETACHED) {
            jni_detach_thread_env();
        }
    }

    int ret = avformat_alloc_output_context2(&output_context_, nullptr, "mp4", output_video_path);
    if (ret < 0) {
        JLOGE("%s alloc context2 error : %s", __func__ , av_err2str(ret));
        return ret;
    }

    if (output_context_ == nullptr) {
        JLOGE("%s output context is nullptr", __func__ );
        return ERR_COMPOSITE_NO_OUTPUT_CONTEXT;
    }

    ret = avformat_open_input(&input_context_, video_paths[0], nullptr, nullptr);
    if (ret != 0) {
        JLOGE("%s open input: %s error : %s", __func__ , video_paths[0], av_err2str(ret));
        OnError(ret);
        return ret;
    }
    if (input_context_ == nullptr) {
        JLOGE("%s input context is nullptr", __func__ );
        OnError(ERR_COMPOSITE_NO_INPUT_CONTEXT);
        return ERR_COMPOSITE_NO_INPUT_CONTEXT;
    }
    ret = avformat_find_stream_info(input_context_, nullptr);
    if (ret < 0) {
        JLOGE("%s find stream info error : %s", __func__ , av_err2str(ret));
        OnError(ret);
        return ret;
    }

    for (int i = 0; i < input_context_->nb_streams; i++) {
        auto input_stream = input_context_->streams[i];
        auto output_stream = avformat_new_stream(output_context_, nullptr);
        if (output_stream == nullptr) {
            JLOGE("%s new stream error", __func__ );
            OnError(ERR_COMPOSITE_NO_OUTPUT_STREAM);
            return ERR_COMPOSITE_NO_OUTPUT_STREAM;
        }
        ret = avcodec_parameters_copy(output_stream->codecpar, input_stream->codecpar);
        if (ret < 0 ) {
            JLOGE("%s avcodec_parameters_copy error: %s", __func__ , av_err2str(ret));
            OnError(ret);
            return ret;
        }
        output_stream->time_base = input_stream->time_base;
        output_stream->codecpar->codec_tag = 0;
        if (output_context_->oformat->flags & AVFMT_GLOBALHEADER) {
            output_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
        }

        if (input_stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            AVDictionaryEntry *rotateEntry = av_dict_get(input_stream->metadata, "rotate", nullptr, 0);
            if (rotateEntry != nullptr) {
                int rotate = atoi(rotateEntry->value);
                if (rotate != 0) {
                    char rotate_str[1024];
                    sprintf(rotate_str, "%d", rotate);
                    av_dict_set(&output_stream->metadata, "rotate", rotate_str, 0);
                }
            }
        }
    }

    if (!(output_context_->flags & AVFMT_NOFILE)) {
        ret = avio_open(&output_context_->pb, output_video_path, AVIO_FLAG_WRITE);
        if (ret < 0) {
            JLOGE("%s avio_open error : %s", __func__ , av_err2str(ret));
            OnError(ret);
            return ret;
        }
    }

    //写入生成文件的头部信息
    ret = avformat_write_header(output_context_, nullptr);
    if (ret < 0) {
        JLOGE("%s write header error: %s", __func__, av_err2str(ret));
        OnError(ret);
        return ret;
    }

    //起一个线程开始执行合成操作
    pthread_create(&composite_thread_, nullptr, CompositeWorkThreadCallback, this);

    return 0;
}

void *VideoComposite::CompositeWorkThreadCallback(void *context) {
    auto composite = reinterpret_cast<VideoComposite*>(context);
    composite->CompositeAction();
    delete composite;
    pthread_exit(nullptr);
}

void VideoComposite::CompositeAction() {
    while (current_video_index_ < input_size_) {
        AVPacket packet;
        int result = av_read_frame(input_context_, &packet);
        if (result < 0) {
            av_packet_unref(&packet);

            //说明当前的文件已经读到结尾了
            //下面如果还要读取新的文件，就要更新pts和dts了
            if (result == AVERROR_EOF) {
                current_video_index_++;
                if (current_video_index_ >= input_size_) {
                    //视频序列已经遍历结束了
                    break;
                }
                int ret;
                do {
                    ret = OpenInputVideo(current_video_index_);
                    if (ret != 0) {
                        current_video_index_++;
                    }
                } while (ret != 0);
                continue;
            }
            continue;
        }

        AVStream* inputStream = input_context_->streams[packet.stream_index];
        if (inputStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            current_video_time_ = (packet.pts * av_q2d(inputStream->time_base)) * 1000;
            video_time_base_ = inputStream->time_base;
        } else if (inputStream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_time_base_ = inputStream->time_base;
        }

        AVStream* outputStream = output_context_->streams[packet.stream_index];
        if (inputStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            // 多段合成时,当前帧的pts+上一个视频的最后一个pts
            current_video_pts_ = av_rescale_q_rnd(packet.pts, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)) +
                                 av_rescale_q_rnd(last_video_pts_, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            current_video_dts_ = av_rescale_q_rnd(packet.dts, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)) +
                                 av_rescale_q_rnd(last_video_pts_, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));

            // 如果当前帧的pts和上一个视频的pts相同,当前帧的pts+packet.duration转换一下,不然写入文件时会报错
            if (current_video_pts_ == last_video_pts_ && current_video_pts_ > 0) {
                current_video_pts_ += av_rescale_q(packet.duration, inputStream->time_base, outputStream->time_base);
                last_video_pts_ = current_video_pts_;
            }
            if (current_video_dts_ == last_video_dts_ && current_video_dts_ > 0) {
                current_video_dts_ += av_rescale_q(packet.duration, inputStream->time_base, outputStream->time_base);
                last_video_dts_ = current_video_dts_;
            }

            packet.pts = current_video_pts_;
            packet.dts = current_video_dts_;
        } else if (inputStream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            current_audio_pts_ = av_rescale_q_rnd(packet.pts, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)) +
                                 av_rescale_q_rnd(last_audio_pts_, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            current_audio_dts_ = av_rescale_q_rnd(packet.dts, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)) +
                                 av_rescale_q_rnd(last_audio_dts_, inputStream->time_base, outputStream->time_base,
                                                  (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            if (current_audio_pts_ == last_audio_pts_ && current_audio_pts_ > 0) {
                current_audio_pts_ += av_rescale_q(packet.duration, inputStream->time_base, outputStream->time_base);
                last_audio_pts_ = current_audio_pts_;
            }
            if (current_audio_dts_ == last_audio_dts_ && current_audio_dts_ > 0) {
                current_audio_dts_ += av_rescale_q(packet.duration, inputStream->time_base, outputStream->time_base);
                last_audio_dts_ = current_audio_dts_;
            }
            packet.pts = current_audio_pts_;
            packet.dts = current_audio_dts_;
        }

        packet.duration = av_rescale_q(packet.duration, inputStream->time_base, outputStream->time_base);
        packet.pos = -1;
        result = av_interleaved_write_frame(output_context_, &packet);
        if (result != 0) {
            av_packet_unref(&packet);
            JLOGE("%s write frame error: %d message: %s", __FUNCTION__, result, av_err2str(result));
            continue;
        }
        av_packet_unref(&packet);
    }

    if (output_context_ != nullptr) {
        av_write_trailer(output_context_);
        if (!(output_context_->oformat->flags & AVFMT_NOFILE)) {
            avio_close(output_context_->pb);
        }
        avformat_free_context(output_context_);
        output_context_ = nullptr;
    }

    if (composite_listener_ != nullptr) {
        JNIEnv *env = nullptr;
        int ret = jni_get_env(&env);
        if (env != nullptr) {
            auto clazz = env->GetObjectClass(composite_listener_);
            env->CallVoidMethod(composite_listener_,env->GetMethodID(clazz, "onComplete", "()V"));
            env->DeleteLocalRef(clazz);
            env->DeleteGlobalRef(composite_listener_);
        }
        if (ret == JNI_EDETACHED) {
            jni_detach_thread_env();
        }
    }

    if (input_paths_ != nullptr) {
        for (int i = 0; i < input_size_; i++) {
            delete[] input_paths_[i];
        }
    }
    input_paths_ = nullptr;

}

int VideoComposite::OpenInputVideo(int index) {
    if (index >= input_size_) {
        return ERR_COMPOSITE_EXCEED_VIDEO_LIMIT;
    }
    duration_ += input_context_->duration / 1000;
    last_video_pts_ = duration_ * video_time_base_.den * video_time_base_.num / 1000;
    last_video_dts_ = duration_ * video_time_base_.den * video_time_base_.num / 1000;
    last_audio_pts_ = duration_ * audio_time_base_.den * audio_time_base_.num / 1000;
    last_audio_dts_ = duration_ * audio_time_base_.den * audio_time_base_.num / 1000;
    current_video_time_ = 0;
    avformat_close_input(&input_context_);
    input_context_ = nullptr;

    int ret = avformat_open_input(&input_context_, input_paths_[index], nullptr, nullptr);
    if (ret != 0) {
        JLOGE("%s open input: %s error: %s", __func__, input_paths_[index], av_err2str(ret));
        return ret;
    }
    if (input_context_ == nullptr) {
        JLOGE("%s input context is nullptr.", __func__);
        return ERR_COMPOSITE_NO_INPUT_CONTEXT;
    }
    ret = avformat_find_stream_info(input_context_, nullptr);
    if (ret < 0) {
        JLOGE("%s find stream info error: %s", __func__, av_err2str(ret));
        return ret;
    }
    return 0;
}


void VideoComposite::OnError(int err_code) {
    if (output_context_ != nullptr) {
        if (!(output_context_->oformat->flags & AVFMT_NOFILE)) {
            avio_close(output_context_->pb);
        }
        avformat_free_context(output_context_);
    }
    output_context_ = nullptr;
    if (input_context_ != nullptr) {
        avformat_close_input(&input_context_);
    }
    input_context_ = nullptr;
    if (composite_listener_ != nullptr) {
        JNIEnv* env = nullptr;
        int ret = jni_get_env(&env);
        if (env != nullptr) {
            auto clazz = env->GetObjectClass(composite_listener_);
            env->CallVoidMethod(composite_listener_, env->GetMethodID(clazz, "onError", "(I)V"), err_code);
            env->DeleteLocalRef(clazz);
            env->DeleteGlobalRef(composite_listener_);
        }
        if (ret == JNI_EDETACHED) {
            jni_detach_thread_env();
        }
    }
}