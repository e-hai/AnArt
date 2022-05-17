package com.an.gl.video

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.widget.FrameLayout
import com.an.gl.base.MoviePlayer
import com.an.gl.base.SpeedControlCallback
import com.an.gl.util.FileUtil
import java.io.File

class VideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var surfaceView: GLSurfaceView

    init {
        initSurfaceView()
    }

    private fun initSurfaceView() {
        surfaceView = GLSurfaceView(context)
        surfaceView.setEGLContextClientVersion(3)
        addView(surfaceView)

        val renderer = VideoRenderer(context)
        renderer.callBack = object : VideoRenderer.CallBack {
            override fun onVideoSize(videoWidth: Int, videoHeight: Int) {
                resetSurfaceViewSize(videoWidth, videoHeight)
            }

            override fun onRequestRender() {
                surfaceView.requestRender()
            }
        }
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    private fun resetSurfaceViewSize(videoWidth: Int, videoHeight: Int) {
        post {
            val ratio = videoWidth.toFloat() / videoHeight.toFloat()
            val layoutParams = LayoutParams(width, (width.toFloat() / ratio).toInt())
            layoutParams.gravity = Gravity.CENTER
            surfaceView.layoutParams = layoutParams
        }
    }

    companion object {
        const val TAG = "VideoView"
    }
}
