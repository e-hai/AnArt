package com.an.gl.base

import com.an.gl.util.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


abstract class Shader(
    private val vertexShaderCode: String,
    private val fragmentShaderCode: String
) {

    var outWidth: Int = 0
    var outHeight: Int = 0
    var programId: Int = 0 //OpenGL ES Program
    lateinit var vertexBuffer: FloatBuffer //顶点位置缓存
    lateinit var textureBuffer: FloatBuffer //纹理位置缓存

    init {
        initData()
        initProgram()
    }

    private fun initData() {
        vertexBuffer =
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

        textureBuffer = ByteBuffer.allocateDirect(DEFAULT_TEXTURE.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(DEFAULT_TEXTURE)
                position(0)
            }
        }
    }

    private fun initProgram() {
        val vertexShader: Int = ShaderUtil.loadVertexShader(vertexShaderCode)
        val fragmentShader: Int = ShaderUtil.loadFragmentShader(fragmentShaderCode)
        programId = ShaderUtil.loadProgram(listOf(vertexShader, fragmentShader))
    }


    fun onSizeChange(width: Int, height: Int) {
        outWidth = width
        outHeight = height
    }


    companion object {
        /**
         * 默认的顶点位置(基于世界坐标)
         * **/
        val DEFAULT_VERTEX = floatArrayOf(
            -1.0f, 1.0f,  //左上
            -1.0f, -1.0f, //左下
            1.0f, -1.0f,  //右下
            1.0f, 1.0f,   //右上
        )

        /**
         * 默认的纹理位置(基于纹理坐标)
         * **/
        val DEFAULT_TEXTURE = floatArrayOf(
            0.0f, 1.0f, //左上
            0.0f, 0.0f, //左下
            1.0f, 0.0f, //右下
            1.0f, 1.0f, //右上
        )
    }
}