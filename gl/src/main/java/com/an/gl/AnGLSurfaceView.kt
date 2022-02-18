package com.an.gl

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.egl.EGLConfig

class AnGLSurfaceView(context: Context) : GLSurfaceView(context) {


    private val renderer: AnGLRenderer

    init {

        // Create an OpenGL ES 3.0 context
        setEGLContextClientVersion(3)

        renderer = AnGLRenderer()

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_WHEN_DIRTY
    }

}

class AnGLRenderer : GLSurfaceView.Renderer {

    /**
     * 初始化配置
     * **/
    override fun onSurfaceCreated(gl: GL10?, config:EGLConfig?) {
        // Set the background frame color
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }


    /**
     * 绘制
     * **/
    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    }

    /**
     * 尺寸更改
     * **/
    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }
}