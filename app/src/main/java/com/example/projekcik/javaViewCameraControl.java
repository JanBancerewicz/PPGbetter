package com.example.projekcik;

import android.content.Context;
import android.hardware.camera2.*;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

public class javaViewCameraControl extends JavaCameraView {
    private CameraManager cameraManager;
    private String cameraId;

    public javaViewCameraControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Log.e("CameraControl", "Camera access error", e);
        }
    }

    public void turnFlashOn() {
        try {
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            Log.e("CameraControl", "Failed to turn on flash", e);
        }
    }

    public void turnFlashOff() {
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            Log.e("CameraControl", "Failed to turn off flash", e);
        }
    }

    public void setFrameRate(int min, int max) {
        Log.w("CameraControl", "setFrameRate is not directly supported in Camera2 API");
        // In Camera2 API, frame rate adjustments need to be handled via CaptureRequest.Builder
    }
}
