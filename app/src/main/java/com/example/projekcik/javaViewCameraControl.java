package com.example.projekcik;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.opencv.android.JavaCameraView;


public class javaViewCameraControl extends JavaCameraView {
    private static final String TAG = "CameraControl";


    private CameraManager mCameraManager;
    private String mCameraId;
    private boolean mFlashOn = false;

    public javaViewCameraControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        findCameraWithFlash();
    }

    private void findCameraWithFlash() {
        try {
            for (String id : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (hasFlash != null && hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = id;
                    Log.d(TAG, "Using camera ID with flash: " + mCameraId);
                    return;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to find camera with flash", e);
        }
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
            if (mCameraId == null) {
                Log.e(TAG, "Camera ID is null, cannot turn on flash");
                return;
            }

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted");
                return;
            }

            mCameraManager.setTorchMode(mCameraId, true);
            mFlashOn = true;
            Log.d(TAG, "Torch mode ON");

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error enabling torch mode", e);
        }
    }

    private void turnFlashOff() {
        try {
            if (mCameraId == null) {
                Log.e(TAG, "Camera ID is null, cannot turn off flash");
                return;
            }

            mCameraManager.setTorchMode(mCameraId, false);
            mFlashOn = false;
            Log.d(TAG, "Torch mode OFF");

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error disabling torch mode", e);
        }
    }

    @Override
    protected void disconnectCamera() {
        turnFlashOff(); // wyłącz latarkę przy rozłączeniu
        super.disconnectCamera();
    }

    public void enableFlashIfOff() {
        if (!mFlashOn) {
            new Handler(Looper.getMainLooper()).postDelayed(this::turnFlashOn, 300);
        }
    }
}
