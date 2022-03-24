package com.an.gl.face

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES31
import com.an.gl.util.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 简单的纹理渲染
 * **/
class SimpleTexturesShader(context: Context) {

    // number of coordinates per vertex in this array
    private val COORDS_PER_VERTEX = 2

    /**
     * 顶点位置
     * **/
    private val squareCoords = floatArrayOf(
        -1.0f, 1.0f,  //top left
        -1.0f, -1.0f, //bottom left
        1.0f, -1.0f,  //bottom right
        1.0f, 1.0f,   //top right
    )

    /**
     * 纹理位置
     * **/
    private val textureCoords = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 0.0f
    )

    // order to draw vertices
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    private var vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(squareCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(squareCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private var textureBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(textureCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(textureCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private var drawOrderBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }


    private var program: Int
    private var positionHandle: Int = 0
    private var texCoorHandle: Int = 0
    private val vertexStride: Int = COORDS_PER_VERTEX * 4   // 4 bytes per vertex
    private val textureId = 1
    val surfaceTexture: SurfaceTexture


    /**
     * 创建并配置纹理，返回纹理对象ID
     * **/
    init {
        //生成一个纹理
        GLES31.glGenTextures(1, intArrayOf(textureId), 0)
        //将此纹理绑定到外部纹理上
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
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

        //以纹理对象ID创建SurfaceTexture
        surfaceTexture = SurfaceTexture(textureId)

        //初始化着色器
        val vertexShaderCode = ShaderUtil.getShaderCodeFromAssets(
            context,
            "vertex_shader_code.glsl"
        )
        val fragmentShaderCode = ShaderUtil.getShaderCodeFromAssets(
            context,
            "fragment_shader_code.glsl"
        )
        val vertexShader: Int = ShaderUtil.loadVertexShader(vertexShaderCode)
        val fragmentShader: Int = ShaderUtil.loadFragmentShader(fragmentShaderCode)

        // create empty OpenGL ES Program
        program = ShaderUtil.loadProgram(listOf(vertexShader, fragmentShader))
    }


    fun draw() {
        surfaceTexture.updateTexImage()

        // Add program to OpenGL ES environment
        GLES31.glUseProgram(program)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)


        // get handle to vertex shader's vPosition member
        positionHandle = GLES31.glGetAttribLocation(program, "vPosition").also {
            // Enable a handle to the triangle vertices
            GLES31.glEnableVertexAttribArray(it)
            // Prepare the triangle coordinate data
            GLES31.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES31.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )
        }
        texCoorHandle = GLES31.glGetAttribLocation(program, "inTexCoor").also {
            // Enable a handle to the triangle vertices
            GLES31.glEnableVertexAttribArray(it)
            // Prepare the triangle coordinate data
            GLES31.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES31.GL_FLOAT,
                false,
                vertexStride,
                textureBuffer
            )
        }

        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,
            drawOrder.size,
            GLES31.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )

        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(texCoorHandle)
    }
}