package com.an.gl.shader

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.view.Surface
import com.an.gl.base.OesFboShader
import com.an.gl.base.FboManager
import com.an.gl.util.ShaderUtil


class CameraShader(
    context: Context,
    frameBufferObject: FboManager
) : OesFboShader(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frameBufferObject) {

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

    override fun onDrawFrame(desTextureId: Int): Int {
        surfaceTexture.getTransformMatrix(mvpMatrix)
        surfaceTexture.updateTexImage()
        return super.onDrawFrame(desTextureId)
    }
}