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
    private val videoFile: File = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")

    init {
        initSurfaceView()
    }

    private fun initSurfaceView() {
        val renderer = VideoRenderer(context) {
            surfaceView.requestRender()
        }
        renderer.setSurfaceRequest(object : VideoRenderer.SurfaceRequest {
            override fun provideSurface(surface: Surface) {
                post { initVideoPlayer(surface) }
            }
        })
        surfaceView = GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        addView(surfaceView)
    }

    private fun initVideoPlayer(surface: Surface) {
        val moviePlayer = MoviePlayer(videoFile, surface, SpeedControlCallback())
        val ratio = moviePlayer.videoWidth.toFloat() / moviePlayer.videoHeight.toFloat()
        val width = surfaceView.width
        val height = (width.toFloat() / ratio).toInt()
        surfaceView.layoutParams = LayoutParams(width, height).apply { gravity = Gravity.CENTER }
        MoviePlayer.PlayTask(moviePlayer, null).execute()
    }

    companion object {
        const val TAG = "VideoView"
    }
}
