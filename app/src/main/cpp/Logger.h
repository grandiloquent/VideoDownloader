#ifndef LOGGER_H
#define LOGGER_H
// #include "Logger.h"

#include <android/log.h>
#include <string>

using namespace std;

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "B5aOx2::", __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, "B5aOx2::", __VA_ARGS__))

void WriteFile(const string &fileName, const string &content);


#endif
