package com.an.gl.shader

import android.content.Context
import android.opengl.GLES31
import com.an.gl.base.TextureShader
import com.an.gl.util.GlUtil

/**
 * 把纹理渲染到屏幕的Surface
 * **/
class ScreenShader(context: Context) : TextureShader(GLES31.GL_TEXTURE_2D) {

    companion object {
        private const val FILE_SIMPLE_VERTEX = "bitmap_vertex.glsl"
        private const val FILE_SIMPLE_FRAGMENT = "bitmap_fragment.glsl"
    }

    init {
        initShader(
            GlUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_VERTEX),
            GlUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_FRAGMENT)
        )
    }

    override fun onDrawFrame(dstTextureId: Int): Int {
        GLES31.glViewport(0, 0, outWidth, outHeight)
        GLES31.glUseProgram(programHandle)
        GLES31.glVertexAttribPointer(
            vertexHandle,
            DEFAULT_VERTEX_SIZE,
            GLES31.GL_FLOAT,
            false,
            0,
            vertexCoordBuffer
        )
        GLES31.glEnableVertexAttribArray(vertexHandle)
        GLES31.glVertexAttribPointer(
            texCoorHandle,
            DEFAULT_TEXTURE_SIZE,
            GLES31.GL_FLOAT,
            false,
            0,
            textureCoordBuffer
        )
        GLES31.glEnableVertexAttribArray(texCoorHandle)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, dstTextureId)
        GLES31.glUniform1i(textureHandle, 0)
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,
            DEFAULT_DRAW.size,
            GLES31.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
        return dstTextureId
    }

}