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
        const val TAG = "WatermarkDraw"
    }

    private val watermarkBitmap = BitmapFactory.decodeResource(
        context.resources, config.watermarkResId
    )
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
            config.locationMode,
            config.switchingTime,
            width,
            height,
            140,
            40,
            config.margin
        )
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
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, watermarkBitmap, 0)

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
        location.updateViewport()
    }

    override fun release() {
        super.release()
        watermarkBitmap.recycle()
    }

    fun setPresentationTime(timeSeconds: Long) {
        location.updateTime(timeSeconds)
    }

}

class Location(
    private val mode: Mode,
    private val switchingTime: Long,
    private val screenWidth: Int,      //屏幕宽
    private val screenHeight: Int,     //屏幕高
    private val watermarkWidth: Int,   //水印宽
    private val watermarkHeight: Int,  //水印高
    private val margin: Int = 0        //水印离屏幕的间隔
) {

    private var viewportX: Int = 0
    private var viewportY: Int = 0
    private var viewportWidth: Int = 0
    private var viewportHeight: Int = 0
    private var modeRunHadChange: Boolean = false

    init {
        initSize()
        when (mode) {
            Mode.RUN -> inTheLeftTop()
            Mode.LEFT_TOP -> inTheLeftTop()
            Mode.LEFT_BOTTOM -> inTheLeftBottom()
            Mode.RIGHT_TOP -> inTheRightTop()
            Mode.RIGHT_BOTTOM -> inTheRightBottom()
        }
    }

    private fun initSize() {
        val scale = if (screenWidth > screenHeight) {
            screenWidth.toFloat() / UI_PX_LONG
        } else {
            screenWidth.toFloat() / UI_PX_SHORT
        }
        viewportWidth = (watermarkWidth * scale).toInt()
        viewportHeight = (watermarkHeight * scale).toInt()
        Log.d(
            TAG,
            "screenWidth=$screenWidth screenHeight=$screenHeight "
                    + "watermarkWidth=$watermarkWidth watermarkHeight=$watermarkHeight "
                    + "viewportWidth=$viewportWidth viewportHeight=$viewportHeight"
        )
    }

    /**
     * 设置位置在左上角
     * **/
    private fun inTheLeftTop() {
        viewportX = margin
        viewportY = screenHeight - viewportHeight - margin
    }

    /**
     * 设置位置在左下角
     * **/
    private fun inTheLeftBottom() {
        viewportX = margin
        viewportY = margin
    }

    /**
     * 设置在右下角
     * **/
    private fun inTheRightTop() {
        viewportX = screenWidth - viewportWidth - margin
        viewportY = screenHeight - viewportHeight - margin
    }

    /**
     * 设置在右下角
     * **/
    private fun inTheRightBottom() {
        viewportX = screenWidth - viewportWidth - margin
        viewportY = margin
    }

    fun updateViewport() {
        GLES31.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
    }

    fun updateTime(timeSeconds: Long) {
        if (!modeRunHadChange && timeSeconds > switchingTime && mode == Mode.RUN) {
            modeRunHadChange = true
            inTheRightBottom()
        }
    }

    enum class Mode {
        LEFT_TOP,    //左上角
        LEFT_BOTTOM, //左下角
        RIGHT_TOP,   //右上角
        RIGHT_BOTTOM,//右下角
        RUN          //对角移动
    }

    companion object {
        const val UI_PX_LONG: Float = 640f  //UI设计图的长边，用于水印实际绘制到屏幕时，计算屏幕缩放比例，得到合适的大小
        const val UI_PX_SHORT: Float = 360f //UI设计图的短边
    }
}

data class WatermarkConfig(
    @DrawableRes val watermarkResId: Int,
    val margin: Int = 10,             //离周围间隔
    val switchingTime: Long = 2,      //切换时机（仅在RUN模式下有效）
    val locationMode: Location.Mode = Location.Mode.RUN
)

