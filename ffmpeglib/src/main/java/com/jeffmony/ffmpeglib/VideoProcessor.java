package com.jeffmony.ffmpeglib;

import androidx.annotation.NonNull;

import com.jeffmony.ffmpeglib.listener.IVideoTransformProgressListener;
import com.jeffmony.ffmpeglib.listener.OnVideoCompositeListener;
import com.jeffmony.ffmpeglib.model.VideoInfo;

import java.util.List;

public class VideoProcessor {

    private IVideoTransformProgressListener mListener;

    private static volatile boolean mIsLibLoaded = false;

    public static void loadLibrariesOnce() {
        synchronized (VideoProcessor.class) {
            if (!mIsLibLoaded) {
                System.loadLibrary("avcodec");
                System.loadLibrary("avfilter");
                System.loadLibrary("avformat");
                System.loadLibrary("avutil");
                System.loadLibrary("postproc");
                System.loadLibrary("swresample");
                System.loadLibrary("swscale");
                System.loadLibrary("jeffmony");

                initFFmpegOptions();

                mIsLibLoaded = true;
            }
        }
    }

    public VideoProcessor() {
        loadLibrariesOnce();
    }

    //初始化设置ffmpeg options
    public static native void initFFmpegOptions();

    //获取视频的基本信息
    public native VideoInfo getVideoInfo(String inputPath);

    //转化视频的封装格式,M3U8 转化为 MP4格式
    public native int transformVideo(String inputPath, String outputPath);

    public void setOnVideoTransformProgressListener(@NonNull IVideoTransformProgressListener listener) {
        mListener = listener;
    }

    //从native层调用上来,回调当前的视频转化进度
    public void invokeVideoTransformProgress(float progress) {
        if (mListener != null) {
            mListener.onTransformProgress(progress);
        }
    }

    //剪切视频长度
    public native int cutVideo(double start, double end, String inputPath, String outputPath);

    //将多个MP4视频合成为一个视频
    public native int compositeVideos(String outputVideoPath, List<String> videos, OnVideoCompositeListener listener);
}
