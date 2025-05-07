package com.example.projekcik;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Stack;
import android.content.pm.PackageManager;

import com.example.projekcik.javaViewCameraControl;

public class MainActivity extends Activity {

    private static final String TAG = "MyOpenCV";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private javaViewCameraControl mOpenCvCameraView; // Zmieniamy na javaViewCameraControl
    private UiDataBundle appData;
    private Mat myInputFrame;
    private DoubleTwoDimQueue dataQ;
    private Handler mHandler;
    private boolean keep_thread_running;
    private File dataPointsFile, fftOutFile;
    private FileOutputStream fileWriter;
    private int FPS;
    private long BPM;
    private int image_processed;
    private Stack<Long> timestampQ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button toggleFlashlightButton = findViewById(R.id.toggleFlashlightButton);
        Button startButton = findViewById(R.id.startButton);

        // Initializing variables
        appData = new UiDataBundle();
        timestampQ = new Stack<>();
        dataQ = new DoubleTwoDimQueue();
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // Update UI elements here
            }
        };

        // Initialize Camera
        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView); // Zmieniamy z CameraBridgeViewBase na javaViewCameraControl
        mOpenCvCameraView.setVisibility(View.VISIBLE);

        // Check permissions and start camera if granted
        if (checkCameraPermission()) {
            startCamera();
        }

        // Button for toggling flashlight
        toggleFlashlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle flashlight using javaViewCameraControl
                mOpenCvCameraView.toggleFlash();
            }
        });

        // Button for starting recording with flashlight
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle flashlight using javaViewCameraControl
                mOpenCvCameraView.enableFlashIfOff();
            }
        });
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void startCamera() {
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.enableView(); // Uruchomienie kamery
        Log.d(TAG, "Camera started successfully.");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialization failed.");
        } else {
            Log.d(TAG, "OpenCV initialized successfully.");
        }

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        keep_thread_running = false;
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false); // Turn off flashlight
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (myInputFrame != null) {
            myInputFrame.release();
        }
    }

    private void myFileSetup() {
        File dir = getExternalFilesDir(null);
        dataPointsFile = new File(dir, "dataPoints.csv");
        fftOutFile = new File(dir, "fftOut.csv");

        try {
            dataPointsFile.createNewFile();
            fftOutFile.createNewFile();
            Log.d("File Paths", "Data: " + dataPointsFile.getAbsolutePath() + " FFT: " + fftOutFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Thread to process frames and handle data
    private Thread processingThread = new Thread() {
        @Override
        public void run() {
            while (keep_thread_running) {
                if (appData != null && appData.image_got > 0) {
                    // Process frames here
                    processFrameData();
                }

                try {
                    Thread.sleep(33); // Ensure processing is done in ~30 FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void processFrameData() {
        // Example: Split the frame and process it
        ArrayList<Mat> img_comp = new ArrayList<>(3);
        Core.split(myInputFrame, img_comp);
        Mat myMat = img_comp.get(0); // Using the first channel (e.g., Green)

        // Calculate average intensity (could be used for pulse detection)
        double frameAvg = getMatAvg(myMat);
        appData.frameAv = frameAvg;

        // Log for debugging
        Log.d(TAG, "Processed frame average: " + frameAvg);

        // Pass data to the handler for UI updates
        Message msg = mHandler.obtainMessage(1, appData);
        mHandler.sendMessage(msg);
    }

    private double getMatAvg(Mat mat) {
        byte[] data = new byte[mat.cols() * mat.rows() * mat.channels()];
        mat.get(0, 0, data);
        double avg = 0;
        for (byte b : data) {
            avg += Math.abs(b);
        }
        return avg / data.length;
    }
}
