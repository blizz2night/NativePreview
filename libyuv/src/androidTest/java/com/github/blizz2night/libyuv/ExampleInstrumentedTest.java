package com.github.blizz2night.libyuv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.github.blizz2night.libyuv.test", appContext.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(appContext.getResources(),R.drawable.photo);
        int byteCount = bitmap.getByteCount();
        //         * int color = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] bytes = YuvUtils.argbBufferToI420(byteBuffer, bitmap.getWidth(), bitmap.getHeight());
        File externalFilesDir = appContext.getExternalMediaDirs()[0];
        dumpYUV(bytes, "" + bitmap.getWidth() + "x" + bitmap.getHeight() + ".i420", externalFilesDir);
    }

    @Test
    public void testToI420() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.github.blizz2night.libyuv.test", appContext.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(appContext.getResources(),R.drawable.photo);
        int byteCount = bitmap.getByteCount();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] bytes = YuvUtils.argbBufferToI420(byteBuffer, bitmap.getWidth(), bitmap.getHeight());
        File externalFilesDir = appContext.getExternalMediaDirs()[0];
        dumpYUV(bytes, "" + bitmap.getWidth() + "x" + bitmap.getHeight() + ".i420", externalFilesDir);
    }


    private static void dumpYUV(byte[] src, String name, File dir) {
        File yuv = new File(dir, name);
        try {
            yuv.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try(FileOutputStream fout = new FileOutputStream(yuv)){
            fout.write(src, 0, src.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dumpYUVToJpeg(YuvImage yuvImage, long name, File dir) {
        File jpeg = new File(dir, "" + name + ".jpeg");
//                jpeg.mkdirs();
        try {
            jpeg.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try(FileOutputStream fout = new FileOutputStream(jpeg)){
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 90, fout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
