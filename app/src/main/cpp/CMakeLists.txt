cmake_minimum_required(VERSION 3.4.1)

# build application's shared lib
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++2a -fno-rtti -fexceptions -Wall")

add_subdirectory(brotli)

# configure import libs
set(distribution_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../distribution)

add_library(lib_ssl SHARED IMPORTED)
set_target_properties(lib_ssl PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/openssl/lib/${ANDROID_ABI}/libssl_1_1.so)

add_library(lib_crypto SHARED IMPORTED)
set_target_properties(lib_crypto PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/openssl/lib/${ANDROID_ABI}/libcrypto_1_1.so)

add_library(z SHARED IMPORTED)
set_target_properties(z PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/zlib/lib/${ANDROID_ABI}/libz.so)

add_definitions(-DCPPHTTPLIB_OPENSSL_SUPPORT
        -DCPPHTTPLIB_BROTLI_SUPPORT
        -DCPPHTTPLIB_ZLIB_SUPPORT)

add_library(httplib SHARED
        httplib/httplib.cc)

add_library(base64 SHARED base64.cpp)

target_include_directories(httplib PRIVATE
        ${distribution_DIR}/openssl/include
        ${CMAKE_CURRENT_SOURCE_DIR}/brotli/c/include
        ${distribution_DIR}/zlib/include
        )
target_link_libraries(httplib
        lib_crypto
        lib_ssl
        brotlicommon
        brotlidec
        brotlienc
        z
        )

add_library(hello-libs SHARED
        Porn91.cpp
        Logger.cpp
        Ck57.cpp
        Shared.cpp
        KuaiShou.cpp
        XVideos.cpp
        DouYin.cpp
        MangoTV.cpp
        Iqiyi.cpp
        AcFun.cpp
        hello-libs.cpp
        )

target_include_directories(hello-libs PRIVATE
        ${distribution_DIR}/openssl/include
        # ${CMAKE_SOURCE_DIR}/rapidjson/include
        )

message("CMAKE_CURRENT_SOURCE_DIR = ${CMAKE_SOURCE_DIR}/rapidjson/include")
message(STATUS "PROJECT_SOURCE_DIR = ${PROJECT_SOURCE_DIR}")
message(WARNING "CMAKE_BINARY_DIR = ${CMAKE_BINARY_DIR}")

target_link_libraries(hello-libs
        android
        httplib
        base64
        log
        )
