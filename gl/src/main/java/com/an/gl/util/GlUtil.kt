package com.an.gl.util

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES31
import android.util.Log
import java.nio.IntBuffer


object GlUtil {

    private const val TAG = "Shader"

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
            }
        }
    }

    fun loadVertexShader(shaderCode: String): Int {
        return loadShader(GLES31.GL_VERTEX_SHADER, shaderCode)
    }

    fun loadFragmentShader(shaderCode: String): Int {
        return loadShader(GLES31.GL_FRAGMENT_SHADER, shaderCode)
    }

    /**
     * 创建OpenGL ES程序对象，并把着色器对象关联起来，并返回程序对象ID
     * **/
    fun loadProgram(shaders: List<Int>): Int {
        return GLES31.glCreateProgram().also { program ->

            // add the shader to program
            shaders.forEach {
                GLES31.glAttachShader(program, it)
            }

            // creates OpenGL ES program executables
            GLES31.glLinkProgram(program)
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
}