#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "libjpeg-turbo/turbojpeg.h"


#define DEFAULT_SUBSAMP  TJSAMP_444
#define DEFAULT_QUALITY  95

const char *subsampName[TJ_NUMSAMP] = {
        "4:4:4", "4:2:2", "4:2:0", "Grayscale", "4:4:0", "4:1:1"
};

const char *colorspaceName[TJ_NUMCS] = {
        "RGB", "YCbCr", "GRAY", "CMYK", "YCCK"
};

tjscalingfactor *scalingFactors = NULL;
int numScalingFactors = 0;

extern "C" JNIEXPORT jobject JNICALL
Java_com_github_blizz2night_libjpeg_JpegUtils_compressNV21(
        JNIEnv* env,
        jclass clazz, jbyteArray yuv420sp, jint width, jint height) {
    jbyte *buffer = env->GetByteArrayElements(yuv420sp, 0);
    const unsigned char * imgBuf = reinterpret_cast<const unsigned char *>(buffer);
    jsize length = env->GetArrayLength(yuv420sp);
    size_t count = sizeof(jbyte) * length;
//    unsigned char *imgBuf = new u_char[count];
//    memcpy(imgBuf,buffer,count);
    int flags = 0;
    unsigned char *jpegBuf = NULL;
    unsigned long jpegSize = 0;
    tjhandle tjInstance = tjInitCompress();
    tjCompressFromYUV(tjInstance,imgBuf,width,1,height,TJSAMP_420,&jpegBuf,&jpegSize,95,flags);
    tjDestroy(tjInstance);
    tjInstance = NULL;
    jobject directByteBuffer = env->NewDirectByteBuffer(jpegBuf, jpegSize);
    jpegBuf = NULL;
    return directByteBuffer;
//    tjFree(jpegBuf);  jpegBuf = NULL;

}
