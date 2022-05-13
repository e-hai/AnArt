package com.an.gl.base

import android.opengl.GLES31

/**
 * 扩展纹理，Camera,Video输入的流
 * **/
abstract class OesFboShader(
    textureTarget: Int,
    private val fbo: FboManager  //fbo离屏渲染
) : Shader(textureTarget) {


    override fun onDrawFrame(dstTextureId: Int): Int {
        drawDstTextureInFbo(dstTextureId)
        fbo.start()
        onDraw()
        fbo.end()
        return fbo.getFboTextureId()
    }

    open fun onDraw() {

    }

    private fun drawDstTextureInFbo(desTextureId: Int) {
        //把输入的纹理渲染到fbo缓存中
        fbo.start()
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
        GLES31.glBindTexture(textureTarget, desTextureId)
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
        GLES31.glBindTexture(textureTarget, 0)
        fbo.end()
    }

    override fun release() {
        super.release()
        fbo.release()
    }

    override fun onSizeChange(width: Int, height: Int) {
        super.onSizeChange(width, height)
        fbo.onSizeChange(width, height)
    }
}
