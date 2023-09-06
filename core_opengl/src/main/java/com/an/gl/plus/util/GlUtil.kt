package com.an.gl.plus.util

import android.content.Context
import android.opengl.GLES31
import android.util.Log
import java.nio.*
import javax.microedition.khronos.opengles.GL10


object GlUtil {

    private const val TAG = "GlUtil"


    fun getShaderCodeFromAssets(context: Context, assetFileName: String): String {
        val code = StringBuilder()
        context.assets.open(assetFileName)
            .bufferedReader()
            .readLines()
            .forEach {
                code.append(it).appendLine()
            }
        return code.toString().trim().apply {
            Log.d(TAG, this)
        }
    }

    /**
     * 编译OpenGL着色语言(GLSL)代码，并返回着色器对象的ID
     * **/
    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES31.glCreateShader(type).also { shader ->
            Log.d(TAG, "shader id=$shader")
            // add the source code to the shader and compile it
            GLES31.glShaderSource(shader, shaderCode)
            GLES31.glCompileShader(shader)
            //检查shader编译状态
            val compileStatus = IntBuffer.allocate(1)
            GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] == GLES31.GL_TRUE) {
                Log.d(TAG, "compile success")
            } else {
                Log.d(TAG, "compile fail: ${GLES31.glGetShaderInfoLog(shader)}")
                GLES31.glDeleteShader(shader)
            }
        }
    }

    fun loadVertexShader(shaderCode: String): Int {
        Log.d(TAG, shaderCode)
        return loadShader(GLES31.GL_VERTEX_SHADER, shaderCode)
    }

    fun loadFragmentShader(shaderCode: String): Int {
        Log.d(TAG, shaderCode)
        return loadShader(GLES31.GL_FRAGMENT_SHADER, shaderCode)
    }

    /**
     * 创建OpenGL ES程序对象，并把着色器对象关联起来，并返回程序对象ID
     * **/
    fun loadProgram(shaders: List<Int>): Int {
        return GLES31.glCreateProgram().also { program ->
            if (program == GLES31.GL_FALSE) {
                Log.d(TAG, "create program fail")
                return@also
            }
            // add the shader to program
            shaders.forEach {
                GLES31.glAttachShader(program, it)
                checkGlError("glAttachShader")
            }

            // creates OpenGL ES program executables
            GLES31.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == GLES31.GL_FALSE) {
                Log.d(TAG, "link program fail：${GLES31.glGetProgramInfoLog(program)}")
                GLES31.glDeleteProgram(program)
            }
        }
    }


    /**
     * Checks to see if a GLES error has been raised.
     */
    fun checkGlError(op: String) {
        val error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            Log.e(TAG, msg)
            throw RuntimeException(msg)
        }
    }

    fun checkTextureMaxSize() {
        val maxTextureSize = IntArray(1)
        GLES31.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
        Log.i(TAG, "Max texture size = " + maxTextureSize[0])
    }

    fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(array.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())
            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(array)
                // set the buffer to read the first coordinate
                position(0)
            }
        }
    }

    fun createShortBuffer(array: ShortArray): ShortBuffer {
        return ByteBuffer.allocateDirect(array.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())
            // create a floating point buffer from the ByteBuffer
            asShortBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(array)
                // set the buffer to read the first coordinate
                position(0)
            }
        }
    }
}