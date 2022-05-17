package com.an.gl.base

import android.content.Context
import android.opengl.EGLContext
import com.an.gl.base.EglCore.FLAG_TRY_GLES3
import com.an.gl.shader.ScreenShader
import java.io.File

class MovieRecorder(val context: Context) {

    private lateinit var encoder: VideoEncoderCore
    private lateinit var egl: EglSurfaceBase
    private lateinit var screenShader: ScreenShader


    fun onCreate(eglContext: EGLContext, videoConfig: VideoConfig) {
        encoder = VideoEncoderCore(
            videoConfig.width,
            videoConfig.height,
            10000,
            videoConfig.frameRate,
            videoConfig.outFile
        )
        egl = EglSurfaceBase(EglCore(eglContext, FLAG_TRY_GLES3))
        egl.createWindowSurface(encoder.inputSurface)
        egl.makeCurrent()
        screenShader = ScreenShader(context)
        screenShader.onSizeChange(videoConfig.width, videoConfig.height)
    }

    fun drawFrame(dstTextureId: Int, timestamp: Long) {
        egl.makeCurrent()
        screenShader.onDrawFrame(dstTextureId)
        egl.setPresentationTime(timestamp)
        egl.swapBuffers()
        encoder.drainEncoder(false)
    }

    data class VideoConfig(val width: Int, val height: Int, val frameRate: Int, val outFile: File)
}