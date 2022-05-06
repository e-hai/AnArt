package com.an.gl.video

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.widget.FrameLayout
import com.an.gl.R
import com.an.gl.camera.CamareTexturesShader
import com.an.gl.util.FileUtil
import com.an.gl.util.ShaderUtil
import com.an.gl.video.VideoView.Companion.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.log

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
        val renderer = VideoRenderer(context)
        renderer.callBack = object : VideoRenderer.CallBack {
            override fun provideSurface(surface: Surface) {
                post {
                    initVideoPlayer(surface)
                }
            }
        }
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        addView(surfaceView)
    }

    private fun initVideoPlayer(surface: Surface) {
        val videoFile: File = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")
        val moviePlayer = MoviePlayer(
            videoFile,
            surface,
            object : MoviePlayer.FrameCallback {
                override fun preRender(presentationTimeUsec: Long) {
                }

                override fun postRender() {
                    surfaceView.requestRender()
                }

                override fun loopReset() {
                }
            })

        val ratio = moviePlayer.videoWidth.toFloat() / moviePlayer.videoHeight.toFloat()
        val width = surfaceView.width
        val height = (width.toFloat() / ratio).toInt()
        surfaceView.layoutParams = LayoutParams(width, height).apply { gravity = Gravity.CENTER }
        Log.d(TAG, "w=${width} h=${height}")
        MoviePlayer.PlayTask(moviePlayer, null).execute()
    }

    companion object {
        const val TAG = "VideoView"
    }
}

class VideoRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private lateinit var videoShader: CamareTexturesShader
    private lateinit var logoShader: LogoTexturesShader

    /**
     * 初始化配置
     * **/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        videoShader = CamareTexturesShader(context).apply {
            setOnFrameAvailableListener {
            }
        }
        logoShader = LogoTexturesShader(context).apply {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.app_name)
            Log.d(TAG, "logo w=${logo.width} h=${logo.height}")
            ShaderUtil.bindBitmapTexture(logo, this.textureId)
        }
        callBack?.provideSurface(videoShader.surface)
    }


    /**
     * 尺寸更改
     * **/
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged=$width $height")
        GLES31.glViewport(0, 0, width, height)
        videoShader.setDefaultBufferSize(width, height)
    }

    /**
     * 绘制
     * **/
    override fun onDrawFrame(gl: GL10) {
        // Redraw background color
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        videoShader.draw()
//        logoShader.draw()
    }

    interface CallBack {
        fun provideSurface(surface: Surface)
    }

    var callBack: CallBack? = null
}