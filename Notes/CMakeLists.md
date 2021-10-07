
## CMakeLists

### 宏

```
add_definitions(-DCPPHTTPLIB_OPENSSL_SUPPORT
        -DCPPHTTPLIB_BROTLI_SUPPORT
        -DCPPHTTPLIB_ZLIB_SUPPORT)
```

### message()

```
message("CMAKE_CURRENT_SOURCE_DIR = ${CMAKE_SOURCE_DIR}/rapidjson/include")
message(STATUS "PROJECT_SOURCE_DIR = ${PROJECT_SOURCE_DIR}")
message(WARNING "CMAKE_BINARY_DIR = ${CMAKE_BINARY_DIR}")
```

查看消息请打开项目中的`\app\.cxx\cmake\debug\arm64-v8a\build_output.txt`文件
