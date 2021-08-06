# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
PROJECT(native-lib C)

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)


###############################################################################
# INSTALL DIRECTORIES
###############################################################################


# set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS}  -Wall -Werror")

# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++1z -Wall -UNDEBUG")
# -lstdc++fs

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED
        native-lib.c)

#add_definitions(-DMG_MAX_HTTP_SEND_MBUF=8092 -DMG_ENABLE_HTTP_STREAMING_MULTIPART)

add_library(json
        STATIC
        cJSON.c
        )

include_directories(.)
include_directories(include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/include)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/mbedtls)

##　target_compile_definitions(lib_exe1 PRIVATE -DForExe1)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

add_library(https
        STATIC
        https.c
        )

#target_link_libraries( # Specifies the target library.
#        https
#        mbedtls)

target_link_libraries(https mbedx509)
target_link_libraries(https mbedtls)
target_link_libraries(https mbedcrypto)

target_link_libraries( # Specifies the target library.
        native-lib
        json
        https
        ${log-lib})