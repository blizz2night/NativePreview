package com.github.blizz2inght.nativepreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Utils {
    private static final String TAG = "GLUtils";

    public static String getStringFromFileInAssets(Context context, String filename){
        StringBuilder builder = new StringBuilder();
        try (InputStream ins = context.getAssets().open(filename);
             InputStreamReader insReader = new InputStreamReader(ins);
             BufferedReader reader = new BufferedReader(insReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "getStringFromFileInAssets: ", e);
        }
        return builder.toString();
    }

    public static int loadShader(String strSource, int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.d(TAG,
                    "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }

    public static int loadProgram(String strVSource, String strFSource) {
        int[] link = new int[1];
        int vShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
        if (vShader == 0) {
            throw new RuntimeException("Compile Vertex Shader Failed");
        }
        int fShader = loadShader(strFSource,  GLES20.GL_FRAGMENT_SHADER);
        if (fShader == 0) {
            throw new RuntimeException("Compile Fragment Shader Failed");
        }

        int program = GLES20.glCreateProgram();

        GLES20.glAttachShader(program, vShader);
        GLES20.glAttachShader(program, fShader);

        GLES20.glLinkProgram(program);

        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            throw new RuntimeException("Link Shader Program Failed");
        }
        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);
        return program;
    }


    public static Bitmap generateSquareLutBitmap(){
        int block = 8;
        int pixel = 64;
        int factor = 256 / pixel;
        int width = block * pixel;
        int height = width;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final int[] pixels = new int[width * height];
        for (int b = 0; b < pixel; b++) {
            for (int g = 0; g < pixel; g++) {
                for (int r = 0; r < pixel; r++) {
                    pixels[r + b % block * pixel + width * (g + b / block * pixel)] = 0xFF000000 | (r * factor << 16) | (g * factor << 8) | b * factor;
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static Bitmap generateColumnLut() {
        int pixel = 16;
        int factor = 256 / pixel;
        final int width = 16;
        final int height = 256;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final int[] pixels = new int[width * height];
        for (int b = 0; b < pixel; b++) {
            for (int g = 0; g < pixel; g++) {
                for (int r = 0; r < pixel; r++) {
                    pixels[r + width * g + height * b] = 0xFF000000 | (r * factor << 16) | (g * factor << 8) | b * factor;
                }
            }
        }
        bitmap.setPixels(pixels,0, width,0,0, width, height);
        return bitmap;
    }

    public static void writeToDisk(Context context, Bitmap bitmap, String name) {
        final File dir = context.getExternalMediaDirs()[0];
        final File file = new File(dir, name);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream stream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        /**
         * plane[1] ByteBuffer从U开始, plane[2] ByteBuffer从V开始
         * 如果UV交错存储,理论上应该有结构
         *         NV21   buffer= [V, U, V, U]
         *       plane[1] buffer= [   U, V, U]
         *       plane[2] buffer= [V, U, V,  ]
         *
         * 注意
         * 1. 如上述两个buffer比原始NV21小1byte的情况,
         *  直接使用plane[2]的buffer会丢失末尾一个字节的U
         *  todo 考虑取plane[1] 补偿dst[dst.length-1]
         * 2. 不兼容NV12,I420,YV12
         *         NV12   buffer= [U, V, U, V]
         *       plane[1] buffer= [U, V, U,  ]
         *       plane[2] buffer= [   V, U, V]
         *
         * @param image Image对象，{@link ImageFormat#YUV_420_888}
         * @param dst  返回NV21格式数据
         */
        public static boolean imageToNV21(Image image, byte[] dst) {
            Log.i(TAG, "imageToNV21: ");
            final int imageWidth = image.getWidth();
            final int imageHeight = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            int offset = 0;
//            byte[] temp = new byte[5];
//            for (int channel = 0; channel < planes.length; channel++) {
//                Image.Plane plane = planes[channel];
//                final ByteBuffer buffer = plane.getBuffer();
//                buffer.position(buffer.remaining() - 5);
//                buffer.get(temp, 0, 5);
//                Log.i(TAG, "imageToNV21: channel=" + channel + ", array=" + Arrays.toString(temp));
//                buffer.position(0);
//            }
            for (int channel = 0; channel < planes.length; channel += 2) {
                Image.Plane plane = planes[channel];
                final ByteBuffer buffer = plane.getBuffer();
                final int rowStride = plane.getRowStride();
                // plane.getPixelStride()==2, 说明UV交错存储在buffer上
                final int pixelStride = plane.getPixelStride();
                // YUV420: UV数据是Y的1/2
                int rows = imageHeight / pixelStride;
                // 图像通道每行无填充对齐, 一次读完
                if (rowStride == imageWidth) {
                    // Copy whole plane from buffer into |data| at once.
                    int length = rowStride * rows;
                    // plane[2]的buffer比实际图像VU小1byte,读buffer实际大小
                    buffer.get(dst, offset, buffer.remaining());
                    offset += length;
                } else {
                    //图像每行有填充额外数据做字节对齐
                    for (int i = 0; i < rows - 1; i++) {
                        buffer.get(dst, offset, rowStride);
                        offset += imageWidth;
                    }
                    //plane[2]的buffer比实际图像VU小1byte,最后一行读buffer实际大小
                    int lastRowLength = Math.min(imageWidth, buffer.remaining());
                    buffer.get(dst, offset, lastRowLength);
                    offset += imageWidth;
                }
            }
            return true;
        }

    public static boolean imageToNV21withStride(Image image, byte[] dst) {
        Log.i(TAG, "imageToNV21: ");
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        int offset = 0;
        for (int channel = 0; channel < planes.length; channel += 2) {
            Image.Plane plane = planes[channel];
            final ByteBuffer buffer = plane.getBuffer();
            final int rowStride = plane.getRowStride();
            // plane.getPixelStride()==2, 说明UV交错存储在buffer上
            final int pixelStride = plane.getPixelStride();
            // YUV420: UV数据是Y的1/2
            int rows = imageHeight / pixelStride;
            // Copy whole plane from buffer into |data| at once.
            int length = rowStride * rows;
            Log.i(TAG, "imageToNV21withStride: channel "+channel+", rowStride" + rowStride+", width="+imageWidth);
            // plane[2]的buffer比实际图像VU小1byte,读buffer实际大小
            buffer.get(dst, offset, buffer.remaining());
            offset += length;
        }
        return true;
    }

    native static void init(SurfaceTexture yuvTexture);

    native static void process(byte[] dst, int width, int height, SurfaceTexture yuvTexture);
}
