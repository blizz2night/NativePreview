# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        yuv_utils_jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
        yuv_utils_jni.cpp)


include_directories(libyuv/include)
add_subdirectory(libyuv)


find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        yuv_utils_jni

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
        yuv
        )