#ifndef PORN91_H
#define PORN91_H
// #include "porn91.h"
#include "http.h"
#include "str.h"
#include "url.h"
//

int get91PornVideo(char *request, size_t request_len) {
  // =========================
  uintptr_t rc = HAL_TCP_Establish("91porn.com", 80);
  if (rc == -1) {
    LOGE("Error, %s", "HAL_TCP_Establish");
    return -1;
  }
  // =========================
  int32_t ret = HAL_TCP_Write(rc, request, request_len, 5000);
  if (ret == -1) {
    LOGE("Error, %s", "HAL_TCP_Write");
    close(rc);
    return -1;
  }
  size_t buffer_size = 1024;
  size_t response_buffer_size = 32768;

  char buf[buffer_size];
  char rsp[response_buffer_size];

  rsp[0] = 0;
  char *body;
  int found = 0;
  int total = 0;

  // =========================
  while (1) {
    int len = HAL_TCP_Read(rc, buf, buffer_size, 3000);
    if (len < 0) {
      break;
    }
    total += len;
    if (total >= response_buffer_size) {
      break;
    }
    strncat(rsp, buf, len);
    if (!found && (body = strstr(rsp, "\r\n\r\n"))) {
      found = 1;
    }
    if (found && strstr(rsp, "\r\n0\r\n")) {
      break;
    }
  }

  if (body == NULL) {
    LOGE("Error, %s", "HAL_TCP_Read");
     close(rc);
    return -1;
  }
  // =========================
  body += 4;
  memset(buf, 0, buffer_size);
  int count = 0;
  const char *tmp = body;
  char result[response_buffer_size];
  result[0] = 0;
  total = 0;

  while (*tmp) {
    char c = *tmp;
    if (c != '\r') {
      buf[count++] = c;
    } else {
      size_t len = strtoul(buf, 0, 16);
      if (len == 0) break;
      strncat(result, tmp + 2, len);
      total += len;
      tmp += len + 4;
      memset(buf, 0, buffer_size);
      count = 0;
      continue;
    }
    tmp++;
  }
  result[total] = 0;
  buf[0] = 0;
  int encodeSize = substring((const char *)result,
                             "document.write(strencode2(\"", "\"", buf);
  if (encodeSize == -1) {
     close(rc);
    return -1;
  }
  urldecode(result, buf);
  request[0] = 0;
  encodeSize = substring(result, "src='", "'", request);
  close(rc);
  return encodeSize;
}

// https://github.com/Tencent/TencentOS-tiny/blob/master/components/connectivity/iotkit-embedded-3.0.1/3rdparty/wrappers/os/ubuntu/HAL_TCP_linux.c

#endif
