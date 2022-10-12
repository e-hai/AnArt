package com.an.gl.plus.base.texture

import android.opengl.GLES31

class Texture2dOes : Texture {

    private val textureIds = IntArray(1)
    private val textureTarget = GLES31.GL_TEXTURE_2D

    init {
        //生成一个纹理
        GLES31.glGenTextures(textureIds.size, textureIds, 0)
        GLES31.glBindTexture(textureTarget, textureIds[0])
        //设置纹理过滤参数
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST.toFloat()
        )
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_LINEAR.toFloat()
        )
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_WRAP_S,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_WRAP_T,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glBindTexture(textureTarget, 0)
    }

    override fun getTextureId(): Int {
        return textureIds[0]
    }

    /**
     * 将此纹理绑定到外部扩展纹理上
     * **/
    override fun bindTexture() {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(textureTarget, textureIds[0])
    }

    /**
     * 解绑纹理
     * **/
    override fun unbindTexture() {
        GLES31.glBindTexture(textureTarget, 0)
    }

    override fun release() {
        GLES31.glDeleteTextures(textureIds.size, textureIds, 0)
    }
}