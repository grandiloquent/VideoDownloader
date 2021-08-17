package com.jeffmony.ffmpeglib.listener;

public interface IVideoCutListener {

    void onVideoCutFailed(Exception e);

    void onVideoCutFinised();
}
