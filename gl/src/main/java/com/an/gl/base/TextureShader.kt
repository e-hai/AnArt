package com.an.gl.base

import android.opengl.GLES31
import com.an.gl.util.GlUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * GL文件中应固定这几个参数名
 * **/
const val GL_NAME_VERTEX_COORD = "aPosition" //顶点坐标
const val GL_NAME_TEXTURE_COORD = "aTexCoor" //纹理坐标
const val GL_NAME_TEXTURE = "uTexture"       //纹理
const val GL_NAME_MVP_MATRIX = "uMVPMatrix"  //总变换矩阵

abstract class TextureShader(
    val textureTarget: Int    //纹理的目标（GL_TEXTURE_EXTERNAL_OES、GL_TEXTURE_2D）
) {
    /**
     * 默认变换矩阵数据
     * **/
    val DEFAULT_MATRIX = floatArrayOf(
        1f, 0f, 0f, 0f, //屏幕左上
        0f, 1f, 0f, 0f, //屏幕左下
        0f, 0f, 1f, 0f, //屏幕右下
        0f, 0f, 0f, 1f, //屏幕右上
    )

    /**
     * 默认的顶点位置(基于世界坐标,x,y,z)
     * **/
    val DEFAULT_VERTEX = floatArrayOf(
        -1.0f, 1.0f,  //屏幕左上
        -1.0f, -1.0f, //屏幕左下
        1.0f, -1.0f,  //屏幕右下
        1.0f, 1.0f    //屏幕右上
    )

    val DEFAULT_VERTEX_SIZE = 2 //一个顶点坐标有几个轴组成

    /**
     * 默认的纹理位置(基于纹理坐标,x,y)
     * **/
    val DEFAULT_TEXTURE = floatArrayOf(
        0.0f, 1.0f, //屏幕左上
        0.0f, 0.0f, //屏幕左下
        1.0f, 0.0f, //屏幕右下
        1.0f, 1.0f  //屏幕右上
    )

    val DEFAULT_TEXTURE_SIZE = 2

    val DEFAULT_DRAW = shortArrayOf(0, 1, 2, 0, 2, 3)


    val mvpMatrix = DEFAULT_MATRIX //总变换矩阵数据

    lateinit var vertexCoordBuffer: FloatBuffer  //顶点位置数据缓存
    lateinit var textureCoordBuffer: FloatBuffer //纹理位置数据缓存
    lateinit var drawOrderBuffer: ShortBuffer
    private val textureId = IntArray(1)

    var programHandle: Int = 0     //OpenGL ES Program索引
    var vertexHandle: Int = 0      //顶点索引
    var texCoorHandle: Int = 0     //纹理坐标索引
    var textureHandle: Int = 0     //纹理索引
    var mvpMatrixHandle: Int = 0   //总变换矩阵索引

    var outWidth: Int = 0
    var outHeight: Int = 0


    open fun initShader(vertexShaderCode: String, fragmentShaderCode: String) {
        initCoord()
        initTexture()
        initProgram(vertexShaderCode, fragmentShaderCode)
    }

    private fun initCoord() {
        vertexCoordBuffer =
                // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(DEFAULT_VERTEX.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())
                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(DEFAULT_VERTEX)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }

        textureCoordBuffer = ByteBuffer.allocateDirect(DEFAULT_TEXTURE.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(DEFAULT_TEXTURE)
                position(0)
            }
        }

        drawOrderBuffer = ByteBuffer.allocateDirect(DEFAULT_DRAW.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(DEFAULT_DRAW)
                position(0)
            }
        }
    }

    fun initTexture() {
        //生成一个纹理
        GLES31.glGenTextures(1, textureId, 0)
        //将此纹理绑定到外部扩展纹理上
        GLES31.glBindTexture(textureTarget, textureId[0])
        //设置纹理过滤参数
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST.toFloat()
        )
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_LINEAR.toFloat()
        )
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_WRAP_S,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES31.glTexParameterf(
            textureTarget,
            GLES31.GL_TEXTURE_WRAP_T,
            GLES31.GL_CLAMP_TO_EDGE.toFloat()
        )
        //解绑纹理
        GLES31.glBindTexture(textureTarget, 0)
    }

    private fun initProgram(vertexShaderCode: String, fragmentShaderCode: String) {
        val vertexShader: Int = GlUtil.loadVertexShader(vertexShaderCode)
        val fragmentShader: Int = GlUtil.loadFragmentShader(fragmentShaderCode)
        programHandle = GlUtil.loadProgram(listOf(vertexShader, fragmentShader))
        vertexHandle = GLES31.glGetAttribLocation(programHandle, GL_NAME_VERTEX_COORD)
        texCoorHandle = GLES31.glGetAttribLocation(programHandle, GL_NAME_TEXTURE_COORD)
        mvpMatrixHandle = GLES31.glGetUniformLocation(programHandle, GL_NAME_MVP_MATRIX)
        textureHandle = GLES31.glGetUniformLocation(programHandle, GL_NAME_TEXTURE)
    }


    /**
     * 修改顶点坐标
     * **/
    fun updateVertexCoord(data: FloatArray) {
        vertexCoordBuffer.clear()
        vertexCoordBuffer.put(data)
        vertexCoordBuffer.position(0)
    }

    /**
     * 修改纹理坐标
     * **/
    fun updateTextureCoord(data: FloatArray) {
        textureCoordBuffer.clear()
        textureCoordBuffer.put(data)
        textureCoordBuffer.position(0)
    }

    fun getTextureId(): Int {
        return textureId[0]
    }

    /**
     * 释放资源
     * **/
    open fun release() {
        GLES31.glDeleteProgram(programHandle)
    }


    open fun onSizeChange(width: Int, height: Int) {
        outWidth = width
        outHeight = height
    }


    abstract fun onDrawFrame(dstTextureId: Int): Int

    companion object {
        const val TAG = "Shader"
    }
}