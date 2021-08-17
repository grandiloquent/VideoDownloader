#include <android/log.h>

static int use_log_report = 0;

#define FF_LOG_TAG     "TAG"

/**
 * Print no output.
 */
#define AV_LOG_QUIET    -8

/**
 * Something went really wrong and we will crash now.
 */
#define AV_LOG_PANIC     0

/**
 * Something went wrong and recovery is not possible.
 * For example, no header was found for a format which depends
 * on headers or an illegal combination of parameters is used.
 */
#define AV_LOG_FATAL     8

/**
 * Something went wrong and cannot losslessly be recovered.
 * However, not all future data is affected.
 */
#define AV_LOG_ERROR    16

/**
 * Something somehow does not look correct. This may or may not
 * lead to problems. An example would be the use of '-vstrict -2'.
 */
#define AV_LOG_WARNING  24

/**
 * Standard information.
 */
#define AV_LOG_INFO     32

/**
 * Detailed information.
 */
#define AV_LOG_VERBOSE  40

/**
 * Stuff which is only useful for libav* developers.
 */
#define AV_LOG_DEBUG    48

/**
 * Extremely verbose debugging, useful for libav* development.
 */
#define AV_LOG_TRACE    56

#define FF_LOG_VERBOSE        ANDROID_LOG_VERBOSE
#define FF_LOG_DEBUG          ANDROID_LOG_DEBUG
#define FF_LOG_INFO           ANDROID_LOG_INFO
#define FF_LOG_WARN           ANDROID_LOG_WARN
#define FF_LOG_ERROR          ANDROID_LOG_ERROR

// 打印可变参数
#define VLOG(level, TAG, ...)    ((void)__android_log_vprint(level, TAG, __VA_ARGS__))

#define ALOG(level, TAG, ...)    ((void)__android_log_print(level, TAG, __VA_ARGS__))

#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, FF_LOG_TAG, format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  FF_LOG_TAG, format, ##__VA_ARGS__)


// 原样输出FFmpeg日志
static void ffp_log_callback_brief(void *ptr, int level, const char *fmt, va_list vl) {
    int ffplv = FF_LOG_VERBOSE;
    if (level <= AV_LOG_ERROR)
        ffplv = FF_LOG_ERROR;
    else if (level <= AV_LOG_WARNING)
        ffplv = FF_LOG_WARN;
    else if (level <= AV_LOG_INFO)
        ffplv = FF_LOG_INFO;
    else if (level <= AV_LOG_VERBOSE)
        ffplv = FF_LOG_VERBOSE;
    else
        ffplv = FF_LOG_DEBUG;

    if (level <= AV_LOG_INFO)
        VLOG(ffplv, FF_LOG_TAG, fmt, vl);
}

// 对FFmpeg日志进行格式化
static void ffp_log_callback_report(void *ptr, int level, const char *fmt, va_list vl) {
    int ffplv = FF_LOG_VERBOSE;
    if (level <= AV_LOG_ERROR)
        ffplv = FF_LOG_ERROR;
    else if (level <= AV_LOG_WARNING)
        ffplv = FF_LOG_WARN;
    else if (level <= AV_LOG_INFO)
        ffplv = FF_LOG_INFO;
    else if (level <= AV_LOG_VERBOSE)
        ffplv = FF_LOG_VERBOSE;
    else
        ffplv = FF_LOG_DEBUG;

    va_list vl2;
    char line[1024];
    static int print_prefix = 1;

    va_copy(vl2, vl);
    av_log_format_line(ptr, level, fmt, vl2, line, sizeof(line), &print_prefix);
    va_end(vl2);
    ALOG(ffplv, FF_LOG_TAG, "%s", line);
}