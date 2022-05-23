package com.an.gl.base

interface Texture {

    fun getTextureId(): Int

    fun bindTexture()

    fun unbindTexture()
}