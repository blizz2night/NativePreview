package com.github.blizz2night.libjpeg;

public class JpegUtils {
    native void compressNV21(byte[] nv21, int width, int height);
}
