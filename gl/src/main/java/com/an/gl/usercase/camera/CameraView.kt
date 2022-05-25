package com.an.gl.usercase.camera

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import com.an.gl.usercase.camera.CameraRender.Companion.TAG


class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var cameraRender: CameraRender


    val surfaceProvider = Preview.SurfaceProvider { request ->
        initSurfaceView(request)
    }

    private fun initSurfaceView(request: SurfaceRequest) {
        cameraRender = CameraRender(context) {
            surfaceView.requestRender()
        }.apply {
            setSurfaceRequest(request)
        }
        surfaceView = GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(cameraRender)
            renderMode = RENDERMODE_WHEN_DIRTY
        }
        //根据Camera预览比例设置Surface比例，避免变形
        Log.d(TAG, "${request.resolution.width} ${request.resolution.height}")
        val ratio = request.resolution.width.toFloat() / request.resolution.height.toFloat()
        val width = width
        val height = (width.toFloat() * ratio).toInt()
        surfaceView.layoutParams = LayoutParams(width, height)
            .apply { gravity = Gravity.CENTER }
        addView(surfaceView)
    }

}


