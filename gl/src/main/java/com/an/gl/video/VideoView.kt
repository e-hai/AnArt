package com.an.gl.video

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import com.an.gl.camera.CamareTexturesShader
import com.an.gl.util.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

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
        post {
            surfaceView = GLSurfaceView(context)
                .apply {
                    setEGLContextClientVersion(3)
                    setRenderer(gLRenderer)
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                }

            addView(surfaceView)
        }
    }


    private val gLRenderer = object : GLSurfaceView.Renderer {
        private lateinit var shader: CamareTexturesShader
        private lateinit var moviePlayer: MoviePlayer

        /**
         * 初始化配置
         * **/
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            val videoFile = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")
            shader = CamareTexturesShader(context).apply {
                setOnFrameAvailableListener {
                    Log.d(TAG, "requestRender")
                    surfaceView.requestRender()
                }
            }
            moviePlayer = MoviePlayer(
                videoFile,
                shader.getSurface(),
                object : MoviePlayer.FrameCallback {
                    override fun preRender(presentationTimeUsec: Long) {
                    }

                    override fun postRender() {
                    }

                    override fun loopReset() {
                    }
                })
            post {
                val ratio = moviePlayer.videoWidth.toFloat() / moviePlayer.videoHeight.toFloat()
                val width = width
                val height = (width.toFloat() / ratio).toInt()
                Log.d(TAG, "h=${height} w=${width}")

                surfaceView.layoutParams = LayoutParams(width, height)
                    .apply { gravity = Gravity.CENTER }
                shader.setDefaultBufferSize(moviePlayer.videoHeight, moviePlayer.videoWidth)

                MoviePlayer.PlayTask(moviePlayer, null).execute()
            }

        }


        /**
         * 绘制
         * **/
        override fun onDrawFrame(gl: GL10) {
            // Redraw background color
            GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
            shader.draw()
        }

        /**
         * 尺寸更改
         * **/
        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES31.glViewport(0, 0, width, height)
        }
    }

    companion object {
        const val TAG = "VideoView"
    }
}