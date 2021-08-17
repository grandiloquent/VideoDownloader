package com.jeffmony.ffmpeglib.utils;

import android.util.Log;

public class LogUtils {

    private static final boolean IS_DEBUG = true;

    private static final boolean IS_INFO = true;

    private static final boolean IS_WARN = true;

    private static final boolean IS_ERROR = true;

    public static void d(String tag, String msg) {
        if (IS_DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (IS_INFO) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (IS_WARN) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (IS_ERROR) {
            Log.e(tag, msg);
        }
    }
}
