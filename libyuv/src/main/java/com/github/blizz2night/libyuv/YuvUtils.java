package com.github.blizz2night.libyuv;

import java.nio.ByteBuffer;

public class YuvUtils {
    static{
        System.loadLibrary("yuv_utils_jni");
    }
    public static native byte[] argbToI420(byte[] argb, int width, int height);
    public static native byte[] argbBufferToI420(ByteBuffer buffer, int width, int height);
    public static native byte[] nv21ToI420(byte[] nv21, int width, int height, int rowStride);
}
