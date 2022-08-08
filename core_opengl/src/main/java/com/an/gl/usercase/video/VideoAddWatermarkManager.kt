package com.an.gl.usercase.video

import android.content.Context
import android.opengl.GLES31
import android.view.Surface
import com.an.gl.R
import com.an.gl.base.*
import com.an.gl.base.egl.EglCore
import com.an.gl.base.egl.EglCore.Companion.FLAG_TRY_GLES3
import com.an.gl.base.egl.EglSurfaceBase
import com.an.gl.usercase.MaskConfig
import com.an.gl.usercase.MaskDraw
import com.an.gl.usercase.WatermarkConfig
import com.an.gl.usercase.WatermarkDraw
import java.io.File
import java.util.concurrent.TimeUnit


class VideoAddWatermarkManager(
    val context: Context,
    private val fromFile: File,
    private val outFile: File,
    private val watermarkConfig: WatermarkConfig
) {

    companion object {
        const val TAG = "VideoAddWatermark"
        val SECONDS_TO_NANOS = TimeUnit.SECONDS.toNanos(1)  //一秒的纳秒长度
    }

    private lateinit var movieDecoder: VideoDecode      //视频解码器
    private lateinit var movieEncoder: VideoEncode      //视频编码器
    private lateinit var eglManager: EglSurfaceBase     //EGL环境管理类
    private lateinit var mediaEglManager: MediaEglManager
    private lateinit var watermarkDraw: WatermarkDraw
    private lateinit var topMaskDraw: MaskDraw
    private lateinit var bottomMaskDraw: MaskDraw

    private var width: Int = 0
    private var height: Int = 0
    private var presentationTime: Long = 0
    private var frameTime = (1f / 30 * SECONDS_TO_NANOS).toLong()

    init {
        initVideo()
        initEGL()
    }


    private fun initVideo() {
        movieDecoder = VideoDecode(
            fromFile,
            null,
            object : VideoDecode.FrameCallback {
                override fun preRender(presentationTimeUsec: Long) {

                }

                override fun postRender() {
                    onDrawFrame()
                }

                override fun finishRender() {
                    drawFinish()
                }
            })
        movieEncoder = VideoEncode(
            movieDecoder.videoWidth,
            movieDecoder.videoHeight,
            movieDecoder.frameRate,
            outFile
        )
        width = movieEncoder.width
        height = movieEncoder.height
        frameTime = (1f / movieEncoder.frameRate * SECONDS_TO_NANOS).toLong()
    }


    private fun initEGL() {
        val eglCore = EglCore(null, FLAG_TRY_GLES3)
        eglManager = EglSurfaceBase(eglCore)
        bindResultSurface()
    }

    /**
     * 绑定渲染结果Surface
     * **/
    private fun bindResultSurface() {
        //结果渲染到编码器的Surface上
        val resultSurface = movieEncoder.inputSurface
        eglManager.createWindowSurface(resultSurface)
        eglManager.makeCurrent()
        onSurfaceCreated()
        onSurfaceChanged(width, height)
    }


    /**
     * 初始化配置
     * **/
    private fun onSurfaceCreated() {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        mediaEglManager = MediaEglManager().apply {
            bindSourceSurface(surface)
        }
        watermarkDraw = WatermarkDraw(context, watermarkConfig)
        topMaskDraw = MaskDraw(context,
            MaskConfig(R.drawable.mask_top, height = 72)
        )
        bottomMaskDraw = MaskDraw(
            context,
            MaskConfig(R.drawable.mask_bottom, MaskConfig.Direction.BOTTOM, height = 72)
        )
    }


    /**
     * 绑定视频流源数据的Surface
     * **/
    private fun bindSourceSurface(surface: Surface) {
        movieDecoder.setOutputSurface(surface)
    }

    /**
     * 尺寸更改
     * **/
    private fun onSurfaceChanged(width: Int, height: Int) {
        mediaEglManager.onSizeChange(width, height)
        watermarkDraw.onSizeChange(width, height)
        topMaskDraw.onSizeChange(width, height)
        bottomMaskDraw.onSizeChange(width, height)
    }


    /**
     * 绘制
     * **/
    private fun onDrawFrame() {
        //给当前帧设置时间戳，解决给编码器设置帧数无效的问题
        eglManager.setPresentationTime(presentationTime)
        watermarkDraw.setPresentationTime(presentationTime / SECONDS_TO_NANOS)
        presentationTime += frameTime
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        mediaEglManager.onDraw {
            bottomMaskDraw.onDraw()
            topMaskDraw.onDraw()
            watermarkDraw.onDraw()
        }
        eglManager.swapBuffers()
        movieEncoder.drainEncoder(false)
    }

    private fun drawFinish() {
        //渲染结束必须调用，否则视频无法播放
        movieEncoder.release()
    }

    fun start() {
        movieDecoder.play()
    }

}


