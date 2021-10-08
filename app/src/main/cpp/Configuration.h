#ifndef CONFIGURATION_H
#define CONFIGURATION_H
// #include "Configuration.h"

#include <jni.h>

#define USER_AGENT_ANDROID "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Mobile Safari/537.36"
#define USER_AGENT_PC "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"

#define PARSE_JSON(X)      \
  Document d;              \
  d.Parse(X);              \
  if (d.HasParseError()) { \
    return {};             \
  }

namespace jni {
    template<typename... ToTypes>
    struct Convert;

    template<>
    struct Convert<std::string> {
        static std::string from(JNIEnv *env, const jstring &value) {
            typedef std::unique_ptr<const char[], std::function<void(const char *)>>
                    JniString;

            JniString cstr(env->GetStringUTFChars(value, nullptr), [=](const char *p) {
                env->ReleaseStringUTFChars(value, p);
            });

            if (cstr == nullptr) {
            }

            return cstr.get();
        }
    };

    template<>
    struct Convert<jstring> {
        static jstring from(JNIEnv *env, const std::string &from) {
            return env->NewStringUTF(from.c_str());
        }
    };
}

#endif

