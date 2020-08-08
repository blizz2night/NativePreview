#include <jni.h>
#include <string>
#include <android/native_window.h>
#include <android/surface_texture.h>
#include <android/surface_texture_jni.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES/gl.h>

ANativeWindow *window = nullptr;
extern "C" JNIEXPORT jstring JNICALL
Java_com_github_blizz2inght_nativepreview_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_github_blizz2inght_nativepreview_Utils_init(JNIEnv *env, jclass clazz,
                                                     jobject yuv_texture, jint width, jint height) {
    ASurfaceTexture *sf = ASurfaceTexture_fromSurfaceTexture(env, yuv_texture);
    window = ASurfaceTexture_acquireANativeWindow(sf);
    ANativeWindow_setBuffersTransform(window,ANATIVEWINDOW_TRANSFORM_ROTATE_90);
    int32_t format = ANativeWindow_getFormat(window);
    int32_t ww = ANativeWindow_getWidth(window);
    int32_t wh = ANativeWindow_getHeight(window);
    __android_log_print(ANDROID_LOG_INFO, "native-lib nativeWindow",
                        "width=%d,height=%d,format=%d", ww, wh, format);
    ANativeWindow_setBuffersGeometry(window, width, height, AHARDWAREBUFFER_FORMAT_Y8Cb8Cr8_420);
//    ANativeWindow_setBuffersGeometry(window, width, height, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);

    ASurfaceTexture_release(sf);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_blizz2inght_nativepreview_Utils_process(JNIEnv *env, jclass clazz, jbyteArray image,
                                                        jint width, jint height,
                                                        jobject yuv_texture) {
    if (window != nullptr) {
        jsize length = env->GetArrayLength(image);
        size_t count = sizeof(jbyte) * length;
        jbyte *arr = (jbyte *) malloc(count);
        memset(arr, 0, count);
        env->GetByteArrayRegion(image, 0, length, arr);
        ANativeWindow_Buffer buffer;
        ANativeWindow_lock(window,&buffer,NULL);
        __android_log_print(ANDROID_LOG_INFO, "native-lib",
                            "width=%d,height=%d, stride=%d, size=%d,format=%d", buffer.width,
                            buffer.height, buffer.stride, buffer.width * buffer.height,
                            buffer.format);
        memcpy(buffer.bits, image, count);
        ANativeWindow_unlockAndPost(window);
        free(arr);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_blizz2inght_nativepreview_Utils_uninit(JNIEnv *env, jclass clazz) {
    if (window != nullptr) {
        ANativeWindow_release(window);
        window = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_blizz2inght_nativepreview_Utils_processBuffer(JNIEnv *env, jclass clazz,
                                                              jobject byteBuffer, jint width,
                                                              jint height, jobject yuv_texture) {
    jbyte *address = static_cast<jbyte *>(env->GetDirectBufferAddress(byteBuffer));
    jlong length = env->GetDirectBufferCapacity(byteBuffer);
    size_t count = static_cast<size_t>(sizeof(jbyte) * length * 3 / 2);
    jbyte *buf = (jbyte *) malloc(count);
    memset(buf, 0, count);
    ANativeWindow_Buffer buffer;
    ANativeWindow_lock(window,&buffer,NULL);
    __android_log_print(ANDROID_LOG_INFO, "native-lib",
                        "width=%d,height=%d, stride=%d, size=%d,format=%d", buffer.width,
                        buffer.height, buffer.stride, buffer.width * buffer.height,
                        buffer.format);
    jbyte *addr = static_cast<jbyte *>(buffer.bits);
    memcpy(addr, address, count);
    ANativeWindow_unlockAndPost(window);
}