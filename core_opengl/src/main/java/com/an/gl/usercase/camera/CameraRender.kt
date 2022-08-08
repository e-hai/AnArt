package com.an.gl.usercase.camera

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import androidx.annotation.UiThread
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import com.an.gl.R
import com.an.gl.base.MediaEglManager
import com.an.gl.usercase.WatermarkConfig
import com.an.gl.usercase.WatermarkDraw
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRender(private val context: Context, private val onRequestRender: () -> Unit) :
    GLSurfaceView.Renderer {

    private var surfaceRequest: SurfaceRequest? = null
    private lateinit var mediaEglManager: MediaEglManager
    private lateinit var watermarkDraw: WatermarkDraw

    fun setSurfaceRequest(request: SurfaceRequest) {
        cancelPreviousRequest()
        surfaceRequest = request
    }

    @UiThread
    private fun cancelPreviousRequest() {
        surfaceRequest?.willNotProvideSurface()
    }

    private fun provideSurfaceRequest(surface: Surface) {
        val surfaceRequest = surfaceRequest ?: return
        surfaceRequest.provideSurface(
            surface, ContextCompat.getMainExecutor(context)
        ) {
            it.surface.release()
        }
    }

    /**
     * 初始化配置
     * **/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        gl?.glGetString(GL10.GL_VERSION).also {
            Log.d(TAG, "Version: $it")
        }

        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        mediaEglManager = MediaEglManager().apply {
            setOnFrameAvailableListener {
                onRequestRender()
            }
            provideSurfaceRequest(surface)
        }
//        watermarkDraw = WatermarkDraw(context, WatermarkConfig(R.drawable.watermark))
    }

    /**
     * 尺寸更改
     * **/
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mediaEglManager.onSizeChange(width, height)
        watermarkDraw.onSizeChange(width, height)
    }


    /**
     * 绘制
     * **/
    override fun onDrawFrame(gl: GL10) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        mediaEglManager.onDraw {
            watermarkDraw.onDraw()
        }
    }


    companion object {
        const val TAG = "CameraRender"
    }
}