package com.an.gl.video

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.view.Surface
import com.an.gl.base.BitmapShader
import com.an.gl.util.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class LogoTexturesShader(context: Context) : BitmapShader() {

    companion object {
        // number of coordinates per vertex in this array
        private const val COORDS_PER_VERTEX = 3
        private const val COORDS_PER_TEXTURE = 4
        private const val FILE_SIMPLE_VERTEX = "bitmap_vertex.glsl"
        private const val FILE_SIMPLE_FRAGMENT = "bitmap_fragment.glsl"
    }


    /**
     * 顶点位置
     * **/
    private val squareCoords = floatArrayOf(
        -1.0f, 1.0f, 0.0f,  //top left
        -1.0f, -1.0f, 0.0f,//bottom left
        1.0f, -1.0f, 0.0f,  //bottom right
        1.0f, 1.0f, 0.0f,  //top right
    )

    /**
     * 纹理位置
     * **/
    private val textureCoords = floatArrayOf(
        0.0f, 1.0f, 0.0f, 1.0f,  //左上
        0.0f, 0.0f, 0.0f, 1.0f,  //左下
        1.0f, 0.0f, 0.0f, 1.0f,  //右下
        1.0f, 1.0f, 0.0f, 1.0f   //右上
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
    val surface: Surface

    /**
     * 创建并配置纹理，返回纹理对象ID
     * **/
    init {
        initTexture()
        surface = Surface(surfaceTexture)

        //初始化着色器
        val vertexShaderCode = ShaderUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_VERTEX)
        val fragmentShaderCode = ShaderUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_FRAGMENT)
        val vertexShader: Int = ShaderUtil.loadVertexShader(vertexShaderCode)
        val fragmentShader: Int = ShaderUtil.loadFragmentShader(fragmentShaderCode)

        // create empty OpenGL ES Program
        program = ShaderUtil.loadProgram(listOf(vertexShader, fragmentShader))
    }


    override fun draw() {
        super.draw()
        // Add program to OpenGL ES environment
        GLES31.glUseProgram(program)

        val textureParamHandle = GLES31.glGetUniformLocation(program, "texture")
        val textureTransformHandle = GLES31.glGetUniformLocation(program, "textureTransform")

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
                0,
                vertexBuffer
            )
        }
        texCoorHandle = GLES31.glGetAttribLocation(program, "vTexCoordinate").also {
            // Enable a handle to the triangle vertices
            GLES31.glEnableVertexAttribArray(it)
            // Prepare the triangle coordinate data
            GLES31.glVertexAttribPointer(
                it,
                COORDS_PER_TEXTURE,
                GLES31.GL_FLOAT,
                false,
                0,
                textureBuffer
            )
        }

        //指定一个当前的textureParamHandle对象为一个全局的uniform 变量
        GLES31.glUniform1i(textureParamHandle, 0)
        GLES31.glUniformMatrix4fv(textureTransformHandle, 1, false, transformMatrix, 0)

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