#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include "libyuv.h"
#define TAG "yuv_utils_jni"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_github_blizz2night_libyuv_YuvUtils_argbToI420(JNIEnv *env, jclass thiz, jbyteArray argb,
                                               jint width, jint height) {
    uint8_t * src = (uint8_t* )env->GetByteArrayElements(argb, 0);
    jint YSize = width * height;
    jint USize = width * height / 4;
    jint dstSize = width * height * 3 / 2;
    jbyteArray dstArr = env->NewByteArray(dstSize);
    jbyte *dst = env->GetByteArrayElements(dstArr, 0);
    uint8_t *y = reinterpret_cast<uint8_t *>(dst);
    uint8_t *u = y + YSize;
    uint8_t *v = u + USize;
    libyuv::ABGRToI420(src, width * 4, y, width, u, width / 2, v, width / 2, width, height);
    env->ReleaseByteArrayElements(dstArr, dst, 0);
    free(dst);
    return dstArr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_github_blizz2night_libyuv_YuvUtils_argbBufferToI420(JNIEnv *env, jclass thiz, jobject argb_byte_buffer,
                                                       jint width, jint height) {
    void *address = env->GetDirectBufferAddress(argb_byte_buffer);
    uint8_t *src = static_cast<uint8_t *>(address);
    jint YSize = width * height;
    jint USize = width * height / 4;
    jint dstSize = width * height * 3 / 2;
    jbyteArray dstArr = env->NewByteArray(dstSize);
    jbyte *dst = env->GetByteArrayElements(dstArr, 0);
    uint8_t *y = reinterpret_cast<uint8_t *>(dst);
    uint8_t *u = y + YSize;
    uint8_t *v = u + USize;
    libyuv::ABGRToI420(src, width * 4, y, width, u, width / 2, v, width / 2, width, height);
    env->ReleaseByteArrayElements(dstArr, dst, 0);
    return dstArr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_github_blizz2night_libyuv_YuvUtils_nv21ToI420(JNIEnv *env, jclass thiz, jbyteArray nv21,
                                                       jint width, jint height, jint row_stride) {
    jbyte *src = env->GetByteArrayElements(nv21, 0);
    uint8_t *src_y = reinterpret_cast<uint8_t *>(src);
    jsize length = env->GetArrayLength(nv21);
    uint8_t *src_vu = src_y + height * row_stride;
    jint YSize = width * height;
    jint USize = width * height / 4;
    jint dstSize = width * height * 3 / 2;
    uint8_t *dst = static_cast<uint8_t *>(malloc(sizeof(uint8_t) * dstSize));
    uint8_t *y = dst;
    uint8_t *u = y + YSize;
    uint8_t *v = u + USize;
    LOGD("NV21ToI420---");
    libyuv::NV21ToI420(src_y, row_stride, src_vu, row_stride, y, width, u, width / 2, v, width / 2, width, height);
    LOGD("NV21ToI420+++");
    jbyteArray pArray = env->NewByteArray(dstSize);
    env->SetByteArrayRegion(pArray, 0, dstSize, (jbyte *) dst);
    env->ReleaseByteArrayElements(nv21, src, 0);
    free(dst);
    return pArray;
}
