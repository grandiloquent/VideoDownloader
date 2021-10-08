#ifndef CONFIGURATION_H
#define CONFIGURATION_H
// #include "Configuration.h"

#define USER_AGENT_ANDROID "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Mobile Safari/537.36"
#define USER_AGENT_PC "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"

#define  PARSE_JSON(X) Document d; \
d.Parse(X); \
if (d.HasParseError()) { \
return {};\
}
#endif

