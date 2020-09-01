package com.github.blizz2night.libjpeg;

import java.nio.ByteBuffer;

public class JpegUtils {
    static {
        System.loadLibrary("jpeg_utils_jni");
    }
    static native ByteBuffer compressNV21(byte[] nv21, int width, int height);
}
