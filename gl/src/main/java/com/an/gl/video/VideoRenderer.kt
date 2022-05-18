package com.an.gl.video

import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.os.FileUtils
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceView
import android.widget.FrameLayout
import com.an.gl.R
import com.an.gl.base.*
import com.an.gl.shader.CameraShader
import com.an.gl.shader.LogoShader
import com.an.gl.shader.ScreenShader
import com.an.gl.util.FileUtil
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private val videoFile: File = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")
    private val saveFile: File = FileUtil.createFile(context, "456.mp4")
    private lateinit var videoShader: CameraShader
    private lateinit var logoShader: LogoShader
    private lateinit var screenShader: ScreenShader

    private lateinit var moviePlayer: MoviePlayer

    var callBack: CallBack? = null

    init {
        CameraShader(context, FboManager())
    }

    /**
     * 初始化配置
     * **/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        val frameBufferObject = FboManager()
        videoShader = CameraShader(context, frameBufferObject)
        val surfaceTexture = videoShader.surfaceTexture
        surfaceTexture.setOnFrameAvailableListener {
            callBack?.onRequestRender()
        }
        logoShader = LogoShader(context, R.drawable.watermark, frameBufferObject)
        screenShader = ScreenShader(context)
        initVideo(videoShader.surface)
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


    private fun initVideo(surface: Surface) {
        moviePlayer = MoviePlayer(videoFile, surface, object :MoviePlayer.FrameCallback{
            override fun preRender(presentationTimeUsec: Long) {
                TODO("Not yet implemented")
            }

            override fun postRender(over: Boolean) {
                TODO("Not yet implemented")
            }

            override fun loopReset() {
                TODO("Not yet implemented")
            }
        })
        callBack?.onVideoSize(moviePlayer.videoWidth, moviePlayer.videoHeight)
        MoviePlayer.PlayTask(moviePlayer, null).execute()
    }

    interface CallBack {
        fun onVideoSize(videoWidth: Int, videoHeight: Int)
        fun onRequestRender()
    }
}