package com.jeffmony.ffmpeglib;

public class FFmpegCmdUtils {

    static {

        System.loadLibrary("jeffmony");
        System.loadLibrary("avcodec");
        System.loadLibrary("avfilter");
        System.loadLibrary("avformat");
        System.loadLibrary("avutil");
        System.loadLibrary("postproc");
        System.loadLibrary("swresample");
        System.loadLibrary("swscale");
    }

    public static native int ffmpegExecute(String[] cmds);
}
