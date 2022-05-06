package com.an.gl.base

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES31

abstract class TexturesShader {

    lateinit var surfaceTexture: SurfaceTexture
    val transformMatrix = FloatArray(16)
    val textureId = 1

    fun initTexture() {
        //生成一个纹理
        GLES31.glGenTextures(1, intArrayOf(textureId), 0)
        //将此纹理绑定到外部纹理上
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        //设置纹理过滤参数
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST.toFloat()
        )
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_LINEAR.toFloat()
        )
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_WRAP_S,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_WRAP_T,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        //解绑纹理
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        //以纹理对象ID创建SurfaceTexture
        surfaceTexture = SurfaceTexture(textureId)
    }


    fun setDefaultBufferSize(width: Int, height: Int) {
        surfaceTexture.setDefaultBufferSize(width, height)
    }

    fun setOnFrameAvailableListener(listener: SurfaceTexture.OnFrameAvailableListener) {
        surfaceTexture.setOnFrameAvailableListener(listener)
    }

    fun release() {
        surfaceTexture.release()
    }

    open fun draw(){
        surfaceTexture.getTransformMatrix(transformMatrix)
    }

}