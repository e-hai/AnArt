package com.an.gl.base

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES31

/**
 * 扩展纹理，Camera,Video输入的流
 * **/
abstract class ExternalOesFboShader : Shader() {

    private val frameBufferObject = FrameBufferObject()

    private val textureId = IntArray(1)

    override fun initShader(vertexShaderCode: String, fragmentShaderCode: String) {
        super.initShader(vertexShaderCode, fragmentShaderCode)
        initTexture()
    }

    private fun initTexture() {
        //生成一个纹理
        GLES31.glGenTextures(1, textureId, 0)
        //将此纹理绑定到外部扩展纹理上
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0])
        //设置纹理过滤参数
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST.toFloat()
        )
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_LINEAR.toFloat()
        )
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_WRAP_S,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES31.GL_TEXTURE_WRAP_T,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        //解绑纹理
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    override fun onDrawFrame(textureId: Int): Int {
        //把输入的纹理渲染到fbo缓存中
        frameBufferObject.start()
        //设置显示窗口大小
        GLES31.glViewport(0, 0, outWidth, outHeight)
        //启用GL程序
        GLES31.glUseProgram(programHandle)
        //填充数据到顶点坐标索引
        GLES31.glVertexAttribPointer(
            vertexHandle,
            DEFAULT_VERTEX_SIZE,
            GLES31.GL_FLOAT,
            false,
            0,
            vertexCoordBuffer
        )
        //启用顶点索引
        GLES31.glEnableVertexAttribArray(vertexHandle)
        //填充数据到纹理坐标索引
        GLES31.glVertexAttribPointer(
            texCoorHandle,
            DEFAULT_TEXTURE_SIZE,
            GLES31.GL_FLOAT,
            false,
            0,
            textureCoordBuffer
        )
        GLES31.glEnableVertexAttribArray(texCoorHandle)
        //填充数据到总变换矩阵
        GLES31.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        //启用纹理功能
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        //绑定传入的纹理
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        //把绑定的纹理填充到纹理索引
        GLES31.glUniform1i(textureHandle, 0)
        //绘制纹理
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,
            DEFAULT_DRAW.size,
            GLES31.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )
        //解绑传入的纹理
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        frameBufferObject.end()

        return frameBufferObject.getFboTextureId()
    }

    override fun release() {
        super.release()
        frameBufferObject.release()
    }

    override fun onSizeChange(width: Int, height: Int) {
        super.onSizeChange(width, height)
        frameBufferObject.init(width, height)
    }

    fun getTextureId(): Int {
        return textureId[0]
    }
}
