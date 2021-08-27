PROJECT(native-lib C)

cmake_minimum_required(VERSION 3.4.1)


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

add_library(mbedtls
        STATIC
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/aes.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/aesni.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/arc4.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/aria.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/asn1parse.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/asn1write.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/base64.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/bignum.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/blowfish.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/camellia.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ccm.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/certs.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/chacha20.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/chachapoly.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/cipher.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/cipher_wrap.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/cmac.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ctr_drbg.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/debug.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/des.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/dhm.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ecdh.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ecdsa.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ecjpake.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ecp.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ecp_curves.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/entropy.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/entropy_poll.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/error.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/gcm.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/havege.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/hkdf.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/hmac_drbg.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/md.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/md2.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/md4.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/md5.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/md_wrap.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/memory_buffer_alloc.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/net_sockets.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/nist_kw.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/oid.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/padlock.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pem.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pk.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pkcs11.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pkcs12.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pkcs5.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pkparse.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pkwrite.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/pk_wrap.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/platform.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/platform_util.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/poly1305.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ripemd160.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/rsa.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/rsa_internal.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/sha1.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/sha256.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/sha512.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_cache.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_ciphersuites.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_cli.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_cookie.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_srv.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_ticket.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/ssl_tls.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/threading.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/timing.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/version.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/version_features.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509write_crt.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509write_csr.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509_create.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509_crl.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509_crt.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/x509_csr.c
        ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/xtea.c
        )

include_directories(.)

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

target_link_libraries( # Specifies the target library.
        native-lib
        json
        mbedtls
        ${log-lib})

