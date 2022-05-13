package com.an.gl.base

import android.opengl.GLES20
import android.opengl.GLES31

class FboManager {

    private val fboId = IntArray(1)         //FBO ID
    private val fboTextureId = IntArray(1)  //用于FBO操作的纹理 ID

    fun onSizeChange(width: Int, height: Int) {
        release()

        //创建FBO
        GLES31.glGenFramebuffers(fboId.size, fboId, 0)

        //创建纹理
        GLES31.glGenTextures(fboTextureId.size, fboTextureId, 0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, getFboTextureId())
        GLES31.glTexParameterf(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST.toFloat()
        )
        GLES31.glTexParameterf(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_LINEAR.toFloat()
        )
        GLES31.glTexParameterf(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_WRAP_S,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glTexParameterf(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_WRAP_T,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)

        //关联FBO与纹理
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D,getFboTextureId())
        GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, width, height,
            0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, null
        )
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboId[0])
        GLES31.glFramebufferTexture2D(
            GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0,
            GLES31.GL_TEXTURE_2D, getFboTextureId(), 0
        )
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
    }

    fun start() {
        GLES31.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0])
    }

    fun end() {
        GLES31.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun getFboTextureId(): Int {
        return fboTextureId[0]
    }

    fun release() {
        GLES31.glDeleteFramebuffers(fboId.size, fboId, 0)
        GLES31.glDeleteTextures(fboTextureId.size, fboTextureId, 0)
    }
}