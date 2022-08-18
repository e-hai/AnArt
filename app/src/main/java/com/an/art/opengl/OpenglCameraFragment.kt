package com.an.art.opengl

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.an.art.MainActivity
import com.an.art.PermissionListener
import com.an.art.PermissionsFragment
import com.an.art.databinding.FragmentCameraBinding
import com.an.gl.usercase.camera.CameraView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.io.File
import java.util.concurrent.Executor

class OpenglCameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var cameraExecutor: Executor
    private lateinit var imageCapture: ImageCapture
    private lateinit var detector: FaceDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        cameraExecutor = ContextCompat.getMainExecutor(context)
        PermissionsFragment.load(this)
            .requestPermissions(arrayOf(Manifest.permission.CAMERA), object : PermissionListener {
                override fun invoke(isGranted: Boolean) {
                    if (isGranted) {
                        initML()
                        initCamera()
                    }
                }
            })
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

    private fun initCamera() {
        val context = context ?: return
//        ProcessCameraProvider.getInstance(context).apply {
//            addListener(
//                {
//                    val cameraProvider = get() ?: return@addListener
//                    bindPreview(binding.previewView, cameraProvider)
//                },
//                cameraExecutor
//            )
//        }

    }


    private fun bindPreview(previewView: CameraView, cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            // enable the following line if RGBA output is needed.
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(
            cameraExecutor
        ) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // insert your code here.
            val alpha = imageProxy.planes[0].buffer[0]
            val red = imageProxy.planes[0].buffer[1]
            val green = imageProxy.planes[0].buffer[2]
            val blue = imageProxy.planes[0].buffer[3]

            //ml detector
            detectorByML(imageProxy)
        }

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(previewView.display.rotation)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
//            .addUseCase(imageAnalysis)
            .addUseCase(imageCapture)
            .build()

        cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            useCaseGroup
        )
        Log.d(MainActivity.TAG, "INIT")
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun detectorByML(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (null == mediaImage) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.size > 0) {
                    Log.d(MainActivity.TAG, "face size=${faces.size}")
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
                    getFacePoints(image, bounds).let {
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun getFacePoints(image: InputImage, bounds: Rect): FloatArray {
        val imageRectF = RectF().apply {
            left = 0f
            right = image.height.toFloat()
            top = 0f
            bottom = image.width.toFloat()
        }
        val faceRectF = RectF(bounds)
        //转换成Opengl的标准化坐标
        return matrixPoints(faceRectF, imageRectF)
    }

    private fun matrixPoints(srcRectF: RectF, mapRectF: RectF): FloatArray {
        val dstRectF = RectF()
        val matrix = Matrix()
        matrix.setTranslate(-mapRectF.centerX(), -mapRectF.centerY())
        matrix.mapRect(dstRectF, srcRectF)
        matrix.setScale(1f / (mapRectF.width() / 2f), 1f / (mapRectF.height() / 2f))
        matrix.mapRect(dstRectF, dstRectF)
        matrix.setScale(1f, -1f)
        matrix.mapRect(dstRectF, dstRectF)
        Log.d(
            MainActivity.TAG,
            "scale $dstRectF}"
        )

        return floatArrayOf(
            dstRectF.left, dstRectF.top,    //左上角
            dstRectF.left, dstRectF.bottom, //左下角
            dstRectF.right, dstRectF.bottom,//右下角
            dstRectF.right, dstRectF.top    //右上角
        )
    }

    fun clickTakePicture() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File("")).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    // insert your code here.
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // insert your code here.
                }
            })
    }
}