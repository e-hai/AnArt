package com.an.gl.usercase

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES31
import android.opengl.GLUtils
import android.util.Log
import androidx.annotation.DrawableRes
import com.an.gl.base.draw.GL_COORDINATE_SYSTEM_XY
import com.an.gl.base.draw.SimpleDraw
import com.an.gl.base.texture.Texture2dOes
import com.an.gl.usercase.WatermarkDraw.Companion.TAG

class WatermarkDraw(
    context: Context,
    private val config: WatermarkConfig,
    texture: Texture2dOes = Texture2dOes()
) : SimpleDraw(texture) {

    companion object {
        const val TAG = "LogoShader"
    }

    private val watermarkBitmap =
        BitmapFactory.decodeResource(context.resources, config.watermarkResId)
    private lateinit var location: Location

    init {
        //正确处理纹理坐标在世界坐标的显示效果
        val watermarkTextureCoord = floatArrayOf(
            0.0f, 0.0f, //屏幕左上
            0.0f, 1.0f, //屏幕左下
            1.0f, 1.0f, //屏幕右下
            1.0f, 0.0f  //屏幕右上
        )
        textureCoordBuffer.clear()
        textureCoordBuffer.put(watermarkTextureCoord)
        textureCoordBuffer.position(0)
    }

    override fun onSizeChange(width: Int, height: Int) {
        super.onSizeChange(width, height)
        location = Location(
            width, height,
            watermarkBitmap.width, watermarkBitmap.height,
            config.margin
        )
    }

    override fun onDraw() {
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
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, watermarkBitmap, 0)

        //把绑定的纹理传进渲染管线
        GLES31.glUniform1i(textureHandle, 0)

        //按照绘制数据来绘制顶点
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,          //图元的形状（点、线、三角形）
            dataDrawOrder.size, //顶点数量
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
        GLES31.glViewport(location.x, location.y, watermarkBitmap.width, watermarkBitmap.height)
    }

    override fun release() {
        super.release()
        watermarkBitmap.recycle()
    }

    fun changeLocation() {
        if (config.locationMode != LocationMode.ALL) {
            return
        }
        location.nextLocation()
    }
}

class Location(
    private val parentWidth: Int = 0,
    private val parentHeight: Int = 0,
    private val width: Int = 0,
    private val height: Int = 0,
    private val margin: Int = 0
) {
    var x = margin
    var y = margin

    private val locationList = listOf(
        LocationMode.LEFT_BOTTOM,
        LocationMode.LEFT_TOP,
        LocationMode.RIGHT_TOP,
        LocationMode.RIGHT_BOTTOM
    )
    private var locationPos = 0

    fun nextLocation() {
        val select = locationPos % locationList.size
        when (locationList[select]) {
            LocationMode.LEFT_BOTTOM -> {
                x = margin
                y = margin
            }
            LocationMode.LEFT_TOP -> {
                x = margin
                y = parentHeight - height - margin
            }
            LocationMode.RIGHT_TOP -> {
                x = parentWidth - width - margin
                y = parentHeight - height - margin
            }
            LocationMode.RIGHT_BOTTOM -> {
                x = parentWidth - width - margin
                y = margin
            }
            else -> {}
        }
        locationPos++
    }
}

data class WatermarkConfig(
    @DrawableRes val watermarkResId: Int,
    val margin: Int = 10,               //离周围间隔
    val duration: Long = 2,             //轮播时长
    val locationMode: LocationMode = LocationMode.LEFT_TOP
)

enum class LocationMode {
    LEFT_TOP,    //左上角
    LEFT_BOTTOM, //左下角
    RIGHT_TOP,   //右上角
    RIGHT_BOTTOM,//右下角
    ALL          //四个角定时轮播
}