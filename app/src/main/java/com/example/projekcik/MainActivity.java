package com.example.projekcik;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends Activity {
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private SurfaceHolder surfaceHolder;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private String cameraId;
    private CameraManager cameraManager;
    private boolean isRecording = false;


//    private final BlockingQueue<Double> greenSamples = new ArrayBlockingQueue<>(512);

    private Queue<Double> greenSamples = new ArrayDeque<>();
    private Queue<Double> sampleTimestamps = new ArrayDeque<>();
    private TextView heartRateTextView;
    private final int fftSize = 256;
    private long startTime = 0;
    private double timer = 0.0;
    private final Deque<Integer> recentBpms = new ArrayDeque<>();
    private final int bpmWindowSize = 5;
    private int lastBpm = -1;

    private SurfaceHolder greenHolder;

    ImageReader imageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        heartRateTextView = findViewById(R.id.heartRateTextView);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        SurfaceView greenSurfaceView = findViewById(R.id.greenSurfaceView);
        greenHolder = greenSurfaceView.getHolder();


        if (checkPermissions()) {
            setupCamera();
        }

    }

    private Double[] applyMovingAverage(Double[] samples, int windowSize) {
        Double[] result = new Double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            int count = 0;
            Double sum = 0.0;
            // Sumuj ostatnie 'windowSize' próbek (lub mniej, jeśli nie ma ich wystarczająco)
            for (int j = Math.max(0, i - windowSize + 1); j <= i; j++) {
                sum += samples[j];
                count++;
            }
            result[i] = sum / count;
        }
        return result;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Brak uprawnień do kamery", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    private void setupCamera() {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    openCamera();
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e("Camera setup error: ", e.toString());
        }
    }

    public void onToggleRecording(View view) {
        Button button = (Button) view;
        Log.d("START RECORDING", "elo");
        if (!isRecording) {
            try {
                if (captureSession != null) {
                    captureSession.close();
                    captureSession = null;
                }
                prepareMediaRecorder();
                if (cameraId == null) {
                    Toast.makeText(this, "Nie wykryto kamery", Toast.LENGTH_SHORT).show();
                    return;
                }
                openCamera();
                button.setText(R.string.measurement_button_end);
                isRecording = true;
            } catch (Exception e) {
                Toast.makeText(this, "Błąd nagrywania", Toast.LENGTH_SHORT).show();
                Log.e("Start recording error: ", e.toString());
            }
        } else {
            stopRecording();
            button.setText(R.string.measurement_button_start);
            isRecording = false;

        }
    }


    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                return;
            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            Log.e("Camera open error: ", e.toString());
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            if (isRecording) {
                startRecordingSession();
            } else {
                startPreviewSession();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private void startRecordingSession() {
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(this::analyzeImage, null);

            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(mediaRecorder.getSurface());
            builder.addTarget(surfaceHolder.getSurface());
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

            cameraDevice.createCaptureSession(
                    Arrays.asList(mediaRecorder.getSurface(), surfaceHolder.getSurface(), imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(builder.build(), null, null);
                                startTime = System.nanoTime();
                                mediaRecorder.start();
                            } catch (CameraAccessException e) {
                                Log.e("Recording session error: ", e.toString());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(MainActivity.this, "Błąd konfiguracji sesji", Toast.LENGTH_SHORT).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            Log.e("Camera access error: ", e.toString());
        }
    }

    private void startPreviewSession() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surfaceHolder.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            cameraDevice.createCaptureSession(
                    Collections.singletonList(surfaceHolder.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(builder.build(), null, null);
                            } catch (CameraAccessException e) {
                                Log.e("Preview session error: ", e.toString());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(MainActivity.this, "Błąd konfiguracji podglądu", Toast.LENGTH_SHORT).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            Log.e("Preview error: ", e.toString());
        }
    }

    private void prepareMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        String fn = "video_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4";
        ContentValues v = new ContentValues();
        v.put(MediaStore.Video.Media.DISPLAY_NAME, fn);
        v.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        v.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ppg_better");

        Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new IOException("Nie można utworzyć pliku video");

        FileDescriptor fd = Objects.requireNonNull(getContentResolver().openFileDescriptor(uri, "w")).getFileDescriptor();
        mediaRecorder.setOutputFile(fd);

        mediaRecorder.setVideoEncodingBitRate(10_000_000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.prepare();
    }

    private void stopRecording() {
        try {
            captureSession.stopRepeating();
            captureSession.abortCaptures();
            mediaRecorder.stop();
            mediaRecorder.release();
            cameraDevice.close();
            cameraDevice = null;
            if(imageReader != null) {
                imageReader.close();
            }
            Toast.makeText(this, "Wideo zapisane!", Toast.LENGTH_SHORT).show();

            openCamera();
        } catch (Exception e) {
            Log.e("Stop recording error", e.toString());
        }
    }

    private void analyzeImage(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image == null) return;

        long now = System.nanoTime();
        timer = (now - startTime) / 1_000_000_000.0;
        Log.d("TIMER", "Time since start: " + timer + " s");


        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        int pixelStride = yPlane.getPixelStride();
        int rowStride = yPlane.getRowStride();

        // Tworzymy bitmapę do wyświetlenia
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Tablica do tymczasowego przechowania luma -> pseudozieleni
        int[] pixels = new int[width * height];

        yBuffer.rewind();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int yIndex = row * rowStride + col * pixelStride;
                if (yIndex >= yBuffer.limit()) continue;
                int y = yBuffer.get(yIndex) & 0xFF;
                int greenColor = (0xFF << 24) | (0 << 16) | (y << 8) | 0;
                pixels[row * width + col] = greenColor;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        // Wyświetl na greenHolder
        Canvas canvas = greenHolder.lockCanvas();
        if (canvas != null) {
//            canvas.drawBitmap(bitmap, 0, 0, null);
            canvas.drawBitmap(bitmap, null, greenHolder.getSurfaceFrame(), null);
            greenHolder.unlockCanvasAndPost(canvas);
        }

        // Średnia jasność jako próbka sygnału
        int sum = 0;
        int count = 0;
        yBuffer.rewind();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int yIndex = row * rowStride + col * pixelStride;
                if (yIndex >= yBuffer.limit()) continue;
                int y = yBuffer.get(yIndex) & 0xFF;
                sum += y;
                count++;
            }
        }
        double average = sum / (double) count;

        if (greenSamples.size() >= fftSize) {
            greenSamples.poll();
            sampleTimestamps.poll();
        }
        greenSamples.offer(average);
        sampleTimestamps.offer(timer);

        image.close();

        if (greenSamples.size() >= fftSize) {
            var bpm = computeHeartRate();
            runOnUiThread(() -> heartRateTextView.setText("HR: " + bpm + " BPM"));
        }
        else {
            //TODO: this is not necessery but it should display in remaining seconds
            int remainingSamples = fftSize - greenSamples.size();
            runOnUiThread(() ->
                    heartRateTextView.setText(String.format("%d more samples", remainingSamples)));
        }
    }


    private int computeHeartRate() {
        if (greenSamples.size() != sampleTimestamps.size() || greenSamples.size() < fftSize) {
            return 0;
        }

        // Zbieramy próbki i ich czasy
        List<Double> values = new ArrayList<>(greenSamples);
        List<Double> times = new ArrayList<>(sampleTimestamps);

        int N = fftSize;
        double startTime = times.get(0);
        double endTime = times.get(N - 1);
        double duration = endTime - startTime;

        // Ustal równomierną siatkę czasową
        double[] uniformTime = new double[N];
        double dt = duration / (N - 1);
        for (int i = 0; i < N; i++) {
            uniformTime[i] = startTime + i * dt;
        }

        // Interpolacja liniowa sygnału do równych odstępów czasowych
        double[] interpolated = new double[N];
        int j = 0;
        for (int i = 0; i < N; i++) {
            double t = uniformTime[i];
            while (j < N - 2 && times.get(j + 1) < t) {
                j++;
            }
            double t1 = times.get(j);
            double t2 = times.get(j + 1);
            double v1 = values.get(j);
            double v2 = values.get(j + 1);
            double alpha = (t - t1) / (t2 - t1);
            interpolated[i] = v1 + alpha * (v2 - v1);
        }

        // Zastosuj filtrację i okno Hamming
        Double[] interpolatedObj = Arrays.stream(interpolated).boxed().toArray(Double[]::new);
        Double[] movingAvgSamples = applyMovingAverage(interpolatedObj, 5);

        double[] filteredHigh = fftLib.highPassFilter(movingAvgSamples, 1.0 / dt, 0.8); // cutoff = 0.8 Hz
        double[] filtered = fftLib.lowPassFilter(filteredHigh, 1.0 / dt, 2.5);           // cutoff = 2.5 Hz
        fftLib.applyHammingWindow(filtered);

        // Przygotuj dane do FFT
        double[][] fftInput = new double[N][2];
        for (int i = 0; i < N; i++) {
            fftInput[i][0] = filtered[i];
            fftInput[i][1] = 0.0;
        }

        // Oblicz energię FFT
        double[] energy = fftLib.fft_energy_squared(fftInput, N);

        double sampleRate = 1.0 / dt; // rzeczywista częstotliwość próbkowania po interpolacji

        // Znajdź dominującą częstotliwość w zakresie 0.8–2.5 Hz (48–150 BPM)
        double minFreq = 0.8;
        double maxFreq = 2.5;
        int start = (int) (minFreq * N / sampleRate);
        int end = Math.min((int) (maxFreq * N / sampleRate), energy.length);

        int maxIndex = start;
        for (int i = start + 1; i < end; i++) {
            if (energy[i] > energy[maxIndex]) {
                maxIndex = i;
            }
        }

        double peak = energy[maxIndex];
        double avgSurrounding = 0.0;
        int count = 0;
        for (int i = maxIndex - 3; i <= maxIndex + 3; i++) {
            if (i >= start && i < end && i != maxIndex) {
                avgSurrounding += energy[i];
                count++;
            }
        }
        avgSurrounding /= count;

        if (peak < avgSurrounding * 1.3) {
            Log.d("HR", "Peak too weak: " + peak + " vs avg: " + avgSurrounding);
            return lastBpm > 0 ? lastBpm : 0;
        }

        double frequency = (double) maxIndex * sampleRate / N;
        int bpm = (int) (frequency * 60);

        if (lastBpm > 0) {
            int maxDelta = 10;
            bpm = Math.max(lastBpm - maxDelta, Math.min(lastBpm + maxDelta, bpm));
        }
        lastBpm = bpm;

        // Dodaj do bufora wygładzania
        recentBpms.add(bpm);
        if (recentBpms.size() > bpmWindowSize) {
            recentBpms.removeFirst();
        }

        int smoothedBpm = 0;
        for (int val : recentBpms) {
            smoothedBpm += val;
        }
        smoothedBpm /= recentBpms.size();

        Log.d("HR", "Samples duration: " + duration + " s");
        Log.d("HR", "Interpolated sampleRate: " + sampleRate + " Hz");
        Log.d("HR", "Max FFT index: " + maxIndex + " -> frequency: " + frequency + " Hz");
        Log.d("HR", "Computed BPM: " + bpm + " | Smoothed BPM: " + smoothedBpm);

        return smoothedBpm;
    }



}
