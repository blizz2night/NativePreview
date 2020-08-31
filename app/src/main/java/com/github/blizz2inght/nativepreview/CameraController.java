package com.github.blizz2inght.nativepreview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.HardwareBuffer;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import static android.content.Context.CAMERA_SERVICE;

public class CameraController {
    private static final String TAG = "CameraController";
    public static final int OPEN_CAMERA = 1;
    public static final int CLOSE_CAMERA= 2;
    private final Context mContext;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private CameraManager mCameraManager;
    private String[] mCameraIdList;
    private String mCameraId;
    private CameraCharacteristics mCharacteristics;
    private CameraDevice mCameraDevice;
    private StreamConfigurationMap mConfigurationMap;
    private Size[] mSupportedPreviewSizes;
    private Size[] mSupportedYuvSizes;

    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mRequestBuilder;
//    private Surface mSurface;
    private int mSensorOrientation = 90;
    private Size mPreviewSize;
    private Size mYuvSize;
    private ImageReader mYuvReader;
    private SurfaceTexture mYuvTexture;

    public CameraController(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread("TAG");
        mHandlerThread.start();
        mHandlerThread.getLooper();
        mHandler = new Handler(mHandlerThread.getLooper(), mCameraHandlerCallback);

        mCameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        Log.i(TAG, "onResume: "+mCameraManager);
        if (mCameraManager != null) {
            try {
                mCameraIdList = mCameraManager.getCameraIdList();
                if (mCameraIdList.length > 0) {
                    mCameraId = mCameraIdList[0];

                    mCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
                    mSensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    mConfigurationMap = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    final List<CaptureRequest.Key<?>> sessionKeys = mCharacteristics.getAvailableSessionKeys();
                    Log.i(TAG, "CameraController: sessionKeys=" + sessionKeys);
                    if (mConfigurationMap != null) {
                        mSupportedPreviewSizes = mConfigurationMap.getOutputSizes(SurfaceTexture.class);
                        mSupportedYuvSizes = mConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);
                    }
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "openCamera: ", e);
            }
        }
    }

    private Handler.Callback mCameraHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case OPEN_CAMERA:
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            Log.i(TAG, "handleMessage: openCamera+++");
                            mCameraManager.openCamera(mCameraId, mCameraCallback, null);
                        } catch (CameraAccessException e) {
                            Log.e(TAG, "handleMessage: ", e);
                        }
                    }
                    break;
                case CLOSE_CAMERA:
                    Log.i(TAG, "handleMessage: CLOSE_CAMERA");
                    Log.i(TAG, "handleMessage: close session+++");

                    if (mSession != null) {
                        try {
//                            mSession.abortCaptures();
                            mSession.stopRepeating();
                        } catch (CameraAccessException e) {
                            Log.e(TAG, "handleMessage: abortCaptures error", e);
                        }
                        mSession.close();
                        mSession = null;
                    }
                    Log.i(TAG, "handleMessage: close session---");

                    Log.i(TAG, "handleMessage: close device+++");
                    if (mCameraDevice != null) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }
                    Log.i(TAG, "handleMessage: close device---");
                    if (mYuvReader != null) {
                        mYuvReader.close();
                    }
                    break;
            }
            return false;
        }
    };

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "onOpened---: "+camera);
            mCameraDevice = camera;
            try {
                List<Surface> surfaces = new ArrayList<>();
                mYuvSize = getNearestSize(mSupportedYuvSizes, mPreviewSize);
                mYuvReader = ImageReader.newInstance(mYuvSize.getWidth(), mYuvSize.getHeight(), ImageFormat.YUV_420_888, 2);
                mYuvReader.setOnImageAvailableListener(mYuvListener, null);
                final Surface surface = mYuvReader.getSurface();
                surfaces.add(surface);
                mRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                mRequestBuilder.addTarget(surface);
                mCameraDevice.createCaptureSession(surfaces, mSessionCallback, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "onOpened: ", e);
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            Log.i(TAG, "onClosed: ");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "onDisconnected: ");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.w(TAG, "onError: "+error);
            camera.close();
            mCameraDevice = null;
        }
    };

    private CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "onConfigured: ");
            mSession = session;
            CaptureRequest captureRequest = mRequestBuilder.build();
            try {
                mSession.setRepeatingRequest(captureRequest, mPreviewStateCallBack, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "onConfigured: ", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "onConfigureFailed: " + session);
        }
    };

    private CameraCaptureSession.CaptureCallback mPreviewStateCallBack = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//            final Integer afMode = result.get(CaptureResult.CONTROL_AF_MODE);
//            final Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//            final Integer aeMode = result.get(CaptureResult.CONTROL_AE_MODE);
//            final Integer AeState = result.get(CaptureResult.CONTROL_AE_STATE);
//            Log.i(TAG, "onCaptureCompleted: afMode=" + afMode + ", afState=" + afState
//                    + ", aeMode=" + aeMode
//                    + ", aeState=" + AeState);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            Log.i(TAG, "onCaptureFailed: "+failure);
        }
    };

    public void close() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(CLOSE_CAMERA);
    }

    public void open(SurfaceTexture yuvTexture) {
        mYuvTexture = yuvTexture;
        Utils.init(mYuvTexture, mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mHandler.obtainMessage(OPEN_CAMERA).sendToTarget();
    }


    public void release() {
        mHandler.getLooper().quitSafely();
    }

    private ImageReader.OnImageAvailableListener mYuvListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            final Image image = reader.acquireLatestImage();
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                HardwareBuffer hardwareBuffer = image.getHardwareBuffer();
//                Log.i(TAG, "onImageAvailable: " + hardwareBuffer);
//                Utils.processBuffer(image.getPlanes()[0].getBuffer(), width, height, mYuvTexture);
                Utils.processHardwareBuffer(hardwareBuffer, width, height, mYuvTexture);

                image.close();
            }
        }
    };


    public Size filterPreviewSize(int targetW, int targetH) {
        final Size[] previewSizes = mSupportedPreviewSizes;
        //相机的sensor方向通常和手机屏幕垂直
        final boolean reverseHW = mSensorOrientation % 180 != 0;
        if (reverseHW) {
            int temp = targetW;
            targetW = targetH;
            targetH = temp;
        }
        Size targetSize = new Size(targetW, targetH);
        final Size size = getNearestSize(previewSizes, targetSize);
        mPreviewSize = size;
        return size;
    }

    private Size getNearestSize(Size[] supported, Size targetSize) {
        if (supported == null || supported.length < 1) {
            throw new RuntimeException("no supported preview sizes!");
        }

        Size size = supported[0];
        int distance = calManDistance(size, targetSize);
        for (Size s : supported) {
            int d = calManDistance(s, targetSize);
            if (d == 0) {
                size = s;
                break;
            }else if (d < distance) {
                size = s;
                distance = d;
            }
        }
        Log.i(TAG, "filterPreviewSize: "+size);
        return size;
    }

    public static int calManDistance(Size src, Size target){
        final int dw = target.getWidth() - src.getWidth();
        final int dh = target.getHeight() - src.getHeight();
        return Math.abs(dw)+Math.abs(dh);
    }


}
