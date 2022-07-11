package com.an.art

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

class FaceDetectViewModel(app: Application) : AndroidViewModel(app) {

    private lateinit var detector: FaceDetector

    init {
        initML()
    }

    private fun initML() {
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(realTimeOpts)
    }

    fun detectorByML(photoUri: Uri) {
        val image = InputImage.fromFilePath(getApplication(), photoUri)
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.size > 0) {
                    Log.d(TAG, "face size=${faces.size}")
                }

                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                    leftEar?.let {
                        val leftEarPos = leftEar.position
                    }

                    // If contour detection was enabled:
                    val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
                    val upperLipBottomContour =
                        face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points

                    // If classification was enabled:
                    if (face.smilingProbability != null) {
                        val smileProb = face.smilingProbability
                    }
                    if (face.rightEyeOpenProbability != null) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                    }

                    // If face tracking was enabled:
                    if (face.trackingId != null) {
                        val id = face.trackingId
                    }
                    Log.d(TAG,"$face")
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
            }
    }


    companion object{
        const val TAG="FaceDetectViewModel"
    }
}