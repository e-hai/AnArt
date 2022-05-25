package com.an.gl.base.texture

interface Texture {

    fun getTextureId(): Int

    fun bindTexture()

    fun unbindTexture()

    fun release()
}