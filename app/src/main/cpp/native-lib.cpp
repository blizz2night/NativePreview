//#define EGL_EGLEXT_PROTOTYPES
//#define GL_GLEXT_PROTOTYPES
#include <jni.h>
#include <string>
#include <android/native_window.h>
#include <android/surface_texture_jni.h>
#include <android/log.h>
#include <android/hardware_buffer_jni.h>
#include <android/hardware_buffer.h>
//#include <EGL/egl.h>
//#include <EGL/eglext.h>
//#include <GLES2/gl2.h>
//#include <GLES2/gl2ext.h>

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

extern "C"
JNIEXPORT void JNICALL
Java_com_github_blizz2inght_nativepreview_Utils_processHardwareBuffer(JNIEnv *env, jclass clazz,
                                                                      jobject hardwareBufferObj, jint width,
                                                                      jint height,
                                                                      jobject yuv_texture) {
    AHardwareBuffer *hardwareBuffer = AHardwareBuffer_fromHardwareBuffer(env, hardwareBufferObj);
    AHardwareBuffer_acquire(hardwareBuffer);
    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(hardwareBuffer, &desc);
    __android_log_print(ANDROID_LOG_INFO, "hardwareBuffer",
                        "width=%d,height=%d, stride=%d,format=%d, layers=%d, usage=%llu", desc.width,
                        desc.height, desc.stride, desc.format, desc.layers,desc.usage);
    void* bufferData = NULL;;

    AHardwareBuffer_lock(hardwareBuffer, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, NULL, &bufferData);

    ANativeWindow_setBuffersGeometry(window, desc.width, desc.height, desc.format);
    ANativeWindow_Buffer buffer;
    ANativeWindow_lock(window,&buffer,NULL);
    __android_log_print(ANDROID_LOG_INFO, "ANativeWindow_Buffer",
                        "width=%d,height=%d, stride=%d, format=%d", buffer.width,
                        buffer.height, buffer.stride, buffer.format);
    jbyte *src = static_cast<jbyte *>(bufferData);
    jbyte *dst = static_cast<jbyte *>(buffer.bits);
    if(buffer.stride==desc.stride) {
        memcpy(dst, src, sizeof(jbyte)*desc.stride*height*3/2);
    } else {
        for (int i = 0; i < height*3/2; ++i) {
            memcpy(dst, src, sizeof(jbyte)*desc.width);
            dst+=buffer.stride;
            src+=desc.stride;
        }
    }
//    memcpy(dst, src, sizeof(jbyte) * desc.height * desc.stride);
//    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

//    eglChooseConfig()
// Create an EGL Image with these attributes
//    EGLint eglImgAttrs[] = { EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE, EGL_NONE };
//    EGLClientBuffer clientBuffer = eglGetNativeClientBufferANDROID(hardwareBuffer);
//    eglCreateContext()
//    EGLint eglImageAttributes[] = {EGL_WIDTH, width, EGL_HEIGHT, height, EGL_MATCH_FORMAT_KHR,  EGL_FORMAT_RGB_565_KHR, EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE};
//    EGLImageKHR eglImageHandle = eglCreateImageKHR(display, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, clientBuffer, eglImgAttrs);
//    EGLSurface eglSurface = eglCreatePlatformWindowSurfaceEXT(display,eglConfig,window,eglImgAttrs);
//    eglCreatePbufferFromClientBuffer()
//    eglCreateStreamProducerSurfaceKHR()
//    PFNEGLCREATESTREAMATTRIBKHRPROC
//    glEGLImageTargetTexture2DOES(GL_TEXTURE_EXTERNAL_OES,eglImageHandle);
    int32_t fence = -1;
    AHardwareBuffer_unlock(hardwareBuffer, &fence);
    AHardwareBuffer_release(hardwareBuffer);
    ANativeWindow_unlockAndPost(window);
}