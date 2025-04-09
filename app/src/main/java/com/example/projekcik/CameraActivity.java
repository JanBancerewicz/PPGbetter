//package com.example.projekcik;
//
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.SurfaceTexture;
//import android.hardware.camera2.CameraAccessException;
//import android.hardware.camera2.CameraCaptureSession;
//import android.hardware.camera2.CameraCharacteristics;
//import android.hardware.camera2.CameraDevice;
//import android.hardware.camera2.CameraManager;
//import android.hardware.camera2.CaptureRequest;
//import android.os.Bundle;
//import android.view.Surface;
//import android.view.TextureView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import java.util.Collections;
//
//public class CameraActivity extends AppCompatActivity {
//    private TextureView textureView;
//    private CameraDevice cameraDevice;
//    private CameraCaptureSession captureSession;
//    private CaptureRequest.Builder captureRequestBuilder;
//    private String cameraId;
//    private CameraManager cameraManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        textureView = new TextureView(this);
//        setContentView(textureView);
//
//        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//
//        textureView.setSurfaceTextureListener(textureListener);
//    }
//
//    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            openCamera();
//        }
//
//        @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
//        @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }
//        @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
//    };
//
//    private void openCamera() {
//        try {
//            for (String id : cameraManager.getCameraIdList()) {
//                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
//                Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
//                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
//
//                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK && flashAvailable != null && flashAvailable) {
//                    cameraId = id;
//                    break;
//                }
//            }
//
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
//                return;
//            }
//
//            cameraManager.openCamera(cameraId, stateCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
//        @Override
//        public void onOpened(@NonNull CameraDevice camera) {
//            cameraDevice = camera;
//            startPreview();
//        }
//
//        @Override public void onDisconnected(@NonNull CameraDevice camera) {
//            cameraDevice.close();
//        }
//
//        @Override public void onError(@NonNull CameraDevice camera, int error) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//    };
//
//    private void startPreview() {
//        try {
//            SurfaceTexture texture = textureView.getSurfaceTexture();
//            if (texture == null) return;
//
//            texture.setDefaultBufferSize(1920, 1080);
//            Surface surface = new Surface(texture);
//
//            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureRequestBuilder.addTarget(surface);
//
//            // 🔥 Ustawienie latarki (ciągłego światła)
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
//            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
//
//            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession session) {
//                    captureSession = session;
//                    try {
//                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
//            }, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (cameraDevice != null) {
//            cameraDevice.close();
//        }
//    }
//}
