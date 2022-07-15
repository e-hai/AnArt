package com.an.art.demo_opencv

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.an.file.FileManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC3
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import org.opencv.imgproc.Subdiv2D
import java.io.File
import kotlin.time.Duration

class FaceDetectViewModel(app: Application) : AndroidViewModel(app) {

    private lateinit var detector: FaceDetector
    private lateinit var personSrcParams: FaceSrcParams
    private lateinit var animalSrcParams: FaceSrcParams

    val drawFacePoint = MutableStateFlow(FaceStandardPoints())
    val transformResult = MutableStateFlow(FaceTransformResult())

    init {
        initML()
    }

    private fun initML() {
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(highAccuracyOpts)
    }

    fun detectorByML(srcPhoto: Uri) {
        val context = getApplication<Application>()
        val fileName = System.currentTimeMillis().toString()
        val fileInputStream = context.contentResolver.openInputStream(srcPhoto) ?: return
        val photoFile = FileManager.specificStorage(context)
            .savePicture(fileName, fileInputStream)
            .toFile()
        detector(photoFile)
    }


    /**
     * For face detection, you should use an image with dimensions of at least 480x360 pixels.
     * */
    private fun detector(srcPhoto: File) {
        val image = InputImage.fromFilePath(getApplication(), srcPhoto.toUri())
        val width = image.width.toFloat()
        val height = image.height.toFloat()
        Log.d(TAG, "w=$width h=$height")

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

                    processFace(srcPhoto, width, height, face)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun processFace(
        srcPhoto: File,
        photoWidth: Float,
        photoHeight: Float,
        face: Face
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val contours = getFaceContours(face)
            transformFace(srcPhoto, contours)
            emitDrawFacePoints(contours, photoWidth, photoHeight)
        }
    }

    private suspend fun transformFace(srcPhoto: File, contours: List<PointF>) {
        val delaunay = generateDelaunay(srcPhoto, contours)
        if (!this::personSrcParams.isInitialized) {
            personSrcParams = FaceSrcParams(srcPhoto, contours, delaunay)
        } else if (!this::animalSrcParams.isInitialized) {
            animalSrcParams = FaceSrcParams(srcPhoto, contours, delaunay)
        }
        if (this::personSrcParams.isInitialized && this::animalSrcParams.isInitialized) {
            generateMorphing(
                personSrcParams.photo,
                personSrcParams.contours,
                animalSrcParams.photo,
                animalSrcParams.contours,
                personSrcParams.delaunay.toArray()
            )
        }
    }

    private suspend fun emitDrawFacePoints(
        contours: List<PointF>,
        photoWidth: Float,
        photoHeight: Float
    ) {
        val standardPoints = convertStandard(contours, photoWidth, photoHeight)
        val pointResult = FaceStandardPoints(standardPoints)
        drawFacePoint.emit(pointResult)
    }

    private fun getFaceContours(face: Face): List<PointF> {
        val points = mutableListOf<PointF>()
        face.allContours.forEach {
            it.points.forEach { point ->
                points.add(point)
            }
        }
//        //脸轮廓
//        face.getContour(FaceContour.FACE)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //左眼眉
//        face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //右眼眉
//        face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //眼睛
//        face.getContour(FaceContour.LEFT_EYE)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.RIGHT_EYE)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //鼻子
//        face.getContour(FaceContour.NOSE_BRIDGE)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.NOSE_BOTTOM)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //上唇
//        face.getContour(FaceContour.UPPER_LIP_TOP)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //下唇
//        face.getContour(FaceContour.LOWER_LIP_TOP)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//
//        //脸颊
//        face.getContour(FaceContour.LEFT_CHEEK)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
//        face.getContour(FaceContour.RIGHT_CHEEK)?.points?.forEach {
//            points.add(PointF(it.x, it.y))
//        }
        return points
    }

    /**
     * 把关键点的坐标转换成标准坐标（0-1）
     * **/
    private fun convertStandard(
        facePoints: List<PointF>,
        width: Float,
        height: Float
    ): List<PointF> {
        facePoints.forEach {
            it.x = it.x / width
            it.y = it.y / height
        }
        return facePoints
    }

    private fun generateDelaunay(photoFile: File, points: List<PointF>): MatOfFloat6 {
        val mat = Imgcodecs.imread(photoFile.absolutePath)
        val size = mat.size()
        val subdiv2D = Subdiv2D().apply {
            initDelaunay(Rect(0, 0, size.width.toInt(), size.height.toInt()))
        }
        val triangleList = MatOfFloat6()
        points.forEach {
            val item = Point(it.x.toDouble(), it.y.toDouble())
            Log.d(TAG, "item=$item")
            subdiv2D.insert(item)
        }
        subdiv2D.getTriangleList(triangleList)

        return triangleList
    }

    private suspend fun generateMorphing(
        personPhoto: File,
        personContours: List<PointF>,
        animalPhoto: File,
        animalContours: List<PointF>,
        triangles: FloatArray
    ) {
        val img1 = Imgcodecs.imread(personPhoto.absolutePath)
        val img2 = Imgcodecs.imread(animalPhoto.absolutePath)

        cvtColor(img1, img1, COLOR_RGBA2RGB)
        cvtColor(img2, img2, COLOR_RGBA2RGB)

        triangles.forEachIndexed { index, fl ->
            if (0 == index % 6) {
                triangleMorph(img1, triangles.toList().subList(index, index + 6))
                delay(1 * 1000)
            }
        }
    }

    private fun triangleMorph(img: Mat, triangle: List<Float>) {
        val rect = boundingRect(
            MatOfPoint().apply {
                fromArray(
                    Point(triangle[0].toDouble(), triangle[1].toDouble()),
                    Point(triangle[2].toDouble(), triangle[3].toDouble()),
                    Point(triangle[4].toDouble(), triangle[5].toDouble())
                )
            })
        val mask = Mat.zeros(rect.width, rect.height, CV_8UC3)
        val trianglePointInMask = MatOfPoint().also {
            it.fromArray(
                Point((triangle[0] - rect.x).toDouble(), (triangle[1] - rect.y).toDouble()),
                Point((triangle[2] - rect.x).toDouble(), (triangle[3] - rect.y).toDouble()),
                Point((triangle[4] - rect.x).toDouble(), (triangle[5] - rect.y).toDouble())
            )
        }
        fillConvexPoly(mask, trianglePointInMask, Scalar(255.0, 182.0, 193.0))
//        mask.setTo(Scalar(255.0, 182.0, 193.0))
        val bitmap = Bitmap.createBitmap(rect.height, rect.width, Bitmap.Config.RGB_565)
        matToBitmap(mask, bitmap)

        viewModelScope.launch {
            transformResult.emit(FaceTransformResult(bitmap))
        }
    }


    companion object {
        const val TAG = "FaceDetectViewModel"
    }
}


/**
 * 待变换的参数
 * **/
data class FaceSrcParams(
    val photo: File,
    val contours: List<PointF>,
    val delaunay: MatOfFloat6
)

/**
 * 变换生成的结果
 * **/
data class FaceTransformResult(
    val bitmap: Bitmap? = null
)

/**
 * 标准化的关键点
 * **/
data class FaceStandardPoints(
    val points: List<PointF> = emptyList()
)