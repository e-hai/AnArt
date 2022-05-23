package com.an.gl.base

import android.opengl.GLES31
import com.an.gl.util.GlUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 标志该坐标的轴数
 * **/
const val GL_COORDINATE_SYSTEM_XY = 2    //x,y轴组成一个坐标
const val GL_COORDINATE_SYSTEM_XYZ = 3   //x,y,z轴组成一个坐标
const val GL_COORDINATE_SYSTEM_XYZW = 4  //x,y,z,w轴组成一个坐标

/**
 * GL文件中应固定这几个参数名
 * **/
const val GL_NAME_VERTEX_COORD = "aPosition" //顶点坐标
const val GL_NAME_TEXTURE_COORD = "aTexCoor" //纹理坐标
const val GL_NAME_TEXTURE = "uTexture"       //纹理
const val GL_NAME_MVP_MATRIX = "uMVPMatrix"  //总变换矩阵

class SimpleDraw(val texture: Texture) : Draw {

    private lateinit var mvpMatrixBuffer: FloatBuffer    //总变换矩阵
    private lateinit var vertexCoordBuffer: FloatBuffer  //顶点坐标
    private lateinit var textureCoordBuffer: FloatBuffer //纹理坐标
    private lateinit var drawOrderBuffer: ShortBuffer    //绘制路径

    var programHandle: Int = 0       //OpenGL ES Program索引
    var vertexCoorHandle: Int = 0    //顶点索引
    var textureCoorHandle: Int = 0   //纹理坐标索引
    var textureHandle: Int = 0       //纹理索引
    var mvpMatrixHandle: Int = 0     //总变换矩阵索引

    var outWidth: Int = 0
    var outHeight: Int = 0

    override fun initCoordinateData() {
        val defaultMvpMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, //屏幕左上
            0f, 1f, 0f, 0f, //屏幕左下
            0f, 0f, 1f, 0f, //屏幕右下
            0f, 0f, 0f, 1f, //屏幕右上
        )
        val defaultVertexCoord = floatArrayOf(
            -1.0f, 1.0f,  //屏幕左上
            -1.0f, -1.0f, //屏幕左下
            1.0f, -1.0f,  //屏幕右下
            1.0f, 1.0f    //屏幕右上
        )
        val defaultTextureCoord = floatArrayOf(
            0.0f, 1.0f, //屏幕左上
            0.0f, 0.0f, //屏幕左下
            1.0f, 0.0f, //屏幕右下
            1.0f, 1.0f  //屏幕右上
        )
        val defaultDrawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
        mvpMatrixBuffer = GlUtil.createFloatBuffer(defaultMvpMatrix)
        vertexCoordBuffer = GlUtil.createFloatBuffer(defaultVertexCoord)
        textureCoordBuffer = GlUtil.createFloatBuffer(defaultTextureCoord)
        drawOrderBuffer = GlUtil.createShortBuffer(defaultDrawOrder)
    }

    override fun getVertexShadeCode(): String {
        return """
            attribute vec4 $GL_NAME_VERTEX_COORD;
            attribute vec2 $GL_NAME_TEXTURE_COORD;
            uniform mat4 $GL_NAME_MVP_MATRIX;
            varying vec2 vTexCoordinate;
            
            void main(){
                vTexCoordinate = ($GL_NAME_MVP_MATRIX * $GL_NAME_TEXTURE_COORD).xy;
                gl_Position = aPosition;
            }
        """.trimIndent()
    }

    override fun getFragmentShadeCode(): String {
        return """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            uniform sampler2D $GL_NAME_TEXTURE;
            varying vec2 vTexCoordinate;

            void main () {
                gl_FragColor = texture2D($GL_NAME_TEXTURE, vTexCoordinate);
            }
        """.trimIndent()
    }

    override fun initShadeProgram() {
        val vertexShader: Int = GlUtil.loadVertexShader(getVertexShadeCode())
        val fragmentShader: Int = GlUtil.loadFragmentShader(getFragmentShadeCode())
        programHandle = GlUtil.loadProgram(listOf(vertexShader, fragmentShader))
        vertexCoorHandle = GLES31.glGetAttribLocation(programHandle, GL_NAME_VERTEX_COORD)
        textureCoorHandle = GLES31.glGetAttribLocation(programHandle, GL_NAME_TEXTURE_COORD)
        mvpMatrixHandle = GLES31.glGetUniformLocation(programHandle, GL_NAME_MVP_MATRIX)
        textureHandle = GLES31.glGetUniformLocation(programHandle, GL_NAME_TEXTURE)
    }

    override fun release() {
        GLES31.glDeleteProgram(programHandle)
    }

    override fun onSizeChange(width: Int, height: Int) {
        outWidth = width
        outHeight = height
    }

    override fun onDraw() {
        //设置视图区域
        GLES31.glViewport(0, 0, outWidth, outHeight)

        //要启用的着色器程序
        GLES31.glUseProgram(programHandle)

        //给顶点坐标属性添加数据
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

        //给纹理坐标属性添加数据
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

        //绑定的纹理，关联到着色器的纹理属性上
        GLES31.glUniform1i(textureHandle, 0)

        //按照绘制数据来绘制顶点
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,          //图元的形状（点、线、三角形）
            drawOrderBuffer.array().size, //顶点数量
            GLES31.GL_UNSIGNED_SHORT,     //元素类型
            drawOrderBuffer               //元素数据
        )

        //解绑纹理
        texture.unbindTexture()

        //关闭使用纹理坐标属性
        GLES31.glDisableVertexAttribArray(textureCoorHandle)

        //关闭使用顶点坐标属性
        GLES31.glDisableVertexAttribArray(vertexCoorHandle)
    }
}