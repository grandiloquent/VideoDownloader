set(libs
    mbedtls
)

if(USE_PKCS11_HELPER_LIBRARY)
    set(libs ${libs} pkcs11-helper)
endif(USE_PKCS11_HELPER_LIBRARY)

if(ENABLE_ZLIB_SUPPORT)
    set(libs ${libs} ${ZLIB_LIBRARIES})
endif(ENABLE_ZLIB_SUPPORT)

function(add_test_suite suite_name)
    if(ARGV1)
        set(data_name ${ARGV1})
    else()
        set(data_name ${suite_name})
    endif()

    add_custom_command(
        OUTPUT test_suite_${data_name}.c
        COMMAND ${PERL_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/scripts/generate_code.pl ${CMAKE_CURRENT_SOURCE_DIR}/suites test_suite_${suite_name} test_suite_${data_name}
        DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/scripts/generate_code.pl mbedtls suites/helpers.function suites/main_test.function suites/test_suite_${suite_name}.function suites/test_suite_${data_name}.data
    )

    include_directories(${CMAKE_CURRENT_SOURCE_DIR})
    add_executable(test_suite_${data_name} test_suite_${data_name}.c)
    target_link_libraries(test_suite_${data_name} ${libs})
    add_test(${data_name}-suite test_suite_${data_name})
endfunction(add_test_suite)

if(CMAKE_COMPILER_IS_GNUCC OR CMAKE_COMPILER_IS_CLANG)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused-function")
endif(CMAKE_COMPILER_IS_GNUCC OR CMAKE_COMPILER_IS_CLANG)

if(MSVC)
    # If a warning level has been defined, suppress all warnings for test code
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /W0")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /WX-")
endif(MSVC)

# Make data_files available in an out-of-source build
if (NOT ${CMAKE_CURRENT_BINARY_DIR} STREQUAL ${CMAKE_CURRENT_SOURCE_DIR})
    # Get OS dependent path to use in `execute_process`
    file(TO_NATIVE_PATH "${CMAKE_CURRENT_BINARY_DIR}/data_files" link)
    file(TO_NATIVE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/data_files" target)

    if (NOT EXISTS ${link})
        if (CMAKE_HOST_UNIX)
            set(command ln -s ${target} ${link})
        else()
            set(command cmd.exe /c mklink /j ${link} ${target})
        endif()

        execute_process(COMMAND ${command}
                        RESULT_VARIABLE result
                        ERROR_VARIABLE output)

        if (NOT ${result} EQUAL 0)
            message(FATAL_ERROR "Could not create symbolic link for: ${target} --> ${output}")
        endif()
    endif()
endif()

