package com.an.gl.camera

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class GLPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var surfaceView: GLSurfaceView

    var facePoints: FloatArray? = null


    val surfaceProvider = Preview.SurfaceProvider { request ->
        gLRenderer.setSurfaceRequest(request)
        surfaceView = GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(gLRenderer)
            //设置刷新渲染模式为: 调用requestRender()时刷新
            renderMode = RENDERMODE_WHEN_DIRTY
        }
        val ratio = request.resolution.width.toFloat() / request.resolution.height.toFloat()
        val width = width
        val height = (width.toFloat() * ratio).toInt()
        Log.d(
            TAG, " ratio=$ratio " +
                    "w=${width} " +
                    "h=${height}"
        )
        surfaceView.layoutParams = LayoutParams(width, height)
            .apply { gravity = Gravity.CENTER }
        addView(surfaceView)
    }

    private val gLRenderer = object : GLSurfaceView.Renderer {

        private var surfaceRequest: SurfaceRequest? = null
        private lateinit var camareTextures: CamareTexturesShader
        private lateinit var faceDetection: FaceDetectionShader


        fun setSurfaceRequest(request: SurfaceRequest) {
            cancelPreviousRequest()
            surfaceRequest = request
        }

        @UiThread
        private fun cancelPreviousRequest() {
            surfaceRequest?.willNotProvideSurface()
        }

        /**
         * 初始化配置
         * **/
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            Log.d(TAG, "onSurfaceCreated()=${Thread.currentThread().name}")
            gl?.glGetString(GL10.GL_VERSION).also {
                Log.d(TAG, "Version: $it")
            }
            // Set the background frame color
            GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            camareTextures = CamareTexturesShader(context)
            camareTextures.setOnFrameAvailableListener {
                Log.d(TAG, "OnFrame: ${Thread.currentThread().name}")

                surfaceView.requestRender()
            }
            provideSurfaceRequest()

            faceDetection = FaceDetectionShader(context)
        }


        /**
         * 绘制
         * **/
        override fun onDrawFrame(gl: GL10) {
            // Redraw background color
            GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
            facePoints?.let {
                Log.d(TAG, "$facePoints")
                faceDetection.setFacesPoint(it)
            }
            camareTextures.draw()
            faceDetection.draw()
        }

        /**
         * 尺寸更改
         * **/
        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            Log.d(TAG, "w=$width h=$height")
            GLES31.glViewport(0, 0, width, height)
        }


        private fun provideSurfaceRequest() {
            val surfaceRequest = surfaceRequest ?: return
            Log.d(
                TAG, " provideSurfaceRequest " +
                        "w=${surfaceRequest.resolution.width} " +
                        "h=${surfaceRequest.resolution.height}"
            )

            camareTextures.setDefaultBufferSize(
                surfaceRequest.resolution.width,
                surfaceRequest.resolution.height
            )
            val surface = camareTextures.surface
            // Provide the surface and wait for the result to clean up the surface.
            surfaceRequest.provideSurface(
                surface, ContextCompat.getMainExecutor(surfaceView.context)
            ) {
                it.surface.release()
                camareTextures.release()
            }
        }
    }


    companion object {
        const val TAG = "GLPreviewView"
    }
}


