package com.example.projekcik;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.content.Context;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.projekcik.javaViewCameraControl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class oldmain {

/*
    public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
        private static final String TAG = "MyOpenCV";

        private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

        private javaViewCameraControl mOpenCvCameraView;
        //    private CameraBridgeViewBase mOpenCvCameraView;
        private TextView title_tv;
        private TextView frameNum;
        private TextView frameSize;
        private TextView frameAvg;
        private TextView imgProcessed;

        private UiDataBundle appData;
        private Mat myInputFrame;
        private DoubleTwoDimQueue dataQ;
        private int startPointer;
        private int endPointer;
        private int fftPoints;
        private int image_processed;
        private boolean first_fft_run;
        private boolean start_fft;
        private boolean init_frames_discard;
        private boolean keep_thread_running;

        private File dataPointsFile;
        private File fftOutFile;
        private FileOutputStream fileWriter;

        private int FPS;
        private long BPM;
        private int state_fft;

        private Handler mHandler;
        private int bad_frame_count;
        private Stack<Long> timestampQ;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Inicjalizacja wszystkich wymaganych obiektów
            appData = new UiDataBundle();
            timestampQ = new Stack<>();
            dataQ = new DoubleTwoDimQueue(); // Zakładając, że ta klasa istnieje
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    // Aktualizacja UI
                }
            };

            // Inicjalizacja kamery
            mOpenCvCameraView = findViewById(R.id.HelloOpenCvView);
            mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);

            // Ustawienie flag
            keep_thread_running = true;
            first_fft_run = true;
            init_frames_discard = false;

            // Reszta inicjalizacji
            if (checkCameraPermission()) {
                startCamera();
            }
        }


        private boolean checkCameraPermission() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        }


        public void myFileSetup() {
            // Używaj przestrzeni dla aplikacji w zamiast dostępu do /sdcard/
            File dir = getExternalFilesDir(null); // lub getExternalFilesDir("data") jeśli chcesz subfolder
            dataPointsFile = new File(dir, "dataPoints.csv");
            fftOutFile = new File(dir, "fftOut.csv");

            try {
                // Tworzenie nowych plików
                dataPointsFile.createNewFile();
                fftOutFile.createNewFile();

                Log.d("Data points path = ", dataPointsFile.getAbsolutePath());
                Log.d("FFT points path = ", fftOutFile.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.enableView();
            }
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, null); // Usunięcie mLoaderCallback

            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
            }
        }

        @Override
        public void onPause() {
            keep_thread_running = false;
            try {
                CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mOpenCvCameraView != null) {
//            mOpenCvCameraView.turnFlashOff(); // Wyłączamy latarkę
                mOpenCvCameraView.disableView();
            }
            super.onPause();
            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.disconnectCamera();
            }
        }

        @Override
        public void onDestroy() {
            keep_thread_running = false;
            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.disableView();
            }
            if (myInputFrame != null) {
                myInputFrame.release();
            }
            super.onDestroy();
        }

        @Override
        public void onCameraViewStarted(int width, int height) {
            // Initialize any necessary camera-related data when camera view starts
        }

        @Override
        public void onCameraViewStopped() {
            // Handle cleanup when camera view stops
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            // Inicjalizacja jeśli jeszcze nie istnieje (podwójne zabezpieczenie)
            if (appData == null) {
                appData = new UiDataBundle();
            }
            if (timestampQ == null) {
                timestampQ = new Stack<>();
            }

            appData.image_got = (appData.image_got == null) ? 1 : appData.image_got + 1;
            myInputFrame = inputFrame.rgba();
            timestampQ.push(System.currentTimeMillis());

            return myInputFrame;
        }

        private void startCamera() {
            // Inicjalizujemy widok kamery, aby działał poprawnie po przyznaniu uprawnień
            mOpenCvCameraView.setCameraPermissionGranted(); // Sprawdzamy dostępność kamery
            mOpenCvCameraView.enableView(); // Umożliwiamy widok kamery
//        mOpenCvCameraView.turnFlashOn(); // Umożliwiamy widok kamery
            Log.d(TAG, "Camera started successfully with flash.");
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
        }


        public void onClickButtonxd(View view) {
            if (mOpenCvCameraView != null) {
                try {
                    // Najpierw zatrzymaj kamerę
                    mOpenCvCameraView.disableView();

                    // Potem włącz latarkę
//                mOpenCvCameraView.turnFlashOn();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ((javaViewCameraControl) mOpenCvCameraView).toggleFlash();
                    }, 300); // 300ms opóźnienia

                    // Ponownie uruchom kamerę
                    mOpenCvCameraView.enableView();
                } catch (Exception e) {
                    Log.e(TAG, "Flash control error", e);
                }
            }
        }


        // Convert Mat to byte[]
        public byte[] Mat2Byte(Mat img) {
            int total_bytes = img.cols() * img.rows() * img.channels();
            byte[] return_byte = new byte[total_bytes];
            img.get(0, 0, return_byte);
            return return_byte;
        }

        // Calculate average of Mat
        public double getMatAvg(Mat img) {
            double avg = 0.0;
            byte[] b_arr = Mat2Byte(img);

            for (int i = 0; i < b_arr.length; i++) {
                int val = (int) b_arr[i];

                if (val < 0)
                    val = 256 + val;

                avg += val;
            }
            avg = avg / b_arr.length;
            return avg;
        }

        public void handleInputData(double data) {
            int state = 0;
            double queueData[][] = new double[1][2];

            if (data < 180) {
                state = 1;
            }

            queueData[0][0] = data;
            queueData[0][1] = 0.0;

            switch (state) {
                case 0:
                    bad_frame_count = 0;
                    image_processed++;
                    dataQ.Qpush(queueData);
                    break;
                case 1:
                    ++bad_frame_count;
                    image_processed++;
                    dataQ.Qpush(queueData);

                    if (bad_frame_count > 5) {
                        Log.e(TAG, "Expect errors. " + bad_frame_count + " consecutive bad frames");
                    }
                    break;
                default:
                    Log.e(TAG, "ERROR : UNKNOWN STATE");
            }

            // Discard the first 30 frames as they might contain junk data
            // Reset pointers to new initial conditions
            if ((!init_frames_discard) && (image_processed >= 30)) {
                startPointer = 30;
                endPointer = 30;
                image_processed = 0;
                init_frames_discard = true;
                Log.d(TAG + " My Thread", "Discarded first 30 frames");
            }

            // Triggering FFT process
            if (first_fft_run) {
                if (image_processed >= 1024) {
                    fftPoints = 1024;
                    startPointer = 30;
                    endPointer = 30 + image_processed - 1;
                    start_fft = true;
                    Log.d(TAG + " My Thread", "Start FFT set");
                    first_fft_run = false;
                    image_processed = 0;
                } else if ((image_processed >= 768) && (image_processed < 512) && (state_fft == 2)) {
                    state_fft++;
                    fftPoints = 512;
                    endPointer = 30 + image_processed - 1;
                    start_fft = true;
                    Log.d(TAG + " My Thread", "Start FFT set. State = " + state_fft);
                } else if ((image_processed >= 512) && (image_processed < 1024) && (state_fft == 1)) {
                    state_fft++;
                    fftPoints = 512;
                    endPointer = 30 + image_processed - 1;
                    start_fft = true;
                    Log.d(TAG + " My Thread", "Start FFT set. State = " + state_fft);
                } else if ((image_processed >= 256) && (image_processed < 512) && (state_fft == 0)) {
                    state_fft++;
                    fftPoints = 256;
                    endPointer = 30 + image_processed - 1;
                    start_fft = true;
                    Log.d(TAG + " My Thread", "Start FFT set");
                }
            } else {
                if (image_processed >= 128) {
                    startPointer = startPointer + image_processed;
                    endPointer = endPointer + image_processed;

                    start_fft = true;
                    Log.d(TAG + " My Thread", "Start FFT set");

                    image_processed = 0;
                }
            }
        }

        // 111111111111111111
        Thread myThread = new Thread() {
            @Override
            public void run() {
                // Dodatkowe zabezpieczenie przed null
                if (appData == null || timestampQ == null || mHandler == null) {
                    Log.e(TAG, "Critical objects not initialized!");
                    return;
                }
                // Czekaj, aż otrzymamy pierwszą klatkę
                while (true) {
                    try {
                        // Sprawdzanie, czy appData jest null
                        if (appData == null) {
                            throw new NullPointerException("appData is null");
                        }

                        // Jeśli obraz nie jest jeszcze dostępny, czekaj
                        if (appData.image_got <= 0) {
                            Log.d(TAG, "Waiting for image");
                            Thread.sleep(1000); // Czekaj przez 1 sekundę
                            continue; // Kontynuuj w pętli, aby sprawdzić ponownie
                        }

                        // Jeśli appData jest poprawnie zainicjowane, przerwij pętlę
                        break;

                    } catch (NullPointerException e) {
                        Log.e(TAG, "appData is not initialized yet, waiting...");
                        try {
                            Thread.sleep(1000); // Czekaj przez 1 sekundę, jeśli appData jest null
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // Obsługuje inne wyjątki
                    }
                }

                // Włączenie lampy błyskowej (jeśli jest dostępna)
                CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    String cameraId = cameraManager.getCameraIdList()[0]; // Używamy pierwszej dostępnej kamery
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                    if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                        cameraManager.setTorchMode(cameraId, true); // Włączamy lampę błyskową
                        Log.d(TAG, "Flashlight turned ON.");
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                // Próbujemy uzyskać 30 FPS (33ms na klatkę)
                int targetFPS = 30; // Ustawiamy 30 FPS
                long frameTime = 1000 / targetFPS; // Czas na jedną klatkę (w ms)

                int image_got_local = -1;

                while (keep_thread_running) {
                    // Czekaj, aż nowa klatka będzie dostępna
                    while (image_got_local == appData.image_got) {
                        try {
                            Thread.sleep(11); // Utrzymanie płynności
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Zmierz czas rozpoczęcia przetwarzania klatki
                    long startTime = System.currentTimeMillis();

                    // Operacje na obrazie (np. rozdzielanie kanałów, obliczanie średniej)
                    appData.frameSz = myInputFrame.size();
                    ArrayList<Mat> img_comp = new ArrayList<>(3);
                    org.opencv.core.Core.split(myInputFrame, img_comp);

                    // Przykład przetwarzania (tu używamy zielonego kanału)
                    Mat myMat = img_comp.get(0);
                    appData.frameAv = getMatAvg(myMat);

                    // Wysyłanie danych do wątku UI
                    Message uiMessage = mHandler.obtainMessage(1, appData);
                    uiMessage.sendToTarget();

                    handleInputData(appData.frameAv);
                    image_got_local = appData.image_got;

                    // Obliczanie opóźnienia i synchronizacja wątku, by uzyskać 30 FPS
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long sleepTime = Math.max(frameTime - elapsedTime, 0); // Ustalamy czas czekania na kolejną klatkę
                    try {
                        Thread.sleep(sleepTime); // Czekaj na następną klatkę
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };


//    myThread.start();

//myThread.start();

        // Thread 2: FFT processing thread
        Thread myFFTThread = new Thread() {
            @Override
            public void run() {
                while (keep_thread_running) {
                    if (!start_fft) {
                        Log.d(TAG + " FFT Thread", "Start FFT is not set");
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d(TAG + " FFT Started", "Clearing the variable");
                        start_fft = false;

                        double[][] sample_arr = new double[fftPoints][2];
                        double[] input_arr = new double[fftPoints];
                        double[] freq_arr = new double[fftPoints];
                        fftLib f = new fftLib();

                        Log.d(TAG, "StartPointer = " + startPointer + " EndPointer = " + endPointer);
                        sample_arr = dataQ.toArray(startPointer, endPointer);
                        input_arr = dataQ.toArray(startPointer, endPointer, 0);

                        long timeStart = timestampQ.get(startPointer);
                        long timeEnd = timestampQ.get(endPointer);

                        FPS = (fftPoints * 1000) / (int) (timeEnd - timeStart);
                        Log.d(TAG, "FPS Calculated = " + FPS);

                        freq_arr = f.fft_energy_squared(sample_arr, fftPoints);

//                    Log.d("FFT OUT : ", Arrays.toString(freq_arr));
//                    Log.d("Data points : ", Arrays.toString(input_arr));

                        Log.d("FFT OUT : ", Arrays.toString(Arrays.copyOfRange(freq_arr, 0, 10)));  // Logowanie pierwszych 10 elementów
                        Log.d("Data points : ", Arrays.toString(Arrays.copyOfRange(input_arr, 0, 10)));  // Logowanie pierwszych 10 elementów


                        // Frequency calculation and BPM detection
                        double factor = fftPoints / FPS;  // (N / Fs)
                        double nMinFactor = 0.75;  // The frequency corresponding to 45bpm
                        double nMaxFactor = 2.5;   // The frequency corresponding to 150bpm

                        int nMin = (int) Math.floor(nMinFactor * factor);
                        int nMax = (int) Math.ceil(nMaxFactor * factor);

                        double max = freq_arr[nMin];
                        int pos = nMin;
                        for (int i = nMin; i <= nMax; i++) {
                            if (freq_arr[i] > max) {
                                max = freq_arr[i];
                                pos = i;
                            }
                        }

                        double bps = pos / factor;  // Calculate the frequency
                        double bpm = 60.0 * bps;    // Calculate BPM
                        BPM = Math.round(bpm);
                        Log.d(TAG + " FFT Thread", "MAX = " + max + " pos = " + pos);
                    }
                }
            }
        };
    }*/
}
