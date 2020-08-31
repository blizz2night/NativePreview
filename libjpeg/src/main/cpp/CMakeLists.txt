# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#abiFilters "armeabi", "armeabi-v7a" , "arm64-v8a", "x86", "x86_64", "mips", "mips64"
message(WARNING "cmake libjpeg module")
if(${ANDROID_ABI} STREQUAL "arm64-v8a") #针对不同 ABI 设置不同的编译代码 或 if(${ANDROID_ABI} STREQUAL "areambi")
    message(WARNING build ${ANDROID_ABI}.so)
    SET(CMAKE_ASM_FLAGS --target=aarch64-linux-android${ANDROID_PLATFORM})
elseif(${ANDROID_ABI} STREQUAL  "armeabi-v7a")
    message(WARNING build ${ANDROID_ABI}.so)
    SET(CMAKE_ASM_FLAGS --target=arm-linux-android${ANDROID_PLATFORM})
endif()

include_directories(libjpeg-turbo)
add_subdirectory(libjpeg-turbo)

add_library( # Sets the name of the library.
        jpeg_utils_jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
        jpeg_utils_jni.cpp)

include_directories(${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libjpeg-turbo)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                    jpeg_utils_jni

                    jpeg
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )