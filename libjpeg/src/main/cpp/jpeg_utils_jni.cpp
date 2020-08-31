#include <jni.h>
#include <string>
#ifdef __arm__
#include "libjpeg-turbo/armeabi-v7a/jconfig.h"
#elif __aarch64__
#include "libjpeg-turbo/arm64-v8a/jconfig.h"
#endif
#include "libjpeg-turbo/jpeglib.h"

extern "C" JNIEXPORT void JNICALL
Java_com_github_blizz2night_libjpeg_JpegUtils_compressNV21(
        JNIEnv* env,
        jobject /* this */, jbyteArray nv21, jint width, jint height) {
    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_compress(&cinfo);
    // 创建代表压缩的结构体


}
