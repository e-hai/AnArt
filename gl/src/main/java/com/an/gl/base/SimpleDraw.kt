package com.an.gl.base

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class SimpleDraw : Draw {

    private lateinit var mvpMatrixBuffer: FloatBuffer    //总变换矩阵
    private lateinit var vertexCoordBuffer: FloatBuffer  //顶点坐标
    private lateinit var textureCoordBuffer: FloatBuffer //纹理坐标
    private lateinit var drawOrderBuffer: ShortBuffer    //绘制路径

    override fun initCoordinateData() {
        val defaultMvpMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, //屏幕左上
            0f, 1f, 0f, 0f, //屏幕左下
            0f, 0f, 1f, 0f, //屏幕右下
            0f, 0f, 0f, 1f, //屏幕右上
        )

        val defaultVertex = floatArrayOf(
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

    override fun getVertexShadeCode(): String {
        TODO("Not yet implemented")
    }

    override fun getFragmentShadeCode(): String {
        TODO("Not yet implemented")
    }

    override fun initShadeProgram() {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }

    override fun onSizeChange(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onDraw() {
        TODO("Not yet implemented")
    }
}