#include <jni.h>
#include <string>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/surface_texture.h>
#include <android/surface_texture_jni.h>
#include <android/log.h>
#include <android/native_activity.h>
#include <android/window.h>
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
                                                     jobject yuv_texture) {
    // TODO: implement init()
}extern "C"
JNIEXPORT void JNICALL
Java_com_github_blizz2inght_nativepreview_Utils_process(JNIEnv *env, jclass clazz, jbyteArray image,
                                                        jint width, jint height,
                                                        jobject yuv_texture) {
    // TODO: implement process()
    jsize length = env->GetArrayLength(image);
    size_t count = sizeof(jbyte) * length;
    jbyte *buf = (jbyte *) malloc(count);
    memset(buf, 0, count);
    env->GetByteArrayRegion(image, 0, length, buf);
    ASurfaceTexture *sf = ASurfaceTexture_fromSurfaceTexture(env, yuv_texture);
    ANativeWindow *window = ASurfaceTexture_acquireANativeWindow(sf);
    ANativeWindow_setBuffersGeometry(window,width,height,AHARDWAREBUFFER_FORMAT_Y8Cb8Cr8_420);
    ANativeWindow_Buffer buffer;
    ANativeWindow_lock(window,&buffer,NULL);
    __android_log_print(ANDROID_LOG_INFO, "native-lib", "format=%d", buffer.format);
    __android_log_print(ANDROID_LOG_INFO, "native-lib", "width=%d,height=%d, stride=%d, size=%d",buffer.width, buffer.height, buffer.stride, buffer.width * buffer.height);
    memcpy(buffer.bits, buf, count);
    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
    ASurfaceTexture_release(sf);
    free(buf);
}