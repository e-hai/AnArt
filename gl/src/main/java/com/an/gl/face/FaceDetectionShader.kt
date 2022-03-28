package com.an.gl.face

import android.content.Context
import android.opengl.GLES31
import com.an.gl.util.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class FaceDetectionShader(context: Context) {

    companion object {
        // number of coordinates per vertex in this array
        private const val COORDS_PER_VERTEX = 2
        private const val FILE_SIMPLE_VERTEX = "face_detection_vertex.glsl"
        private const val FILE_SIMPLE_FRAGMENT = "face_detection_fragment.glsl"
    }

    /**
     * 顶点位置
     * **/
    private val squareCoords = floatArrayOf(
        -0.5f, 0.5f,  //top left
        -0.5f, -0.5f, //bottom left
        0.5f, -0.5f,  //bottom right
        0.5f, 0.5f,   //top right
    )

    // order to draw vertices
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    // Set color with red, green, blue and alpha (opacity) values
    private val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 0.3f)


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
    private var colorHandle: Int = 0
    private val vertexStride: Int = COORDS_PER_VERTEX * 4   // 4 bytes per vertex


    /**
     * 创建并配置纹理，返回纹理对象ID
     * **/
    init {
        //初始化着色器
        val vertexShaderCode = ShaderUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_VERTEX)
        val fragmentShaderCode = ShaderUtil.getShaderCodeFromAssets(context, FILE_SIMPLE_FRAGMENT)
        val vertexShader: Int = ShaderUtil.loadVertexShader(vertexShaderCode)
        val fragmentShader: Int = ShaderUtil.loadFragmentShader(fragmentShaderCode)

        // create empty OpenGL ES Program
        program = ShaderUtil.loadProgram(listOf(vertexShader, fragmentShader))
    }


    fun draw() {
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_SRC_ALPHA, GLES31.GL_ONE_MINUS_SRC_ALPHA)
        // Add program to OpenGL ES environment
        GLES31.glUseProgram(program)

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

        // get handle to fragment shader's vColor member
        colorHandle = GLES31.glGetUniformLocation(program, "vColor").also {
            // Set color for drawing the triangle
            GLES31.glUniform4fv(it, 1, color, 0)
        }

        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,
            drawOrder.size,
            GLES31.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )

        GLES31.glDisableVertexAttribArray(positionHandle)
        GLES31.glDisableVertexAttribArray(colorHandle)
    }

    fun setFacesPoint(face: FloatArray) {
        vertexBuffer.clear()
        vertexBuffer.put(face)
        vertexBuffer.position(0)
    }
}