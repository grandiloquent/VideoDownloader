package com.jeffmony.ffmpeglib;

public class FFmpegInfoUtils {

    // Used to load the 'native-lib' library on application startup.
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

    public static native String stringFromJNI();

    public static native String avcodecInfo();

    public static native String avfilterInfo();

    public static native String avformatInfo();

    public static native String protocolInfo();
}
