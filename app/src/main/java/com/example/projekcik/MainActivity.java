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


    private final BlockingQueue<Double> greenSamples = new ArrayBlockingQueue<>(512);
    private TextView heartRateTextView;
    private final int fftSize = 256;

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
            canvas.drawBitmap(bitmap, 0, 0, null);
            greenHolder.unlockCanvasAndPost(canvas);
        }

        // Średnia jasność jako próbka sygnału
        int sum = 0;
        int count = pixels.length;
        for (int val : pixels) {
            int green = (val >> 8) & 0xFF;
            sum += green;
        }
        double average = sum / (double) count;

        if (greenSamples.remainingCapacity() == 0) {
            greenSamples.poll();
        }
        greenSamples.offer(average);

        image.close();

        if (greenSamples.size() >= fftSize) {
            computeHeartRate();
        }
    }


    private void computeHeartRate() {
        double[][] fftInput = new double[fftSize][2];
        Double[] samplesArray = greenSamples.toArray(new Double[0]);
        for (int i = 0; i < fftSize; i++) {
            fftInput[i][0] = samplesArray[i]; // Real part
            fftInput[i][1] = 0.0;             // Imaginary part
        }

        double[] energy = fftLib.fft_energy_squared(fftInput, fftSize);

        // Szukaj dominującej częstotliwości w zakresie 0.66–3.33 Hz (40–200 BPM)
        // Hz
        int sampleRate = 30;
        int start = (int) (0.66 * fftSize / sampleRate);
        int end = (int) (3.33 * fftSize / sampleRate);
        int maxIndex = start;
        for (int i = start + 1; i < end; i++) {
            if (energy[i] > energy[maxIndex]) {
                maxIndex = i;
            }
        }

        double frequency = (double) maxIndex * sampleRate / fftSize;
        int bpm = (int) (frequency * 60);

        Log.d("HR", "Samples size: " + samplesArray.length);

        for (int i = 0; i < 10; i++) {
            Log.d("HR", "Sample[" + i + "]: " + samplesArray[i]);
        }

        Log.d("HR", "Max FFT index: " + maxIndex + " -> frequency: " + frequency + " Hz");
        Log.d("HR", "Computed BPM: " + bpm);


        runOnUiThread(() -> heartRateTextView.setText("HR: " + bpm + " BPM"));
    }


}
