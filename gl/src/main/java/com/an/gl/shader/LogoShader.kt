package com.an.gl.shader

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES31
import android.opengl.GLUtils
import androidx.annotation.DrawableRes
import com.an.gl.base.FboManager
import com.an.gl.base.OesFboShader
import com.an.gl.util.GlUtil

class LogoShader(
    val context: Context,
    @DrawableRes val logoResId: Int,
    frameBufferObject: FboManager
) : OesFboShader(GLES31.GL_TEXTURE_2D, frameBufferObject) {

    companion object {
        const val TAG = "LogoShader"
        private const val FILE_SIMPLE_VERTEX = "bitmap_vertex.glsl"
        private const val FILE_SIMPLE_FRAGMENT = "bitmap_fragment.glsl"
    }

    private val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)

    private val logoTexture = floatArrayOf(
        0.0f, 0.0f, //屏幕左上
        0.0f, 1.0f, //屏幕左下
        1.0f, 1.0f, //屏幕右下
        1.0f, 0.0f  //屏幕右上
    )

    init {
        initShader(
            GlUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_VERTEX),
            GlUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_FRAGMENT)
        )
    }

    override fun onDraw() {
        updateTextureCoord(logoTexture)
        GLES31.glViewport(0, 0, logoBitmap.width, logoBitmap.height)
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_ONE, GLES31.GL_ONE_MINUS_SRC_ALPHA)

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
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, getTextureId())
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, logoBitmap, 0)
        GLES31.glUniform1i(textureHandle, 0)
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,
            DEFAULT_DRAW.size,
            GLES31.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
        GLES31.glDisable(GLES31.GL_BLEND)
        updateTextureCoord(DEFAULT_TEXTURE)
    }

    override fun release() {
        super.release()
        logoBitmap.recycle()
    }
}