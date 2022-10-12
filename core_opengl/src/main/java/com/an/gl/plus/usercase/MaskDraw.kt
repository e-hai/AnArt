package com.an.gl.usercase

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLUtils
import androidx.annotation.DrawableRes
import com.an.gl.plus.base.draw.*
import com.an.gl.plus.base.texture.Texture2dOes
import com.an.gl.util.FileUtil

class MaskDraw(
    context: Context,
    private val config: MaskConfig,
    texture: Texture2dOes = Texture2dOes()
) : SimpleDraw(texture) {

    companion object {
        const val TAG = "MaskDraw"
    }

    private val maskBitmap = FileUtil.getBitmap(
        context.resources, config.maskResId
    )

    init {
        //正确处理纹理坐标在世界坐标的显示效果
        val maskTextureCoord = floatArrayOf(
            0.0f, 0.0f, //屏幕左上
            0.0f, 1.0f, //屏幕左下
            1.0f, 1.0f, //屏幕右下
            1.0f, 0.0f  //屏幕右上
        )
        textureCoordBuffer.clear()
        textureCoordBuffer.put(maskTextureCoord)
        textureCoordBuffer.position(0)
    }


    override fun onDraw() {
        //纹理重叠时，设置有透明度背景
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_ONE, GLES31.GL_ONE_MINUS_SRC_ALPHA)

        //设置视图区域
        updateViewport()

        //要启用的着色器程序
        GLES31.glUseProgram(programHandle)

        //将矩阵数据传进渲染管线
        GLES31.glUniformMatrix4fv(mvpMatrixHandle, 1, false, dataMvpMatrix, 0)

        //将顶点坐标数据传进渲染管线
        GLES31.glVertexAttribPointer(
            vertexCoorHandle,
            GL_COORDINATE_SYSTEM_XY,
            GLES31.GL_FLOAT,
            false,
            0,
            vertexCoordBuffer
        )
        //启用顶点坐标属性，这里会影响后续的顶点操作（纹理坐标映射、顶点绘制）
        GLES31.glEnableVertexAttribArray(vertexCoorHandle)

        //将纹理坐标数据传进渲染管线
        GLES31.glVertexAttribPointer(
            textureCoorHandle,
            GL_COORDINATE_SYSTEM_XY,
            GLES31.GL_FLOAT,
            false,
            0,
            textureCoordBuffer
        )
        //启用纹理坐标属性
        GLES31.glEnableVertexAttribArray(textureCoorHandle)

        //绑定纹理，这里会影响后续纹理操作
        texture.bindTexture()

        //加载bitmap到纹理上
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, maskBitmap, 0)

        //把绑定的纹理传进渲染管线
        GLES31.glUniform1i(textureHandle, 0)

        //按照绘制数据来绘制顶点
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,          //图元的形状（点、线、三角形）
            dataDrawOrder.size,           //顶点数量
            GLES31.GL_UNSIGNED_SHORT,     //元素类型
            drawOrderBuffer               //元素数据
        )

        //解绑纹理
        texture.unbindTexture()

        //关闭使用纹理坐标属性
        GLES31.glDisableVertexAttribArray(textureCoorHandle)

        //关闭使用顶点坐标属性
        GLES31.glDisableVertexAttribArray(vertexCoorHandle)
        GLES31.glDisable(GLES31.GL_BLEND)
    }

    override fun updateViewport() {
        val width = if (config.width == -1) {
            outWidth
        } else {
            config.width
        }
        val height = if (config.height == -1) {
            outHeight
        } else {
            config.height
        }
        when (config.direction) {
            MaskConfig.Direction.TOP -> {
                GLES31.glViewport(0, outHeight - height, width, height)
            }
            MaskConfig.Direction.BOTTOM -> {
                GLES31.glViewport(0, 0, width, height)
            }
            MaskConfig.Direction.LEFT -> {
                GLES31.glViewport(0, 0, width, height)
            }
            MaskConfig.Direction.RIGHT -> {
                GLES31.glViewport(outWidth - width, 0, width, height)
            }
        }
    }

    override fun release() {
        super.release()
        maskBitmap?.recycle()
    }
}

data class MaskConfig(
    @DrawableRes val maskResId: Int,
    val direction: Direction = Direction.TOP,
    val width: Int = -1,
    val height: Int = -1
) {
    enum class Direction {
        TOP, BOTTOM, LEFT, RIGHT
    }
}