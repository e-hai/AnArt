package com.an.gl.shader

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import com.an.gl.base.ExternalOesFboShader
import com.an.gl.util.ShaderUtil


class CameraShader(context: Context) : ExternalOesFboShader() {

    companion object {
        private const val FILE_SIMPLE_VERTEX = "camera_vertex.glsl"
        private const val FILE_SIMPLE_FRAGMENT = "camera_fragment.glsl"
    }

    val surface: Surface
    val surfaceTexture: SurfaceTexture

    init {
        initShader(
            ShaderUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_VERTEX),
            ShaderUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_FRAGMENT)
        )
        surfaceTexture = SurfaceTexture(getTextureId())
        surface = Surface(surfaceTexture)
    }

    override fun onSizeChange(width: Int, height: Int) {
        super.onSizeChange(width, height)
        surfaceTexture.setDefaultBufferSize(width, height)
    }

    override fun onDrawFrame(textureId: Int): Int {
        surfaceTexture.getTransformMatrix(mvpMatrix)
        surfaceTexture.updateTexImage()
        return super.onDrawFrame(textureId)
    }
}