package com.an.gl.sample

import android.opengl.GLES31
import com.an.gl.util.GlUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


class Triangle(vertexShaderCode: String, fragmentShaderCode: String) {

    // number of coordinates per vertex in this array
    private val COORDS_PER_VERTEX = 3

    private var triangleCoords = floatArrayOf(     // in counterclockwise order:
        0.0f, 0.622008459f, 0.0f,      // top
        -0.5f, -0.311004243f, 0.0f,    // bottom left
        0.5f, -0.311004243f, 0.0f      // bottom right
    )

    // Set color with red, green, blue and alpha (opacity) values
    private val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    private var vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(triangleCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(triangleCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private var mProgram: Int
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0
    private val vertexCount: Int = triangleCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4   // 4 bytes per vertex

    init {
        val vertexShader: Int = GlUtil.loadVertexShader(vertexShaderCode)
        val fragmentShader: Int = GlUtil.loadFragmentShader(fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GlUtil.loadProgram(listOf(vertexShader, fragmentShader))
    }



    fun draw() {
        // Add program to OpenGL ES environment
        GLES31.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES31.glGetAttribLocation(mProgram, "vPosition").also {
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

            // get handle to fragment shader's vColor member
            mColorHandle = GLES31.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
                // Set color for drawing the triangle
                GLES31.glUniform4fv(colorHandle, 1, color, 0)
            }
            // Draw the triangle
            GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, vertexCount)
            // Disable vertex array
            GLES31.glDisableVertexAttribArray(it)
        }
    }
}
