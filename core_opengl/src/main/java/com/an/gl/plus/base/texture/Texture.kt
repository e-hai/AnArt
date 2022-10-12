package com.an.gl.plus.base.texture

interface Texture {

    fun getTextureId(): Int

    fun bindTexture()

    fun unbindTexture()

    fun release()
}