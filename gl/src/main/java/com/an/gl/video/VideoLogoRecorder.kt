package com.an.gl.video

import android.content.Context
import android.opengl.GLES31
import android.util.Log
import android.view.Surface
import com.an.gl.R
import com.an.gl.base.*
import com.an.gl.base.EglCore.FLAG_TRY_GLES3
import com.an.gl.shader.CameraShader
import com.an.gl.shader.LogoShader
import com.an.gl.shader.ScreenShader
import java.io.File
import java.util.concurrent.TimeUnit


class VideoLogoRecorder(
    val context: Context,
    private val fromFile: File,
    private val outFile: File
) {

    companion object {
        const val TAG = "VideoLogoRecorder"
    }

    private lateinit var moviePlayer: MoviePlayer
    private lateinit var movieEncoder: VideoEncoderCore
    private lateinit var egl: EglSurfaceBase
    private lateinit var videoShader: CameraShader
    private lateinit var logoShader: LogoShader
    private lateinit var screenShader: ScreenShader
    private var width: Int = 0
    private var height: Int = 0

    init {
        initVideo()
        initEGL(movieEncoder.inputSurface)
    }

    var firstPresentationTimeUsec: Long = 0

    private fun initVideo() {
        val callback = object : MoviePlayer.FrameCallback {
            override fun preRender(presentationTimeUsec: Long) {
                egl.setPresentationTime(firstPresentationTimeUsec)
                firstPresentationTimeUsec += TimeUnit.MILLISECONDS.toNanos((1f / 30f * 1000).toLong()) //30帧每秒
            }

            override fun postRender(over: Boolean) {
                if (over) {
                    movieEncoder.release()
                } else {
                    onDrawFrame()
                }
            }

            override fun loopReset() {
                Log.d(TAG, "loopReset")
            }
        }
        moviePlayer = MoviePlayer(fromFile, null, callback)
        width = moviePlayer.videoWidth
        height = moviePlayer.videoHeight

        val bitRate = width * height
        movieEncoder = VideoEncoderCore(width, height, bitRate, outFile)
    }

    /**
     * @param inputSurface 把内容渲染到该Surface中
     * **/
    private fun initEGL(inputSurface: Surface) {
        val eglCore = EglCore(null, FLAG_TRY_GLES3)
        egl = EglSurfaceBase(eglCore)
        egl.createWindowSurface(inputSurface)
        egl.makeCurrent()
        onSurfaceCreated()
        onSurfaceChanged(width, height)
    }

    /**
     * 初始化配置
     * **/
    private fun onSurfaceCreated() {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        val frameBufferObject = FboManager()
        videoShader = CameraShader(context, frameBufferObject)
        //绑定解压出来的视频数据帧
        moviePlayer.setOutputSurface(videoShader.surface)
        logoShader = LogoShader(context, R.drawable.watermark, frameBufferObject)
        screenShader = ScreenShader(context)
    }

    /**
     * 尺寸更改
     * **/
    private fun onSurfaceChanged(width: Int, height: Int) {
        videoShader.onSizeChange(width, height)
        logoShader.onSizeChange(width, height)
        screenShader.onSizeChange(width, height)
    }

    /**
     * 绘制
     * **/
    private fun onDrawFrame() {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        var textureId = videoShader.getTextureId()
        textureId = videoShader.onDrawFrame(textureId)
        textureId = logoShader.onDrawFrame(textureId)
        screenShader.onDrawFrame(textureId)
        egl.swapBuffers()
        movieEncoder.drainEncoder(false)
    }

    fun start() {
        moviePlayer.play()
    }
}