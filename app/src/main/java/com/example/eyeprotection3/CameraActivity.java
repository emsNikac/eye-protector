package com.example.eyeprotection3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.common.InputImage;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            setupCamera();
        }
    }

    private void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 960))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            detectFace(imageProxy);
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void detectFace(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        Log.d(TAG, "Image processed for face detection");

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        Log.d(TAG, "No faces detected");
                    } else {
                        Log.d(TAG, faces.size() + " faces detected");
                        for (Face face : faces) {
                            float distanceToScreen = calculateDistanceToScreen(face);
                            if (distanceToScreen < 25.0f) { // Check if distance is less than 25 cm
                                handleCloseFaceDetection();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed: ", e);
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                });
    }

    private void handleCloseFaceDetection() {
        // Start the service to handle notifications
        Intent serviceIntent = new Intent(this, FaceMonitoringService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private float calculateDistanceToScreen(Face face) {
        float faceWidth = face.getBoundingBox().width();
        float averageFaceWidth = 15.0f; // average face width in cm

        // Calculate the distance to the screen
        float distanceToScreen = (averageFaceWidth * 1280) / faceWidth;

        // Apply correction factor to adjust the measurement
        float correctionFactor = 13.0f; // Adjust as necessary based on testing
        return distanceToScreen + correctionFactor;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
