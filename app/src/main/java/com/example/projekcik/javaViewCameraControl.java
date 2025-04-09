package com.example.projekcik;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

import org.opencv.android.JavaCameraView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class javaViewCameraControl extends JavaCameraView {
    private static final String TAG = "CameraControl";
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraManager mCameraManager;
    private String mCameraId;
    private Surface mPreviewSurface;
    private boolean mFlashOn = false;

    public javaViewCameraControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    protected boolean initializeCamera(int width, int height) {
        boolean result = super.initializeCamera(width, height);

        try {
            // Pobierz dostępne kamery
            String[] cameraIds = mCameraManager.getCameraIdList();
            if (cameraIds.length == 0) {
                Log.e(TAG, "No cameras available");
                return false;
            }

            // Użyj pierwszej dostępnej kamery
            mCameraId = cameraIds[0];
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);

            // Sprawdź czy kamera ma latarkę
            Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (flashAvailable == null || !flashAvailable) {
                Log.w(TAG, "Flash not available");
            }

            // Pobierz SurfaceTexture z klasy bazowej
            try {
                java.lang.reflect.Field field = JavaCameraView.class.getDeclaredField("mSurfaceTexture");
                field.setAccessible(true);
                mPreviewSurface = new Surface((android.graphics.SurfaceTexture) field.get(this));
            } catch (Exception e) {
                Log.e(TAG, "Error getting preview surface", e);
                return false;
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error", e);
            return false;
        }

        return result;
    }

    public void toggleFlash() {
        if (mFlashOn) {
            turnFlashOff();
        } else {
            turnFlashOn();
        }
    }

    private void turnFlashOn() {
        try {
            // 1. Stop current preview
//            disconnectCamera();

            // 2. Open camera with permission check
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    try {
                        // 3. Create capture request with flash
                        mPreviewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

                        // 4. Create list of surfaces (CRITICAL FIX)
                        List<Surface> surfaces = new ArrayList<>();
                        if (mPreviewSurface != null && mPreviewSurface.isValid()) {
                            surfaces.add(mPreviewSurface);
                            mPreviewRequestBuilder.addTarget(mPreviewSurface); // Add surface to request
                        } else {
                            Log.e(TAG, "Preview surface is null or invalid");
                            return;
                        }

                        // 5. Create and start session
                        camera.createCaptureSession(
                                surfaces,
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {
                                        mCaptureSession = session;
                                        try {
                                            // 6. Set repeating request
                                            session.setRepeatingRequest(
                                                    mPreviewRequestBuilder.build(),
                                                    null,
                                                    null
                                            );
                                            mFlashOn = true;
                                            Log.d(TAG, "Flash turned ON successfully");
                                        } catch (CameraAccessException e) {
                                            Log.e(TAG, "Failed to start preview", e);
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                        Log.e(TAG, "Failed to configure session");
                                    }
                                },
                                null
                        );
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Camera access error", e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
                // ... rest of callbacks ...
            }, null);
        } catch (Exception e) {
            Log.e(TAG, "Error turning flash on", e);
        }
    }

    private void turnFlashOff() {
        try {
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            // Ponownie włącz normalny podgląd
            enableView();
            mFlashOn = false;
            Log.d(TAG, "Flash turned OFF");
        } catch (Exception e) {
            Log.e(TAG, "Error turning flash off", e);
        }
    }

    @Override
    protected void disconnectCamera() {
        turnFlashOff();
        super.disconnectCamera();
    }

    private void closeCamera() {
        try {
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing camera", e);
        }
    }
}