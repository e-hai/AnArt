package com.an.gl.usercase.video

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout

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

        val renderer = VideoRender(context)
        renderer.callBack = object : VideoRender.CallBack {
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
