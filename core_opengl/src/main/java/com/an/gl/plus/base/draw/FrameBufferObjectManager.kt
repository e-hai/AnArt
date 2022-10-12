package com.an.gl.plus.base.draw

import android.opengl.GLES20
import android.opengl.GLES31
import com.an.gl.plus.base.texture.Texture
import com.an.gl.plus.base.texture.Texture2dOes

/**
 *在回调中绘制的内容会保存在FBO中
 * **/
fun FrameBufferObjectManager.load(drawInFBO: () -> Unit) {
    start()
    drawInFBO()
    end()
}

/**
 * 把external oes转成 2d oes并保存在FBO中
 * **/
class FrameBufferObjectManager {

    private val framebufferIds = IntArray(1)
    private val texture: Texture2dOes

    init {
        //创建FBO
        GLES31.glGenFramebuffers(framebufferIds.size, framebufferIds, 0)
        //创建纹理
        texture = Texture2dOes()
    }

    fun start() {
        GLES31.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferIds[0])
    }

    fun end() {
        GLES31.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun onSizeChange(width: Int, height: Int) {
        //启用纹理
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture.getTextureId())
        //创建与输入尺寸一样大小的存储区域
        GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D,
            0,
            GLES31.GL_RGBA,
            width,
            height,
            0,
            GLES31.GL_RGBA,
            GLES31.GL_UNSIGNED_BYTE,
            null
        )
        //启用FBO
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, framebufferIds[0])
        //绑定FBO与纹理,后续的绘制结果生成在该纹理中
        GLES31.glFramebufferTexture2D(
            GLES31.GL_FRAMEBUFFER,
            GLES31.GL_COLOR_ATTACHMENT0,
            GLES31.GL_TEXTURE_2D,
            texture.getTextureId(),
            0
        )
        //停用纹理
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
        //停FBO
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
    }


    fun getTexture(): Texture {
        return texture
    }

    fun release() {
        GLES31.glDeleteFramebuffers(framebufferIds.size, framebufferIds, 0)
    }
}