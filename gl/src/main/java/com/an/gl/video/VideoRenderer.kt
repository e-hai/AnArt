package com.an.gl.video

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.view.Surface
import com.an.gl.R
import com.an.gl.base.FboManager
import com.an.gl.shader.CameraShader
import com.an.gl.shader.LogoShader
import com.an.gl.shader.ScreenShader
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRenderer(private val context: Context, private val onRequestRender: () -> Unit) :
    GLSurfaceView.Renderer {

    private lateinit var videoShader: CameraShader
    private lateinit var logoShader: LogoShader
    private lateinit var screenShader: ScreenShader
    private var surfaceRequest: SurfaceRequest? = null

    fun setSurfaceRequest(request: SurfaceRequest) {
        surfaceRequest = request
    }

    /**
     * 初始化配置
     * **/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        val frameBufferObject = FboManager()
        videoShader = CameraShader(context, frameBufferObject)
        logoShader = LogoShader(context, R.drawable.app_name, frameBufferObject)
        screenShader = ScreenShader(context)
        videoShader.surfaceTexture.setOnFrameAvailableListener {
            onRequestRender()
        }
        provideSurfaceRequest(videoShader.surface)
    }


    /**
     * 尺寸更改
     * **/
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        videoShader.onSizeChange(width, height)
        logoShader.onSizeChange(width, height)
        screenShader.onSizeChange(width, height)
    }

    /**
     * 绘制
     * **/
    override fun onDrawFrame(gl: GL10) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        var textureId = videoShader.getTextureId()
        textureId = videoShader.onDrawFrame(textureId)
        textureId = logoShader.onDrawFrame(textureId)
        screenShader.onDrawFrame(textureId)
    }

    private fun provideSurfaceRequest(surface: Surface) {
        val surfaceRequest = surfaceRequest ?: return
        surfaceRequest.provideSurface(surface)
    }

    interface SurfaceRequest {
        fun provideSurface(surface: Surface)
    }
}