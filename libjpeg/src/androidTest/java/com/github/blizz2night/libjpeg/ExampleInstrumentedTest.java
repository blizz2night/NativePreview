package com.github.blizz2night.libjpeg;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

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
        try (InputStream ins = appContext.getResources().openRawResource(R.raw.sample_i420_220x220)){
            int len = ins.available();
            byte[] yuv = new byte[len];
            ins.read(yuv, 0, len);
            ByteBuffer byteBuffer = JpegUtils.compressNV21(yuv, 220, 220);
            int remaining = byteBuffer.remaining();
            byte[] jpeg = new byte[remaining];
            byteBuffer.get(jpeg, 0, remaining);
            File externalFilesDir = appContext.getExternalMediaDirs()[0];
            dump(jpeg, "sample.jpeg", externalFilesDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        appContext.getResources().openRawResource(R.raw.sample_i420_220x220);
        assertEquals("com.github.blizz2night.libjpeg.test", appContext.getPackageName());
    }

    private static void dump(byte[] src, String name, File dir) {
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
}
