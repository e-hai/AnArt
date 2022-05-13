package com.an.gl.camera

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import androidx.annotation.UiThread
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import com.an.gl.R
import com.an.gl.base.FboManager
import com.an.gl.shader.CameraShader
import com.an.gl.shader.LogoShader
import com.an.gl.shader.ScreenShader
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRender(private val context: Context, private val onRequestRender: () -> Unit) :
    GLSurfaceView.Renderer {

    private var surfaceRequest: SurfaceRequest? = null
    private lateinit var cameraShader: CameraShader
    private lateinit var logoShader: LogoShader
    private lateinit var screenShader: ScreenShader

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
        gl?.glGetString(GL10.GL_VERSION).also {
            Log.d(TAG, "Version: $it")
        }
        val frameBufferObject = FboManager()
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        screenShader = ScreenShader(context)
        logoShader = LogoShader(context, R.drawable.app_name, frameBufferObject)
        cameraShader = CameraShader(context, frameBufferObject)
        cameraShader.surfaceTexture.setOnFrameAvailableListener {
            onRequestRender()
        }
        provideSurfaceRequest(cameraShader.surface)
    }


    /**
     * 绘制
     * **/
    override fun onDrawFrame(gl: GL10) {
        // Redraw background color
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        var textureId = cameraShader.getTextureId()
        textureId = cameraShader.onDrawFrame(textureId)
        textureId = logoShader.onDrawFrame(textureId)
        screenShader.onDrawFrame(textureId)
    }

    /**
     * 尺寸更改
     * **/
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        screenShader.onSizeChange(width, height)
        cameraShader.onSizeChange(width, height)
        logoShader.onSizeChange(width, height)
    }


    private fun provideSurfaceRequest(surface: Surface) {
        val surfaceRequest = surfaceRequest ?: return
        surfaceRequest.provideSurface(
            surface, ContextCompat.getMainExecutor(context)
        ) {
            it.surface.release()
        }
    }


    companion object {
        const val TAG = "CameraRender"
    }
}