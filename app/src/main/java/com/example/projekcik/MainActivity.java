package com.example.projekcik;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MyOpenCV";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private javaViewCameraControl mOpenCvCameraView;
    private UiDataBundle appData;
    private Mat myInputFrame;
    private DoubleTwoDimQueue dataQ;
    private Stack<Long> timestampQ;
    private boolean keep_thread_running = true;
    private boolean isMeasuring = false;
    private Handler mHandler;
    private int image_processed;
    private int startPointer, endPointer, fftPoints;
    private boolean first_fft_run = true;
    private boolean start_fft = false;
    private boolean init_frames_discard = false;

    private String currentVideoPath;
    private Uri videoUri;

    private CameraManager cameraManager;
    private String cameraId;

    private MediaRecorder mediaRecorder;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appData = new UiDataBundle();
        timestampQ = new Stack<>();
        dataQ = new DoubleTwoDimQueue();
        mHandler = new Handler(Looper.getMainLooper());

        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        initFlashlight();

        if (checkPermissions()) {
            startCamera();
        }
    }

    private boolean checkPermissions() {
        // teraz jedynie CAMERA
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
            return false;
        }
        return true;
    }

    private void startCamera() {
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.enableView();
        Log.d(TAG, "Camera started successfully.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Wymagane jest uprawnienie do kamery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initFlashlight() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (hasFlash!=null && hasFlash && facing==CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "initFlashlight error", e);
        }
    }

    private void toggleFlashlight(boolean state) {
        try {
            if (cameraId!=null) cameraManager.setTorchMode(cameraId, state);
        } catch (CameraAccessException e) {
            Log.e(TAG, "toggleFlashlight error", e);
        }
    }

    public void onToggleMeasurement(View view) {
        Button btn = (Button) view;
        if (!isMeasuring) {
            toggleFlashlight(true);
            // START
            keep_thread_running = true;
            new Thread(mainProcessingRunnable).start();
            new Thread(fftProcessingRunnable).start();
            isMeasuring = true;
            btn.setText("Zatrzymaj pomiar");
            try {
                prepareMediaRecorder();
                mediaRecorder.start();
            } catch (IOException e) {
                Toast.makeText(this, "Błąd uruchamiania nagrywania", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "MediaRecorder start error", e);
            }
        } else {
            // STOP
            keep_thread_running = false;
            isMeasuring = false;
            toggleFlashlight(false);
            btn.setText("Rozpocznij pomiar");
            try {
                mediaRecorder.stop();
            } catch (Exception ignore) {}
            mediaRecorder.release();
            Toast.makeText(this, "Wideo zapisane w Galerii", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Zapisano video: " + currentVideoPath);
        }
    }

    private void prepareMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();

        // <-- TU ZMIANA: CAMERA zamiast SURFACE
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        String fn = "video_"+ new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) +".mp4";
        ContentValues v = new ContentValues();
        v.put(MediaStore.Video.Media.DISPLAY_NAME, fn);
        v.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        v.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES+"/PomiarVideos");

        Uri uri = getContentResolver()
                .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
        if (uri==null) throw new IOException("Nie można utworzyć pliku video");
        videoUri = uri;
        currentVideoPath = uri.toString();

        FileDescriptor fd = getContentResolver().openFileDescriptor(uri, "w").getFileDescriptor();
        mediaRecorder.setOutputFile(fd);

        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(10_000_000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280,720);

        // podgląd w Twoim SurfaceView
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

        mediaRecorder.prepare();
    }

    @Override public void onCameraViewStarted(int w,int h){}
    @Override public void onCameraViewStopped(){}
    @Override public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame f){
        myInputFrame = f.rgba();
        timestampQ.push(System.currentTimeMillis());
        return myInputFrame;
    }

    private Runnable mainProcessingRunnable = ()->{ /* ... jak dawniej ... */ };
    private Runnable fftProcessingRunnable = ()->{ /* ... jak dawniej ... */ };

    @Override protected void onPause(){
        super.onPause();
        keep_thread_running=false;
        if (mOpenCvCameraView!=null) mOpenCvCameraView.disableView();
    }
    @Override protected void onDestroy(){
        super.onDestroy();
        keep_thread_running=false;
        if (mOpenCvCameraView!=null) mOpenCvCameraView.disableView();
    }
    @Override protected void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) Log.d(TAG,"OpenCV load failed.");
        else Log.d(TAG,"OpenCV loaded.");
        if (mOpenCvCameraView!=null) mOpenCvCameraView.enableView();
    }
}
