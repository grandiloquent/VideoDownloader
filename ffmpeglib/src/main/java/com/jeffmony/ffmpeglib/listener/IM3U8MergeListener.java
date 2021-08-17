package com.jeffmony.ffmpeglib.listener;

public interface IM3U8MergeListener {

    void onM3U8MergeProgress(float progress);

    void onMergedFinished();

    void onMergeFailed(Exception e);
}
