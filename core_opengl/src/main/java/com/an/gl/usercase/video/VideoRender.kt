package com.an.gl.usercase.video

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.view.Surface
import com.an.gl.base.*
import com.an.gl.usercase.WatermarkDraw
import com.an.gl.util.FileUtil
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRender(private val context: Context) : GLSurfaceView.Renderer {

    private val videoFile: File = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")
    private lateinit var mediaEglManager: MediaEglManager
    private lateinit var watermarkDraw: WatermarkDraw
    private lateinit var videoDecode: VideoDecode

    var callBack: CallBack? = null


    /**
     * 初始化配置
     * **/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        mediaEglManager = MediaEglManager().apply {
            setOnFrameAvailableListener {
                callBack?.onRequestRender()
            }
            initVideo(surface)
        }
//        watermarkDraw = WatermarkDraw(context, WatermarkConfig(R.drawable.watermark))
    }

    /**
     * 尺寸更改
     * **/
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mediaEglManager.onSizeChange(width, height)
        watermarkDraw.onSizeChange(width, height)
    }

    /**
     * 绘制
     * **/
    override fun onDrawFrame(gl: GL10) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        mediaEglManager.onDraw {
            watermarkDraw.onDraw()
        }
    }


    private fun initVideo(surface: Surface) {
        videoDecode = VideoDecode(
            videoFile,
            surface,
            object : VideoDecode.FrameCallback {
                override fun preRender(presentationTimeUsec: Long) {
                }

                override fun postRender() {
                }

                override fun finishRender() {
                }
            })
        callBack?.onVideoSize(videoDecode.videoWidth, videoDecode.videoHeight)
        VideoDecode.PlayTask(videoDecode, null).execute()
    }

    interface CallBack {
        fun onVideoSize(videoWidth: Int, videoHeight: Int)
        fun onRequestRender()
    }
}